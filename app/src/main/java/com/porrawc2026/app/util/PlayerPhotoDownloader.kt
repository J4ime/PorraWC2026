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

    suspend fun precacheTopPlayers(context: Context) {
        withContext(Dispatchers.IO) {
            Log.d("PhotoDownloader", "Precaching ${topGoalScorers.size} top player photos...")
            var downloaded = 0
            for (name in topGoalScorers) {
                val result = download(context, name)
                if (result != null) downloaded++
            }
            Log.d("PhotoDownloader", "Precache done: $downloaded/${topGoalScorers.size} downloaded")
        }
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

                val imageUrl = searchWikipediaImage(playerName)
                if (imageUrl == null) {
                    Log.d("PhotoDownloader", "No Wikipedia image found for '$playerName'")
                    return@withContext null
                }

                val canonical = bestMatchTopScorer(playerName) ?: playerName.trim().lowercase()
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
                Log.d("PhotoDownloader", "Downloaded photo for '$playerName' → ${destFile.absolutePath}")
                destFile.absolutePath
            } catch (e: Exception) {
                Log.e("PhotoDownloader", "Failed for '$playerName': ${e.message}")
                null
            }
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
                curr[j + 1] = minOf(
                    curr[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + cost
                )
            }
            prev.copyInto(curr)
            curr.copyInto(prev)
        }

        val distance = prev[b.length]
        return 1f - (distance.toFloat() / maxLen)
    }

    private fun searchWikipediaImage(playerName: String): String? {
        try {
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=opensearch" +
                "&search=${URLEncoder.encode("$playerName footballer", "UTF-8")}" +
                "&limit=1&format=json"

            val searchRequest = Request.Builder().url(searchUrl).build()
            val searchResponse = client.newCall(searchRequest).execute()
            if (!searchResponse.isSuccessful) return null

            val body = searchResponse.body?.string() ?: return null
            val arr = org.json.JSONArray(body)
            if (arr.length() < 2) return null

            val titles = arr.getJSONArray(1)
            if (titles.length() == 0) {
                val searchUrl2 = "https://en.wikipedia.org/w/api.php?action=opensearch" +
                    "&search=${URLEncoder.encode(playerName, "UTF-8")}" +
                    "&limit=1&format=json"
                val req2 = Request.Builder().url(searchUrl2).build()
                val resp2 = client.newCall(req2).execute()
                if (!resp2.isSuccessful) return null
                val body2 = resp2.body?.string() ?: return null
                val arr2 = org.json.JSONArray(body2)
                if (arr2.length() < 2) return null
                val titles2 = arr2.getJSONArray(1)
                if (titles2.length() == 0) return null
                val pageTitle = titles2.getString(0)
                return fetchImageUrl(pageTitle)
            }

            val pageTitle = titles.getString(0)
            return fetchImageUrl(pageTitle)
        } catch (e: Exception) {
            Log.e("PhotoDownloader", "Search failed: ${e.message}")
            return null
        }
    }

    private fun fetchImageUrl(pageTitle: String): String? {
        try {
            val imageUrl = "https://en.wikipedia.org/w/api.php?action=query" +
                "&prop=pageimages&format=json&piprop=original" +
                "&titles=${URLEncoder.encode(pageTitle, "UTF-8")}"

            val request = Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val pages = json.getJSONObject("query").getJSONObject("pages")
            val firstPage = pages.keys().next()
            val page = pages.getJSONObject(firstPage)

            val original = page.optString("original", "")
            if (original.isNotBlank()) return original

            val thumbnail = page.optJSONObject("thumbnail")
            return thumbnail?.optString("source", "")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e("PhotoDownloader", "Image fetch failed: ${e.message}")
            return null
        }
    }
}
