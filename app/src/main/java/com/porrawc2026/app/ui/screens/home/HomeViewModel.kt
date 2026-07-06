package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.local.entity.TeamEntity
import com.porrawc2026.app.data.remote.LiveScoreService
import com.porrawc2026.app.data.remote.LiveScoreUpdate
import com.porrawc2026.app.data.remote.MatchScheduleProvider
import com.porrawc2026.app.data.remote.ShootoutAttempt
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.KnockoutCalculator
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.domain.usecase.CheckAppUpdateUseCase
import com.porrawc2026.app.domain.usecase.InstallResult
import com.porrawc2026.app.data.remote.LiveScorer
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.GoalEventBus
import com.porrawc2026.app.util.GoalNotifier
import com.porrawc2026.app.util.GsonHolder
import com.porrawc2026.app.util.DateTimeUtil
import com.porrawc2026.app.util.LiveMatchStore
import com.porrawc2026.app.util.LogManager
import com.porrawc2026.app.util.PlayerPhotoDownloader
import com.porrawc2026.app.util.PrefsManager
import com.porrawc2026.app.util.ValidationResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

enum class MatchStatus { UPCOMING, LIVE, FINISHED }

data class GoalEvent(val playerName: String, val minute: Int, val minuteLabel: String? = null)

data class ShootoutAttemptDisplay(
    val playerName: String,
    val isScored: Boolean,
    val isHome: Boolean
)

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
    val awayScorers: List<GoalEvent> = emptyList(),
    val dayKey: String = "",
    val sortKey: String = "",
    val isKnockout: Boolean = false,
    val hasKnockoutPred: Boolean = false,
    val winnerTeam: String? = null,
    val homeShootoutScore: Int = 0,
    val awayShootoutScore: Int = 0,
    val homePossibleTeams: String = "",
    val awayPossibleTeams: String = "",
    val shootoutAttempts: List<ShootoutAttemptDisplay> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val liveScoreService: LiveScoreService,
    private val prefsManager: PrefsManager,
    private val goalEventBus: GoalEventBus,
    private val liveMatchStore: LiveMatchStore,
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {



    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()
    private val _advancementPoints = MutableStateFlow(0)
    val advancementPoints: StateFlow<Int> = _advancementPoints.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()
    private val _hasData = MutableStateFlow(false)
    val hasData: StateFlow<Boolean> = _hasData.asStateFlow()
    private val _upcomingMatches = MutableStateFlow<List<MatchDisplay>>(emptyList())
    val upcomingMatches: StateFlow<List<MatchDisplay>> = _upcomingMatches.asStateFlow()
    private val _yesterdayMatches = MutableStateFlow<List<MatchDisplay>>(emptyList())
    val yesterdayMatches: StateFlow<List<MatchDisplay>> = _yesterdayMatches.asStateFlow()
    private val _sectionTitle = MutableStateFlow("")
    val sectionTitle: StateFlow<String> = _sectionTitle.asStateFlow()
    private val _players = MutableStateFlow<List<PlayerPredictionEntity>>(emptyList())
    val players: StateFlow<List<PlayerPredictionEntity>> = _players.asStateFlow()
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    private val _isBusy = MutableStateFlow(true)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()
    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()
    private val _appVersion = MutableStateFlow("")
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()
    private val _excelFileName = MutableStateFlow<String?>(null)
    val excelFileName: StateFlow<String?> = _excelFileName.asStateFlow()
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    private val _allMatches = MutableStateFlow<List<MatchDisplay>>(emptyList())
    val allMatches: StateFlow<List<MatchDisplay>> = _allMatches.asStateFlow()
    private val _dayKeys = MutableStateFlow<List<String>>(emptyList())
    val dayKeys: StateFlow<List<String>> = _dayKeys.asStateFlow()
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    @Volatile
    private var cachedMatches: List<MatchEntity> = emptyList()
    private val lastWrittenScores = ConcurrentHashMap<Int, Pair<Int, Int>>()
    private val goalScorers = ConcurrentHashMap<Int, Pair<List<GoalEvent>, List<GoalEvent>>>()
    private val shootoutAttempts = ConcurrentHashMap<Int, List<ShootoutAttempt>>()
    private val liveMinutes = ConcurrentHashMap<Int, String>()
    private var knockoutPredictionMap = mapOf<Int, Boolean>()
    private var knockoutPointsMap = mapOf<Int, Int>()
    private var matchPointsMap = mapOf<Int, Int>()
    private val seenScorers = ConcurrentHashMap<Int, MutableSet<String>>()
    private val notifiedScorers = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val processedGoalKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val relevantMatchIds = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    init {
        LogManager.init(context)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
                _appVersion.value = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
            }.onFailure { Log.e(TAG, "Failed to init PDFBox or get app version", it) }
        }
        viewModelScope.launch {
            runCatching {
                _excelFileName.value = prefsManager.getExcelFileNameSync()
                _notificationsEnabled.value = prefsManager.getNotificationsSync()
                _userName.value = prefsManager.getUserNameSync()
                liveMinutes.putAll(liveMatchStore.liveMinutes)
                goalScorers.putAll(liveMatchStore.goalScorers)
            }.onFailure { Log.e(TAG, "Failed to load prefs on init", it) }
        }
        refreshPoints(); loadPlayers(); preloadSchedule()
        forceCheckUpdate()
    }

    private fun preloadSchedule() {
        _isBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val staticSchedule = MatchScheduleProvider.buildMatchEntities()
                cachedMatches = staticSchedule
                val dbMatches = repository.getAllMatches().first()

                if (dbMatches.isNotEmpty()) {
                    cachedMatches = dbMatches
                    val dbIds = dbMatches.map { it.id }.toSet()
                    val newMatches = staticSchedule.filter { it.id !in dbIds }
                    if (newMatches.isNotEmpty()) {
                        cachedMatches = (cachedMatches + newMatches).sortedBy { it.id }
                        repository.insertMatches(newMatches)
                    }
                } else {
                    repository.insertMatches(staticSchedule)
                    prefsManager.setCacheTimestamp(System.currentTimeMillis())
                }
                // Apply hardcoded Octavos/Cuartos team names from static schedule if DB has blanks
                cachedMatches = cachedMatches.map { m ->
                    if (m.id in 89..100 && m.homeTeam.isBlank() && m.awayTeam.isBlank()) {
                        val ss = staticSchedule.firstOrNull { it.id == m.id }
                        if (ss != null && ss.homeTeam.isNotBlank())
                            m.copy(homeTeam = ss.homeTeam, awayTeam = ss.awayTeam)
                        else m
                    } else m
                }
                // Persist the applied team names
                val toUpdate = cachedMatches.filter { it.id in 89..100 && it.homeTeam.isNotBlank() }
                repository.insertMatches(toUpdate)
                // Migration: reset dieciseisavo placeholder names ("2º Grupo A") to empty,
                // and force correct dates from schedule so findMatchByDate matches API times
                fixDieciseisavoDatesAndNames()
                enrichSchedule()
                recalcAllPoints()
                refreshPoints()
                loadPlayers()
                processedGoalKeys.addAll(prefsManager.getProcessedGoalKeys())
                // Don't generate dieciseisavo names yet — wait for API data
                lastWrittenScores.clear()
                refreshUpcomingMatches()
                _hasData.value = true
                _isReady.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error in preloadSchedule", e)
                LogManager.log(TAG, "Error in preloadSchedule", e)
            } finally {
                _isBusy.value = false
            }
        }
        // Slow operations (API calls, photos) en segundo plano, sin bloquear la UI
        viewModelScope.launch(Dispatchers.IO) {
            fetchLiveResults()
            downloadPlayerPhotos()
            precachePhotosInBackground()
        }
        // Polling: cada minuto si hay live (events endpoint), cada hora para descubrir partidos nuevos (scoreboard)
        viewModelScope.launch(Dispatchers.IO) {
            var lastFullFetch = System.currentTimeMillis()
            var lastDay = LocalDate.now(madridZone)
            while (true) {
                try {
                    delay(60_000L)
                    val hasLive = hasLiveMatches()
                    if (hasLive) {
                        fetchLiveMatchUpdates()
                    }
                    val now = System.currentTimeMillis()
                    if (now - lastFullFetch >= FETCH_COOLDOWN_MS) {
                        lastFullFetch = now
                        fetchLiveResults()
                    }
                    val today = LocalDate.now(madridZone)
                    if (today != lastDay) {
                        lastDay = today
                        LogManager.log(TAG, "Day changed to $today, refreshing matches")
                        refreshUpcomingMatches()
                    }
                } catch (e: Exception) {
                    LogManager.log(TAG, "Polling error: ${e.message}")
                }
            }
        }
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            repository.getPlayerPredictions().collect { dbPlayers ->
                _players.value = dbPlayers.sortedBy { it.rank }
            }
        }
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _isBusy.value = true
            _validationResult.value = null
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val displayName = cursor.getString(nameIndex)
                            _excelFileName.value = displayName
                            prefsManager.setExcelFileName(displayName)
                            extractUserName(displayName)?.let {
                                _userName.value = it
                                prefsManager.setUserName(it)
                            }
                        }
                    }
                }
                val data = ExcelParser.parse(context, uri)
                val validation = ExcelParser.validate()
                _validationResult.value = validation
                if (validation.isValid) {
                    repository.insertAllData(
                        data.teams, data.matches, data.questions,
                        data.playerPredictions, data.knockoutPredictions, data.standings
                    )
                    cachedMatches = data.matches
                    lastWrittenScores.clear()
                    enrichSchedule()
                    recalcAllPoints()
                    refreshPoints()
                    refreshAll()
                    _hasData.value = true
                    loadPlayers()
                    downloadPlayerPhotos()
                }
            }.onFailure { e ->
                viewModelScope.launch { _errorMessage.emit("Error al cargar el Excel: ${e.message}") }
            }
            _isLoading.value = false
            _isBusy.value = false
        }
    }

    fun dismissValidation() { _validationResult.value = null }

    fun forceCheckUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val info = checkAppUpdateUseCase.check()
                _updateAvailable.value = info?.isNewer == true
            }.onFailure { Log.e(TAG, "Failed to check for update", it) }
        }
    }

    fun toggleNotifications() {
        _notificationsEnabled.value = !_notificationsEnabled.value
        viewModelScope.launch { prefsManager.setNotificationsEnabled(_notificationsEnabled.value) }
    }

    @Volatile
    private var lastFetchTime: Long = 0L
    private val FETCH_COOLDOWN_MS = 60 * 60 * 1000L

    fun refreshLiveScores() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            if (hasLiveMatches()) {
                fetchLiveMatchUpdates()
            } else {
                refreshAll()
            }
            _isBusy.value = false
        }
    }

    fun clearAndRefreshCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            repository.clearAllMatchScores()
            repository.resetAllPlayerGoals()
            val refreshed = repository.getPlayerPredictionsList().sortedBy { it.rank }
            _players.value = refreshed
            prefsManager.clearProcessedGoalKeys()
            goalScorers.clear()
            liveMinutes.clear()
            lastWrittenScores.clear()
            notifiedScorers.clear()
            seenScorers.clear()
            processedGoalKeys.clear()
            liveMatchStore.goalScorers.clear()
            liveMatchStore.liveMinutes.clear()
            fixDieciseisavoDatesAndNames()
            fetchLiveResults(fullFetch = true)
            _isBusy.value = false
        }
    }

    fun installUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdating.value = true
            runCatching {
                when (val result = checkAppUpdateUseCase.install(context)) {
                    is InstallResult.Success -> _appVersion.value = result.version
                    is InstallResult.Error -> _errorMessage.emit(result.message)
                }
            }.onFailure { e ->
                _errorMessage.emit("Error al actualizar: ${e.message}")
            }
            _isUpdating.value = false
        }
    }

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            repository.insertAllData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            repository.saveKnockoutTeamProgress(emptyList())
            cachedMatches = cachedMatches.map { match ->
                match.copy(predictedHomeGoals = null, predictedAwayGoals = null, pointsEarned = 0, homeGoals = null, awayGoals = null)
            }
            lastWrittenScores.clear()
            _hasData.value = false
            _totalPoints.value = 0
            _players.value = emptyList()
            _excelFileName.value = null
            prefsManager.setExcelFileName(null)
            refreshUpcomingMatches()
            _isBusy.value = false
        }
    }

    fun refreshPoints() {
        viewModelScope.launch {
            val adv = _advancementPoints.value
            _totalPoints.value = repository.calculateTotalPoints(adv)
        }
    }

    private fun downloadPlayerPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            val predictions = repository.getPlayerPredictionsList()
            coroutineScope {
                predictions.map { p ->
                    async {
                        val name = p.predictedName ?: return@async
                        if (!p.photoPath.isNullOrBlank()) return@async
                        val path = PlayerPhotoDownloader.download(context, name)
                        if (path != null) {
                            val updated = p.copy(photoPath = path)
                            repository.updatePlayerPrediction(updated)
                        }
                    }
                }
            }
            loadPlayers()
            _isBusy.value = false
        }
    }

    private suspend fun precachePhotosInBackground() {
        PlayerPhotoDownloader.precacheTopPlayers(context)
    }

    private suspend fun recalcAllPoints() {
        val koPredictions = repository.getKnockoutPredictions().first()
        knockoutPredictionMap = koPredictions.associate { it.matchNumber to when (it.matchNumber) { 103, 104 -> true; else -> it.winner != null } }

        // Group/match prediction points (10+10+30)
        cachedMatches = cachedMatches.map { m ->
            val pts = PointsCalculator.calculateMatchPoints(m)
            if (pts != m.pointsEarned) repository.updateMatchPoints(m.id, pts)
            m.copy(pointsEarned = pts)
        }

        // Build live round lists (with provisional teams during live matches)
        val liveRoundLists = KnockoutCalculator.buildLiveRoundLists(cachedMatches)

        // Compute knockout points per prediction using live round lists
        val (allMatchPoints, _) = KnockoutCalculator.computePointsFromLiveLists(
            koPredictions, liveRoundLists, cachedMatches
        )

        // For Octavos+ matches (89-104), compute points based on next-round predictions (0/1/2 logic)
        val nextRoundPoints = KnockoutCalculator.computeNextRoundMatchPoints(
            koPredictions, cachedMatches
        )

        // Combine: Dieciseisavos (73-88) uses old logic, Octavos+ uses new 0/1/2 logic
        matchPointsMap = mutableMapOf<Int, Int>().apply {
            putAll(allMatchPoints.filterKeys { it in 73..88 })
            putAll(nextRoundPoints)
        }
        knockoutPointsMap = matchPointsMap

        // Save points to knockout_predictions table so total points calculation works
        for (prediction in koPredictions) {
            val pts = knockoutPointsMap[prediction.matchNumber] ?: 0
            repository.updateKnockoutPredictionPoints(prediction.matchNumber, pts)
        }

        _advancementPoints.value = 0
    }

    private suspend fun checkGoalNotifications() {
        if (!_notificationsEnabled.value) return
        val predictedNames = _players.value.mapNotNull { it.predictedName }.map { it.lowercase() }.toSet()
        if (predictedNames.isEmpty()) return
        var anyNotified = false
        for ((matchId, pair) in goalScorers) {
            val status = liveMinutes[matchId]
            if (status == null || status == "FINAL") continue
            val seen = seenScorers.getOrPut(matchId) { mutableSetOf() }
            for (scorer in pair.first + pair.second) {
                val name = scorer.playerName.lowercase().trim()
                val key = "$name:${scorer.minute}"
                val notificationKey = "notif:$matchId:$key"
                if (notificationKey !in processedGoalKeys && seen.add(key)) {
                    val normalizedName = normalizeName(name)
                    val matches = predictedNames.any { pred ->
                        val normalizedPred = normalizeName(pred)
                        normalizedName.contains(normalizedPred) || normalizedPred.contains(normalizedName)
                    }
                    if (matches) {
                        val displayName = scorer.playerName.split(" ").last()
                        GoalNotifier.notifyGoal(context, displayName)
                        processedGoalKeys.add(notificationKey)
                        anyNotified = true
                    }
                }
            }
        }
        if (anyNotified) {
            prefsManager.setProcessedGoalKeys(processedGoalKeys.toSet())
        }
    }

    private fun enrichSchedule() {
        cachedMatches = MatchScheduleProvider.enrichSchedule(cachedMatches)
    }

    private suspend fun fixDieciseisavoDatesAndNames() {
        val schedule = MatchScheduleProvider.getDieciseisavosSchedule()
        val now = Instant.now()
        // For matches that already started, preserve team names (they come from API).
        // Only reset placeholder names for future matches or empty names.
        cachedMatches = cachedMatches.map { m ->
            if (m.id in 73..88) {
                val sched = schedule[m.id]
                if (sched != null) {
                    val isPlaceholder = m.homeTeam.contains("Grupo") || m.homeTeam.contains("/") ||
                                        m.awayTeam.contains("Grupo") || m.awayTeam.contains("/")
                    val hasRealNames = !isPlaceholder && m.homeTeam.isNotBlank() && m.awayTeam.isNotBlank()
                    val matchStart = try { parseMadridInstant(m.dateTime) } catch (e: Exception) { null }
                    val alreadyStarted = matchStart != null && !matchStart.isAfter(now)
                    if (hasRealNames && alreadyStarted) {
                        // Preserve API-set names and results, just ensure correct date
                        m.copy(dateTime = sched.date)
                    } else {
                        m.copy(
                            homeTeam = "",
                            awayTeam = "",
                            dateTime = sched.date,
                            homeGoals = null, awayGoals = null,
                            homeScorers = null, awayScorers = null,
                            homeRedCards = null, awayRedCards = null,
                            homeYellowCards = null, awayYellowCards = null,
                            homeMissedPenalties = 0, awayMissedPenalties = 0,
                            winnerTeam = null,
                            homeHeadedGoals = 0, awayHeadedGoals = 0,
                            hasSubGoal = false,
                            homeShootoutScore = 0, awayShootoutScore = 0,
                            pointsEarned = 0
                        )
                    }
                } else m
            } else if (m.id in 89..100) {
                val hasRealNames = m.homeTeam.isNotBlank() && m.awayTeam.isNotBlank()
                if (hasRealNames) m else m.copy(
                    homeTeam = "", awayTeam = "",
                    homeGoals = null, awayGoals = null,
                    homeScorers = null, awayScorers = null,
                    homeRedCards = null, awayRedCards = null,
                    homeYellowCards = null, awayYellowCards = null,
                    homeMissedPenalties = 0, awayMissedPenalties = 0,
                    winnerTeam = null,
                    homeHeadedGoals = 0, awayHeadedGoals = 0,
                    hasSubGoal = false,
                    homeShootoutScore = 0, awayShootoutScore = 0,
                    pointsEarned = 0
                )
            } else m
        }
        // Only persist the cleared data; preserve API-set scores for already-started matches
        val toPersist = cachedMatches.filter { it.id in 73..100 }
        repository.insertMatches(toPersist)
    }

    

    fun parseMadridInstant(dateTime: String): Instant? =
        DateTimeUtil.parseMadridInstant(dateTime, TAG)

    private fun getTodayMatchesWithDates(): List<MatchEntity> {
        val todayZoned = LocalDate.now(madridZone)
        return cachedMatches.filter { m ->
            val d = parseMadridInstant(m.dateTime) ?: return@filter false
            val matchDate = d.atZone(madridZone).toLocalDate()
            matchDate == todayZoned
        }
    }

    private suspend fun refreshAll() {
        fetchLiveResults()
    }

    private suspend fun <T> fetchWithRetry(maxAttempts: Int = 10, block: suspend () -> T?): T? {
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            val result = runCatching { block() }
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) return data
                lastError = null
            } else {
                lastError = result.exceptionOrNull()
            }
            if (attempt < maxAttempts - 1) delay(2000L shl attempt.coerceAtMost(4))
        }
        if (lastError != null) Log.e(TAG, "fetchWithRetry exhausted after $maxAttempts attempts", lastError!!)
        return null
    }

    private fun loadCachedScorers(): Set<Int> {
        val cachedIds = mutableSetOf<Int>()
        for (match in cachedMatches) {
            val homeRaw = match.homeScorers
            val awayRaw = match.awayScorers
            if (homeRaw != null && awayRaw != null) {
                try {
                    val homeList: List<LiveScorer> = gson.fromJson(homeRaw, scorerListType) ?: emptyList()
                    val awayList: List<LiveScorer> = gson.fromJson(awayRaw, scorerListType) ?: emptyList()
                    val homeGoals = homeList.map { GoalEvent(it.playerName, it.minute, it.minuteLabel) }
                    val awayGoals = awayList.map { GoalEvent(it.playerName, it.minute, it.minuteLabel) }
                    goalScorers[match.id] = Pair(homeGoals, awayGoals)
                    liveMatchStore.goalScorers[match.id] = Pair(homeGoals, awayGoals)
                    cachedIds.add(match.id)
                } catch (e: Exception) {
                Log.e(TAG, "Error loading cached scorers for match ${match.id}", e)
                LogManager.log(TAG, "Error loading cached scorers for match ${match.id}", e)
            }
            }
        }
        return cachedIds
    }

    private fun hasLiveMatches(): Boolean {
        return cachedMatches.any { matchStatus(it) == MatchStatus.LIVE }
    }

    private fun isFinishedByTime(match: MatchEntity): Boolean {
        val start = parseMadridInstant(match.dateTime) ?: return false
        return match.homeGoals != null && match.awayGoals != null &&
            Instant.now().isAfter(start.plusSeconds(MATCH_WINDOW_SECONDS))
    }

    private suspend fun fetchLiveResults(fullFetch: Boolean = false) {
        val wcMatches = filterMatchesForFetch(fullFetch) ?: return
        val ids = wcMatches.map { it.id }.sorted()
        val dates = wcMatches.mapNotNull { it.dateTime.take(10) }.distinct().sorted()
        LogManager.log("HomeVM", "Fetching match IDs=$ids dates=$dates")
        val scoreUpdates = fetchWithRetry { liveScoreService.fetchScoreUpdates(wcMatches) }.orEmpty()
        if (scoreUpdates.isNotEmpty()) {
            LogManager.log("HomeVM", "API returned ${scoreUpdates.size} updates: ${scoreUpdates.map { "${it.matchId}:${it.apiHomeTeam?:""} ${it.homeGoals}-${it.awayGoals} ${it.apiAwayTeam?:""}" }}")
            val canadaMatches = scoreUpdates.filter { it.matchId in 7..12 }
            if (canadaMatches.isNotEmpty()) {
                LogManager.log("HomeVM", "Group B from API:")
                canadaMatches.forEach { m ->
                    LogManager.log("HomeVM", "  #${m.matchId}: ${m.apiHomeTeam} ${m.homeGoals}-${m.awayGoals} ${m.apiAwayTeam} finished=${m.isFinished}")
                }
            }
        } else {
            LogManager.log("HomeVM", "API returned 0 updates for date range ${dates.firstOrNull()}..${dates.lastOrNull()}")
        }
        val canadaCached = cachedMatches.filter { it.id in 7..12 }
        LogManager.log("HomeVM", "Group B cached scores:")
        canadaCached.forEach { m ->
            LogManager.log("HomeVM", "  #${m.id}: ${m.homeTeam} ${m.homeGoals}-${m.awayGoals} ${m.awayTeam}")
        }
        processScoreUpdates(scoreUpdates)

        // Backfill: KO matches that finished without shootout data — only recent ones
        val twoDaysAgo = Instant.now().minusSeconds(2 * 24 * 60 * 60)
        val koNoShootout = cachedMatches.filter {
            val d = parseMadridInstant(it.dateTime)
            it.id in 73..104 && d != null && d.isAfter(twoDaysAgo) &&
            it.homeGoals != null && it.awayGoals != null &&
            isFinishedByTime(it) && it.homeShootoutScore == 0 && it.awayShootoutScore == 0
        }
        if (koNoShootout.isNotEmpty()) {
            LogManager.log("HomeVM", "Backfilling shootout for KO matches: ${koNoShootout.map { it.id }}")
            val fullUpdates = koNoShootout.mapNotNull { liveScoreService.fetchFullScoreByEspnId(it) }
            if (fullUpdates.isNotEmpty()) {
                processScoreUpdates(fullUpdates)
            }
        }

        notifyGoalEvents()
        recalculateAllPlayerGoals()
        refreshPoints()
        checkGoalNotifications()
        refreshUpcomingMatches()
    }

    private suspend fun fetchLiveMatchUpdates() {
        val liveMatches = cachedMatches.filter { matchStatus(it) == MatchStatus.LIVE && it.espnId != null }
        if (liveMatches.isEmpty()) return
        LogManager.log("HomeVM", "Fetching live match updates by espnId: ${liveMatches.map { "${it.id}:${it.espnId}" }}")
        val updates = liveMatches.mapNotNull { liveScoreService.fetchLiveScoreByEspnId(it) }
        if (updates.isEmpty()) return
        processScoreUpdates(updates)

        // For live KO matches (73-104), fetch summary endpoint for shootout data during penalties
        val liveKo = liveMatches.filter { it.id in 73..104 }
        if (liveKo.isNotEmpty()) {
            LogManager.log("HomeVM", "Fetching summary for live KO matches: ${liveKo.map { it.id }}")
            val shootoutUpdates = liveKo.mapNotNull { liveScoreService.fetchFullScoreByEspnId(it) }
                .filter { it.homeShootoutScore > 0 || it.awayShootoutScore > 0 }
            if (shootoutUpdates.isNotEmpty()) {
                LogManager.log("HomeVM", "Shootout updates: ${shootoutUpdates.map { "${it.matchId}:${it.homeShootoutScore}-${it.awayShootoutScore}" }}")
                processScoreUpdates(shootoutUpdates)
            }
        }

        // Backfill: if any knockout match just finished without shootout data, fetch full scoreboard data
        val finishedNoShootout = updates.filter {
            it.isFinished && it.matchId in 73..104 &&
            it.homeShootoutScore == 0 && it.awayShootoutScore == 0
        }
        if (finishedNoShootout.isNotEmpty()) {
            LogManager.log("HomeVM", "Backfilling shootout for finished matches: ${finishedNoShootout.map { it.matchId }}")
            val fullUpdates = finishedNoShootout.mapNotNull { update ->
                val match = cachedMatches.firstOrNull { it.id == update.matchId }
                if (match != null) liveScoreService.fetchFullScoreByEspnId(match) else null
            }
            if (fullUpdates.isNotEmpty()) {
                processScoreUpdates(fullUpdates)
            }
        }

        notifyGoalEvents()
        recalculateAllPlayerGoals()
        refreshPoints()
        checkGoalNotifications()
        refreshUpcomingMatches()
    }

    private suspend fun filterMatchesForFetch(fullFetch: Boolean): List<MatchEntity>? {
        val todayMatches = getTodayMatchesWithDates()
        val staleMatches = if (fullFetch) emptyList() else getStaleMatches()
        val koToFetch = if (fullFetch) emptyList() else {
            cachedMatches.filter {
                it.id in 73..104 && (
                    matchStatus(it) == MatchStatus.LIVE ||
                    getTodayMatchesWithDates().any { m -> m.id == it.id } ||
                    !isFinishedByTime(it)
                )
            }
        }
        val matchesForWc = if (fullFetch) {
            cachedMatches
        } else {
            (todayMatches + staleMatches + koToFetch).distinctBy { it.id }
        }
        if (matchesForWc.isEmpty()) return null

        // Load cached scorers into memory for display
        loadCachedScorers()

        // Finished matches with saved results: never fetch from API again, use DB only
        // But for KO matches, re-fetch if shootout scores are missing (Option B)
        val finishedInDb = matchesForWc.filter { match ->
            match.homeGoals != null && match.awayGoals != null && isFinishedByTime(match) &&
            !(match.id in 73..104 && match.homeShootoutScore == 0 && match.awayShootoutScore == 0)
        }.map { it.id }.toSet()

        val wcMatches = if (finishedInDb.size == matchesForWc.size) emptyList()
        else matchesForWc.filter { it.id !in finishedInDb }

        if (wcMatches.isEmpty()) {
            refreshUpcomingMatches()
            return null
        }
        return wcMatches
    }

    private fun getStaleMatches(): List<MatchEntity> {
        val now = Instant.now()
        return cachedMatches.filter { match ->
            val start = parseMadridInstant(match.dateTime) ?: return@filter false
            val matchEnd = start.plusSeconds(MATCH_WINDOW_SECONDS)
            now.isAfter(matchEnd) &&
                !isFinishedByTime(match) &&
                liveMinutes[match.id] != "FINAL"
        }
    }

    private suspend fun processScoreUpdates(scoreUpdates: List<LiveScoreUpdate>) {
        scoreUpdates.forEach { update ->
            if (update.isFinished) {
                liveMinutes[update.matchId] = "FINAL"
                liveMatchStore.liveMinutes[update.matchId] = "FINAL"
            } else if (update.liveMinute != null) {
                liveMinutes[update.matchId] = update.liveMinute
                liveMatchStore.liveMinutes[update.matchId] = update.liveMinute
            }
            val match = cachedMatches.firstOrNull { it.id == update.matchId }
            if (match != null) {
                val start = parseMadridInstant(match.dateTime)
                if (start != null && start.isAfter(Instant.now())) {
                    // Future match: update team names only (scores can't exist yet)
        if (update.matchId in 73..100 && update.apiHomeTeam != null && update.apiAwayTeam != null) {
                        val esHome = convertEspnRefToGanador(TeamNameNormalizer.enToEs(update.apiHomeTeam))
                        val esAway = convertEspnRefToGanador(TeamNameNormalizer.enToEs(update.apiAwayTeam))
                        if (match.homeTeam != esHome || match.awayTeam != esAway) {
                            LogManager.log("HomeVM", "Setting KO #${update.matchId} from API (future): ${match.homeTeam} vs ${match.awayTeam} → $esHome vs $esAway")
                            repository.updateMatchTeams(update.matchId, esHome, esAway)
                            cachedMatches = cachedMatches.map {
                                if (it.id == update.matchId) it.copy(homeTeam = esHome, awayTeam = esAway) else it
                            }
                            // Recalculate points when teams are updated
                            recalcAllPoints(); refreshPoints()
                        }
                        // Check if Spain is playing and add RTVE to tvChannel
                        val homeIsSpain = TeamNameNormalizer.matches(esHome, "España")
                        val awayIsSpain = TeamNameNormalizer.matches(esAway, "España")
                        if (homeIsSpain || awayIsSpain) {
                            val currentMatch = cachedMatches.firstOrNull { it.id == update.matchId }
                            if (currentMatch != null && !currentMatch.tvChannel.contains("RTVE")) {
                                val newTvChannel = if (currentMatch.tvChannel.isBlank()) "RTVE" else "${currentMatch.tvChannel},RTVE"
                                repository.updateMatchTvChannel(update.matchId, newTvChannel)
                                cachedMatches = cachedMatches.map {
                                    if (it.id == update.matchId) it.copy(tvChannel = newTvChannel) else it
                                }
                            }
                        }
                    }
                    // Save ESPN event ID for future fetches
                    if (update.espnId != null && match.espnId != update.espnId) {
                        repository.updateMatchEspnId(update.matchId, update.espnId)
                        cachedMatches = cachedMatches.map {
                            if (it.id == update.matchId) it.copy(espnId = update.espnId) else it
                        }
                    }
                    return@forEach
                }
            }
            // Update winnerTeam in cachedMatches BEFORE recalcAllPoints (called inside updateMatchScores)
            if (update.winnerTeam != null) {
                cachedMatches = cachedMatches.map {
                    if (it.id == update.matchId) it.copy(winnerTeam = update.winnerTeam) else it
                }
            }
            updateMatchScores(update)
            updateGoalScorers(update)
            if (update.shootoutAttempts.isNotEmpty()) {
                shootoutAttempts[update.matchId] = update.shootoutAttempts
            }
            if (update.isFinished) {
                repository.updateMatchCards(update.matchId, update.homeRedCards, update.awayRedCards, update.homeYellowCards, update.awayYellowCards)
                repository.updateMatchMissedPenalties(update.matchId, update.homeMissedPenalties, update.awayMissedPenalties)
                repository.updateMatchHeadedGoals(update.matchId, update.homeHeadedGoals, update.awayHeadedGoals)
                repository.updateMatchSubGoal(update.matchId, update.hasSubGoal)
                if (update.winnerTeam != null) {
                    repository.updateMatchWinner(update.matchId, update.winnerTeam)
                }
                if (update.homeShootoutScore > 0 || update.awayShootoutScore > 0) {
                    repository.updateMatchShootout(update.matchId, update.homeShootoutScore, update.awayShootoutScore)
                    cachedMatches = cachedMatches.map {
                        if (it.id == update.matchId) it.copy(
                            homeShootoutScore = update.homeShootoutScore,
                            awayShootoutScore = update.awayShootoutScore
                        ) else it
                    }
                }
            }
            // Dieciseisavo team names come from ESPN API in updateMatchScores()
            // tryGenerateDieciseisavos() preserves the API-set names.
        }
    }

    private suspend fun updateMatchScores(update: LiveScoreUpdate) {
        val prev = lastWrittenScores[update.matchId]
        if (prev == null || prev.first != update.homeGoals || prev.second != update.awayGoals) {
            lastWrittenScores[update.matchId] = update.homeGoals to update.awayGoals
            cachedMatches = cachedMatches.map {
                if (it.id == update.matchId) it.copy(homeGoals = update.homeGoals, awayGoals = update.awayGoals) else it
            }
            recalcAllPoints(); refreshPoints()
            if (update.isFinished) {
                repository.updateMatchResults(update.matchId, update.homeGoals, update.awayGoals)
            }
        }
        // For knockout matches, always use API team names (authoritative source), converted to Spanish
        if (update.matchId in 73..100 && update.apiHomeTeam != null && update.apiAwayTeam != null) {
            val esHome = convertEspnRefToGanador(TeamNameNormalizer.enToEs(update.apiHomeTeam))
            val esAway = convertEspnRefToGanador(TeamNameNormalizer.enToEs(update.apiAwayTeam))
            val match = cachedMatches.firstOrNull { it.id == update.matchId }
            if (match != null) {
                if (match.homeTeam != esHome || match.awayTeam != esAway) {
                    LogManager.log("HomeVM", "Setting KO #${update.matchId} from API: ${match.homeTeam} vs ${match.awayTeam} → $esHome vs $esAway")
                    repository.updateMatchTeams(update.matchId, esHome, esAway)
                    cachedMatches = cachedMatches.map {
                        if (it.id == update.matchId) it.copy(homeTeam = esHome, awayTeam = esAway) else it
                    }
                    // Recalculate points when teams are updated
                    recalcAllPoints(); refreshPoints()
                }
                // Check if Spain is playing and add RTVE to tvChannel
                val homeIsSpain = TeamNameNormalizer.matches(esHome, "España")
                val awayIsSpain = TeamNameNormalizer.matches(esAway, "España")
                if (homeIsSpain || awayIsSpain) {
                    val currentMatch = cachedMatches.firstOrNull { it.id == update.matchId }
                    if (currentMatch != null && !currentMatch.tvChannel.contains("RTVE")) {
                        val newTvChannel = if (currentMatch.tvChannel.isBlank()) "RTVE" else "${currentMatch.tvChannel},RTVE"
                        repository.updateMatchTvChannel(update.matchId, newTvChannel)
                        cachedMatches = cachedMatches.map {
                            if (it.id == update.matchId) it.copy(tvChannel = newTvChannel) else it
                        }
                    }
                }
            }
        }
        // Save ESPN event ID when received from API
        if (update.espnId != null) {
            val match = cachedMatches.firstOrNull { it.id == update.matchId }
            if (match != null && match.espnId != update.espnId) {
                repository.updateMatchEspnId(update.matchId, update.espnId)
                cachedMatches = cachedMatches.map {
                    if (it.id == update.matchId) it.copy(espnId = update.espnId) else it
                }
            }
        }
    }

    private suspend fun updateGoalScorers(update: LiveScoreUpdate) {
        val homeScr = update.homeScorers.map { GoalEvent(it.playerName, it.minute, it.minuteLabel) }
        val awayScr = update.awayScorers.map { GoalEvent(it.playerName, it.minute, it.minuteLabel) }
        if (homeScr.isNotEmpty() || awayScr.isNotEmpty()) {
            goalScorers[update.matchId] = Pair(homeScr, awayScr)
            liveMatchStore.goalScorers[update.matchId] = Pair(homeScr, awayScr)
            val homeJson = gson.toJson(update.homeScorers)
            val awayJson = gson.toJson(update.awayScorers)
            cachedMatches = cachedMatches.map {
                if (it.id == update.matchId) it.copy(homeScorers = homeJson, awayScorers = awayJson) else it
            }
            repository.updateMatchScorers(update.matchId, homeJson, awayJson)
        }
    }

    private suspend fun notifyGoalEvents() {
        for ((matchId, pair) in goalScorers) {
            if (relevantMatchIds.isNotEmpty() && matchId !in relevantMatchIds) continue
            var goalsChanged = false
            for (scorer in pair.first + pair.second) {
                val key = "$matchId:${scorer.playerName}:${scorer.minute}"
                if (notifiedScorers.add(key) && processedGoalKeys.add(key)) {
                    goalEventBus.notifyGoal()
                    goalsChanged = true
                }
            }
            if (goalsChanged) {
                prefsManager.setProcessedGoalKeys(processedGoalKeys.toSet())
            }
        }
    }

    private fun normalizeName(name: String): String {
        return Normalizer.normalize(name.lowercase().trim(), Normalizer.Form.NFD)
            .replace(diacriticsRegex, "")
            .replace(".", "").replace("-", " ").replace("'", " ")
            .replace(whitespaceRegex, " ").trim()
    }

    private fun lastWord(name: String): String {
        val parts = name.split(" ")
        return if (parts.size > 1) parts.last() else name
    }

    private suspend fun recalculateAllPlayerGoals() {
        val goalCounts = mutableMapOf<String, Int>()
        for (match in cachedMatches) {
            val homeRaw = match.homeScorers
            val awayRaw = match.awayScorers
            if (homeRaw != null) {
                try {
                    val scorers: List<LiveScorer> = gson.fromJson(homeRaw, scorerListType) ?: emptyList()
                    scorers.forEach { s -> goalCounts.merge(s.playerName, 1) { a, b -> a + b } }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing home scorers for match ${match.id}", e)
                    LogManager.log(TAG, "Error parsing home scorers for match ${match.id}", e)
                }
            }
            if (awayRaw != null) {
                try {
                    val scorers: List<LiveScorer> = gson.fromJson(awayRaw, scorerListType) ?: emptyList()
                    scorers.forEach { s -> goalCounts.merge(s.playerName, 1) { a, b -> a + b } }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing away scorers for match ${match.id}", e)
                    LogManager.log(TAG, "Error parsing away scorers for match ${match.id}", e)
                }
            }
        }
        val currentPlayers = repository.getPlayerPredictionsList()
        var anyChanged = false
        for (player in currentPlayers) {
            val predName = player.predictedName ?: continue
            val predNorm = normalizeName(predName)
            val predLast = lastWord(predNorm)
            var totalGoals = 0
            for ((scorerName, count) in goalCounts) {
                val scorerNorm = normalizeName(scorerName)
                val scorerLast = lastWord(scorerNorm)
                if (predNorm.contains(scorerLast) || scorerNorm.contains(predLast) || predLast == scorerLast) {
                    totalGoals += count
                }
            }
            val newPoints = totalGoals * player.pointsPerGoal
            if (totalGoals != player.goalsScored || newPoints != player.pointsEarned) {
                repository.updatePlayerPrediction(player.copy(goalsScored = totalGoals, pointsEarned = newPoints))
                anyChanged = true
            }
        }
        if (anyChanged) {
            _players.value = repository.getPlayerPredictionsList().sortedBy { it.rank }
        }
    }

    fun refreshUpcomingMatches() {
        val todayZoned = LocalDate.now(madridZone)
        val matches = cachedMatches

        val allSorted = matches
            .sortedBy { it.dateTime.ifBlank { "zzz" } }
        val maxVisibleId = MatchScheduleProvider.getMaxVisibleRoundId()
        val allDisplay = allSorted.map { match -> toDisplay(match) }
            .filter { it.homeTeam.isNotBlank() && it.awayTeam.isNotBlank() }
            .filter { it.id <= maxVisibleId }

        val yesterdayMatches = allDisplay.filter { display ->
            val d = parseMadridInstant(matches.first { it.id == display.id }.dateTime) ?: return@filter false
            val matchDate = d.atZone(madridZone).toLocalDate()
            matchDate == todayZoned.minusDays(1)
        }.sortedBy { display ->
            parseMadridInstant(matches.first { it.id == display.id }.dateTime)
        }
        _yesterdayMatches.value = yesterdayMatches

        val todayMatches = allDisplay.filter { display ->
            val d = parseMadridInstant(matches.first { it.id == display.id }.dateTime) ?: return@filter false
            val matchDate = d.atZone(madridZone).toLocalDate()
            matchDate == todayZoned
        }

        if (todayMatches.isNotEmpty()) {
            _sectionTitle.value = "HOY \u2014 ${todayMatches.first().dateLabel.uppercase()}"
            _upcomingMatches.value = todayMatches
        } else {
            val futureMatches = allDisplay.filter { display ->
                val d = parseMadridInstant(matches.first { it.id == display.id }.dateTime) ?: return@filter false
                d.isAfter(Instant.now())
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
        _allMatches.value = allDisplay
        _dayKeys.value = allDisplay
            .filter { it.dayKey.isNotBlank() }
            .map { it.dayKey }.distinct()
            .sortedBy { d -> allDisplay.firstOrNull { it.dayKey == d }?.sortKey ?: d }
    }

    private fun convertEspnRefToGanador(teamName: String): String {
        val r32Pattern = "Round of 32 (\\d+) Winner".toRegex()
        val r32Match = r32Pattern.find(teamName)
        if (r32Match != null) {
            val num = r32Match.groupValues[1].toIntOrNull() ?: return teamName
            val matchId = ESPN_R32_TO_MATCH_ID[num] ?: return teamName
            return "Ganador $matchId"
        }
        val r16Pattern = "Round of 16 (\\d+) Winner".toRegex()
        val r16Match = r16Pattern.find(teamName)
        if (r16Match != null) {
            val num = r16Match.groupValues[1].toIntOrNull() ?: return teamName
            val matchId = ESPN_R16_TO_MATCH_ID[num] ?: return teamName
            return "Ganador $matchId"
        }
        return teamName
    }

    private fun resolveTeamNameIfPossible(teamName: String): String {
        val refMatchId = extractRefMatchId(teamName) ?: return teamName
        val refMatch = cachedMatches.firstOrNull { it.id == refMatchId } ?: return teamName
        return getWinnerName(refMatch) ?: teamName
    }

    private fun getWinnerName(refMatch: MatchEntity): String? {
        val start = parseMadridInstant(refMatch.dateTime) ?: return null
        if (Instant.now().isBefore(start.plusSeconds(MATCH_WINDOW_SECONDS))) return null
        val apiWinner = refMatch.winnerTeam
        if (apiWinner != null) return TeamNameNormalizer.enToEs(apiWinner)
        val hg = refMatch.homeGoals ?: return null
        val ag = refMatch.awayGoals ?: return null
        return when {
            hg > ag -> refMatch.homeTeam
            ag > hg -> refMatch.awayTeam
            refMatch.homeShootoutScore > refMatch.awayShootoutScore -> refMatch.homeTeam
            refMatch.awayShootoutScore > refMatch.homeShootoutScore -> refMatch.awayTeam
            else -> null
        }
    }

    private fun resolvePossibleTeams(homeTeam: String, awayTeam: String): Pair<Pair<String, String>, Pair<String, String>> {
        val homeRefMatchId = extractRefMatchId(homeTeam)
        val awayRefMatchId = extractRefMatchId(awayTeam)

        val homeDisplay = if (homeRefMatchId != null) {
            val refMatch = cachedMatches.firstOrNull { it.id == homeRefMatchId }
            if (refMatch != null && refMatch.homeTeam.isNotBlank() && refMatch.awayTeam.isNotBlank()) {
                if (getWinnerName(refMatch) != null) {
                    homeTeam to ""
                } else {
                    "Ganador" to "${refMatch.homeTeam}-${refMatch.awayTeam}"
                }
            } else {
                "Ganador" to "P$homeRefMatchId"
            }
        } else {
            homeTeam to ""
        }

        val awayDisplay = if (awayRefMatchId != null) {
            val refMatch = cachedMatches.firstOrNull { it.id == awayRefMatchId }
            if (refMatch != null && refMatch.homeTeam.isNotBlank() && refMatch.awayTeam.isNotBlank()) {
                if (getWinnerName(refMatch) != null) {
                    awayTeam to ""
                } else {
                    "Ganador" to "${refMatch.homeTeam}-${refMatch.awayTeam}"
                }
            } else {
                "Ganador" to "P$awayRefMatchId"
            }
        } else {
            awayTeam to ""
        }

        return homeDisplay to awayDisplay
    }
    
    private fun extractRefMatchId(teamName: String): Int? {
        val patterns = listOf(
            "Ganador (\\d+)".toRegex(),
            "Perdedor (\\d+)".toRegex(),
            "W(\\d+)".toRegex(),
            "L(\\d+)".toRegex()
        )
        for (pattern in patterns) {
            pattern.find(teamName)?.let { return it.groupValues[1].toIntOrNull() }
        }
        return null
    }

    fun toDisplay(match: MatchEntity): MatchDisplay {
        val date = parseMadridInstant(match.dateTime)
        val zoned = date?.atZone(madridZone)
        val time = if (zoned != null) timeFormatter.format(zoned) else ""
        val dateLabel = if (zoned != null) {
            val today = LocalDate.now(madridZone)
            val matchDate = zoned.toLocalDate()
            when {
                matchDate == today.minusDays(1) -> "AYER"
                matchDate == today -> "HOY"
                matchDate == today.plusDays(1) -> "MA\u00D1ANA"
                else -> dateFormatter.format(zoned).replace(".", "")
            }
        } else ""
        val status = matchStatus(match)

        val resolvedHome = resolveTeamNameIfPossible(match.homeTeam)
        val resolvedAway = resolveTeamNameIfPossible(match.awayTeam)

        val liveMin = if (date != null && date.isAfter(Instant.now())) null else when {
            status == MatchStatus.FINISHED -> "FINAL"
            liveMinutes.containsKey(match.id) -> {
                val lm = liveMinutes[match.id]
                if (lm == "LIVE") null else lm
            }
            else -> null
        } ?: if (status == MatchStatus.LIVE) "LIVE" else null
        val s = goalScorers[match.id]
        val homeScr = s?.first ?: emptyList()
        val awayScr = s?.second ?: emptyList()
        val dayKey = if (zoned != null) "${dayAbbrFormatter.format(zoned).replace(".", "").uppercase(Locale.ROOT)} ${dayNumFormatter.format(zoned)}" else ""
        val sortKey = if (zoned != null) "${monthDayFormatter.format(zoned)}" else ""

        val (homeDisplay, awayDisplay) = resolvePossibleTeams(match.homeTeam, match.awayTeam)
        val homePossibleTeams = if (resolvedHome == match.homeTeam) homeDisplay.second else ""
        val awayPossibleTeams = if (resolvedAway == match.awayTeam) awayDisplay.second else ""

        return MatchDisplay(
            id = match.id, dateLabel = dateLabel, time = time,
            homeTeam = resolvedHome, awayTeam = resolvedAway,
            homeFlag = ExcelParser.getFlagEmoji(resolvedHome),
            awayFlag = ExcelParser.getFlagEmoji(resolvedAway),
            homeGoals = match.homeGoals, awayGoals = match.awayGoals,
            predictedHomeGoals = match.predictedHomeGoals,
            predictedAwayGoals = match.predictedAwayGoals,
            pointsEarned = if (match.isKnockout) (matchPointsMap[match.id] ?: 0) else match.pointsEarned,
            groupLabel = match.groupName, status = status, tvChannel = match.tvChannel,
            liveMinute = liveMin,
            homeScorers = homeScr,
            awayScorers = awayScr,
            dayKey = dayKey,
            sortKey = sortKey,
            isKnockout = match.isKnockout,
            hasKnockoutPred = knockoutPredictionMap[match.id] == true,
            winnerTeam = match.winnerTeam,
            homeShootoutScore = match.homeShootoutScore,
            awayShootoutScore = match.awayShootoutScore,
            homePossibleTeams = homePossibleTeams,
            awayPossibleTeams = awayPossibleTeams,
            shootoutAttempts = shootoutAttempts[match.id]?.map {
                ShootoutAttemptDisplay(it.playerName, it.isScored, it.isHome)
            } ?: emptyList()
        )
    }

    fun matchStatus(match: MatchEntity): MatchStatus {
        val start = parseMadridInstant(match.dateTime)
        if (start != null && start.isAfter(Instant.now())) return MatchStatus.UPCOMING
        val lm = liveMinutes[match.id]
        if (lm == "FINAL") return MatchStatus.FINISHED
        if (lm != null) return MatchStatus.LIVE
        if (start != null) {
            val now = Instant.now()
            val end = start.plusSeconds(MATCH_WINDOW_SECONDS)
            if (now.isAfter(end)) return MatchStatus.FINISHED
        }
        if (match.homeGoals != null || match.awayGoals != null) return MatchStatus.LIVE
        return MatchStatus.UPCOMING
    }

    

    private fun extractUserName(displayName: String?): String? {
        if (displayName == null) return null
        var name = displayName
        name = name.replace(".xlsx", "", ignoreCase = true)
            .replace(".xls", "", ignoreCase = true)
        val separators = listOf(" - ", "-", " _ ", "_", " ")
        for (sep in separators) {
            val parts = name.split(sep)
            if (parts.size >= 2) {
                val last = parts.last().trim()
                if (last.matches(nameRegex) && last.length >= 3) {
                    return last
                }
            }
        }
        return name.takeIf { it.matches(nameRegex) && it.length >= 3 }
    }

    companion object {
        private const val TAG = "HomeViewModel"
        private const val MATCH_WINDOW_SECONDS = 150L * 60
        private const val KNOCKOUT_START_ID = 73
        private val ESPN_R32_TO_MATCH_ID = mapOf(
            1 to 73, 2 to 75, 3 to 76, 4 to 74,
            5 to 78, 6 to 77, 7 to 79, 8 to 80,
            9 to 82, 10 to 81, 11 to 83, 12 to 84,
            13 to 85, 14 to 87, 15 to 88, 16 to 86
        )
        private val ESPN_R16_TO_MATCH_ID = mapOf(
            1 to 90, 2 to 89, 3 to 91, 4 to 92,
            5 to 93, 6 to 94, 7 to 95, 8 to 96
        )
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale("es", "ES"))
        private val dayAbbrFormatter = DateTimeFormatter.ofPattern("EEE", Locale("es", "ES"))
        private val dayNumFormatter = DateTimeFormatter.ofPattern("dd", Locale.US)
        private val monthDayFormatter = DateTimeFormatter.ofPattern("MMdd", Locale.US)
        private val diacriticsRegex = Regex("\\p{M}")
        private val whitespaceRegex = Regex("\\s+")
        private val nameRegex = Regex(".*[a-zA-Z].*")
        private val gson get() = GsonHolder.gson
        private val scorerListType get() = GsonHolder.scorerListType
        private val madridZone = ZoneId.of("Europe/Madrid")
    }
}
