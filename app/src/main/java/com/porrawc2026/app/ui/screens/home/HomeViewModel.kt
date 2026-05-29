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

    private val madridTZ = TimeZone.getTimeZone("Europe/Madrid")
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

    init { refreshPoints() }

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
                enrichScheduleFromApi()
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
        viewModelScope.launch { _totalPoints.value = repository.calculateTotalPoints() }
    }

    private suspend fun enrichScheduleFromApi() {
        try {
            val response = apiService.getWorldCupMatches()
            Log.d("HomeVM", "API schedule: ${response.matches.size} matches")
            response.matches.forEach { fm ->
                val entities = cachedMatches.filter {
                    fm.homeTeam?.name?.contains(it.homeTeam, ignoreCase = true) == true ||
                    it.homeTeam.contains(fm.homeTeam?.name ?: "", ignoreCase = true) ||
                    it.homeTeam.contains(fm.homeTeam?.shortName ?: "", ignoreCase = true)
                }
                if (entities.size == 1) {
                    val entity = entities.first()
                    val apiDate = fm.utcDate
                    val apiTv = fm.stage ?: ""
                    cachedMatches = cachedMatches.map {
                        if (it.id == entity.id) it.copy(
                            dateTime = apiDate,
                            tvChannel = resolveTvChannel(it, fm)
                        ) else it
                    }
                    Log.d("HomeVM", "Schedule match: ${entity.homeTeam} vs ${entity.awayTeam} @ $apiDate TV=${resolveTvChannel(entity, fm)}")
                }
            }
        } catch (e: Exception) {
            Log.d("HomeVM", "API schedule fetch failed: ${e.message}, using fallback")
            enrichScheduleFallback()
        }
    }

    private fun enrichScheduleFallback() {
        val fallbackDates = hardcodedMatchDates()
        cachedMatches = cachedMatches.map { match ->
            val fb = fallbackDates[match.id]
            if (fb != null) {
                match.copy(dateTime = fb.first, tvChannel = resolveTvChannelFallback(match))
            } else if (match.dateTime.isBlank()) {
                match.copy(tvChannel = resolveTvChannelFallback(match))
            } else {
                match.copy(tvChannel = match.tvChannel.ifBlank { resolveTvChannelFallback(match) })
            }
        }
    }

    private fun resolveTvChannel(match: MatchEntity, fm: com.porrawc2026.app.data.remote.FootballMatch): String {
        if (match.tvChannel.isNotBlank()) return match.tvChannel
        return resolveTvChannelFallback(match)
    }

    private fun resolveTvChannelFallback(match: MatchEntity): String {
        val esp = listOf("españa", "spain")
        val isSpainMatch = esp.any { match.homeTeam.contains(it, ignoreCase = true) }
                       || esp.any { match.awayTeam.contains(it, ignoreCase = true) }
        return when {
            isSpainMatch -> "RTVE"
            match.groupName.uppercase() in listOf("SEMIFINALES", "FINAL", "3ER PUESTO", "CUARTOS") -> "RTVE"
            match.isKnockout -> "DAZN"
            match.id <= 2 -> "RTVE"
            else -> "DAZN"
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            var lastDay = Calendar.getInstance(madridTZ).get(Calendar.DAY_OF_YEAR)
            while (isActive) {
                val now = Calendar.getInstance(madridTZ)
                val currentDay = now.get(Calendar.DAY_OF_YEAR)
                if (currentDay != lastDay) {
                    lastDay = currentDay
                    refreshUpcomingMatches()
                }
                checkLiveWindow()
                delay(60_000)
            }
        }
    }

    private suspend fun checkLiveWindow() {
        val todayWithDates = getTodayMatchesWithDates()
        if (todayWithDates.isEmpty()) { livePollJob?.cancel(); return }
        val now = Date()
        val times = todayWithDates.map { parseMadridDate(it.dateTime) }.filterNotNull()
        val firstStart = times.minOrNull() ?: return
        val lastEnd = Date((times.maxOrNull()?.time ?: return) + 150L * 60 * 1000)
        if (now.before(firstStart) || now.after(lastEnd)) { livePollJob?.cancel(); return }
        if (livePollJob?.isActive != true) startLivePolling()
    }

    private fun parseMadridDate(dateTime: String): Date? {
        if (dateTime.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(dateTime)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(dateTime)
            } catch (e2: Exception) { null }
        }
    }

    private fun getTodayMatchesWithDates(): List<MatchEntity> {
        val cal = Calendar.getInstance(madridTZ)
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        return cachedMatches.filter { m ->
            val d = parseMadridDate(m.dateTime) ?: return@filter false
            val c = Calendar.getInstance(madridTZ).apply { time = d }
            c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today
        }
    }

    private fun startLivePolling() {
        livePollJob?.cancel()
        livePollJob = viewModelScope.launch(Dispatchers.IO) {
            Log.d("HomeVM", "Live polling started")
            while (isActive) {
                val todayMatches = getTodayMatchesWithDates()
                if (todayMatches.isEmpty()) break
                val now = Date()
                val times = todayMatches.map { parseMadridDate(it.dateTime) }.filterNotNull()
                val firstStart = times.minOrNull()
                val lastEnd = times.maxOrNull()?.let { Date(it.time + 150L * 60 * 1000) }
                if (firstStart != null && now.before(firstStart)) break
                if (lastEnd != null && now.after(lastEnd)) break
                try { fetchLiveResults() } catch (e: Exception) { Log.d("HomeVM", "Live fetch: ${e.message}") }
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
                fm.homeTeam?.name?.contains(it.homeTeam, ignoreCase = true) == true ||
                it.homeTeam.contains(fm.homeTeam?.name ?: "", ignoreCase = true)
            }
            if (entities.size == 1) {
                val entity = entities.first()
                val home = fm.score?.fullTime?.home ?: return@forEach
                val away = fm.score?.fullTime?.away ?: return@forEach
                val prev = lastWrittenScores[entity.id]
                if (prev == null || prev.first != home || prev.second != away) {
                    lastWrittenScores[entity.id] = home to away
                    repository.updateMatchResults(entity.id, home, away)
                    cachedMatches = cachedMatches.map { if (it.id == entity.id) it.copy(homeGoals = home, awayGoals = away) else it }
                    refreshPoints()
                    refreshUpcomingMatches()
                }
            }
        }
    }

    private fun refreshUpcomingMatches() {
        val cal = Calendar.getInstance(madridTZ)
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        val groupMatches = cachedMatches.filter { !it.isKnockout }
        val allDisplay = groupMatches.map { match -> toDisplay(match) }
            .sortedBy { it.time.ifBlank { "99:99" } }

        val todayMatches = allDisplay.filter { display ->
            val d = parseMadridDate(cachedMatches.first { it.id == display.id }.dateTime) ?: return@filter false
            val c = Calendar.getInstance(madridTZ).apply { time = d }
            c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today
        }

        if (todayMatches.isNotEmpty()) {
            _sectionTitle.value = "HOY \u2014 ${todayMatches.first().dateLabel.uppercase()}"
            _upcomingMatches.value = todayMatches
        } else {
            val futureMatches = allDisplay.filter { display ->
                val d = parseMadridDate(cachedMatches.first { it.id == display.id }.dateTime) ?: return@filter false
                d.after(Date())
            }
            if (futureMatches.isNotEmpty()) {
                val firstDate = futureMatches.first().dateLabel
                _sectionTitle.value = "PR\u00D3XIMA JORNADA \u2014 ${firstDate.uppercase()}"
                _upcomingMatches.value = futureMatches.filter { it.dateLabel == firstDate }
            } else if (allDisplay.isNotEmpty()) {
                _sectionTitle.value = "TODOS LOS PARTIDOS"
                _upcomingMatches.value = allDisplay.take(8)
            } else {
                _sectionTitle.value = "SIN PARTIDOS"
                _upcomingMatches.value = emptyList()
            }
        }
        Log.d("HomeVM", "refresh: today=${todayMatches.size} future=${
            allDisplay.count { d ->
                val dateStr = cachedMatches.firstOrNull { it.id == d.id }?.dateTime ?: ""
                if (dateStr.isBlank()) false
                else { val pd = parseMadridDate(dateStr); pd != null && pd.after(Date()) }
            }
        } total=${allDisplay.size}")
    }

    private fun toDisplay(match: MatchEntity): MatchDisplay {
        val date = parseMadridDate(match.dateTime)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = madridTZ }
        val dateFmt = SimpleDateFormat("EEE d MMM", Locale("es", "ES")).apply { timeZone = madridTZ }
        val time = if (date != null) timeFmt.format(date) else ""
        val dateLabel = if (date != null) dateFmt.format(date).replace(".", "") else ""
        val status = matchStatus(match)
        return MatchDisplay(
            id = match.id, dateLabel = dateLabel, time = time,
            homeTeam = match.homeTeam, awayTeam = match.awayTeam,
            homeFlag = ExcelParser.getFlagEmoji(match.homeTeam),
            awayFlag = ExcelParser.getFlagEmoji(match.awayTeam),
            homeGoals = match.homeGoals, awayGoals = match.awayGoals,
            groupLabel = match.groupName, status = status, tvChannel = match.tvChannel
        )
    }

    private fun matchStatus(match: MatchEntity): MatchStatus {
        if (match.homeGoals != null && match.awayGoals != null) return MatchStatus.FINISHED
        val start = parseMadridDate(match.dateTime) ?: return MatchStatus.UPCOMING
        val now = Date()
        val end = Date(start.time + 150L * 60 * 1000)
        return if (now.after(start) && now.before(end)) MatchStatus.LIVE else MatchStatus.UPCOMING
    }

    private fun hardcodedMatchDates(): Map<Int, Pair<String, String>> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"))
        cal.set(2026, Calendar.JUNE, 11, 19, 0, 0)
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        fmt.timeZone = madridTZ
        val matches = mutableMapOf<Int, Pair<String, String>>()
        var id = 1
        while (id <= 72) {
            matches[id++] = Pair(fmt.format(cal.time), "")
            cal.add(Calendar.HOUR_OF_DAY, 4)
        }
        return matches
    }

    override fun onCleared() {
        refreshJob?.cancel()
        livePollJob?.cancel()
        super.onCleared()
    }
}
