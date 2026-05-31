package com.porrawc2026.app.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.util.Locale

object PlayerPhotoDownloader {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (compatible; PorraWC2026/1.0)")
                .build()
            chain.proceed(req)
        }
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val topGoalScorers = listOf(
        "Kylian Mbappé", "Erling Haaland", "Harry Kane",
        "Cristiano Ronaldo", "Lionel Messi", "Mohamed Salah",
        "Neymar", "Vinícius Júnior", "Lautaro Martínez",
        "Romelu Lukaku", "Lamine Yamal", "Julián Álvarez",
        "Son Heung-min", "Viktor Gyökeres", "Bukayo Saka",
        "Memphis Depay", "Kai Havertz", "Alexander Isak",
        "Luis Díaz", "Jude Bellingham"
    )

    private var squadEntryMap: Map<String, SquadEntry>? = null

    data class SquadEntry(val fullName: String, val wikiUrl: String)

    suspend fun precacheTopPlayers(context: Context) {
        withContext(Dispatchers.IO) {
            ensureSquadMap(context)
            Log.d("PhotoDownloader", "Precaching ${topGoalScorers.size} top player photos...")
            var downloaded = 0
            for (name in topGoalScorers) {
                val result = download(context, name)
                if (result != null) downloaded++
            }
            Log.d("PhotoDownloader", "Precache done: $downloaded/${topGoalScorers.size} downloaded")
        }
    }

    fun lookupCache(context: Context, playerName: String): String? {
        val cacheDir = File(context.filesDir, "photos")
        if (!cacheDir.exists()) return null
        return findCachedFile(cacheDir, playerName)?.absolutePath
    }

    suspend fun download(context: Context, playerName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.filesDir, "photos")
                if (!cacheDir.exists()) cacheDir.mkdirs()

                val cached = findCachedFile(cacheDir, playerName)
                if (cached != null) {
                    Log.d("PhotoDownloader", "Cache hit for '$playerName' → ${cached.name}")
                    return@withContext cached.absolutePath
                }

                ensureSquadMap(context)
                val resolved = resolveFullName(playerName)
                val entry = squadEntryMap?.get(normalize(resolved))
                val wikiUrl = entry?.wikiUrl ?: "https://en.wikipedia.org/wiki/${resolved.replace(" ", "_")}"
                Log.d("PhotoDownloader", "Resolved '$playerName' → '$resolved' wiki=$wikiUrl")

                val imageUrl = fetchOgImage(wikiUrl)
                if (imageUrl == null) {
                    Log.d("PhotoDownloader", "No og:image for '$resolved'")
                    return@withContext null
                }

                val canonical = bestMatchTopScorer(resolved) ?: resolved.trim().lowercase()
                val fileName = sanitizeForFile(canonical) + ".jpg"
                val destFile = File(cacheDir, fileName)
                Log.d("PhotoDownloader", "Downloading $imageUrl → ${destFile.name}...")

                val imgRequest = Request.Builder()
                    .url(imageUrl)
                    .header("Referer", wikiUrl)
                    .build()
                client.newCall(imgRequest).execute().use { imgResponse ->
                    if (!imgResponse.isSuccessful) {
                        Log.d("PhotoDownloader", "Image download failed: HTTP ${imgResponse.code}")
                        return@withContext null
                    }
                    val body = imgResponse.body ?: run {
                        Log.d("PhotoDownloader", "Image download: null body")
                        return@withContext null
                    }
                    body.byteStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d("PhotoDownloader", "Downloaded photo for '$resolved' (${body.contentLength()} bytes) → ${destFile.absolutePath}")
                }
                destFile.absolutePath
            } catch (e: Exception) {
                Log.e("PhotoDownloader", "Failed for '$playerName': ${e.message}")
                null
            }
        }
    }

    private fun resolveFullName(name: String): String {
        val canonical = bestMatchTopScorer(name)
        if (canonical != null) return canonical

        val map = squadEntryMap ?: return name
        val norm = normalize(name)
        map[norm]?.let { return it.fullName }

        val words = norm.split(" ")
        for (word in words.filter { it.length >= 4 }) {
            map[word]?.let { return it.fullName }
        }
        return name
    }

    private fun fetchOgImage(wikiUrl: String): String? {
        try {
            val request = Request.Builder().url(wikiUrl).build()
            val html = client.newCall(request).execute().use { r ->
                if (!r.isSuccessful) {
                    Log.d("PhotoDownloader", "HTTP ${r.code} for $wikiUrl")
                    return null
                }
                r.body?.string() ?: return null
            }

            val ogRegex = Regex(
                """property\s*=\s*"og:image"\s*content\s*=\s*"([^"]+)"""",
                RegexOption.IGNORE_CASE
            )
            val match = ogRegex.find(html)
            if (match != null) {
                val url = match.groupValues[1]
                Log.d("PhotoDownloader", "og:image → $url")
                return url
            }

            Log.d("PhotoDownloader", "No og:image found, HTML length=${html.length}")
            return null
        } catch (e: Exception) {
            Log.e("PhotoDownloader", "Fetch failed: ${e.message}")
            return null
        }
    }

    private suspend fun ensureSquadMap(context: Context) {
        if (squadEntryMap != null) return
        try {
            val cacheFile = File(context.filesDir, "photos/squad_names.json")
            if (cacheFile.exists()) {
                val json = JSONObject(cacheFile.readText())
                val map = mutableMapOf<String, SquadEntry>()
                json.keys().forEach { key ->
                    val obj = json.getJSONObject(key)
                    map[key] = SquadEntry(
                        fullName = obj.optString("n", ""),
                        wikiUrl = obj.optString("u", "")
                    )
                }
                squadEntryMap = map
                Log.d("PhotoDownloader", "Loaded ${map.size} squad entries from cache")
                return
            }

            Log.d("PhotoDownloader", "Fetching squad page to build player map...")
            val request = Request.Builder()
                .url("https://en.wikipedia.org/w/api.php?action=parse&page=2026_FIFA_World_Cup_squads&prop=text&format=json&formatversion=2")
                .build()
            val respBody = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return
                response.body?.string() ?: return
            }

            val json = JSONObject(respBody)
            val html = json.getJSONObject("parse").getString("text")

            val map = mutableMapOf<String, SquadEntry>()
            val linkRegex = Regex("""<a\s[^>]*href\s*=\s*"([^"]*)"[^>]*title="([^"]*)"[^>]*>""")
            val rowRegex = Regex("""<td[^>]*>\s*4FW\s*</td>((?:(?!<td[^>]*>4FW</td>).)*)""")

            for (row in rowRegex.findAll(html)) {
                val cell = row.value
                val links = linkRegex.findAll(cell)
                for (link in links) {
                    val href = link.groupValues[1]
                    val title = link.groupValues[2]
                    if (!href.startsWith("/wiki/")) continue
                    if (title.contains(":")) continue
                    if (title.contains("(disambiguation)")) continue
                    if (title.contains("Coach") || title.contains("Manager")) continue
                    if (title.contains("footballer") || title.contains("born") || title.contains("captain")) continue

                    val wikiUrl = "https://en.wikipedia.org$href"
                    val entry = SquadEntry(fullName = title, wikiUrl = wikiUrl)
                    val nameNorm = normalize(title)

                    for (w in nameNorm.split(" ")) {
                        if (w.length >= 4 && w != "junior") {
                            val existing = map[w]
                            if (existing == null || existing.fullName.length > title.length) {
                                map[w] = entry
                            }
                        }
                    }
                    map[nameNorm] = entry
                }
            }

            squadEntryMap = map
            val saveJson = JSONObject()
            for ((key, entry) in map) {
                saveJson.put(key, JSONObject().apply {
                    put("n", entry.fullName)
                    put("u", entry.wikiUrl)
                })
            }
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(saveJson.toString())
            Log.d("PhotoDownloader", "Built squad map: ${map.size} entries")
        } catch (e: Exception) {
            Log.e("PhotoDownloader", "Failed to build squad map: ${e.message}")
        }
    }

    private fun findCachedFile(cacheDir: File, playerName: String): File? {
        val norm = normalize(playerName)
        val files = cacheDir.listFiles { f -> f.isFile && f.extension == "jpg" } ?: return null
        var best: File? = null
        var bestScore = 0f
        for (file in files) {
            val fileNorm = normalize(file.nameWithoutExtension)
            val score = similarity(norm, fileNorm)
            if (score > bestScore) {
                bestScore = score
                best = file
            }
        }
        return if (bestScore >= 0.65f) best else null
    }

    private fun bestMatchTopScorer(playerName: String): String? {
        val norm = normalize(playerName)
        var best: String? = null
        var bestScore = 0f
        for (name in topGoalScorers) {
            val score = similarity(norm, normalize(name))
            if (score > bestScore) {
                bestScore = score
                best = name
            }
        }
        return if (bestScore >= 0.65f) best else null
    }

    private fun normalize(s: String): String {
        return Normalizer.normalize(s.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun sanitizeForFile(s: String): String {
        return s.trim().lowercase()
            .replace(" ", "-")
            .replace(Regex("[^a-z0-9-]"), "")
    }

    private fun similarity(a: String, b: String): Float {
        if (a == b) return 1f
        if (a.isEmpty() || b.isEmpty()) return 0f
        val shorter = if (a.length <= b.length) a else b
        val longer = if (a.length > b.length) a else b
        if (longer.contains(shorter) && shorter.length >= 3) return 0.85f
        if (shorter.contains(longer) && longer.length >= 3) return 0.85f
        val aWords = a.split(" ").toSet()
        val bWords = b.split(" ").toSet()
        if (aWords.isNotEmpty() && bWords.isNotEmpty()) {
            val intersection = aWords.intersect(bWords).size.toFloat()
            val union = aWords.union(bWords).size.toFloat()
            val wordOverlap = intersection / union
            if (wordOverlap >= 0.5f) return 0.7f + wordOverlap * 0.3f
        }
        return levenshteinRatio(a, b)
    }

    private fun levenshteinRatio(a: String, b: String): Float {
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1f
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in a.indices) {
            curr[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            prev.copyInto(curr)
            curr.copyInto(prev)
        }
        val distance = prev[b.length]
        return 1f - (distance.toFloat() / maxLen)
    }
}
