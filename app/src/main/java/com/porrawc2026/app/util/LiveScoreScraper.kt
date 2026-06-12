package com.porrawc2026.app.util

import android.util.Log
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
    val liveMinute: Int = 0,
    val tournamentName: String = ""
)

data class GoalDetail(val playerName: String, val minute: Int)

object LiveScoreScraper {

    private const val TAG = "LiveScoreScraper"

    fun fetchMatches(): List<ScrapedMatch> {
        return fetchWithFilter { match ->
            val wcTeams = com.porrawc2026.app.ui.screens.home.HomeViewModel.WC_TEAMS
            (wcTeams.any { match.homeTeam.contains(it, true) || it.contains(match.homeTeam, true) } ||
             wcTeams.any { match.awayTeam.contains(it, true) || it.contains(match.awayTeam, true) }) &&
            isSeniorNational(match.homeTeam) && isSeniorNational(match.awayTeam)
        }
    }

    fun fetchWcMatches(): List<ScrapedMatch> {
        return fetchWithFilter { match ->
            val tn = match.tournamentName
            tn.contains("World Cup", true) || tn.contains("FIFA", true) ||
            tn.contains("Mundial", true) || tn.contains("WM", true)
        }
    }

    private fun fetchWithFilter(filter: (ScrapedMatch) -> Boolean): List<ScrapedMatch> {
        for (source in listOf(::fetchLiveScoreCom, ::fetchSofaScore, ::fetchFotmob)) {
            try {
                val m = source()
                if (m.isNotEmpty()) {
                    val filtered = m.filter(filter)
                    val sorted = filtered.sortedByDescending { it.utcDate }
                    val last = sorted.sortedBy { it.utcDate }
                    Log.d(TAG, "Found ${m.size} matches, ${filtered.size} after filter, showing ${last.size}")
                    return last
                }
            } catch (_: Exception) {}
        }
        Log.d(TAG, "No matches from any source")
        return emptyList()
    }

    private fun isSeniorNational(name: String): Boolean {
        val youthPatterns = listOf("U23", "U22", "U21", "U20", "U19", "U18", "U17", "U16",
            "Olympic", "Women", "WU23", "WU20", "WU19", "WU17", "Women's")
        return youthPatterns.none { name.contains(it, true) }
    }

    fun fetchGoalDetails(eventId: Long): Pair<List<GoalDetail>, List<GoalDetail>> {
        // Try livescore.com first
        val lscGoals = fetchLiveScoreComGoalDetails(eventId)
        if (lscGoals.first.isNotEmpty() || lscGoals.second.isNotEmpty()) return lscGoals
        // Fallback to Sofascore
        return fetchSofaScoreGoalDetails(eventId)
    }

    private fun fetchLiveScoreComGoalDetails(eventId: Long): Pair<List<GoalDetail>, List<GoalDetail>> {
        val home = mutableListOf<GoalDetail>()
        val away = mutableListOf<GoalDetail>()
        try {
            val urlStr = "https://www.livescore.com/es/futbol/internacional/world-cup-2026/n-a/$eventId/"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            val html = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
            val idx = html.indexOf("__NEXT_DATA__")
            if (idx < 0) return Pair(home, away)
            val jsonStr = html.substring(idx + 48).substringBefore("</script>")
            val root = JSONObject(jsonStr)
            val incs = root.optJSONObject("props")?.optJSONObject("pageProps")
                ?.optJSONObject("initialEventData")?.optJSONObject("event")
                ?.optJSONObject("incidents")?.optJSONObject("incs")
                ?.optJSONObject("football1") ?: return Pair(home, away)
            val keys = incs.keys()
            while (keys.hasNext()) {
                val minuteKey = keys.next()
                val minuteArr = incs.optJSONArray(minuteKey) ?: continue
                for (i in 0 until minuteArr.length()) {
                    val entry = minuteArr.getJSONObject(i)
                    val homeArr = entry.optJSONArray("HOME")
                    val awayArr = entry.optJSONArray("AWAY")
                    if (homeArr != null) {
                        for (j in 0 until homeArr.length()) {
                            val inc = homeArr.getJSONObject(j)
                            if (inc.optString("type") == "FootballGoal") {
                                val name = inc.optString("shortName", inc.optString("name", ""))
                                val time = inc.optString("time", "0").replace("'", "").toIntOrNull() ?: 0
                                home.add(GoalDetail(name, time))
                            }
                        }
                    }
                    if (awayArr != null) {
                        for (j in 0 until awayArr.length()) {
                            val inc = awayArr.getJSONObject(j)
                            if (inc.optString("type") == "FootballGoal") {
                                val name = inc.optString("shortName", inc.optString("name", ""))
                                val time = inc.optString("time", "0").replace("'", "").toIntOrNull() ?: 0
                                away.add(GoalDetail(name, time))
                            }
                        }
                    }
                }
            }
            home.reverse(); away.reverse()
            if (home.isNotEmpty() || away.isNotEmpty()) Log.d(TAG, "LSC goals for $eventId: H=$home A=$away")
        } catch (e: Exception) { Log.d(TAG, "LSC goals failed: ${e.message}") }
        return Pair(home, away)
    }

