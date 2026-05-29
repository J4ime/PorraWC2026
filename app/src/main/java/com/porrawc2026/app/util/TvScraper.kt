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
        val cleaned = html
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ").replace("&aacute;", "á")
            .replace("&eacute;", "é").replace("&iacute;", "í")
            .replace("&oacute;", "ó").replace("&uacute;", "ú")
            .replace("&ntilde;", "ñ").replace(Regex("\\s+"), " ")
            .replace("R.D. Congo", "RD Congo")
            .replace("Holanda", "Países Bajos")
            .replace("Costa de Marfil", "Costa Marfil")
            .replace("Arabia Saudí", "Arabia Saudita")
            .replace("República Checa", "Republica Checa")
            .replace("Corea del Sur", "Corea Sur")
            .replace("Estados Unidos", "EEUU")

        val results = mutableListOf<TvMatch>()
        val marker = "FIFA Copa Mundial 2026 Fase de grupos "
        var idx = 0
        while (true) {
            idx = cleaned.indexOf(marker, idx)
            if (idx < 0) break
            val after = cleaned.substring(idx + marker.length)
            val parts = after.trim().split(Regex("\\s{2,}"), limit = 4)
            if (parts.size >= 3) {
                val home = parts[0].trim()
                val away = parts[1].trim()
                val tvBlock = if (parts.size > 2) parts[2] else ""
                val channels = mutableListOf<String>()
                val tvUpper = tvBlock.uppercase()
                if (tvUpper.contains("LA 1 TVE") || tvUpper.contains("RTVE PLAY") || tvUpper.contains("TELEDEPORTE")) channels.add("RTVE")
                if (tvUpper.contains("DAZN")) channels.add("DAZN")
                if (channels.isEmpty()) channels.add("DAZN")
                if (home.length in 3..25 && away.length in 3..25) {
                    results.add(TvMatch(home, away, channels.joinToString(",")))
                }
            }
            idx += marker.length
        }
        Log.d("TvScraper", "Parsed ${results.size} matches: ${results.take(5).joinToString { "${it.homeTeam} vs ${it.awayTeam} → ${it.tvChannel}" }}")
        return results
    }

    fun matchTv(homeTeam: String, awayTeam: String, schedule: List<TvMatch>): String {
        val h = homeTeam.trim().lowercase()
        val a = awayTeam.trim().lowercase()
        val hNorm = normalizeTeam(h)
        val aNorm = normalizeTeam(a)
        for (m in schedule) {
            val mh = normalizeTeam(m.homeTeam.lowercase())
            val ma = normalizeTeam(m.awayTeam.lowercase())
            if ((mh.contains(hNorm) || hNorm.contains(mh)) && (ma.contains(aNorm) || aNorm.contains(ma))) return m.tvChannel
            if ((mh.contains(aNorm) || aNorm.contains(mh)) && (ma.contains(hNorm) || hNorm.contains(ma))) return m.tvChannel
        }
        return "DAZN"
    }

    private fun normalizeTeam(name: String): String {
        return name.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            .replace(" ", "")
    }
}
