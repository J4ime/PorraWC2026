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
        val matches = fetchEspn()
        if (matches.isNotEmpty()) {
            Log.d(TAG, "ESPN: ${matches.size} friendlies found")
            return matches.take(5)
        }
        val livescore = fetchLiveScore()
        if (livescore.isNotEmpty()) {
            Log.d(TAG, "LiveScore: ${livescore.size} friendlies found")
            return livescore.take(5)
        }
        Log.d(TAG, "No friendlies found from any source")
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
            val obj = JSONObject(json)
            val events = obj.optJSONArray("events") ?: return emptyList()
            val matches = mutableListOf<ScrapedMatch>()
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                val comps = event.optJSONArray("competitions") ?: continue
                val comp = comps.getJSONObject(0)
                val compName = comp.optString("displayName", "")
                if (compName.isBlank()) continue
                val competitors = comp.optJSONArray("competitors") ?: continue
                if (competitors.length() < 2) continue
                val home = competitors.getJSONObject(0)
                val away = competitors.getJSONObject(1)
                val homeTeam = home.optJSONObject("team")?.optString("displayName") ?: home.optString("homeAway", "")
                val awayTeam = away.optJSONObject("team")?.optString("displayName") ?: away.optString("homeAway", "")
                val homeScore = if (home.has("score")) home.optString("score") else null
                val awayScore = if (away.has("score")) away.optString("score") else null
                val hg = homeScore?.toIntOrNull()
                val ag = awayScore?.toIntOrNull()
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

    private fun fetchLiveScore(): List<ScrapedMatch> {
        try {
            val url = URL("https://www.livescore.com/en/football/international-friendlies/friendlies/")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            conn.connectTimeout = 15000; conn.readTimeout = 15000
            val html = conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
            return parseLiveScoreHtml(html)
        } catch (e: Exception) {
            Log.d(TAG, "LiveScore HTML failed: ${e.message}")
            return emptyList()
        }
    }

    private fun parseLiveScoreHtml(html: String): List<ScrapedMatch> {
        val matches = mutableListOf<ScrapedMatch>()
        val jsonBlocks = Regex("""window\.__INITIAL_STATE__\s*=\s*(\{.*?\})\s*;""", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(html).toList()
        if (jsonBlocks.isEmpty()) {
            val jsonScripts = Regex("""<script[^>]*type="application/json"[^>]*>(.*?)</script>""", setOf(RegexOption.DOT_MATCHES_ALL)).findAll(html).toList()
            for (script in jsonScripts) {
                try {
                    val obj = JSONObject(script.groupValues[1])
                    findMatchesInJson(obj, matches)
                } catch (_: Exception) {}
            }
        }
        for (block in jsonBlocks) {
            try {
                val obj = JSONObject(block.groupValues[1])
                findMatchesInJson(obj, matches)
            } catch (_: Exception) {}
        }
        return matches.distinctBy { "${it.homeTeam}${it.awayTeam}" }
    }

    private fun findMatchesInJson(obj: JSONObject, matches: MutableList<ScrapedMatch>, depth: Int = 0) {
        if (depth > 10 || matches.size >= 10) return
        if (obj.has("Hn") && obj.has("An")) {
            matches.add(ScrapedMatch(
                obj.optString("Hn"), obj.optString("An"),
                obj.optString("Esd", ""), obj.optString("Eps", "TIMED"),
                if (obj.has("Hs")) obj.optInt("Hs", -1).takeIf { it >= 0 } else null,
                if (obj.has("As")) obj.optInt("As", -1).takeIf { it >= 0 } else null
            ))
        }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            try { obj.optJSONObject(key)?.let { findMatchesInJson(it, matches, depth + 1) } } catch (_: Exception) {}
            try { obj.optJSONArray(key)?.let { for (i in 0 until it.length()) { try { findMatchesInJson(it.getJSONObject(i), matches, depth + 1) } catch (_: Exception) {} } } } catch (_: Exception) {}
        }
    }
}