    private fun fetchSofaScoreGoalDetails(eventId: Long): Pair<List<GoalDetail>, List<GoalDetail>> {
        val home = mutableListOf<GoalDetail>()
        val away = mutableListOf<GoalDetail>()
        try {
            val url = URL("https://api.sofascore.com/api/v1/event/$eventId/incidents")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            conn.connectTimeout = 8000; conn.readTimeout = 8000
            val json = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
            val root = JSONObject(json)
            val incidents = root.optJSONArray("incidents")
            if (incidents == null) {
                val event = root.optJSONObject("event")
                if (event != null) {
                    val eventIncidents = event.optJSONArray("incidents")
                    if (eventIncidents != null) parseGoals(eventIncidents, home, away)
                }
            } else {
                parseGoals(incidents, home, away)
            }
            Log.w(TAG, "Goal details for $eventId: H=${home} A=${away}")
            return Pair(home, away)
        } catch (e: Exception) {
            Log.w(TAG, "Goal details failed for $eventId: ${e.message}")
            return Pair(emptyList(), emptyList())
        }
    }

    private fun parseGoals(incidents: org.json.JSONArray, home: MutableList<GoalDetail>, away: MutableList<GoalDetail>) {
        for (i in 0 until incidents.length()) {
            val inc = incidents.getJSONObject(i)
            val type = inc.optString("incidentType", "")
            if (type != "goal") continue
            val player = inc.optJSONObject("player")?.optString("name") ?: continue
            var min = inc.optInt("time", 0)
            if (inc.has("addedTime")) min += inc.optInt("addedTime", 0)
            val isHome = inc.optBoolean("isHome", false)
            val detail = GoalDetail(player, min.coerceAtLeast(1))
            if (isHome) home.add(detail) else away.add(detail)
        }
        home.reverse()
        away.reverse()
    }

