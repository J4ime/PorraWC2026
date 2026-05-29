package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class MatchStatus { UPCOMING, LIVE, FINISHED }

data class MatchDisplay(
    val id: Int,
    val dateLabel: String,
    val time: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeFlag: String,
    val awayFlag: String,
    val homeGoals: Int?,
    val awayGoals: Int?,
    val groupLabel: String,
    val status: MatchStatus,
    val tvChannel: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

    private val _hasData = MutableStateFlow(false)
    val hasData: StateFlow<Boolean> = _hasData.asStateFlow()

    private val _upcomingMatches = MutableStateFlow<List<MatchDisplay>>(emptyList())
    val upcomingMatches: StateFlow<List<MatchDisplay>> = _upcomingMatches.asStateFlow()

    private val _sectionTitle = MutableStateFlow("")
    val sectionTitle: StateFlow<String> = _sectionTitle.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private var cachedMatches: List<MatchEntity> = emptyList()
    private var refreshJob: Job? = null
    private var livePollJob: Job? = null
    private val lastWrittenScores = mutableMapOf<Int, Pair<Int, Int>>()

    init {
        refreshPoints()
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _validationResult.value = null
            try {
                val data = ExcelParser.parse(context, uri)
                val validation = ExcelParser.validate()
                _validationResult.value = validation
                repository.insertAllData(
                    data.teams, data.matches, data.questions,
                    data.playerPredictions, data.knockoutPredictions, data.standings
                )
                _hasData.value = true
                cachedMatches = data.matches
                lastWrittenScores.clear()
                refreshPoints()
                refreshUpcomingMatches()
                startAutoRefresh()
            } catch (e: Exception) {
                _errorMessage.emit("Error al cargar el Excel: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissValidation() { _validationResult.value = null }

    fun refreshPoints() {
        viewModelScope.launch {
            _totalPoints.value = repository.calculateTotalPoints()
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            var lastDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            while (isActive) {
                val now = Calendar.getInstance()
                val currentDay = now.get(Calendar.DAY_OF_YEAR)
                if (currentDay != lastDay) {
                    lastDay = currentDay
                    refreshUpcomingMatches()
                }
                checkAndStartLivePolling()
                delay(60_000)
            }
        }
    }

    private suspend fun checkAndStartLivePolling() {
        if (hasLiveMatch()) {
            if (livePollJob?.isActive != true) {
                startLivePolling()
            }
        } else {
            livePollJob?.cancel()
        }
    }

    private fun hasLiveMatch(): Boolean {
        val now = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        return cachedMatches.any { match ->
            try {
                if (match.dateTime.isBlank()) return@any false
                val start = sdf.parse(match.dateTime) ?: return@any false
                val end = Date(start.time + 150L * 60 * 1000) // 2.5h match window
                now.after(start) && now.before(end)
            } catch (e: Exception) { false }
        }
    }

    private fun startLivePolling() {
        livePollJob?.cancel()
        livePollJob = viewModelScope.launch(Dispatchers.IO) {
            Log.d("HomeVM", "Live polling started")
            while (isActive && hasLiveMatch()) {
                try {
                    fetchLiveResults()
                } catch (e: Exception) {
                    Log.d("HomeVM", "Live fetch failed: ${e.message}")
                }
                delay(5 * 60_000L)
            }
            Log.d("HomeVM", "Live polling stopped")
        }
    }

    private suspend fun fetchLiveResults() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val response = apiService.getWorldCupMatches(dateFrom = dateStr, dateTo = dateStr)
        response.matches.forEach { fm ->
            val entities = cachedMatches.filter {
                it.homeTeam.contains(fm.homeTeam?.name ?: "", ignoreCase = true) ||
                it.homeTeam.contains(fm.homeTeam?.shortName ?: "", ignoreCase = true) ||
                fm.homeTeam?.name?.contains(it.homeTeam, ignoreCase = true) == true
            }
            if (entities.size == 1) {
                val entity = entities.first()
                val home = fm.score?.fullTime?.home ?: return@forEach
                val away = fm.score?.fullTime?.away ?: return@forEach
                val prev = lastWrittenScores[entity.id]
                if (prev == null || prev.first != home || prev.second != away) {
                    lastWrittenScores[entity.id] = home to away
                    repository.updateMatchResults(entity.id, home, away)
                    Log.d("HomeVM", "Live result: ${entity.homeTeam} $home-$away ${entity.awayTeam}")
                    refreshPoints()
                    cachedMatches = cachedMatches.map {
                        if (it.id == entity.id) it.copy(homeGoals = home, awayGoals = away) else it
                    }
                    refreshUpcomingMatches()
                }
            }
        }
    }

    private fun refreshUpcomingMatches() {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
        val dateFmt = SimpleDateFormat("EEE d MMM", Locale("es", "ES"))
        val now = Calendar.getInstance()
        val today = now.get(Calendar.DAY_OF_YEAR)
        val year = now.get(Calendar.YEAR)

        val groupMatches = cachedMatches.filter { !it.isKnockout }
        val allDisplay = groupMatches.map { match ->
            val time = if (match.dateTime.isNotBlank()) {
                try { val d = sdf.parse(match.dateTime); if (d != null) timeFmt.format(d) else "" } catch (e: Exception) { "" }
            } else ""
            val dateLabel = if (match.dateTime.isNotBlank()) {
                try { val d = sdf.parse(match.dateTime); if (d != null) dateFmt.format(d).replace(".", "") else "" } catch (e: Exception) { "" }
            } else ""
            val status = matchStatus(match)
            MatchDisplay(
                id = match.id,
                dateLabel = dateLabel,
                time = time,
                homeTeam = match.homeTeam,
                awayTeam = match.awayTeam,
                homeFlag = ExcelParser.getFlagEmoji(match.homeTeam),
                awayFlag = ExcelParser.getFlagEmoji(match.awayTeam),
                homeGoals = match.homeGoals,
                awayGoals = match.awayGoals,
                groupLabel = match.groupName,
                status = status,
                tvChannel = match.tvChannel
            )
        }.sortedBy { it.time }

        val todayMatches = allDisplay.filter { display ->
            try {
                val m = cachedMatches.firstOrNull { it.id == display.id } ?: return@filter false
                if (m.dateTime.isBlank()) return@filter false
                val d = sdf.parse(m.dateTime) ?: return@filter false
                val c = Calendar.getInstance().apply { time = d }
                c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today
            } catch (e: Exception) { false }
        }

        if (todayMatches.isNotEmpty()) {
            _sectionTitle.value = "HOY \u2014 ${todayMatches.first().dateLabel.uppercase()}"
            _upcomingMatches.value = todayMatches
        } else {
            val futureMatches = allDisplay.filter { display ->
                try {
                    val m = cachedMatches.firstOrNull { it.id == display.id } ?: return@filter false
                    if (m.dateTime.isBlank()) return@filter false
                    val d = sdf.parse(m.dateTime) ?: return@filter false
                    d.after(Date())
                } catch (e: Exception) { false }
            }
            if (futureMatches.isNotEmpty()) {
                val firstDate = futureMatches.first().dateLabel
                _sectionTitle.value = "PR\u00D3XIMA JORNADA \u2014 ${firstDate.uppercase()}"
                _upcomingMatches.value = futureMatches.filter { it.dateLabel == firstDate }.take(8)
            } else {
                _sectionTitle.value = "SIN PARTIDOS PR\u00D3XIMOS"
                _upcomingMatches.value = emptyList()
            }
        }
    }

    private fun matchStatus(match: MatchEntity): MatchStatus {
        if (match.homeGoals != null && match.awayGoals != null) return MatchStatus.FINISHED
        val now = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        return try {
            if (match.dateTime.isBlank()) return MatchStatus.UPCOMING
            val start = sdf.parse(match.dateTime) ?: return MatchStatus.UPCOMING
            val end = Date(start.time + 150L * 60 * 1000)
            if (now.after(start) && now.before(end)) MatchStatus.LIVE
            else MatchStatus.UPCOMING
        } catch (e: Exception) { MatchStatus.UPCOMING }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        livePollJob?.cancel()
        super.onCleared()
    }
}
