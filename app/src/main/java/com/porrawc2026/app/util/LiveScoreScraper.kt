package com.porrawc2026.app.util

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ScrapedMatch(
    val homeTeam: String,
    val awayTeam: String,
    val utcDate: String,
    val status: String,
    val homeGoals: Int?,
    val awayGoals: Int?,
    val eventId: Long = 0,
    val homeScorers: List<GoalDetail> = emptyList(),
    val awayScorers: List<GoalDetail> = emptyList()
)

data class GoalDetail(val playerName: String, val minute: Int)

object LiveScoreScraper {

    private const val TAG = "LiveScoreScraper"

    fun fetchMatches(): List<ScrapedMatch> {
        for (source in listOf(::fetchSofaScore, ::fetchFotmob, ::fetchEspn)) {
            try {
                val m = source()
                if (m.isNotEmpty()) {
                    val sorted = m.sortedByDescending { it.utcDate }
                    val last5 = sorted.take(5).sortedBy { it.utcDate }
                    Log.d(TAG, "Found ${m.size} matches via ${source.name}, showing last ${last5.size}")
                    return last5
                }
            } catch (_: Exception) {}
        }
        Log.d(TAG, "No matches from any source")
        return emptyList()
    }

    fun fetchGoalDetails(eventId: Long): Pair<List<GoalDetail>, List<GoalDetail>> {
        try {
            val url = URL("https://api.sofascore.com/api/v1/event/$eventId")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            val json = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
            val event = JSONObject(json).optJSONObject("event") ?: return Pair(emptyList(), emptyList())
            val incidents = event.optJSONArray("incidents") ?: return Pair(emptyList(), emptyList())
            val home = mutableListOf<GoalDetail>()
            val away = mutableListOf<GoalDetail>()
            for (i in 0 until incidents.length()) {
                val inc = incidents.getJSONObject(i)
                val type = inc.optString("incidentType", "")
                if (type != "goal") continue
                val player = inc.optJSONObject("player")?.optString("name") ?: continue
                val minute = inc.optInt("time", inc.optInt("addedTime", 0) + (inc.optInt("time", 0)))
                val isHome = inc.optString("scoringTeam", "") == "home" ||
                    inc.optString("isHome", "") == "true"
                val detail = GoalDetail(player, minute.coerceAtLeast(0))
                if (isHome) home.add(detail) else away.add(detail)
            }
            return Pair(home, away)
        } catch (_: Exception) { return Pair(emptyList(), emptyList()) }
    }

