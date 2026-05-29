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
            val parsed = parseHtml(body)
            Log.d("TvScraper", "Web parsed ${parsed.size} matches")
            return parsed
        } catch (e: Exception) {
            Log.e("TvScraper", "Web failed: ${e.message}")
            return emptyList()
        }
    }

    private fun parseHtml(html: String): List<TvMatch> {
        val results = mutableListOf<TvMatch>()
        val text = html.replace(Regex("<[^>]+>"), " ").replace("&nbsp;", " ")
            .replace("&aacute;", "á").replace("&eacute;", "é").replace("&iacute;", "í")
            .replace("&oacute;", "ó").replace("&uacute;", "ú").replace("&ntilde;", "ñ")
            .replace(Regex("\\s+"), " ")

        val teamPairRegex = Regex("""([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+(?:\s(?:de|del|la|los|y|el|R\.D\.|da|dos|das))?\s?(?:[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)?)\s{2,}([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+(?:\s(?:de|del|la|los|y|el|R\.D\.|da|dos|das))?\s?(?:[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)?)\s{2,}DAZN""")

        val matches = teamPairRegex.findAll(text)
        for (m in matches) {
            val home = m.groupValues[1].trim()
            val away = m.groupValues[2].trim()
            if (home.length in 3..30 && away.length in 3..30) {
                val searchArea = text.substring(maxOf(0, m.range.first), minOf(text.length, m.range.last + 200))
                val tv = resolveTv(searchArea)
                results.add(TvMatch(home, away, tv))
            }
        }
        return results
    }

    private fun resolveTv(text: String): String {
        val upper = text.uppercase()
        val channels = mutableListOf<String>()
        if (upper.contains("LA 1 TVE") || upper.contains("RTVE PLAY") || upper.contains("TELEDEPORTE") || upper.contains("LA 2")) channels.add("RTVE")
        if (upper.contains("DAZN")) channels.add("DAZN")
        return channels.joinToString(",").ifBlank { "DAZN" }
    }

    fun matchTv(homeTeam: String, awayTeam: String, schedule: List<TvMatch>): String {
        val h = normalizeTeam(homeTeam)
        val a = normalizeTeam(awayTeam)
        for (m in schedule) {
            val mh = normalizeTeam(m.homeTeam)
            val ma = normalizeTeam(m.awayTeam)
            if ((mh.contains(h) || h.contains(mh)) && (ma.contains(a) || a.contains(ma))) return m.tvChannel
            if ((mh.contains(a) || a.contains(mh)) && (ma.contains(h) || h.contains(ma))) return m.tvChannel
        }
        return "DAZN"
    }

    fun normalizeTeam(name: String): String {
        return name.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            .replace(" ", "")
    }

    fun getHardcodedTv(): Map<String, String> {
        val key = { h: String, a: String -> normalizeTeam("#$h#$a") }
        val m = mutableMapOf<String, String>()
        // RTVE matches from futbolenlatv.es data
        m[key("México","Sudáfrica")] = "RTVE,DAZN"
        m[key("Canadá","Bosnia")] = "RTVE,DAZN"
        m[key("Brasil","Marruecos")] = "RTVE,DAZN"
        m[key("Alemania","Curazao")] = "RTVE,DAZN"
        m[key("España","Cabo Verde")] = "RTVE,DAZN"
        m[key("Francia","Senegal")] = "RTVE,DAZN"
        m[key("Portugal","RD Congo")] = "RTVE,DAZN"
        m[key("Suiza","Bosnia")] = "RTVE,DAZN"
        m[key("EEUU","Australia")] = "RTVE,DAZN"
        m[key("Escocia","Marruecos")] = "RTVE,DAZN"
        m[key("Holanda","Suecia")] = "RTVE,DAZN"
        m[key("España","Arabia Saudita")] = "RTVE,DAZN"
        m[key("Argentina","Austria")] = "RTVE,DAZN"
        m[key("Inglaterra","Ghana")] = "RTVE,DAZN"
        m[key("Ecuador","Alemania")] = "RTVE,DAZN"
        m[key("Escocia","Brasil")] = "RTVE,DAZN"
        m[key("Uruguay","España")] = "RTVE,DAZN"
        m[key("Colombia","Portugal")] = "RTVE,DAZN"
        m[key("Argelia","Austria")] = "RTVE,DAZN"
        m[key("Panamá","Inglaterra")] = "RTVE,DAZN"
        m[key("Catar","Suiza")] = "RTVE,DAZN"
        m[key("Inglaterra","Croacia")] = "RTVE,DAZN"
        return m
    }
}
