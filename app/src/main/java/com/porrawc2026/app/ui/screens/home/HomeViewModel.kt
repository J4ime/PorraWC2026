package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.util.TvScraper
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.PlayerPhotoDownloader
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
    val predictedHomeGoals: Int?,
    val predictedAwayGoals: Int?,
    val pointsEarned: Int,
    val groupLabel: String,
    val status: MatchStatus,
    val tvChannel: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val apiService: ApiService,
    @param:ApplicationContext private val context: Context
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
    private val _players = MutableStateFlow<List<PlayerPredictionEntity>>(emptyList())
    val players: StateFlow<List<PlayerPredictionEntity>> = _players.asStateFlow()
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var cachedMatches: List<MatchEntity> = emptyList()
    private var refreshJob: Job? = null
    private var livePollJob: Job? = null
    private val lastWrittenScores = mutableMapOf<Int, Pair<Int, Int>>()

    init { refreshPoints(); loadPlayers(); preloadSchedule(); precachePhotos() }

    private fun precachePhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            PlayerPhotoDownloader.precacheTopPlayers(context)
        }
    }

    private fun preloadSchedule() {
        loadHardcodedMatches()
        viewModelScope.launch(Dispatchers.IO) {
            val dbMatches = repository.getAllMatches().first()
            if (dbMatches.isNotEmpty()) {
                cachedMatches = dbMatches
                _hasData.value = true
                enrichScheduleFromApi()
                recalcAllPoints()
                refreshPoints()
                loadPlayers()
                downloadPlayerPhotos()
                startAutoRefresh()
                refreshUpcomingMatches()
            } else {
                enrichScheduleFromApi()
                refreshUpcomingMatches()
            }
        }
    }

    private fun loadHardcodedMatches() {
        val scheduleDates = hardcodedMatchDates()
        val groups = listOf("A","B","C","D","E","F","G","H","I","J","K","L")
        cachedMatches = scheduleDates.map { (id, pair) ->
            val (date, tv) = pair
            val groupIndex = (id - 1) / 6
            MatchEntity(
                id = id, groupName = "Grupo ${groups.getOrElse(groupIndex) { "?" }}",
                matchday = "J${(id - 1) % 6 + 1}", dateTime = date,
                homeTeam = "Local", awayTeam = "Visitante",
                tvChannel = tv, isKnockout = false
            )
        }
        refreshUpcomingMatches()
        _isReady.value = true
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
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
                Log.d("HomeVM", "Imported ${cachedMatches.size} matches, enriching schedule...")
                enrichScheduleFromApi()
                recalcAllPoints()
                refreshPoints()
                refreshUpcomingMatches()
                Log.d("HomeVM", "After enrich: match1 tv='${cachedMatches.firstOrNull()?.tvChannel}' dt='${cachedMatches.firstOrNull()?.dateTime}'")
                loadPlayers()
                downloadPlayerPhotos()
                startAutoRefresh()
            } catch (e: Exception) {
                _errorMessage.emit("Error al cargar el Excel: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissValidation() { _validationResult.value = null }

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAllData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            cachedMatches = emptyList()
            _hasData.value = false
            _totalPoints.value = 0
            _players.value = emptyList()
            loadHardcodedMatches()
        }
    }

    fun refreshPoints() {
        viewModelScope.launch { _totalPoints.value = repository.calculateTotalPoints() }
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            repository.getPlayerPredictions().collect { _players.value = it.sortedBy { p -> p.rank } }
        }
    }

    private fun downloadPlayerPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            val predictions = repository.getPlayerPredictionsList()
            for (p in predictions) {
                val name = p.predictedName ?: continue
                if (!p.photoPath.isNullOrBlank()) continue
                val path = PlayerPhotoDownloader.download(context, name)
                if (path != null) {
                    val updated = p.copy(photoPath = path)
                    repository.updatePlayerPrediction(updated)
                }
            }
            loadPlayers()
        }
    }

    private suspend fun recalcAllPoints() {
        cachedMatches = cachedMatches.map { match ->
            val realHome = match.homeGoals
            val realAway = match.awayGoals
            if (realHome != null && realAway != null) {
                val predHome = match.predictedHomeGoals
                val predAway = match.predictedAwayGoals
                var pts = 0
                if (predHome != null && predAway != null) {
                    if (predHome == realHome) pts += 10
                    if (predAway == realAway) pts += 10
                    val predRes = when { predHome > predAway -> "h"; predHome < predAway -> "a"; else -> "d" }
                    val realRes = when { realHome > realAway -> "h"; realHome < realAway -> "a"; else -> "d" }
                    if (predRes == realRes) pts += 30
                }
                val updated = match.copy(pointsEarned = pts)
                repository.updateMatchPrediction(updated)
                updated
            } else match
        }
    }

    private suspend fun enrichScheduleFromApi() {
        enrichScheduleFallback()
        try {
            val response = apiService.getWorldCupMatches()
            Log.d("HomeVM", "API schedule: ${response.matches.size} matches, overlaying dates")
            val sortedApi = response.matches.sortedBy { it.utcDate }
            cachedMatches = cachedMatches.map { match ->
                val apiMatch = sortedApi.getOrNull(match.id - 1)
                if (apiMatch != null) {
                    val home = apiMatch.homeTeam?.name ?: match.homeTeam
                    val away = apiMatch.awayTeam?.name ?: match.awayTeam
                    match.copy(dateTime = apiMatch.utcDate, homeTeam = home, awayTeam = away)
                } else match
            }
        } catch (e: Exception) {
            Log.d("HomeVM", "API schedule failed: ${e.message}")
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
            if (dateTime.endsWith("Z")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(dateTime)
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = madridTZ }.parse(dateTime)
            }
        } catch (e: Exception) { null }
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
            .sortedBy { it.dateTime.ifBlank { "zzz" } }
        val allDisplay = groupMatches.map { match -> toDisplay(match) }

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
            predictedHomeGoals = match.predictedHomeGoals,
            predictedAwayGoals = match.predictedAwayGoals,
            pointsEarned = match.pointsEarned,
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
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        fmt.timeZone = madridTZ

        data class Md(val date: String, val tv: String)
        val data = mutableMapOf<Int, Md>()

        // Jornada 1 - Thu 11 Jun
        data[1] = Md("2026-06-11T21:00:00", "RTVE")
        // Fri 12 Jun
        data[2] = Md("2026-06-12T04:00:00", "DAZN")
        data[7] = Md("2026-06-12T21:00:00", "RTVE")
        // Sat 13 Jun
        data[8] = Md("2026-06-13T03:00:00", "DAZN")
        data[13] = Md("2026-06-13T21:00:00", "DAZN")
        // Sun 14 Jun
        data[19] = Md("2026-06-14T00:00:00", "RTVE")
        data[20] = Md("2026-06-14T03:00:00", "DAZN")
        data[21] = Md("2026-06-14T06:00:00", "DAZN")
        data[25] = Md("2026-06-14T19:00:00", "RTVE")
        data[26] = Md("2026-06-14T22:00:00", "DAZN")
        // Mon 15 Jun
        data[31] = Md("2026-06-15T01:00:00", "DAZN")
        data[32] = Md("2026-06-15T04:00:00", "DAZN")
        data[37] = Md("2026-06-15T18:00:00", "RTVE")
        data[38] = Md("2026-06-15T21:00:00", "DAZN")
        // Tue 16 Jun
        data[43] = Md("2026-06-16T00:00:00", "DAZN")
        data[44] = Md("2026-06-16T03:00:00", "DAZN")
        data[49] = Md("2026-06-16T21:00:00", "RTVE")
        // Wed 17 Jun
        data[50] = Md("2026-06-17T00:00:00", "DAZN")
        data[51] = Md("2026-06-17T03:00:00", "DAZN")
        data[52] = Md("2026-06-17T06:00:00", "DAZN")
        data[61] = Md("2026-06-17T19:00:00", "DAZN")
        data[62] = Md("2026-06-17T22:00:00", "RTVE")
        // Thu 18 Jun
        data[63] = Md("2026-06-18T01:00:00", "DAZN")
        data[64] = Md("2026-06-18T04:00:00", "DAZN")
        data[3] = Md("2026-06-18T18:00:00", "DAZN")
        data[9] = Md("2026-06-18T21:00:00", "RTVE")
        // Fri 19 Jun
        data[10] = Md("2026-06-19T00:00:00", "DAZN")
        data[4] = Md("2026-06-19T03:00:00", "DAZN")
        data[14] = Md("2026-06-19T21:00:00", "RTVE")
        // Sat 20 Jun
        data[15] = Md("2026-06-20T00:00:00", "DAZN")
        data[16] = Md("2026-06-20T03:00:00", "DAZN")
        data[17] = Md("2026-06-20T06:00:00", "DAZN")
        data[27] = Md("2026-06-20T19:00:00", "RTVE")
        data[28] = Md("2026-06-20T22:00:00", "DAZN")
        // Sun 21 Jun
        data[29] = Md("2026-06-21T02:00:00", "DAZN")
        data[30] = Md("2026-06-21T06:00:00", "DAZN")
        data[39] = Md("2026-06-21T18:00:00", "RTVE")
        data[40] = Md("2026-06-21T21:00:00", "DAZN")
        // Mon 22 Jun
        data[45] = Md("2026-06-22T00:00:00", "DAZN")
        data[46] = Md("2026-06-22T03:00:00", "DAZN")
        data[55] = Md("2026-06-22T19:00:00", "RTVE")
        data[53] = Md("2026-06-22T23:00:00", "DAZN")
        // Tue 23 Jun
        data[54] = Md("2026-06-23T02:00:00", "DAZN")
        data[56] = Md("2026-06-23T05:00:00", "DAZN")
        data[65] = Md("2026-06-23T19:00:00", "DAZN")
        data[67] = Md("2026-06-23T22:00:00", "RTVE")
        // Wed 24 Jun
        data[68] = Md("2026-06-24T01:00:00", "DAZN")
        data[69] = Md("2026-06-24T04:00:00", "DAZN")
        data[5] = Md("2026-06-24T21:00:00", "DAZN")
        data[6] = Md("2026-06-24T21:00:00", "DAZN")
        // Thu 25 Jun
        data[22] = Md("2026-06-25T00:00:00", "DAZN")
        data[23] = Md("2026-06-25T00:00:00", "RTVE")
        data[11] = Md("2026-06-25T03:00:00", "DAZN")
        data[12] = Md("2026-06-25T03:00:00", "DAZN")
        data[33] = Md("2026-06-25T22:00:00", "RTVE")
        data[34] = Md("2026-06-25T22:00:00", "DAZN")
        // Fri 26 Jun
        data[35] = Md("2026-06-26T01:00:00", "DAZN")
        data[36] = Md("2026-06-26T01:00:00", "DAZN")
        data[18] = Md("2026-06-26T04:00:00", "DAZN")
        data[24] = Md("2026-06-26T04:00:00", "DAZN")
        data[47] = Md("2026-06-26T21:00:00", "DAZN")
        data[48] = Md("2026-06-26T21:00:00", "RTVE")
        // Sat 27 Jun
        data[41] = Md("2026-06-27T02:00:00", "DAZN")
        data[42] = Md("2026-06-27T02:00:00", "DAZN")
        data[57] = Md("2026-06-27T05:00:00", "DAZN")
        data[58] = Md("2026-06-27T05:00:00", "DAZN")
        data[70] = Md("2026-06-27T23:00:00", "DAZN")
        data[71] = Md("2026-06-27T23:00:00", "RTVE")
        // Sun 28 Jun
        data[66] = Md("2026-06-28T01:30:00", "DAZN")
        data[72] = Md("2026-06-28T01:30:00", "DAZN")
        data[59] = Md("2026-06-28T04:00:00", "DAZN")
        data[60] = Md("2026-06-28T04:00:00", "RTVE")

        val result = mutableMapOf<Int, Pair<String, String>>()
        for (id in 1..72) {
            val md = data[id]
            if (md != null) result[id] = Pair(md.date, md.tv)
        }
        return result
    }

    private fun enrichScheduleFallback() {
        val fallbackDates = hardcodedMatchDates()
        TvScraper.clearCache()

        cachedMatches = cachedMatches.map { match ->
            val fb = fallbackDates[match.id]
            val date = fb?.first ?: match.dateTime
            val tv = if (match.tvChannel.isNotBlank() && match.tvChannel.all { !it.isDigit() }) {
                match.tvChannel
            } else {
                TvScraper.lookupTv(match.homeTeam, match.awayTeam)
            }
            match.copy(dateTime = date, tvChannel = tv)
        }
        val withRtv = cachedMatches.count { it.tvChannel.contains("RTVE") }
        Log.d("HomeVM", "Enriched ${cachedMatches.size} matches, $withRtv with RTVE, dates from schedule")
    }

    override fun onCleared() {
        refreshJob?.cancel()
        livePollJob?.cancel()
        super.onCleared()
    }
}
