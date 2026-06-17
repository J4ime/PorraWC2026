package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.content.Intent
import android.util.Log
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.porrawc2026.app.service.LiveUpdateService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.remote.LiveScoreService
import com.porrawc2026.app.data.remote.MatchScheduleProvider
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.data.remote.LiveScorer
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.GoalEventBus
import com.porrawc2026.app.util.GoalNotifier
import com.porrawc2026.app.util.LiveMatchStore
import com.porrawc2026.app.util.PdfRankingParser
import com.porrawc2026.app.util.PlayerPhotoDownloader
import com.porrawc2026.app.util.PrefsManager
import com.porrawc2026.app.util.UpdateManager
import com.porrawc2026.app.util.ValidationResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
    val awayScorers: List<GoalEvent> = emptyList(),
    val dayKey: String = "",
    val sortKey: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val liveScoreService: LiveScoreService,
    private val prefsManager: PrefsManager,
    private val goalEventBus: GoalEventBus,
    private val liveMatchStore: LiveMatchStore,
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
    private val _appVersion = MutableStateFlow(context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?")
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()
    private val _excelFileName = MutableStateFlow<String?>(null)
    val excelFileName: StateFlow<String?> = _excelFileName.asStateFlow()
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    private val _allMatches = MutableStateFlow<List<MatchDisplay>>(emptyList())
    val allMatches: StateFlow<List<MatchDisplay>> = _allMatches.asStateFlow()
    private val _dayKeys = MutableStateFlow<List<String>>(emptyList())
    val dayKeys: StateFlow<List<String>> = _dayKeys.asStateFlow()
    private val _autoRefreshEnabled = MutableStateFlow(true)
    val autoRefreshEnabled: StateFlow<Boolean> = _autoRefreshEnabled.asStateFlow()
    private val _pdfResult = MutableStateFlow<String?>(null)
    val pdfResult: StateFlow<String?> = _pdfResult.asStateFlow()
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()
    private val _userPosition = MutableStateFlow<Int?>(null)
    val userPosition: StateFlow<Int?> = _userPosition.asStateFlow()
    private val _positionDiff = MutableStateFlow<Int?>(null)
    val positionDiff: StateFlow<Int?> = _positionDiff.asStateFlow()

    private var cachedMatches: List<MatchEntity> = emptyList()
    private var refreshJob: Job? = null
    private val lastWrittenScores = mutableMapOf<Int, Pair<Int, Int>>()
    private val goalScorers = mutableMapOf<Int, Pair<List<GoalEvent>, List<GoalEvent>>>()
    private val seenScorers = mutableMapOf<Int, MutableSet<String>>()
    private val liveMinutes = mutableMapOf<Int, String>()
    private val notifiedScorers = mutableSetOf<String>()
    private val processedGoalKeys = mutableSetOf<String>()
    private val relevantMatchIds = mutableSetOf<Int>()
    private val pdfParser = PdfRankingParser()

    init {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        viewModelScope.launch {
            _excelFileName.value = prefsManager.getExcelFileNameSync()
            _autoRefreshEnabled.value = prefsManager.getAutoRefreshSync()
            _notificationsEnabled.value = prefsManager.getNotificationsSync()
            _userName.value = prefsManager.getUserNameSync()
            _userPosition.value = prefsManager.getUserPositionSync()
            liveMinutes.putAll(liveMatchStore.liveMinutes)
            goalScorers.putAll(liveMatchStore.goalScorers)
        }
        refreshPoints(); loadPlayers(); preloadSchedule()
        forceCheckUpdate()
        startBackgroundService()
    }

    private fun preloadSchedule() {
        _isBusy.value = true
        cachedMatches = MatchScheduleProvider.buildMatchEntities()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbMatches = repository.getAllMatches().first()
                val staticSchedule = MatchScheduleProvider.buildMatchEntities()

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
                enrichSchedule()
                recalcAllPoints()
                refreshPoints()
                loadPlayers()
                processedGoalKeys.addAll(prefsManager.getProcessedGoalKeys())
                startAutoRefresh()
                lastWrittenScores.clear()
                refreshUpcomingMatches()
                _hasData.value = true
                _isReady.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error in preloadSchedule", e)
            } finally {
                _isBusy.value = false
            }
        }
        // Slow operations (API calls, photos) en segundo plano, sin bloquear la UI
        viewModelScope.launch(Dispatchers.IO) {
            fetchLiveResults(fullFetch = true)
            downloadPlayerPhotos()
            precachePhotosInBackground()
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
                    startAutoRefresh()
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
            val info = UpdateManager.checkForUpdate(context)
            _updateAvailable.value = info?.isNewer == true
        }
    }

    fun toggleNotifications() {
        _notificationsEnabled.value = !_notificationsEnabled.value
        viewModelScope.launch { prefsManager.setNotificationsEnabled(_notificationsEnabled.value) }
    }

    fun toggleAutoRefresh() {
        _autoRefreshEnabled.value = !_autoRefreshEnabled.value
        viewModelScope.launch { prefsManager.setAutoRefresh(_autoRefreshEnabled.value) }
    }

    fun refreshLiveScores() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            refreshAll()
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
            cachedMatches = cachedMatches.map { it.copy(homeGoals = null, awayGoals = null, homeScorers = null, awayScorers = null, homeRedCards = null, awayRedCards = null, homeYellowCards = null, awayYellowCards = null) }
            fetchLiveResults(fullFetch = true)
            _isBusy.value = false
        }
    }

    fun refreshCache() {
        clearAndRefreshCache()
    }

    fun setForegroundState(isForeground: Boolean) {
        // Lifecycle hook - no-op since polling is handled by startAutoRefresh
    }

    fun loadPdfResult(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            runCatching {
                val searchName = _userName.value?.takeIf { it.isNotBlank() } ?: "jaimede"
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: error("No se pudo abrir el PDF")
                val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                val position = pdfParser.findNamePosition(doc, searchName)
                doc.close()
                inputStream.close()

                if (position > 0) {
                    val oldPos = _userPosition.value
                    _positionDiff.value = if (oldPos != null && oldPos > 0) oldPos - position else null
                    _userPosition.value = position
                    prefsManager.setUserPosition(position)
                    prefsManager.setPreviousPosition(oldPos)
                    _pdfResult.value = "$position"
                } else {
                    _userPosition.value = null
                    _positionDiff.value = null
                    _pdfResult.value = "No encontrado"
                }
            }.onFailure { e ->
                _pdfResult.value = "Error: ${e.message?.take(25)}"
            }
            _isBusy.value = false
        }
    }

    fun installUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdating.value = true
            runCatching {
                val info = UpdateManager.checkForUpdate(context)
                when {
                    info == null -> _errorMessage.emit("Error al comprobar actualizacion. Sin conexion?")
                    !info.isNewer -> _errorMessage.emit("Ya tienes la ultima version")
                    else -> {
                        val ok = UpdateManager.downloadAndInstall(context, info.downloadUrl)
                        if (!ok) _errorMessage.emit("Error al descargar la actualizacion")
                        else _appVersion.value = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
                    }
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
        viewModelScope.launch { _totalPoints.value = repository.calculateTotalPoints() }
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

    private suspend fun precachePhotosInBackground() {
        PlayerPhotoDownloader.precacheTopPlayers(context)
    }

    private suspend fun recalcAllPoints() {
        cachedMatches = cachedMatches.map { match ->
            val realHome = match.homeGoals
            val realAway = match.awayGoals
            if (realHome != null && realAway != null) {
                val pts = PointsCalculator.calculateMatchPoints(match)
                val updated = match.copy(pointsEarned = pts)
                repository.updateMatchPrediction(updated)
                updated
            } else match
        }
    }

    private fun checkGoalNotifications() {
        if (!_notificationsEnabled.value) return
        val predictedNames = _players.value.mapNotNull { it.predictedName }.map { it.lowercase() }.toSet()
        if (predictedNames.isEmpty()) return
        for ((matchId, pair) in goalScorers) {
            val seen = seenScorers.getOrPut(matchId) { mutableSetOf() }
            val allScorers = pair.first + pair.second
            for (scorer in allScorers) {
                val name = scorer.playerName
                val key = name.lowercase().trim()
                if (seen.add(key)) {
                    val matches = predictedNames.any { pred ->
                        key.contains(pred, ignoreCase = true) || pred.contains(key, ignoreCase = true)
                    }
                    if (matches) {
                        val displayName = name.split(" ").last()
                        GoalNotifier.notifyGoal(context, displayName)
                    }
                }
            }
        }
    }

    private fun enrichSchedule() {
        cachedMatches = MatchScheduleProvider.enrichSchedule(cachedMatches)
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
        if (!_autoRefreshEnabled.value) return
        val todayMatches = getTodayMatchesWithDates()
        if (todayMatches.isEmpty()) return
        val now = Date()
        val times = todayMatches.map { parseMadridDate(it.dateTime) }.filterNotNull()
        val firstStart = times.minOrNull()
        val lastEnd = times.maxOrNull()?.let { Date(it.time + 150L * 60 * 1000) }
        if (firstStart != null && now.before(firstStart)) return
        if (lastEnd != null && now.after(lastEnd)) return
        fetchLiveResults()
    }

    fun parseMadridDate(dateTime: String): Date? {
        if (dateTime.isBlank()) return null
        return try {
            if (dateTime.endsWith("Z")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(dateTime)
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = madridTZ }.parse(dateTime)
            }
        } catch (_: Exception) { null }
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

    private suspend fun refreshAll() {
        fetchLiveResults()
    }

    private suspend fun <T> fetchWithRetry(maxAttempts: Int = 10, block: suspend () -> T?): T? {
        repeat(maxAttempts) { attempt ->
            val result = runCatching { block() }
            if (result.isSuccess) {
                val data = result.getOrNull()
                if (data != null) return data
            }
            if (attempt < maxAttempts - 1) delay(2000L shl attempt.coerceAtMost(4))
        }
        return null
    }

    private fun loadCachedScorers(): Set<Int> {
        val cachedIds = mutableSetOf<Int>()
        for (match in cachedMatches) {
            val homeRaw = match.homeScorers
            val awayRaw = match.awayScorers
            if (homeRaw != null && awayRaw != null) {
                try {
                    val type = object : TypeToken<List<LiveScorer>>() {}.type
                    val homeList: List<LiveScorer> = Gson().fromJson(homeRaw, type) ?: emptyList()
                    val awayList: List<LiveScorer> = Gson().fromJson(awayRaw, type) ?: emptyList()
                    val homeGoals = homeList.map { GoalEvent(it.playerName, it.minute) }
                    val awayGoals = awayList.map { GoalEvent(it.playerName, it.minute) }
                    goalScorers[match.id] = Pair(homeGoals, awayGoals)
                    liveMatchStore.goalScorers[match.id] = Pair(homeGoals, awayGoals)
                    cachedIds.add(match.id)
                } catch (_: Exception) { }
            }
        }
        return cachedIds
    }

    private suspend fun saveFinishedScorersToCache(
        matchId: Int,
        homeScorers: List<LiveScorer>,
        awayScorers: List<LiveScorer>
    ) {
        val gson = Gson()
        val homeJson = gson.toJson(homeScorers)
        val awayJson = gson.toJson(awayScorers)
        repository.updateMatchScorers(matchId, homeJson, awayJson)
        cachedMatches = cachedMatches.map {
            if (it.id == matchId) it.copy(homeScorers = homeJson, awayScorers = awayJson) else it
        }
    }

    private fun isFinishedByTime(match: MatchEntity): Boolean {
        val start = parseMadridDate(match.dateTime) ?: return false
        return match.homeGoals != null && match.awayGoals != null &&
            Date().after(Date(start.time + 150L * 60 * 1000))
    }

    private suspend fun fetchLiveResults(fullFetch: Boolean = false) {
        val todayMatches = getTodayMatchesWithDates()
        val matchesForWc = if (fullFetch) cachedMatches else todayMatches
        if (matchesForWc.isEmpty()) return

        val cachedIds = loadCachedScorers()
        val finishedByTimeIds = matchesForWc.filter { isFinishedByTime(it) }.map { it.id }.toSet()
        val skipApiIds = cachedIds.intersect(finishedByTimeIds)

        val wcMatches = if (skipApiIds.size == matchesForWc.size) emptyList()
        else matchesForWc.filter { it.id !in skipApiIds }

        if (wcMatches.isEmpty()) {
            refreshUpcomingMatches()
            return
        }

        val scoreUpdates = fetchWithRetry { liveScoreService.fetchScoreUpdates(wcMatches) }.orEmpty()

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
                val start = parseMadridDate(match.dateTime)
                if (start != null && start.after(Date())) return@forEach
            }
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
            val homeScr = update.homeScorers.map { GoalEvent(it.playerName, it.minute) }
            val awayScr = update.awayScorers.map { GoalEvent(it.playerName, it.minute) }
            if (homeScr.isNotEmpty() || awayScr.isNotEmpty()) {
                goalScorers[update.matchId] = Pair(homeScr, awayScr)
                liveMatchStore.goalScorers[update.matchId] = Pair(homeScr, awayScr)
            }
            if (update.isFinished && (homeScr.isNotEmpty() || awayScr.isNotEmpty())) {
                saveFinishedScorersToCache(update.matchId, update.homeScorers, update.awayScorers)
            }
            if (update.isFinished) {
                repository.updateMatchCards(update.matchId, update.homeRedCards, update.awayRedCards, update.homeYellowCards, update.awayYellowCards)
            }
        }

        for ((matchId, pair) in goalScorers) {
            if (relevantMatchIds.isNotEmpty() && matchId !in relevantMatchIds) continue
            var goalsChanged = false
            for (scorer in pair.first + pair.second) {
                val key = "$matchId:${scorer.playerName}:${scorer.minute}"
                if (notifiedScorers.add(key) && processedGoalKeys.add(key)) {
                    goalEventBus.notifyGoal()
                    if (updatePlayerGoal(scorer.playerName)) {
                        relevantMatchIds.add(matchId)
                        goalsChanged = true
                    }
                }
            }
            if (goalsChanged) {
                prefsManager.setProcessedGoalKeys(processedGoalKeys.toSet())
                refreshPoints()
            }
        }
        checkGoalNotifications()
        refreshUpcomingMatches()
    }

    private fun normalizeName(name: String): String {
        return name.lowercase().trim()
            .replace("á", "a").replace("à", "a").replace("â", "a").replace("ã", "a").replace("ä", "a")
            .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
            .replace("í", "i").replace("ì", "i").replace("î", "i").replace("ï", "i")
            .replace("ó", "o").replace("ò", "o").replace("ô", "o").replace("õ", "o").replace("ö", "o")
            .replace("ú", "u").replace("ù", "u").replace("û", "u").replace("ü", "u")
            .replace("ç", "c").replace("ñ", "n")
            .replace(".", "").replace("-", " ").replace("'", " ")
            .replace(Regex("\\s+"), " ").trim()
    }

    private fun lastWord(name: String): String {
        val parts = name.split(" ")
        return if (parts.size > 1) parts.last() else name
    }

    private suspend fun updatePlayerGoal(scorerName: String): Boolean {
        val currentPlayers = repository.getPlayerPredictionsList()
        val scorerNorm = normalizeName(scorerName)
        val scorerLast = lastWord(scorerNorm)
        for (player in currentPlayers) {
            val pred = player.predictedName ?: continue
            val predNorm = normalizeName(pred)
            val predLast = lastWord(predNorm)
            if (predNorm.contains(scorerLast) || scorerNorm.contains(predLast) || predLast == scorerLast) {
                val newGoals = player.goalsScored + 1
                val newPoints = newGoals * player.pointsPerGoal
                val updated = player.copy(goalsScored = newGoals, pointsEarned = newPoints)
                repository.updatePlayerPrediction(updated)
                _players.value = _players.value.map { if (it.rank == updated.rank) updated else it }
                return true
            }
        }
        return false
    }

    fun refreshUpcomingMatches() {
        val cal = Calendar.getInstance(madridTZ)
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        val groupMatches = cachedMatches.filter { !it.isKnockout && it.id < 900 }
            .sortedBy { it.dateTime.ifBlank { "zzz" } }
        val allDisplay = groupMatches.map { match -> toDisplay(match) }

        val yesterdayMatches = allDisplay.filter { display ->
            val d = parseMadridDate(cachedMatches.first { it.id == display.id }.dateTime) ?: return@filter false
            val c = Calendar.getInstance(madridTZ).apply { time = d }
            c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today - 1
        }.sortedBy { display ->
            parseMadridDate(cachedMatches.first { it.id == display.id }.dateTime)
        }
        _yesterdayMatches.value = yesterdayMatches

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
        _allMatches.value = allDisplay
        _dayKeys.value = allDisplay.mapNotNull { d -> if (d.dayKey.isBlank()) null else d.dayKey }.distinct()
            .sortedBy { d -> allDisplay.firstOrNull { it.dayKey == d }?.sortKey ?: d }
    }

    fun toDisplay(match: MatchEntity): MatchDisplay {
        val date = parseMadridDate(match.dateTime)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = madridTZ }
        val dateFmt = SimpleDateFormat("EEE d MMM", Locale("es", "ES")).apply { timeZone = madridTZ }
        val dayAbbrFmt = SimpleDateFormat("EEE", Locale("es", "ES")).apply { timeZone = madridTZ }
        val dayNumFmt = SimpleDateFormat("dd", Locale.US).apply { timeZone = madridTZ }
        val time = if (date != null) timeFmt.format(date) else ""
        val dateLabel = if (date != null) {
            val cal = Calendar.getInstance(madridTZ)
            val today = cal.get(Calendar.DAY_OF_YEAR)
            val year = cal.get(Calendar.YEAR)
            val matchCal = Calendar.getInstance(madridTZ).also { it.time = date }
            val matchDay = matchCal.get(Calendar.DAY_OF_YEAR)
            val matchYear = matchCal.get(Calendar.YEAR)
            when {
                matchYear == year && matchDay == today - 1 -> "AYER"
                matchYear == year && matchDay == today -> "HOY"
                matchYear == year && matchDay == today + 1 -> "MA\u00D1ANA"
                else -> dateFmt.format(date).replace(".", "")
            }
        } else ""
        val status = matchStatus(match)

        val liveMin = if (date != null && date.after(Date())) null else when {
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
        val dayKey = if (date != null) "${dayAbbrFmt.format(date).replace(".", "").uppercase()} ${dayNumFmt.format(date)}" else ""
        val sortKey = if (date != null) dayNumFmt.format(date) else ""

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
            awayScorers = awayScr,
            dayKey = dayKey,
            sortKey = sortKey
        )
    }

    fun matchStatus(match: MatchEntity): MatchStatus {
        val start = parseMadridDate(match.dateTime)
        if (start != null && start.after(Date())) return MatchStatus.UPCOMING
        val lm = liveMinutes[match.id]
        if (lm == "FINAL") return MatchStatus.FINISHED
        if (lm != null && lm != "FINAL") return MatchStatus.LIVE
        if (match.homeGoals != null && match.awayGoals != null) {
            if (start != null) {
                val now = Date()
                val end = Date(start.time + 150L * 60 * 1000)
                if (now.after(end)) return MatchStatus.FINISHED
            }
            return MatchStatus.LIVE
        }
        val parsed = start ?: return MatchStatus.UPCOMING
        val now = Date()
        val end = Date(parsed.time + 150L * 60 * 1000)
        return if (now.after(parsed) && now.before(end)) MatchStatus.LIVE else MatchStatus.UPCOMING
    }

    private fun startBackgroundService() {
        val intent = Intent(context, LiveUpdateService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopBackgroundService() {
        val intent = Intent(context, LiveUpdateService::class.java)
        context.stopService(intent)
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
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
                if (last.matches(Regex(".*[a-zA-Z].*")) && last.length >= 3) {
                    return last
                }
            }
        }
        return name.takeIf { it.matches(Regex(".*[a-zA-Z].*")) && it.length >= 3 }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
