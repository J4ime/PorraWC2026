package com.porrawc2026.app.util

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object TvScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class TvMatch(val homeTeam: String, val awayTeam: String, val tvChannel: String)

    fun fetchSchedule(): List<TvMatch> {
        try {
            val request = Request.Builder()
                .url("https://www.futbolenlatv.es/competicion/fifa-world-cup")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            response.close()
            return parseMatches(body)
        } catch (e: Exception) {
            Log.e("TvScraper", "Failed: ${e.message}")
            return emptyList()
        }
    }

    private fun parseMatches(html: String): List<TvMatch> {
        val results = mutableListOf<TvMatch>()
        val text = html
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&aacute;", "á").replace("&eacute;", "é")
            .replace("&iacute;", "í").replace("&oacute;", "ó")
            .replace("&uacute;", "ú").replace("&ntilde;", "ñ")
            .replace("&Aacute;", "Á").replace("&Eacute;", "É")
            .replace("&Oacute;", "Ó").replace("&Uacute;", "Ú")
            .replace(Regex("\\s+"), " ")
            .replace("R.D. Congo", "RD Congo")
            .replace("Holanda", "Países Bajos")

        val pattern = Regex(
            """FIFA Copa Mundial 2026\s+Fase de grupos\s+(.+?)\s{2,}(.+?)\s{2,}DAZN\s""",
            RegexOption.IGNORE_CASE
        )

        val matches = pattern.findAll(text)
        for (m in matches) {
            val home = cleanTeamName(m.groupValues[1])
            val away = cleanTeamName(m.groupValues[2])
            if (home.length > 2 && away.length > 2 && home.length < 30 && away.length < 30) {
                val tv = resolveTv(text, m.range.first)
                results.add(TvMatch(home, away, tv))
            }
        }
        Log.d("TvScraper", "Parsed ${results.size} matches")
        return results
    }

    private fun cleanTeamName(name: String): String {
        return name.trim().replace(Regex("\\s+"), " ")
    }

    private fun resolveTv(text: String, matchStart: Int): String {
        val afterMatch = text.substring(matchStart, minOf(matchStart + 300, text.length))
        val upper = afterMatch.uppercase()
        return when {
            upper.contains("LA 1 TVE") || upper.contains("RTVE PLAY") || upper.contains("TELEDEPORTE") -> "RTVE"
            else -> "DAZN"
        }
    }

    fun matchTv(homeTeam: String, awayTeam: String, schedule: List<TvMatch>): String {
        val h = homeTeam.trim().lowercase()
        val a = awayTeam.trim().lowercase()
        for (m in schedule) {
            val mh = m.homeTeam.lowercase()
            val ma = m.awayTeam.lowercase()
            if ((mh.contains(h) || h.contains(mh)) && (ma.contains(a) || a.contains(ma))) return m.tvChannel
            if ((mh.contains(a) || a.contains(mh)) && (ma.contains(h) || h.contains(ma))) return m.tvChannel
        }
        return "DAZN"
    }
}
