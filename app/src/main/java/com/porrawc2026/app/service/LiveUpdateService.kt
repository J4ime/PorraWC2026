package com.porrawc2026.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.remote.LiveScoreService
import com.porrawc2026.app.data.remote.LiveScorer
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.ui.screens.home.GoalEvent
import com.porrawc2026.app.util.LiveMatchStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LiveUpdateService : Service() {

    @Inject lateinit var liveScoreService: LiveScoreService
    @Inject lateinit var repository: PorraRepository
    @Inject lateinit var liveMatchStore: LiveMatchStore

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var refreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
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
            "Actualización en directo",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificación para mantener la app actualizada en segundo plano"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Porra WC 2026")
            .setContentText("Actualizando resultados en directo...")
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
                    fetchAndStore()
                } catch (_: Exception) { }
                delay(60_000)
            }
        }
    }

    private suspend fun fetchAndStore() {
        val matches = repository.getAllMatches().first()
        if (matches.isEmpty()) return

        val finishedIds = matches.filter { isFinishedByTime(it) }.map { it.id }.toSet()

        val liveMatches = matches.filter { it.id !in finishedIds }
        if (liveMatches.isEmpty()) return

        val updates = liveScoreService.fetchScoreUpdates(liveMatches)
        val gson = Gson()
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
        val start = parseMadridDate(match.dateTime) ?: return false
        return match.homeGoals != null && match.awayGoals != null &&
            Date().after(Date(start.time + 150L * 60 * 1000))
    }

    private fun parseMadridDate(dateTime: String): Date? {
        if (dateTime.isBlank()) return null
        return try {
            if (dateTime.endsWith("Z")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(dateTime)
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("Europe/Madrid") }.parse(dateTime)
            }
        } catch (_: Exception) { null }
    }

    companion object {
        private const val CHANNEL_ID = "live_updates"
        private const val NOTIFICATION_ID = 1001
    }
}
