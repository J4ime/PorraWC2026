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
    val awayGoals: Int?
)

object LiveScoreScraper {

    private const val TAG = "LiveScoreScraper"

    fun fetchMatches(): List<ScrapedMatch> {
        val fotmob = fetchFotmob()
        if (fotmob.isNotEmpty()) { Log.d(TAG, "FotMob: ${fotmob.size} matches"); return fotmob.take(5) }
        val espn = fetchEspn()
        if (espn.isNotEmpty()) { Log.d(TAG, "ESPN: ${espn.size} matches"); return espn.take(5) }
        Log.d(TAG, "No matches from any source")
        return emptyList()
    }

    private fun fetchFotmob(): List<ScrapedMatch> {
        try {
            val dateFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
            dateFmt.timeZone = TimeZone.getTimeZone("UTC")
            val dates = dateFmt.format(java.util.Date())
            val urls = listOf(
                "https://www.fotmob.com/api/matches?date=$dates",
                "https://www.fotmob.com/api/matches?date=$dates&tz=Europe%2FMadrid",
                "https://www.fotmob.com/api/matches?date=${dates}T23:59:59.999Z"
            )
            for (urlStr in urls) {
                val matches = tryFetchFotmob(urlStr)
                if (matches.isNotEmpty()) return matches
            }
            return emptyList()
        } catch (e: Exception) {
            Log.d(TAG, "FotMob failed: ${e.message}")
            return emptyList()
        }
    }

    private fun tryFetchFotmob(urlStr: String): List<ScrapedMatch> {
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,es;q=0.8")
            conn.setRequestProperty("Referer", "https://www.fotmob.com/")
            conn.connectTimeout = 15000; conn.readTimeout = 15000
            val json = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
            val obj = JSONObject(json)
            val leagues = obj.optJSONArray("leagues") ?: return emptyList()
            val matches = mutableListOf<ScrapedMatch>()
            for (i in 0 until leagues.length()) {
                val league = leagues.getJSONObject(i)
                val leagueMatches = league.optJSONArray("matches") ?: continue
                for (j in 0 until leagueMatches.length()) {
                    val m = leagueMatches.getJSONObject(j)
                    val home = m.optJSONObject("home")?.optString("name") ?: ""
                    val away = m.optJSONObject("away")?.optString("name") ?: ""
                    if (home.length < 3 || away.length < 3) continue
                    val statusObj = m.optJSONObject("status")
                    val finished = statusObj?.optBoolean("finished", false) ?: false
                    val started = statusObj?.optBoolean("started", false) ?: false
                    val utc = statusObj?.optString("utcTime", "") ?: ""
                    val statusStr = when { finished -> "FINISHED"; started -> "IN_PLAY"; else -> "TIMED" }
                    val hg = m.optJSONObject("home")?.optInt("score", -1)?.takeIf { it >= 0 && started }
                    val ag = m.optJSONObject("away")?.optInt("score", -1)?.takeIf { it >= 0 && started }
                    matches.add(ScrapedMatch(home, away, utc, statusStr, hg, ag))
                }
            }
            return matches
        } catch (e: Exception) {
            Log.d(TAG, "FotMob URL $urlStr failed: ${e.message}")
            return emptyList()
        }
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
            val obj = JSONObject(json)
            val events = obj.optJSONArray("events") ?: return emptyList()
            val matches = mutableListOf<ScrapedMatch>()
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                val comps = event.optJSONArray("competitions") ?: continue
                val comp = comps.getJSONObject(0)
                val competitors = comp.optJSONArray("competitors") ?: continue
                if (competitors.length() < 2) continue
                val home = competitors.getJSONObject(0)
                val away = competitors.getJSONObject(1)
                val homeTeam = home.optJSONObject("team")?.optString("displayName") ?: ""
                val awayTeam = away.optJSONObject("team")?.optString("displayName") ?: ""
                val hg = if (home.has("score")) home.optString("score")?.toIntOrNull() else null
                val ag = if (away.has("score")) away.optString("score")?.toIntOrNull() else null
                val status = event.optJSONObject("status")?.optJSONObject("type")?.optString("description", "TIMED") ?: "TIMED"
                val utc = event.optString("date", "")
                if (homeTeam.isNotBlank() && awayTeam.isNotBlank() && homeTeam.length > 2 && awayTeam.length > 2) {
                    matches.add(ScrapedMatch(homeTeam, awayTeam, utc, status, hg, ag))
                }
            }
            return matches
        } catch (e: Exception) {
            Log.d(TAG, "ESPN failed: ${e.message}")
            return emptyList()
        }
    }
}
