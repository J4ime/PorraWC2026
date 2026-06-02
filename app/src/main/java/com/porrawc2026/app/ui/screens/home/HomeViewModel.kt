package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.remote.LiveMatchDetail
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.util.TvScraper
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.PlayerPhotoDownloader
import com.porrawc2026.app.util.ValidationResult
import com.porrawc2026.app.util.LiveScoreScraper
import com.porrawc2026.app.util.ScrapedMatch
import com.porrawc2026.app.util.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class MatchStatus { UPCOMING, LIVE, FINISHED }

data class GoalEvent(val playerName: String, val minute: Int)

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
    val tvChannel: String,
    val liveMinute: String? = null,
    val homeScorers: List<GoalEvent> = emptyList(),
    val awayScorers: List<GoalEvent> = emptyList()
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
    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()
    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()
    private val _testModeTitle = MutableStateFlow("")
    val testModeTitle: StateFlow<String> = _testModeTitle.asStateFlow()
    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()
    val appVersion: String = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?" } catch (_: Exception) { "?" }

    private var cachedMatches: List<MatchEntity> = emptyList()
    private var wcCachedMatches: List<MatchEntity> = emptyList()
    private var wcPlayers: List<PlayerPredictionEntity> = emptyList()
    private var wcHasData: Boolean = false
    private var refreshJob: Job? = null
    private var livePollJob: Job? = null
    private var testPollJob: Job? = null
    private val lastWrittenScores = mutableMapOf<Int, Pair<Int, Int>>()
    private val sofascoreEventIds = mutableMapOf<Int, Long>()
    private var liveMatchApiId: Int? = null
    private var liveMinuteStr: String? = null
    private val testScorers = mutableMapOf<Int, Pair<List<GoalEvent>, List<GoalEvent>>>()
    private val testLiveMinutes = mutableMapOf<Int, Int>()
    private val testPlayers = listOf(
        PlayerPredictionEntity(rank = 1, playerName = "Romelu Lukaku", predictedName = "Lukaku", pointsPerGoal = 50),
        PlayerPredictionEntity(rank = 2, playerName = "Luka Modric", predictedName = "Modric", pointsPerGoal = 30),
        PlayerPredictionEntity(rank = 3, playerName = "Kevin De Bruyne", predictedName = "De Bruyne", pointsPerGoal = 10)
    )
    companion object {
        const val MATCH_ID_AMISTOSO = 999
        private const val PREFS = "porra_prefs"
        private const val KEY_TEST = "test_mode"
        val WC_TEAMS = setOf("Mexico", "South Africa", "South Korea", "Czech Republic", "Canada",
            "Switzerland", "Qatar", "Bosnia-Herzegovina", "Brazil", "Morocco", "Haiti", "Scotland",
            "United States", "Paraguay", "Turkey", "Australia", "Germany", "Curacao", "Ivory Coast",
            "Ecuador", "Netherlands", "Japan", "Sweden", "Tunisia", "Belgium", "Egypt", "Iran",
            "New Zealand", "Spain", "Cape Verde", "Saudi Arabia", "Uruguay", "France", "Senegal",
            "Norway", "Iraq", "Argentina", "Jordan", "Austria", "Algeria", "Portugal", "Uzbekistan",
            "Colombia", "Congo DR", "England", "Croatia", "Panama", "Ghana")
    }

    init {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_TEST, false)) {
            _isTestMode.value = true
            enterTestMode()
        } else {
            refreshPoints(); loadPlayers(); preloadSchedule(); precachePhotos()
        }
        checkForUpdate()
    }

    private fun saveTestMode(enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_TEST, enabled).apply()
    }

    fun toggleTestMode() {
        val newMode = !_isTestMode.value
        _isTestMode.value = newMode
        saveTestMode(newMode)
        _isReady.value = false
        livePollJob?.cancel()
        testPollJob?.cancel()
        refreshJob?.cancel()
        liveMatchApiId = null
        liveMinuteStr = null
        testScorers.clear()
        testLiveMinutes.clear()
        sofascoreEventIds.clear()
        lastWrittenScores.clear()
        if (newMode) {
            enterTestMode()
        } else {
            exitTestMode()
        }
        checkForUpdate()
    }

    private fun enterTestMode() {
        wcCachedMatches = cachedMatches.toList()
        wcPlayers = _players.value.toList()
        wcHasData = _hasData.value
        _hasData.value = true
        _players.value = testPlayers
        _testModeTitle.value = ""
        Log.w("HomeVM", "===== enterTestMode: launching fetch =====")
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            fetchFriendlyMatches()
            _isBusy.value = false
            _isReady.value = true
            Log.w("HomeVM", "===== enterTestMode: done, isReady=true, cachedMatches=${cachedMatches.size} =====")
        }
    }

    private fun exitTestMode() {
        cachedMatches = wcCachedMatches
        _players.value = wcPlayers
        _hasData.value = wcHasData
        _testModeTitle.value = ""
        refreshPoints(); loadPlayers(); preloadSchedule(); precachePhotos()
    }

    private fun precachePhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            PlayerPhotoDownloader.precacheTopPlayers(context)
            _isBusy.value = false
        }
    }

    private fun preloadSchedule() {
        loadHardcodedMatches()
        viewModelScope.launch(Dispatchers.IO) {
            val dbMatches = repository.getAllMatches().first()
            if (dbMatches.isNotEmpty()) {
                cachedMatches = dbMatches
                ensureTestMatch()
                _hasData.value = true
                enrichScheduleFromApi()
                recalcAllPoints()
                refreshPoints()
                loadPlayers()
                downloadPlayerPhotos()
                startAutoRefresh()
                refreshUpcomingMatches()
            } else {
                _hasData.value = true
                enrichScheduleFromApi()
                refreshUpcomingMatches()
            }
            _isReady.value = true
        }
    }

    private fun loadHardcodedMatches() {
        val scheduleDates = hardcodedMatchDates()
        val groups = listOf("A","B","C","D","E","F","G","H","I","J","K","L")
        cachedMatches = scheduleDates.map { (id, list) ->
            val date = list[0]
            val tv = list[1]
            val home = list[2]
            val away = list[3]
            val groupIndex = (id - 1) / 6
            MatchEntity(
                id = id, groupName = "Grupo ${groups.getOrElse(groupIndex) { "?" }}",
                matchday = "J${(id - 1) % 6 + 1}", dateTime = date,
                homeTeam = home, awayTeam = away,
                tvChannel = tv, isKnockout = false
            )
        }
        ensureTestMatch()
        loadTestPlayers()
    }

    private fun ensureTestMatch() {
        if (cachedMatches.none { it.id == MATCH_ID_AMISTOSO }) {
            cachedMatches = cachedMatches + MatchEntity(
                id = MATCH_ID_AMISTOSO, groupName = "Amistoso",
                matchday = "Amistoso", dateTime = "2026-06-02T18:00:00",
                homeTeam = "Croacia", awayTeam = "Bélgica",
                tvChannel = "", isKnockout = false,
                predictedHomeGoals = 1, predictedAwayGoals = 2, pointsEarned = 0
            )
        }
    }

    private fun loadTestPlayers() {
        if (_isTestMode.value) _players.value = testPlayers
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            repository.getPlayerPredictions().collect { dbPlayers ->
                _players.value = if (_isTestMode.value) testPlayers
                    else dbPlayers.sortedBy { it.rank }
            }
        }
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _isBusy.value = true
            _validationResult.value = null
            try {
                val data = ExcelParser.parse(context, uri)
                val validation = ExcelParser.validate()
                _validationResult.value = validation
                Log.w("HomeVM", "===== IMPORT: isValid=${validation.isValid} errors=[${validation.errors.joinToString("; ")}] =====")
                if (validation.isValid) {
                    repository.insertAllData(
                        data.teams, data.matches, data.questions,
                        data.playerPredictions, data.knockoutPredictions, data.standings
                    )
                    _hasData.value = true
                    cachedMatches = data.matches
                    ensureTestMatch()
                    lastWrittenScores.clear()
                    Log.w("HomeVM", "===== IMPORT: data inserted, hasData=true, ${cachedMatches.size} matches =====")
                    enrichScheduleFromApi()
                    recalcAllPoints()
                    refreshPoints()
                    refreshUpcomingMatches()
                    loadPlayers()
                    downloadPlayerPhotos()
                    startAutoRefresh()
                }
            } catch (e: Exception) {
                Log.w("HomeVM", "===== IMPORT ERROR: ${e.message} =====", e)
                _errorMessage.emit("Error al cargar el Excel: ${e.message}")
            } finally {
                _isLoading.value = false
                _isBusy.value = false
            }
        }
    }

    fun dismissValidation() { _validationResult.value = null }

    private fun checkForUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = UpdateManager.checkForUpdate(context)
            _updateAvailable.value = info?.isNewer == true
        }
    }

    fun installUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdating.value = true
            try {
                val info = UpdateManager.checkForUpdate(context)
                if (info?.isNewer == true) {
                    UpdateManager.downloadAndInstall(context, info.downloadUrl)
                } else {
                    _errorMessage.emit("Ya tienes la ultima version")
                }
            } catch (e: Exception) {
                _errorMessage.emit("Error al actualizar: ${e.message}")
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            repository.insertAllData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            cachedMatches = cachedMatches.map { match ->
                match.copy(predictedHomeGoals = null, predictedAwayGoals = null, pointsEarned = 0, homeGoals = null, awayGoals = null)
            }
            lastWrittenScores.clear()
            _hasData.value = false
            _totalPoints.value = 0
            _players.value = emptyList()
            refreshUpcomingMatches()
            _isBusy.value = false
        }
    }

    fun refreshPoints() {
        if (_isTestMode.value) {
            val matchPts = cachedMatches.sumOf { it.pointsEarned }
            val playerPts = _players.value.sumOf { it.pointsEarned }
            _totalPoints.value = matchPts + playerPts
        } else {
            viewModelScope.launch { _totalPoints.value = repository.calculateTotalPoints() }
        }
    }

    private fun downloadPlayerPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
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
            _isBusy.value = false
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

    private fun recalcPlayerPoints() {
        val allScorers = testScorers.values.flatMap { it.first + it.second }
        val updated = _players.value.map { player ->
            val matchingScorers = allScorers.count { scorer ->
                val pName = player.predictedName ?: player.playerName
                scorer.playerName.contains(pName, ignoreCase = true) ||
                pName.contains(scorer.playerName, ignoreCase = true)
            }
            if (matchingScorers > 0) {
                val newGoals = player.goalsScored + matchingScorers
                player.copy(goalsScored = newGoals, pointsEarned = newGoals * player.pointsPerGoal)
            } else player
        }
        _players.value = updated
        refreshPoints()
    }

    private suspend fun enrichScheduleFromApi() {
        enrichScheduleFallback()
        try {
            _isBusy.value = true
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
        } finally {
            _isBusy.value = false
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
        if (_isTestMode.value) return
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
        fetchLiveAmistoso()
    }

    private suspend fun fetchFriendlyMatches() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val all = try { apiService.getMatches(dateFrom = dateStr, dateTo = dateStr) } catch (_: Exception) { null }
        val apiMatches = all?.matches?.take(5) ?: emptyList()

        if (apiMatches.isNotEmpty()) {
            buildAndShowMatches(apiMatches.map { m ->
                ScrapedMatch(
                    homeTeam = m.homeTeam?.name ?: "?", awayTeam = m.awayTeam?.name ?: "?",
                    utcDate = m.utcDate, status = m.status ?: "TIMED",
                    homeGoals = if (m.status == "FINISHED") m.score?.fullTime?.home else null,
                    awayGoals = if (m.status == "FINISHED") m.score?.fullTime?.away else null
                )
            })
        } else {
            Log.w("HomeVM", "===== API returned ${apiMatches.size} matches, trying scraper =====")
            val scraped = withContext(Dispatchers.IO) { LiveScoreScraper.fetchMatches() }
            Log.w("HomeVM", "===== Scraper returned ${scraped.size} matches =====")
            if (scraped.isNotEmpty()) {
                Log.d("HomeVM", "LiveScore fallback: ${scraped.size} matches")
                buildAndShowMatches(scraped)
            } else {
                _testModeTitle.value = "SIN AMISTOSOS HOY"
                cachedMatches = emptyList()
                refreshUpcomingMatches()
            }
        }
    }

    private fun buildAndShowMatches(raw: List<ScrapedMatch>) {
        val rng = java.util.Random()
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        Log.w("HomeVM", "===== buildAndShowMatches: ${raw.size} raw matches =====")
        if (raw.isEmpty()) {
            _testModeTitle.value = "SIN AMISTOSOS HOY"
            cachedMatches = emptyList()
            refreshUpcomingMatches()
            return
        }
        val entities = raw.mapIndexed { idx, m ->
            val matchId = 900 + idx
            if (m.eventId > 0) sofascoreEventIds[matchId] = m.eventId
            if (m.liveMinute > 0) testLiveMinutes[matchId] = m.liveMinute
            MatchEntity(
                id = matchId, groupName = "Amistoso",
                matchday = "Amistoso", dateTime = m.utcDate.ifBlank { now },
                homeTeam = m.homeTeam, awayTeam = m.awayTeam,
                tvChannel = "", isKnockout = false,
                predictedHomeGoals = rng.nextInt(3), predictedAwayGoals = rng.nextInt(3),
                pointsEarned = 0, homeGoals = m.homeGoals, awayGoals = m.awayGoals
            )
        }
        cachedMatches = entities
        Log.w("HomeVM", "===== built ${entities.size} entities, first: ${entities.firstOrNull()?.homeTeam} vs ${entities.firstOrNull()?.awayTeam} =====")
        _testModeTitle.value = "AMISTOSOS HOY"
        refreshUpcomingMatches()
        startTestPolling()
    }

    private fun startTestPolling() {
        testPollJob?.cancel()
        testPollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && _isTestMode.value) {
                fetchLiveFriendly()
                delay(5 * 60_000L)
            }
        }
    }

    private suspend fun fetchLiveFriendly() {
        val scraped = withContext(Dispatchers.IO) { LiveScoreScraper.fetchMatches() }
        if (scraped.isEmpty()) return

        var changed = false
        scraped.forEach { sm ->
            val cm = cachedMatches.firstOrNull {
                it.homeTeam.contains(sm.homeTeam, true) || sm.homeTeam.contains(it.homeTeam, true)
            } ?: return@forEach
            if (sm.liveMinute > 0) testLiveMinutes[cm.id] = sm.liveMinute
            val hg = sm.homeGoals; val ag = sm.awayGoals
            val prev = lastWrittenScores[cm.id]
            if (hg != null && ag != null && (prev == null || prev.first != hg || prev.second != ag)) {
                lastWrittenScores[cm.id] = hg to ag
                val idx = cachedMatches.indexOf(cm)
                if (idx >= 0) {
                    cachedMatches = cachedMatches.toMutableList().also { it[idx] = cm.copy(homeGoals = hg, awayGoals = ag) }
                }
                changed = true
            }
        }
        if (changed) {
            Log.w("HomeVM", "===== fetchLiveFriendly: changed=true, fetching goal details =====")
            cachedMatches.forEach { cm ->
                val eId = sofascoreEventIds[cm.id] ?: return@forEach
                Log.w("HomeVM", "===== fetching goal details for match ${cm.id} eventId=$eId =====")
                val (h, a) = withContext(Dispatchers.IO) { LiveScoreScraper.fetchGoalDetails(eId) }
                if (h.isNotEmpty() || a.isNotEmpty()) {
                    val homeEvents = h.map { GoalEvent(it.playerName, it.minute) }
                    val awayEvents = a.map { GoalEvent(it.playerName, it.minute) }
                    testScorers[cm.id] = Pair(homeEvents, awayEvents)
                    Log.w("HomeVM", "===== stored scorers for match ${cm.id}: H=$h A=$a =====")
                } else {
                    Log.w("HomeVM", "===== no scorers for match ${cm.id} (H=$h A=$a) =====")
                }
            }
            cachedMatches = cachedMatches.map { m ->
                val realHome = m.homeGoals; val realAway = m.awayGoals
                if (realHome != null && realAway != null) {
                    val predHome = m.predictedHomeGoals; val predAway = m.predictedAwayGoals
                    var pts = 0
                    if (predHome != null && predAway != null) {
                        if (predHome == realHome) pts += 10
                        if (predAway == realAway) pts += 10
                        val pr = when { predHome > predAway -> "h"; predHome < predAway -> "a"; else -> "d" }
                        val rr = when { realHome > realAway -> "h"; realHome < realAway -> "a"; else -> "d" }
                        if (pr == rr) pts += 30
                    }
                    m.copy(pointsEarned = pts)
                } else m
            }
            recalcPlayerPoints()
            refreshUpcomingMatches()
        }
    }

    private suspend fun fetchLiveAmistoso() {
        if (_isTestMode.value) fetchLiveFriendly()
        val amistoso = cachedMatches.firstOrNull { it.id == MATCH_ID_AMISTOSO } ?: return
        val start = parseMadridDate(amistoso.dateTime) ?: return
        val now = Date()
        val end = Date(start.time + 150L * 60 * 1000)
        if (now.before(start) || now.after(end)) return

        try {
            val detail: LiveMatchDetail
            if (liveMatchApiId != null) {
                detail = apiService.getMatchDetail(matchId = liveMatchApiId!!)
            } else {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val allMatches = apiService.getMatches(dateFrom = dateStr, dateTo = dateStr)
                val found = allMatches.matches.firstOrNull {
                    val h = it.homeTeam?.name ?: ""
                    val a = it.awayTeam?.name ?: ""
                    (h.contains("Croatia", true) || h.contains("Croacia", true)) &&
                    (a.contains("Belgium", true) || a.contains("Bélgica", true))
                }
                if (found == null) {
                    Log.d("HomeVM", "Live amistoso: match not found in API")
                    return
                }
                liveMatchApiId = found.id
                Log.d("HomeVM", "Live amistoso: found API id=$liveMatchApiId")
                detail = apiService.getMatchDetail(matchId = liveMatchApiId!!)
            }

            val homeGoals = detail.score?.fullTime?.home
            val awayGoals = detail.score?.fullTime?.away
            val minute = detail.minute
            val status = detail.status

            val isFinished = status == "FINISHED"
            val prev = lastWrittenScores[MATCH_ID_AMISTOSO]
            val needsUpdate = (homeGoals != null && awayGoals != null) &&
                (prev == null || prev.first != homeGoals || prev.second != awayGoals)

            if (needsUpdate || isFinished) {
                if (homeGoals != null && awayGoals != null) {
                    lastWrittenScores[MATCH_ID_AMISTOSO] = homeGoals to awayGoals
                }
                val homeScr = detail.goals?.filter { it.team?.name?.contains("Croatia", true) == true || it.team?.name?.contains("Croacia", true) == true || (it.team?.name == null && detail.homeTeam?.name?.contains("Croatia", true) == true) }
                    ?.mapNotNull { g -> val name = g.scorer?.name ?: return@mapNotNull null; val m = g.minute ?: return@mapNotNull null; GoalEvent(name, m) } ?: emptyList()
                val awayScr = detail.goals?.filter { it.team?.name?.contains("Belgium", true) == true || it.team?.name?.contains("Bélgica", true) == true || (it.team?.name == null && detail.awayTeam?.name?.contains("Belgium", true) == true) }
                    ?.mapNotNull { g -> val name = g.scorer?.name ?: return@mapNotNull null; val m = g.minute ?: return@mapNotNull null; GoalEvent(name, m) } ?: emptyList()

                liveMinuteStr = if (isFinished) "FINAL" else minute
                testScorers[MATCH_ID_AMISTOSO] = Pair(homeScr, awayScr)

                cachedMatches = cachedMatches.map {
                    if (it.id == MATCH_ID_AMISTOSO) it.copy(
                        homeGoals = homeGoals ?: it.homeGoals,
                        awayGoals = awayGoals ?: it.awayGoals
                    ) else it
                }
                recalcAllPoints()
                recalcPlayerPoints()
                refreshPoints()
                refreshUpcomingMatches()
                Log.d("HomeVM", "Live amistoso: min=$minute status=$status H=$homeGoals A=$awayGoals scorers H=$homeScr A=$awayScr")
            }
        } catch (e: Exception) {
            Log.d("HomeVM", "Live amistoso error: ${e.message}")
        }
    }

    private fun refreshUpcomingMatches() {
        if (_isTestMode.value) {
            val allDisplay = cachedMatches.filter { !it.isKnockout }
                .sortedBy { it.dateTime.ifBlank { "zzz" } }
                .map { toDisplay(it) }
            _sectionTitle.value = _testModeTitle.value.ifBlank { "AMISTOSOS HOY" }
            _upcomingMatches.value = allDisplay
            Log.w("HomeVM", "===== TEST refresh: ${allDisplay.size} matches, first: ${allDisplay.firstOrNull()?.homeTeam} vs ${allDisplay.firstOrNull()?.awayTeam} =====")
            return
        }
        val cal = Calendar.getInstance(madridTZ)
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        val groupMatches = cachedMatches.filter { !it.isKnockout && it.id < 900 }
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

        val liveMin: String?
        val homeScr: List<GoalEvent>
        val awayScr: List<GoalEvent>
        if (_isTestMode.value && match.id >= 900) {
            liveMin = when (status) {
                MatchStatus.FINISHED -> "FINAL"
                MatchStatus.LIVE -> testLiveMinutes[match.id]?.let { "${it}'" } ?: "?"
                else -> null
            }
            val s = testScorers[match.id]
            homeScr = s?.first ?: emptyList()
            awayScr = s?.second ?: emptyList()
        } else if (match.id == MATCH_ID_AMISTOSO) {
            liveMin = liveMinuteStr
            val s = testScorers[MATCH_ID_AMISTOSO]
            homeScr = s?.first ?: emptyList()
            awayScr = s?.second ?: emptyList()
        } else {
            liveMin = null
            homeScr = emptyList()
            awayScr = emptyList()
        }

        return MatchDisplay(
            id = match.id, dateLabel = dateLabel, time = time,
            homeTeam = match.homeTeam, awayTeam = match.awayTeam,
            homeFlag = ExcelParser.getFlagEmoji(match.homeTeam),
            awayFlag = ExcelParser.getFlagEmoji(match.awayTeam),
            homeGoals = match.homeGoals, awayGoals = match.awayGoals,
            predictedHomeGoals = match.predictedHomeGoals,
            predictedAwayGoals = match.predictedAwayGoals,
            pointsEarned = match.pointsEarned,
            groupLabel = match.groupName, status = status, tvChannel = match.tvChannel,
            liveMinute = liveMin,
            homeScorers = homeScr,
            awayScorers = awayScr
        )
    }

    private fun matchStatus(match: MatchEntity): MatchStatus {
        val hasLiveMin = testLiveMinutes[match.id]?.let { it > 0 } == true
        if (hasLiveMin) return MatchStatus.LIVE
        if (match.homeGoals != null && match.awayGoals != null) return MatchStatus.FINISHED
        val start = parseMadridDate(match.dateTime) ?: return MatchStatus.UPCOMING
        val now = Date()
        val end = Date(start.time + 150L * 60 * 1000)
        return if (now.after(start) && now.before(end)) MatchStatus.LIVE else MatchStatus.UPCOMING
    }

    private fun hardcodedMatchDates(): Map<Int, List<String>> {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        fmt.timeZone = madridTZ

        data class Md(val date: String, val tv: String, val home: String, val away: String)
        val data = mutableMapOf<Int, Md>()

        // Group A: Mexico (A), South Korea (A), South Africa (A), Czech Republic (A)
        // Group B: Canada (B), Switzerland (B), Qatar (B), Bosnia-Herzegovina (B)
        // Group C: Brazil (C), Scotland (C), Morocco (C), Haiti (C)
        // Group D: United States (D), Paraguay (D), Turkey (D), Australia (D)
        // Group E: Germany (E), Curacao (E), Ivory Coast (E), Ecuador (E)
        // Group F: Netherlands (F), Japan (F), Sweden (F), Tunisia (F)
        // Group G: Belgium (G), Egypt (G), Iran (G), New Zealand (G)
        // Group H: Spain (H), Cape Verde (H), Saudi Arabia (H), Uruguay (H)
        // Group I: France (I), Senegal (I), Norway (I), Iraq (I)
        // Group J: Argentina (J), Jordan (J), Austria (J), Algeria (J)
        // Group K: Portugal (K), Uzbekistan (K), Colombia (K), Congo DR (K)
        // Group L: England (L), Croatia (L), Panama (L), Ghana (L)

        data[1] = Md("2026-06-11T21:00:00", "RTVE", "México", "Sudáfrica")
        data[2] = Md("2026-06-12T04:00:00", "DAZN", "Corea del Sur", "República Checa")
        data[3] = Md("2026-06-18T18:00:00", "DAZN", "República Checa", "Sudáfrica")
        data[4] = Md("2026-06-19T03:00:00", "DAZN", "México", "Corea del Sur")
        data[5] = Md("2026-06-24T21:00:00", "DAZN", "República Checa", "México")
        data[6] = Md("2026-06-24T21:00:00", "DAZN", "Sudáfrica", "Corea del Sur")

        data[7] = Md("2026-06-12T21:00:00", "RTVE", "Canadá", "Bosnia y Herzegovina")
        data[8] = Md("2026-06-13T03:00:00", "DAZN", "Catar", "Suiza")
        data[9] = Md("2026-06-18T21:00:00", "RTVE", "Suiza", "Bosnia y Herzegovina")
        data[10] = Md("2026-06-19T00:00:00", "DAZN", "Canadá", "Catar")
        data[11] = Md("2026-06-25T03:00:00", "DAZN", "Suiza", "Canadá")
        data[12] = Md("2026-06-25T03:00:00", "DAZN", "Bosnia y Herzegovina", "Catar")

        data[13] = Md("2026-06-13T21:00:00", "DAZN", "Brasil", "Marruecos")
        data[14] = Md("2026-06-19T21:00:00", "RTVE", "Escocia", "Marruecos")
        data[15] = Md("2026-06-20T00:00:00", "DAZN", "Brasil", "Haití")
        data[16] = Md("2026-06-20T03:00:00", "DAZN", "Haití", "Escocia")
        data[17] = Md("2026-06-20T06:00:00", "DAZN", "Escocia", "Brasil")
        data[18] = Md("2026-06-25T03:00:00", "DAZN", "Marruecos", "Haití")

        data[19] = Md("2026-06-14T00:00:00", "RTVE", "Estados Unidos", "Paraguay")
        data[20] = Md("2026-06-14T03:00:00", "DAZN", "Australia", "Turquía")
        data[21] = Md("2026-06-14T06:00:00", "DAZN", "Estados Unidos", "Australia")
        data[22] = Md("2026-06-20T19:00:00", "RTVE", "Turquía", "Paraguay")
        data[23] = Md("2026-06-20T22:00:00", "DAZN", "Turquía", "Estados Unidos")
        data[24] = Md("2026-06-25T03:00:00", "RTVE", "Paraguay", "Australia")

        data[25] = Md("2026-06-14T19:00:00", "RTVE", "Alemania", "Curazao")
        data[26] = Md("2026-06-14T22:00:00", "DAZN", "Costa de Marfil", "Ecuador")
        data[27] = Md("2026-06-20T19:00:00", "RTVE", "Alemania", "Costa de Marfil")
        data[28] = Md("2026-06-20T22:00:00", "DAZN", "Ecuador", "Curazao")
        data[29] = Md("2026-06-21T02:00:00", "DAZN", "Curazao", "Costa de Marfil")
        data[30] = Md("2026-06-21T06:00:00", "DAZN", "Ecuador", "Alemania")

        data[31] = Md("2026-06-15T01:00:00", "DAZN", "Países Bajos", "Japón")
        data[32] = Md("2026-06-15T04:00:00", "DAZN", "Suecia", "Túnez")
        data[33] = Md("2026-06-20T19:00:00", "RTVE", "Países Bajos", "Suecia")
        data[34] = Md("2026-06-20T22:00:00", "DAZN", "Túnez", "Japón")
        data[35] = Md("2026-06-25T23:00:00", "DAZN", "Japón", "Suecia")
        data[36] = Md("2026-06-25T23:00:00", "DAZN", "Túnez", "Países Bajos")

        data[37] = Md("2026-06-15T18:00:00", "RTVE", "Bélgica", "Egipto")
        data[38] = Md("2026-06-15T21:00:00", "DAZN", "Irán", "Nueva Zelanda")
        data[39] = Md("2026-06-21T18:00:00", "RTVE", "Bélgica", "Irán")
        data[40] = Md("2026-06-21T21:00:00", "DAZN", "Nueva Zelanda", "Egipto")
        data[41] = Md("2026-06-27T02:00:00", "DAZN", "Egipto", "Irán")
        data[42] = Md("2026-06-27T02:00:00", "DAZN", "Nueva Zelanda", "Bélgica")

        data[43] = Md("2026-06-16T00:00:00", "DAZN", "España", "Cabo Verde")
        data[44] = Md("2026-06-16T03:00:00", "DAZN", "Arabia Saudita", "Uruguay")
        data[45] = Md("2026-06-21T18:00:00", "RTVE", "España", "Arabia Saudita")
        data[46] = Md("2026-06-21T21:00:00", "DAZN", "Uruguay", "Cabo Verde")
        data[47] = Md("2026-06-26T21:00:00", "DAZN", "Cabo Verde", "Arabia Saudita")
        data[48] = Md("2026-06-26T21:00:00", "RTVE", "Uruguay", "España")

        data[49] = Md("2026-06-16T21:00:00", "RTVE", "Francia", "Senegal")
        data[50] = Md("2026-06-17T00:00:00", "DAZN", "Irak", "Noruega")
        data[51] = Md("2026-06-22T21:00:00", "RTVE", "Francia", "Irak")
        data[52] = Md("2026-06-22T23:00:00", "DAZN", "Noruega", "Senegal")
        data[53] = Md("2026-06-26T19:00:00", "DAZN", "Noruega", "Francia")
        data[54] = Md("2026-06-26T19:00:00", "DAZN", "Senegal", "Irak")

        data[55] = Md("2026-06-17T01:00:00", "RTVE", "Argentina", "Argelia")
        data[56] = Md("2026-06-17T04:00:00", "DAZN", "Austria", "Jordania")
        data[57] = Md("2026-06-22T17:00:00", "RTVE", "Argentina", "Austria")
        data[58] = Md("2026-06-22T20:00:00", "DAZN", "Jordania", "Argelia")
        data[59] = Md("2026-06-27T23:30:00", "DAZN", "Argelia", "Austria")
        data[60] = Md("2026-06-27T23:30:00", "RTVE", "Jordania", "Argentina")

        data[61] = Md("2026-06-17T19:00:00", "DAZN", "Portugal", "RD Congo")
        data[62] = Md("2026-06-17T22:00:00", "RTVE", "Uzbekistán", "Colombia")
        data[63] = Md("2026-06-23T17:00:00", "RTVE", "Portugal", "Uzbekistán")
        data[64] = Md("2026-06-23T20:00:00", "DAZN", "Colombia", "RD Congo")
        data[65] = Md("2026-06-27T23:30:00", "DAZN", "Colombia", "Portugal")
        data[66] = Md("2026-06-27T23:30:00", "RTVE", "RD Congo", "Uzbekistán")

        data[67] = Md("2026-06-17T20:00:00", "RTVE", "Inglaterra", "Croacia")
        data[68] = Md("2026-06-17T23:00:00", "DAZN", "Ghana", "Panamá")
        data[69] = Md("2026-06-23T20:00:00", "RTVE", "Inglaterra", "Ghana")
        data[70] = Md("2026-06-23T23:00:00", "DAZN", "Panamá", "Croacia")
        data[71] = Md("2026-06-27T21:00:00", "RTVE", "Panamá", "Inglaterra")
        data[72] = Md("2026-06-27T21:00:00", "DAZN", "Croacia", "Ghana")

        val result = mutableMapOf<Int, List<String>>()
        for (id in 1..72) {
            val md = data[id]
            if (md != null) result[id] = listOf(md.date, md.tv, md.home, md.away)
        }
        return result
    }

    private fun enrichScheduleFallback() {
        val fallbackDates = hardcodedMatchDates()
        TvScraper.clearCache()

        cachedMatches = cachedMatches.map { match ->
            val fb = fallbackDates[match.id]
            val date = fb?.getOrNull(0) ?: match.dateTime
            val tv = TvScraper.lookupTv(match.homeTeam, match.awayTeam, context.filesDir)
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
