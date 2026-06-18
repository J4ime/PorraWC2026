package com.porrawc2026.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LogManager {
    private var logDir: File? = null
    private const val MAX_LOG_SIZE = 512L * 1024

    fun init(context: Context) {
        val base = context.filesDir
        val dir: File? = try {
            if (base != null && base.isDirectory) {
                File(base, "logs").also { it.mkdirs() }
            } else null
        } catch (_: Exception) { null }
        logDir = dir ?: run {
            val fallback = File(System.getProperty("java.io.tmpdir") ?: ".", "logs_app")
            fallback.mkdirs()
            fallback
        }
    }

    @Synchronized
    fun log(tag: String, message: String, tr: Throwable? = null) {
        Log.e(tag, message, tr)
        val dir = logDir ?: return
        val file = File(dir, "app_log.txt")
        rotateIfNeeded(file)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val stackTrace = if (tr != null) "\n${Log.getStackTraceString(tr)}" else ""
        file.appendText("[$timestamp] [$tag] $message$stackTrace\n")
    }

    @Synchronized
    fun getLogs(): String {
        val dir = logDir ?: return ""
        val file = File(dir, "app_log.txt")
        return if (file.exists()) file.readText() else ""
    }

    @Synchronized
    fun clearLogs() {
        val dir = logDir ?: return
        val file = File(dir, "app_log.txt")
        file.delete()
        file.createNewFile()
    }

    private fun rotateIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_LOG_SIZE) {
            val parent = file.parentFile ?: return
            val rotated = File(parent, "app_log_old.txt")
            file.renameTo(rotated)
        }
    }
}
