package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.util.ExcelData
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class MatchDisplay(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val groupLabel: String,
    val dateTime: String,
    val time: String,
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

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    init {
        refreshPoints()
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _validationResult.value = null
            try {
                val data = ExcelParser.parse(context, uri)
                val validation = ExcelParser.validate(data)
                _validationResult.value = validation
                repository.insertAllData(
                    data.teams, data.matches, data.questions,
                    data.playerPredictions, data.knockoutPredictions, data.standings
                )
                _hasData.value = true
                refreshPoints()
                refreshUpcomingMatches(data.matches)
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

    private fun refreshUpcomingMatches(matches: List<MatchEntity>) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
        val now = Calendar.getInstance()
        val today = now.get(Calendar.DAY_OF_YEAR)
        val year = now.get(Calendar.YEAR)

        val groupMatches = matches.filter { !it.isKnockout }
        val upcoming = groupMatches.mapNotNull { match ->
            try {
                val date = sdf.parse(match.dateTime) ?: return@mapNotNull null
                val cal = Calendar.getInstance().apply { time = date }
                MatchDisplay(
                    id = match.id,
                    homeTeam = match.homeTeam,
                    awayTeam = match.awayTeam,
                    groupLabel = match.groupName,
                    dateTime = match.dateTime,
                    time = timeFmt.format(date),
                    tvChannel = getTvChannel(cal)
                )
            } catch (e: Exception) { null }
        }.sortedBy { it.dateTime }

        // Find today's matches or next matchday
        val todayMatches = upcoming.filter {
            try {
                val d = sdf.parse(it.dateTime) ?: return@filter false
                val c = Calendar.getInstance().apply { time = d }
                c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today
            } catch (e: Exception) { false }
        }

        _upcomingMatches.value = if (todayMatches.isNotEmpty()) {
            todayMatches.take(6)
        } else {
            // Next matchday
            val futureMatches = upcoming.filter {
                try {
                    val d = sdf.parse(it.dateTime) ?: return@filter false
                    d.after(Date())
                } catch (e: Exception) { false }
            }
            if (futureMatches.isNotEmpty()) {
                val firstDate = futureMatches.first().dateTime.take(10)
                futureMatches.filter { it.dateTime.startsWith(firstDate) }.take(6)
            } else {
                upcoming.take(6)
            }
        }
    }

    private fun getTvChannel(date: Calendar): String {
        // World Cup 2026 Spain TV rights - RTVE has main rights
        val hour = date.get(Calendar.HOUR_OF_DAY)
        return when {
            hour >= 20 -> "La 1 TVE"
            hour >= 15 -> "Teledeporte"
            else -> "RTVE Play / La 1"
        }
    }
}