    private fun fetchSofaScore(): List<ScrapedMatch> {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        dateFmt.timeZone = TimeZone.getTimeZone("UTC")
        val date = dateFmt.format(java.util.Date())
        val urls = listOf(
            "https://api.sofascore.com/api/v1/sport/football/scheduled-events/$date",
            "https://api.sofascore.com/api/v1/sport/football/events/live"
        )
        for (urlStr in urls) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                conn.setRequestProperty("Accept", "*/*")
                conn.connectTimeout = 12000; conn.readTimeout = 12000
                val json = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
                val obj = JSONObject(json)
                val events = obj.optJSONArray("events") ?: continue
                val matches = mutableListOf<ScrapedMatch>()
                for (i in 0 until events.length()) {
                    val e = events.getJSONObject(i)
                    val homeTeam = e.optJSONObject("homeTeam") ?: continue
                    val awayTeam = e.optJSONObject("awayTeam") ?: continue
                    val home = homeTeam.optString("name", "")
                    val away = awayTeam.optString("name", "")
                    if (home.length < 3 || away.length < 3) continue
                    val isHomeNational = homeTeam.optBoolean("national", false)
                    val isAwayNational = awayTeam.optBoolean("national", false)
                    if (!isHomeNational || !isAwayNational) continue
                    val statusObj = e.optJSONObject("status") ?: continue
                    val statusCode = statusObj.optString("type", "")
                    val statusStr = when {
                        statusCode == "finished" || statusCode == "after_pen" || statusCode == "after_et" -> "FINISHED"
                        statusCode == "inprogress" || statusCode == "halftime" -> "IN_PLAY"
                        else -> "TIMED"
                    }
                    val homeScore = e.optJSONObject("homeScore")
                    val awayScore = e.optJSONObject("awayScore")
                    val hg = homeScore?.optInt("current", -1)?.takeIf { it >= 0 && statusStr != "TIMED" }
                    val ag = awayScore?.optInt("current", -1)?.takeIf { it >= 0 && statusStr != "TIMED" }
                    val ts = e.optLong("startTimestamp", 0)
                    val utc = if (ts > 0) {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(java.util.Date(ts * 1000))
                    } else ""
                    val eId = e.optLong("id", 0)
                    val isWcTeam = com.porrawc2026.app.ui.screens.home.HomeViewModel.WC_TEAMS.any {
                        home.contains(it, true) || it.contains(home, true)
                    } || com.porrawc2026.app.ui.screens.home.HomeViewModel.WC_TEAMS.any {
                        away.contains(it, true) || it.contains(away, true)
                    }
                    if (!isWcTeam) continue
                    matches.add(ScrapedMatch(home, away, utc, statusStr, hg, ag, eventId = eId))
                }
                Log.d(TAG, "SofaScore: ${matches.size} national team matches from $urlStr")
                if (matches.isNotEmpty()) return matches
            } catch (e: Exception) {
                Log.d(TAG, "SofaScore $urlStr failed: ${e.message}")
            }
        }
        return emptyList()
    }

    private fun fetchFotmob(): List<ScrapedMatch> {
        val dateFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        dateFmt.timeZone = TimeZone.getTimeZone("UTC")
        val dates = dateFmt.format(java.util.Date())
        for (urlStr in listOf(
            "https://www.fotmob.com/api/matches?date=$dates",
            "https://www.fotmob.com/api/matches?date=$dates&tz=Europe%2FMadrid"
        )) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
                conn.setRequestProperty("Accept", "application/json, */*")
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                conn.setRequestProperty("Referer", "https://www.fotmob.com/")
                conn.connectTimeout = 10000; conn.readTimeout = 10000
                val json = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
                val obj = JSONObject(json)
                val leagues = obj.optJSONArray("leagues") ?: return emptyList()
                val m = mutableListOf<ScrapedMatch>()
                for (i in 0 until leagues.length()) {
                    val lm = leagues.getJSONObject(i).optJSONArray("matches") ?: continue
                    for (j in 0 until lm.length()) {
                        val match = lm.getJSONObject(j)
                        val h = match.optJSONObject("home")?.optString("name") ?: ""
                        val a = match.optJSONObject("away")?.optString("name") ?: ""
                        if (h.length < 3 || a.length < 3) continue
                        val s = match.optJSONObject("status")
                        val finished = s?.optBoolean("finished", false) ?: false
                        val started = s?.optBoolean("started", false) ?: false
                        val utc = s?.optString("utcTime", "") ?: ""
                        val st = when { finished -> "FINISHED"; started -> "IN_PLAY"; else -> "TIMED" }
                        val hg = match.optJSONObject("home")?.optInt("score", -1)?.takeIf { it >= 0 && started }
                        val ag = match.optJSONObject("away")?.optInt("score", -1)?.takeIf { it >= 0 && started }
                        m.add(ScrapedMatch(h, a, utc, st, hg, ag))
                    }
                }
                return m
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    private fun fetchEspn(): List<ScrapedMatch> {
        try {
            val dateFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
            dateFmt.timeZone = TimeZone.getTimeZone("UTC")
            val dates = dateFmt.format(java.util.Date())
            val url = URL("https://site.api.espn.com/apis/site/v2/sports/soccer/scoreboard?dates=$dates&limit=50")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000; conn.readTimeout = 10000
            val json = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
            val events = JSONObject(json).optJSONArray("events") ?: return emptyList()
            val m = mutableListOf<ScrapedMatch>()
            for (i in 0 until events.length()) {
                val e = events.getJSONObject(i)
                val comps = e.optJSONArray("competitions") ?: continue
                val comp = comps.getJSONObject(0)
                val competitors = comp.optJSONArray("competitors") ?: continue
                if (competitors.length() < 2) continue
                val h = competitors.getJSONObject(0).optJSONObject("team")?.optString("displayName") ?: ""
                val a = competitors.getJSONObject(1).optJSONObject("team")?.optString("displayName") ?: ""
                if (h.length < 3 || a.length < 3) continue
                val st = e.optJSONObject("status")?.optJSONObject("type")?.optString("description", "TIMED") ?: "TIMED"
                val hg = if (competitors.getJSONObject(0).has("score")) competitors.getJSONObject(0).optString("score")?.toIntOrNull() else null
                val ag = if (competitors.getJSONObject(1).has("score")) competitors.getJSONObject(1).optString("score")?.toIntOrNull() else null
                val utc = e.optString("date", "")
                m.add(ScrapedMatch(h, a, utc, st, hg, ag))
            }
            return m
        } catch (_: Exception) { return emptyList() }
    }
}
