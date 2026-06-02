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
    private const val URL_LIVESCORE = "https://www.livescore.com/en/football/international-friendlies/friendlies/"

    private val nationalTeams = setOf(
        "Croatia", "Belgium", "France", "Germany", "Spain", "England", "Italy", "Portugal",
        "Netherlands", "Argentina", "Brazil", "Uruguay", "Mexico", "Sweden", "Norway", "Denmark",
        "Poland", "Switzerland", "Austria", "Czechia", "Turkey", "Scotland", "Wales", "Ukraine",
        "Serbia", "Japan", "South Korea", "Australia", "USA", "Canada", "Morocco", "Senegal",
        "Egypt", "Nigeria", "Ghana", "Ivory Coast", "Cameroon", "Algeria", "Tunisia", "Iran",
        "Saudi Arabia", "Qatar", "Ecuador", "Colombia", "Chile", "Peru", "Paraguay", "Costa Rica",
        "Panama", "Slovenia", "Slovakia", "Romania", "Bulgaria", "Hungary", "Finland", "Iceland",
        "Ireland", "Northern Ireland", "Greece", "South Africa", "Russia", "Czech Republic",
        "Bosnia", "Congo", "New Zealand", "China", "India", "Jamaica", "Honduras", "El Salvador",
        "Venezuela", "Bolivia", "United Arab Emirates", "Kuwait", "Oman", "Bahrain", "Iraq",
        "Jordan", "Lebanon", "Syria", "Palestine", "Libya", "Sudan", "Mali", "Burkina Faso",
        "Zambia", "Angola", "Mozambique", "Guinea", "Togo", "Benin", "Cape Verde", "Gabon"
    )

    fun fetchMatches(): List<ScrapedMatch> {
        try {
            val html = fetchHtml()
            val matches = parseMatches(html)
            Log.d(TAG, "Scraped ${matches.size} matches from LiveScore")
            return matches.take(5)
        } catch (e: Exception) {
            Log.d(TAG, "LiveScore scrape failed: ${e.message}")
            return emptyList()
        }
    }

    private fun fetchHtml(): String {
        val url = URL(URL_LIVESCORE)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.instanceFollowRedirects = true
        return conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
    }

    private fun parseMatches(html: String): List<ScrapedMatch> {
        val matches = mutableListOf<ScrapedMatch>()
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        dateFmt.timeZone = TimeZone.getTimeZone("UTC")
        val today = dateFmt.format(java.util.Date())

        val scripts = Regex("""<script[^>]*type="application/json"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).toList()

        for (script in scripts) {
            val json = script.groupValues[1]
            if (!json.contains("\"homeTeam\"") && !json.contains("\"Eid\"")) continue
            try {
                extractFromJson(json, matches)
            } catch (_: Exception) {}
        }

        if (matches.isEmpty()) {
            titleBodyPattern(html, matches)
        }

        if (matches.isEmpty()) {
            searchScriptsPattern(html, matches)
        }

        return matches.filter { m ->
            nationalTeams.any { m.homeTeam.contains(it, true) || it.contains(m.homeTeam, true) } &&
            nationalTeams.any { m.awayTeam.contains(it, true) || it.contains(m.awayTeam, true) }
        }
    }

    private fun extractFromJson(json: String, matches: MutableList<ScrapedMatch>) {
        try {
            val obj = JSONObject(json)
            findMatches(obj, matches)
        } catch (_: Exception) {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    try { findMatches(arr.getJSONObject(i), matches) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    private fun findMatches(obj: JSONObject, matches: MutableList<ScrapedMatch>, depth: Int = 0) {
        if (depth > 8 || matches.size >= 5) return
        if (obj.has("homeTeam") && obj.has("awayTeam")) {
            val home = obj.optJSONObject("homeTeam")?.optString("name", "")
                ?: obj.optString("Hn", "")
            val away = obj.optJSONObject("awayTeam")?.optString("name", "")
                ?: obj.optString("An", "")
            val hg = obj.optJSONObject("homeTeam")?.optJSONObject("score")?.optInt("total")
                ?: if (obj.has("Hs")) obj.optInt("Hs", -1) else null
            val ag = obj.optJSONObject("awayTeam")?.optJSONObject("score")?.optInt("total")
                ?: if (obj.has("As")) obj.optInt("As", -1) else null
            val status = obj.optString("status", obj.optString("Eps", "TIMED"))
            val utc = obj.optString("utcDate", obj.optString("Esd", ""))
            if (home.isNotBlank() && away.isNotBlank() && home.length > 2 && away.length > 2) {
                matches.add(ScrapedMatch(home, away, utc, status,
                    if (hg != null && hg >= 0) hg else null,
                    if (ag != null && ag >= 0) ag else null))
            }
        }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            try {
                val child = obj.optJSONObject(key)
                if (child != null) findMatches(child, matches, depth + 1)
            } catch (_: Exception) {}
            try {
                val arr = obj.optJSONArray(key)
                for (i in 0 until arr.length()) {
                    try { findMatches(arr.getJSONObject(i), matches, depth + 1) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    private fun titleBodyPattern(html: String, matches: MutableList<ScrapedMatch>) {
        val blockRegex = Regex("""<span[^>]*class="[^"]*title[^"]*"[^>]*>(.*?)</span>.*?<span[^>]*class="[^"]*body[^"]*"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        blockRegex.findAll(html).forEach { match ->
            val title = match.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            val body = match.groupValues[2].replace(Regex("<[^>]*>"), "").trim()
            val teams = title.split("vs", "v", "-", "–").map { it.trim() }
            if (teams.size == 2) {
                val score = Regex("""(\d+)\s*-\s*(\d+)""").find(body)
                val hg = score?.groupValues?.get(1)?.toIntOrNull()
                val ag = score?.groupValues?.get(2)?.toIntOrNull()
                val isFinished = body.contains("FT", true)
                val isLive = body.contains("'", true)
                val status = when { isFinished -> "FINISHED"; isLive -> "IN_PLAY"; else -> "TIMED" }
                matches.add(ScrapedMatch(teams[0], teams[1], "", status, hg, ag))
            }
        }
    }

    private fun searchScriptsPattern(html: String, matches: MutableList<ScrapedMatch>) {
        val allScripts = Regex("""<script[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).toList()
        val teamRegex = Regex(""""name"\s*:\s*"([^"]+)"""")
        val scoreRegex = Regex("""(\d+)\s*-\s*(\d+)""")
        for (script in allScripts) {
            val content = script.groupValues[1]
            val names = teamRegex.findAll(content).map { it.groupValues[1] }.filter { it.length > 2 }.toList()
            if (names.size >= 2) {
                for (i in 0 until names.size - 1 step 2) {
                    val h = names[i]; val a = names[i + 1]
                    if (nationalTeams.any { h.contains(it, true) } && nationalTeams.any { a.contains(it, true) }) {
                        matches.add(ScrapedMatch(h, a, "", "TIMED", null, null))
                    }
                }
            }
        }
    }
}
