package com.porrawc2026.app.util

import android.util.Log
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtil {

    val madridZone: ZoneId = ZoneId.of("Europe/Madrid")

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    fun parseMadridInstant(dateTime: String, tag: String = "DateTimeUtil"): Instant? {
        if (dateTime.isBlank()) return null
        return try {
            if (dateTime.endsWith("Z")) {
                Instant.parse(dateTime)
            } else {
                val local = LocalDateTime.parse(dateTime, dateTimeFormatter)
                local.atZone(madridZone).toInstant()
            }
        } catch (e: Exception) {
            Log.e(tag, "parseMadridInstant failed for dateTime=$dateTime", e)
            null
        }
    }
}