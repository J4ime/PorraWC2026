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

    private var cachedHtml: String? = null

    fun lookupTv(homeTeam: String, awayTeam: String): String {
        val html = cachedHtml ?: fetchHtml() ?: return "DAZN"
        cachedHtml = html

        val text = html.replace(Regex("<[^>]+>"), " ").replace("&nbsp;", " ")
            .replace("&aacute;", "á").replace("&eacute;", "é").replace("&iacute;", "í")
            .replace("&oacute;", "ó").replace("&uacute;", "ú").replace("&ntilde;", "ñ")
            .replace(Regex("\\s+"), " ")

        val h = homeTeam.trim()
        val a = awayTeam.trim()

        val hNorm = normalizeTeam(h)
        val aNorm = normalizeTeam(a)

        Log.d("TvScraper", "Lookup: '$h' vs '$a'")

        val idx = text.indexOf(h, ignoreCase = true)
        if (idx < 0) {
            val idx2 = text.indexOf(a, ignoreCase = true)
            if (idx2 < 0) return "DAZN"
            val ctx = text.substring(maxOf(0, idx2 - 50), minOf(text.length, idx2 + a.length + 250))
            return resolveTv(ctx)
        }

        val ctx = text.substring(maxOf(0, idx - 50), minOf(text.length, idx + h.length + 250))
        return resolveTv(ctx)
    }

    private fun resolveTv(text: String): String {
        val upper = text.uppercase()
        val channels = mutableListOf<String>()
        if (upper.contains("LA 1 TVE") || upper.contains("RTVE PLAY") || upper.contains("TELEDEPORTE") || upper.contains("LA 2")) channels.add("RTVE")
        if (upper.contains("DAZN")) channels.add("DAZN")
        return channels.joinToString(",").ifBlank { "DAZN" }
    }

    private fun fetchHtml(): String? {
        return try {
            val request = Request.Builder()
                .url("https://www.futbolenlatv.es/competicion/fifa-world-cup")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            response.close()
            Log.d("TvScraper", "Fetched HTML: ${body.length} bytes")
            body
        } catch (e: Exception) {
            Log.e("TvScraper", "Fetch failed: ${e.message}")
            null
        }
    }

    fun clearCache() { cachedHtml = null }

    fun normalizeTeam(name: String): String {
        return name.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            .replace(" ", "")
    }
}
