package com.porrawc2026.app.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

object PlayerPhotoDownloader {

    private val client = OkHttpClient.Builder()
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

    private var squadNameMap: Map<String, String>? = null

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

                val resolved = resolveFullName(context, playerName)
                Log.d("PhotoDownloader", "Resolved '$playerName' → '$resolved'")

                val imageUrl = fetchSummaryImage(resolved)
                if (imageUrl == null) {
                    Log.d("PhotoDownloader", "No Wikipedia REST image for '$resolved'")
                    return@withContext null
                }

                val canonical = bestMatchTopScorer(resolved) ?: resolved.trim().lowercase()
                val fileName = sanitizeForFile(canonical) + ".jpg"
                val destFile = File(cacheDir, fileName)

                val request = Request.Builder().url(imageUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null

                response.body?.byteStream()?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("PhotoDownloader", "Downloaded photo for '$resolved' → ${destFile.absolutePath}")
                destFile.absolutePath
            } catch (e: Exception) {
                Log.e("PhotoDownloader", "Failed for '$playerName': ${e.message}")
                null
            }
        }
    }

    private suspend fun resolveFullName(context: Context, name: String): String {
        val canonical = bestMatchTopScorer(name)
        if (canonical != null) return canonical

        ensureSquadMap(context)
        val map = squadNameMap ?: return name
        val norm = normalize(name)
        map[norm]?.let { return it }

        val words = norm.split(" ")
        for (word in words) {
            if (word.length >= 4) {
                map[word]?.let { return it }
            }
        }
        return name
    }

    private suspend fun ensureSquadMap(context: Context) {
        if (squadNameMap != null) return
        try {
            val mapFile = File(context.filesDir, "photos/squad_names.json")
            if (mapFile.exists()) {
                val json = JSONObject(mapFile.readText())
                val map = mutableMapOf<String, String>()
                json.keys().forEach { key -> map[key] = json.getString(key) }
                squadNameMap = map
                Log.d("PhotoDownloader", "Loaded ${map.size} names from squad cache")
                return
            }

            Log.d("PhotoDownloader", "Fetching squad page to build name map...")
            val request = Request.Builder()
                .url("https://en.wikipedia.org/w/api.php?action=parse&page=2026_FIFA_World_Cup_squads&prop=text&format=json&formatversion=2")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return

            val body = response.body?.string() ?: return
            val json = JSONObject(body)
            val html = json.getJSONObject("parse").getString("text")

            val map = mutableMapOf<String, String>()
            val linkRegex = Regex("""<a\s[^>]*href\s*=\s*"([^"]*)"[^>]*title="([^"]*)"[^>]*>""")
            val rowRegex = Regex("""<td[^>]*>\s*4FW\s*</td>((?:(?!<td[^>]*>4FW</td>).)*)""")

            for (row in rowRegex.findAll(html)) {
                val cell = row.value
                val links = linkRegex.findAll(cell)
                for (link in links) {
                    val href = link.groupValues[1]
                    val title = link.groupValues[2]
                    if (href.startsWith("/wiki/") && !title.contains(":") && !title.contains("(disambiguation)") &&
                        !title.contains("Coach") && !title.contains("Manager") && !title.contains("footballer") && !title.contains("born") && !title.contains("captain")) {
                        val short = normalize(title)
                        val shortWords = short.split(" ")
                        for (w in shortWords) {
                            if (w.length >= 4 && w != "júnior" && w != "junior") {
                                if (!map.containsKey(w) || map[w]!!.length > title.length) {
                                    map[w] = title
                                }
                            }
                        }
                        map[short] = title
                    }
                }
            }

            squadNameMap = map
            mapFile.parentFile?.mkdirs()
            mapFile.writeText(JSONObject(map.toMap()).toString())
            Log.d("PhotoDownloader", "Built name map: ${map.size} entries")
        } catch (e: Exception) {
            Log.e("PhotoDownloader", "Failed to build squad map: ${e.message}")
        }
    }

    private fun fetchSummaryImage(pageTitle: String): String? {
        try {
            val encoded = pageTitle.replace(" ", "_")
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val json = JSONObject(body)

            val original = json.optJSONObject("originalimage")?.optString("source", "") ?: ""
            if (original.isNotBlank()) return original

            val thumb = json.optJSONObject("thumbnail")?.optString("source", "") ?: ""
            if (thumb.isNotBlank()) return thumb

            return null
        } catch (e: Exception) {
            Log.e("PhotoDownloader", "REST summary failed for '$pageTitle': ${e.message}")
            return null
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
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
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
