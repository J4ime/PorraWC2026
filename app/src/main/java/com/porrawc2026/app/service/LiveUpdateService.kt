package com.porrawc2026.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.remote.LiveScoreService
import com.porrawc2026.app.data.remote.LiveScorer
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.ui.screens.home.GoalEvent
import com.porrawc2026.app.util.LiveMatchStore
import com.porrawc2026.app.util.LogManager
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import javax.inject.Inject

@AndroidEntryPoint
class LiveUpdateService : Service() {

    @Inject lateinit var liveScoreService: LiveScoreService
    @Inject lateinit var repository: PorraRepository
    @Inject lateinit var liveMatchStore: LiveMatchStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var refreshJob: Job? = null
    private var consecutiveErrors = 0

    override fun onCreate() {
        super.onCreate()
        LogManager.init(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startRefreshing()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        refreshJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Actualización",
            NotificationManager.IMPORTANCE_NONE
        ).apply {
            description = "Sincronización en segundo plano"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Porra WC 2026")
            .setContentText("Actualizando...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun startRefreshing() {
        refreshJob?.cancel()
        refreshJob = serviceScope.launch {
            while (isActive) {
                try {
                    withContext(Dispatchers.IO) { fetchAndStore() }
                    consecutiveErrors = 0
                } catch (e: Exception) {
                    consecutiveErrors++
                    Log.e("LiveUpdateService", "Error in refresh loop", e)
                    LogManager.log("LiveUpdateService", "Error in refresh loop", e)
                }
                val backoff = min(consecutiveErrors.toLong(), 10L) * 30_000L
                delay(maxOf(60_000L + backoff, 10_000L))
            }
        }
    }

    private suspend fun fetchAndStore() {
        val allMatches = withContext(Dispatchers.IO) { repository.getAllMatches().first() }
        if (allMatches.isEmpty()) return

        val todayAndStale = getTodayAndStaleMatches(allMatches)
        if (todayAndStale.isEmpty()) return

        val finishedIds = todayAndStale.filter { isFinishedByTime(it) }.map { it.id }.toSet()
        val liveMatches = todayAndStale.filter { it.id !in finishedIds }
        if (liveMatches.isEmpty()) return

        val updates = liveScoreService.fetchScoreUpdates(liveMatches)
        updates.forEach { update ->
            if (update.isFinished) {
                liveMatchStore.liveMinutes[update.matchId] = "FINAL"
                repository.updateMatchResults(update.matchId, update.homeGoals, update.awayGoals)
            } else if (update.liveMinute != null) {
                liveMatchStore.liveMinutes[update.matchId] = update.liveMinute
            }

            if (update.homeScorers.isNotEmpty() || update.awayScorers.isNotEmpty()) {
                liveMatchStore.goalScorers[update.matchId] = Pair(
                    update.homeScorers.map { GoalEvent(it.playerName, it.minute) },
                    update.awayScorers.map { GoalEvent(it.playerName, it.minute) }
                )
            }

            if (update.isFinished && (update.homeScorers.isNotEmpty() || update.awayScorers.isNotEmpty())) {
                val homeJson = gson.toJson(update.homeScorers)
                val awayJson = gson.toJson(update.awayScorers)
                repository.updateMatchScorers(update.matchId, homeJson, awayJson)
            }
            if (update.isFinished) {
                repository.updateMatchCards(update.matchId, update.homeRedCards, update.awayRedCards, update.homeYellowCards, update.awayYellowCards)
            }
        }

    }

    private fun isFinishedByTime(match: MatchEntity): Boolean {
        val start = parseInstant(match.dateTime) ?: return false
        return match.homeGoals != null && match.awayGoals != null &&
            Instant.now().isAfter(start.plusSeconds(MATCH_WINDOW_SECONDS))
    }

    private fun getTodayAndStaleMatches(matches: List<MatchEntity>): List<MatchEntity> {
        val now = Instant.now()
        val madridZone = ZoneId.of("Europe/Madrid")
        val today = LocalDate.now(madridZone)
        return matches.filter { match ->
            val start = parseInstant(match.dateTime) ?: return@filter false
            val matchDate = start.atZone(madridZone).toLocalDate()
            if (matchDate == today) return@filter true
            if (matchDate.isBefore(today) && now.isAfter(start.plusSeconds(MATCH_WINDOW_SECONDS))) {
                val isFinal = liveMatchStore.liveMinutes[match.id] == "FINAL" ||
                    (match.homeGoals != null && match.awayGoals != null)
                return@filter !isFinal
            }
            return@filter false
        }
    }

    private fun parseInstant(dateTime: String): Instant? {
        if (dateTime.isBlank()) return null
        return try {
            if (dateTime.endsWith("Z")) {
                Instant.parse(dateTime)
            } else {
                val local = java.time.LocalDateTime.parse(dateTime, dateTimeFormatter)
                local.atZone(ZoneId.of("Europe/Madrid")).toInstant()
            }
        } catch (_: Exception) { null }
    }

    companion object {
        private const val CHANNEL_ID = "live_updates"
        private const val NOTIFICATION_ID = 1001
        private const val MATCH_WINDOW_SECONDS = 150L * 60
        private val gson = Gson()
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }
}
