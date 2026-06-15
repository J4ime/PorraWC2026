package com.porrawc2026.app.util

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TvScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var cachedHtml: String? = null
    private var todayTag: String = ""

    fun lookupTv(homeTeam: String, awayTeam: String, cacheDir: File? = null): String {
        val today = todayTag()
        if (cachedHtml != null && todayTag == today) {
            return resolveFromCache(homeTeam, awayTeam)
        }

        val html = loadCachedOrFetch(cacheDir, today) ?: return "DAZN"
        cachedHtml = html
        todayTag = today
        return resolveFromCache(homeTeam, awayTeam)
    }

    private fun resolveFromCache(homeTeam: String, awayTeam: String): String {
        val html = cachedHtml ?: return "DAZN"
        val text = html.replace(Regex("<[^>]+>"), " ").replace("&nbsp;", " ")
            .replace("&aacute;", "á").replace("&eacute;", "é").replace("&iacute;", "í")
            .replace("&oacute;", "ó").replace("&uacute;", "ú").replace("&ntilde;", "ñ")
            .replace(Regex("\\s+"), " ")

        val h = homeTeam.trim()
        val a = awayTeam.trim()

        val idx = text.indexOf(h, ignoreCase = true)
        if (idx < 0) {
            val idx2 = text.indexOf(a, ignoreCase = true)
            if (idx2 < 0) return "DAZN"
            val ctx = text.substring(maxOf(0, idx2 - 50), minOf(text.length, idx2 + a.length + 250))
            return extractTv(ctx)
        }
        val ctx = text.substring(maxOf(0, idx - 50), minOf(text.length, idx + h.length + 250))
        return extractTv(ctx)
    }

    private fun extractTv(text: String): String {
        val upper = text.uppercase()
        val channels = mutableListOf<String>()
        if (upper.contains("LA 1 TVE") || upper.contains("RTVE PLAY") || upper.contains("TELEDEPORTE") || upper.contains("LA 2")) channels.add("RTVE")
        if (upper.contains("DAZN")) channels.add("DAZN")
        return channels.joinToString(",").ifBlank { "DAZN" }
    }

    private fun loadCachedOrFetch(cacheDir: File?, today: String): String? {
        val cacheFile = cacheDir?.let { File(it, "tv_cache_$today.html") }
        if (cacheFile != null && cacheFile.exists()) {
            val body = cacheFile.readText()
            return body
        }

        val html = fetchHtml() ?: return null
        if (cacheFile != null) {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(html)
        }
        return html
    }

    private fun fetchHtml(): String? {
        return runCatching {
            val request = Request.Builder()
                .url("https://www.futbolenlatv.es/competicion/fifa-world-cup")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            response.close()
            body
        }.getOrNull()
    }

    fun clearCache() {
        cachedHtml = null
        todayTag = ""
    }

    private fun todayTag(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
