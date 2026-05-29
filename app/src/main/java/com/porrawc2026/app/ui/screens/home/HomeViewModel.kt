package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
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
                delay(60_000)
                val now = Calendar.getInstance()
                val currentDay = now.get(Calendar.DAY_OF_YEAR)
                if (currentDay != lastDay) {
                    lastDay = currentDay
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
            val status = when {
                match.homeGoals != null && match.awayGoals != null -> MatchStatus.FINISHED
                else -> MatchStatus.UPCOMING
            }
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

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }
}