    private fun fetchLiveScoreCom(): List<ScrapedMatch> {
        try {
            val url = URL("https://www.livescore.com/es/futbol/internacional/world-cup-2026/fixtures/")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            conn.connectTimeout = 10000; conn.readTimeout = 10000
            val html = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
            val jsonStr = html.substringAfter("__NEXT_DATA__\" type=\"application/json\">").substringBefore("</script>")
            if (jsonStr.isBlank()) return emptyList()
            val root = JSONObject(jsonStr)
            val events = root.optJSONObject("props")?.optJSONObject("pageProps")
                ?.optJSONObject("initialStageData")?.optJSONArray("events")
                ?: return emptyList()
            val matches = mutableListOf<ScrapedMatch>()
            val now = System.currentTimeMillis() / 1000
            for (i in 0 until events.length()) {
                val e = events.getJSONObject(i)
                val status = e.optString("eventStatus", "TIMED")
                val homeName = e.optString("homeTeamNameEn", e.optString("homeTeamName", ""))
                val awayName = e.optString("awayTeamNameEn", e.optString("awayTeamName", ""))
                if (homeName.length < 3 || awayName.length < 3) continue
                val homeScore = e.optString("homeTeamScore", "").let { if (it.isNotBlank()) it.toIntOrNull() else null }
                val awayScore = e.optString("awayTeamScore", "").let { if (it.isNotBlank()) it.toIntOrNull() else null }
                val liveTime = e.optString("status", "")
                val startStr = e.optString("startDateTimeString", "")
                val utc = if (startStr.length >= 12) {
                    val y = startStr.substring(0, 4); val m = startStr.substring(4, 6); val d = startStr.substring(6, 8)
                    val h = startStr.substring(8, 10); val min = startStr.substring(10, 12)
                    "$y-${m}-${d}T$h:${min}:00Z"
                } else ""
                val eventId = e.optString("id", "0").toLongOrNull() ?: 0L
                val statusStr = when (status) {
                    "LIVE", "HALFTIME" -> "IN_PLAY"
                    "FINISHED" -> "FINISHED"
                    else -> "TIMED"
                }
                val liveMin = if (statusStr == "IN_PLAY") {
                    liveTime.replace("'", "").replace("+", "").toIntOrNull() ?: 0
                } else 0
                matches.add(ScrapedMatch(homeName, awayName, utc, statusStr, homeScore, awayScore, eventId = eventId, liveMinute = liveMin, tournamentName = "World Cup"))
            }
            Log.d(TAG, "LiveScoreCom: ${matches.size} matches")
            return matches
        } catch (e: Exception) {
            Log.d(TAG, "LiveScoreCom failed: ${e.message}")
            return emptyList()
        }
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
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UPB1.230918.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.260 Mobile Safari/537.36")
                conn.setRequestProperty("Accept", "application/json, text/plain, */*")
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                conn.setRequestProperty("Referer", "https://www.sofascore.com/")
                conn.setRequestProperty("Origin", "https://www.sofascore.com")
                conn.setRequestProperty("Sec-Fetch-Site", "same-site")
                conn.connectTimeout = 15000; conn.readTimeout = 15000
                val code = conn.responseCode
                if (code != 200) {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    Log.d(TAG, "SofaScore $urlStr HTTP $code: ${err.take(200)}")
                    conn.disconnect()
                    continue
                }
                val json = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
                Log.d(TAG, "SofaScore $urlStr: got ${json.length} bytes")
                val obj = JSONObject(json)
                val events = obj.optJSONArray("events") ?: continue
                val now = System.currentTimeMillis() / 1000
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
                    val tournament = e.optJSONObject("tournament")?.optString("name", "") ?: ""
                    val liveMin = if (statusStr == "IN_PLAY") {
                        val st = e.optJSONObject("statusTime")
                        val stTs = st?.optInt("timestamp", 0) ?: 0
                        val stInit = st?.optInt("initial", 0) ?: 0
                        if (stTs > 0) ((now - stTs + stInit) / 60).toInt().coerceAtLeast(1) else 0
                    } else 0
                    matches.add(ScrapedMatch(home, away, utc, statusStr, hg, ag, eventId = eId, liveMinute = liveMin, tournamentName = tournament))
                }
                Log.d(TAG, "SofaScore: ${matches.size} national matches from $urlStr")
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
        for (urlStr in listOf("https://www.fotmob.com/api/matches?date=$dates")) {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
                conn.setRequestProperty("Accept", "application/json, */*")
                conn.connectTimeout = 10000; conn.readTimeout = 10000
                val json = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
                val obj = JSONObject(json)
                val leagues = obj.optJSONArray("leagues") ?: continue
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
                        val liveMin = if (started) match.optJSONObject("status")?.optInt("liveTime", 0) ?: 0 else 0
                        m.add(ScrapedMatch(h, a, utc, st, hg, ag, liveMinute = liveMin))
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
                val clock = e.optJSONObject("status")?.optInt("displayClock", 0) ?: 0
                val liveMin = if (st.contains("Half", true) || st.contains("'", true)) clock else 0
                m.add(ScrapedMatch(h, a, utc, st, hg, ag, liveMinute = liveMin))
            }
            return m
        } catch (_: Exception) { return emptyList() }
    }
}
