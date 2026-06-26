package com.porrawc2026.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val GITHUB_API = "https://api.github.com/repos/J4ime/PorraWC2026/releases?per_page=1&sort=created&direction=desc"

    data class UpdateInfo(val version: String, val downloadUrl: String, val isNewer: Boolean)

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            withTimeout(15_000) {
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
                val conn = URL(GITHUB_API).openConnection() as HttpURLConnection
                try {
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    conn.connectTimeout = 10_000; conn.readTimeout = 10_000
                    val code = conn.responseCode
                    if (code != 200) return@withTimeout null
                    val json = conn.inputStream.bufferedReader().readText()
                    val arr = JSONArray(json)
                    if (arr.length() == 0) return@withTimeout null
                    val release = arr.getJSONObject(0)
                    val tag = release.getString("tag_name")
                    val assets = release.getJSONArray("assets")
                    val apkAsset = (0 until assets.length()).map { assets.getJSONObject(it) }
                        .firstOrNull { it.getString("name").endsWith(".apk") } ?: return@withTimeout null
                    val downloadUrl = apkAsset.getString("browser_download_url")
                    val isNewer = compareVersions(tag.removePrefix("v"), currentVersion) > 0
                    UpdateInfo(tag, downloadUrl, isNewer)
                } finally {
                    conn.disconnect()
                }
            }
        }.onFailure { android.util.Log.e("UpdateManager", "checkForUpdate failed", it) }.getOrNull()
    }

    suspend fun downloadAndInstall(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            withTimeout(120_000) {
                val file = File(context.cacheDir, "update.apk")
                file.delete()
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 15_000; conn.readTimeout = 60_000
                    conn.setRequestProperty("Accept", "application/octet-stream")
                    val code = conn.responseCode
                    if (code !in 200..299) return@withTimeout false
                    FileOutputStream(file).use { out -> conn.inputStream.copyTo(out, bufferSize = 8192) }
                    if (file.exists() && file.length() > 0) {
                        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(intent)
                        true
                    } else false
                } finally {
                    conn.disconnect()
                }
            }
        }.onFailure { android.util.Log.e("UpdateManager", "downloadAndInstall failed for $url", it) }.getOrDefault(false)
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val a = p1.getOrElse(i) { 0 }; val b = p2.getOrElse(i) { 0 }
            if (a > b) return 1; if (a < b) return -1
        }
        return 0
    }
}
