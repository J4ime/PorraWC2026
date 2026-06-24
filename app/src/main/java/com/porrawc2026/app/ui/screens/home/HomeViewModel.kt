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
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.local.entity.TeamEntity
import com.porrawc2026.app.data.remote.LiveScoreService
import com.porrawc2026.app.data.remote.LiveScoreUpdate
import com.porrawc2026.app.data.remote.MatchScheduleProvider
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.domain.model.KnockoutBracketGenerator
import com.porrawc2026.app.data.remote.LiveScorer
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.GoalEventBus
import com.porrawc2026.app.util.GoalNotifier
import com.porrawc2026.app.util.LiveMatchStore
import com.porrawc2026.app.util.LogManager
import com.porrawc2026.app.util.PdfRankingExtractor
import com.porrawc2026.app.util.PdfRankingParser
import com.porrawc2026.app.util.RankingEntry
import java.io.File
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
import kotlinx.coroutines.isActive
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

    @Volatile
    private var cachedMatches: List<MatchEntity> = emptyList()
    private var refreshJob: Job? = null
    private val lastWrittenScores = ConcurrentHashMap<Int, Pair<Int, Int>>()
    private val goalScorers = ConcurrentHashMap<Int, Pair<List<GoalEvent>, List<GoalEvent>>>()
    private val liveMinutes = ConcurrentHashMap<Int, String>()
    private val seenScorers = ConcurrentHashMap<Int, MutableSet<String>>()
    private val notifiedScorers = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val processedGoalKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val relevantMatchIds = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())
    private val pdfParser = PdfRankingParser()
    private val pdfExtractor = PdfRankingExtractor()

    init {
        LogManager.init(context)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
                _appVersion.value = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
            }
        }
        viewModelScope.launch {
            runCatching {
                _excelFileName.value = prefsManager.getExcelFileNameSync()
                _autoRefreshEnabled.value = prefsManager.getAutoRefreshSync()
                _notificationsEnabled.value = prefsManager.getNotificationsSync()
                _userName.value = prefsManager.getUserNameSync()
                _userPosition.value = prefsManager.getUserPositionSync()
                liveMinutes.putAll(liveMatchStore.liveMinutes)
                goalScorers.putAll(liveMatchStore.goalScorers)
            }
        }
        refreshPoints(); loadPlayers(); preloadSchedule()
        forceCheckUpdate()
        startBackgroundService()
        viewModelScope.launch(Dispatchers.IO) {
            goalEventBus.goalScored.collect {
                tryGenerateDieciseisavos()
            }
        }
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
                LogManager.log(TAG, "Error in preloadSchedule", e)
            } finally {
                _isBusy.value = false
            }
        }
        // Slow operations (API calls, photos) en segundo plano, sin bloquear la UI
        viewModelScope.launch(Dispatchers.IO) {
            fetchLiveResults()
            tryGenerateDieciseisavos()
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
            runCatching {
                val info = UpdateManager.checkForUpdate(context)
                _updateAvailable.value = info?.isNewer == true
            }
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

    fun loadPdfResult(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            Log.i(TAG, "loadPdfResult: uri=$uri")
            runCatching {
                val searchName = _userName.value?.takeIf { it.isNotBlank() }
                    ?: error("No hay nombre de usuario. Importa un Excel primero en Ajustes.")
                Log.i(TAG, "searchName=\"$searchName\"")

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: error("No se pudo abrir el PDF")
                val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                Log.i(TAG, "PDF loaded, pages=${doc.numberOfPages}")

                val entries = pdfExtractor.extract(doc)
                Log.i(TAG, "extracted ${entries.size} entries")

                runCatching {
                    val xlsmFile = File(context.cacheDir, "ClasificacionMundial_2026.xlsm")
                    pdfExtractor.saveAsXlsm(entries, xlsmFile)
                }.onFailure { e ->
                    Log.e(TAG, "Failed to save XLSM", e)
                }

                doc.close()
                inputStream.close()

                val sampleSize = minOf(10, entries.size)
                if (entries.isEmpty()) {
                    Log.w(TAG, "No se extrajeron entradas del PDF")
                } else {
                    for (i in 0 until sampleSize) {
                        val e = entries[i]
                        Log.i(TAG, "  entrada[$i]: \"${e.name}\" (pos=${e.position}, pts=${e.totalPoints})")
                    }
                }

                val searchNorm = normalizeName(searchName)
                Log.i(TAG, "Buscando \"$searchName\" (normalized=\"$searchNorm\") en ${entries.size} entradas")

                val match = findUserInEntries(entries, searchName)

                if (match != null) {
                    val oldPos = _userPosition.value
                    _positionDiff.value = if (oldPos != null && oldPos > 0) oldPos - match.position else null
                    _userPosition.value = match.position
                    prefsManager.setUserPosition(match.position)
                    prefsManager.setPreviousPosition(oldPos)
                    _pdfResult.value = "${match.position}"
                    Log.i(TAG, "Encontrado en posición ${match.position}")
                } else {
                    _userPosition.value = null
                    _positionDiff.value = null
                    _pdfResult.value = "No encontrado"
                    Log.w(TAG, "No encontrado: searchName=\"$searchName\" normalized=\"${normalizeName(searchName)}\"")
                }
            }.onFailure { e ->
                _pdfResult.value = "Error: ${e.message?.take(25)}"
                Log.e(TAG, "loadPdfResult error", e)
            }
            _isBusy.value = false
        }
    }

    private fun findUserInEntries(entries: List<RankingEntry>, searchName: String): RankingEntry? {
        val searchNorm = normalizeName(searchName)
        val searchFlat = searchNorm.replace(" ", "")

        for (entry in entries) {
            val nameNorm = normalizeName(entry.name)
            if (nameNorm.contains(searchNorm)) {
                Log.i(TAG, "Stage1 match: entry=\"${entry.name}\" contains \"$searchNorm\"")
                return entry
            }
        }

        for (entry in entries) {
            val nameNorm = normalizeName(entry.name)
            val flat = nameNorm.replace(" ", "")
            if (flat.contains(searchFlat)) {
                Log.i(TAG, "Stage2 match: entry=\"${entry.name}\" flat=\"$flat\" contains \"$searchFlat\"")
                return entry
            }
        }

        val searchLast = lastWord(searchNorm)
        if (searchLast.length >= 3 && searchLast != searchNorm) {
            for (entry in entries) {
                val nameNorm = normalizeName(entry.name)
                val nameLast = lastWord(nameNorm)
                if (nameLast == searchLast || nameNorm.contains(searchLast)) {
                    Log.i(TAG, "Stage3 match: entry=\"${entry.name}\" last=\"$nameLast\" searchLast=\"$searchLast\"")
                    return entry
                }
            }
        }

        return null
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
        cachedMatches = cachedMatches.map { it.copy(pointsEarned = 0) }

        val teams = repository.getAllTeams().first()
        val knockoutPredictions = repository.getKnockoutPredictions().first()

        val actual = PointsCalculator.computeActualAdvancingTeams(cachedMatches, teams)
        val predicted = PointsCalculator.computePredictedAdvancingTeams(cachedMatches, teams, knockoutPredictions)
        val advPoints = PointsCalculator.calculateTotalAdvancementPoints(predicted, actual)
        _advancementPoints.value = advPoints
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

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            var lastDay = LocalDate.now(madridZone)
            while (isActive) {
                try {
                    val currentDay = LocalDate.now(madridZone)
                    if (currentDay != lastDay) {
                        lastDay = currentDay
                        refreshUpcomingMatches()
                    }
                    checkLiveWindow()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto-refresh loop", e)
                    LogManager.log(TAG, "Error in auto-refresh loop", e)
                }
                delay(60_000)
            }
        }
    }

    private suspend fun checkLiveWindow() {
        if (!_autoRefreshEnabled.value) return
        val todayMatches = getTodayMatchesWithDates()
        val staleMatches = getStaleMatches()
        val anyNeedsUpdate = (todayMatches + staleMatches).any { match ->
            liveMinutes[match.id] != "FINAL" && !isFinishedByTime(match)
        }
        if (!anyNeedsUpdate) return
        fetchLiveResults()
    }

    fun parseMadridInstant(dateTime: String): Instant? {
        if (dateTime.isBlank()) return null
        return try {
            if (dateTime.endsWith("Z")) {
                Instant.parse(dateTime)
            } else {
                val local = LocalDateTime.parse(dateTime, dateTimeFormatter)
                local.atZone(madridZone).toInstant()
            }
        } catch (_: Exception) { null }
    }

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
        tryGenerateDieciseisavos()
    }

    private suspend fun tryGenerateDieciseisavos() {
        val existingDieciseisavos = cachedMatches.filter { it.id in 73..88 }
        val teams = repository.getAllTeams().first()
        val dieciseisavos = KnockoutBracketGenerator.generateDieciseisavos(cachedMatches, teams)
        if (dieciseisavos.isEmpty()) return

        val preserved = dieciseisavos.map { gen ->
            val existing = existingDieciseisavos.firstOrNull { it.id == gen.id }
            if (existing != null && existing.homeTeam == gen.homeTeam && existing.awayTeam == gen.awayTeam) {
                return@map existing
            }
            if (existing != null) {
                gen.copy(
                    homeGoals = existing.homeGoals, awayGoals = existing.awayGoals,
                    predictedHomeGoals = existing.predictedHomeGoals,
                    predictedAwayGoals = existing.predictedAwayGoals,
                    pointsEarned = existing.pointsEarned,
                    homeRedCards = existing.homeRedCards, awayRedCards = existing.awayRedCards,
                    homeYellowCards = existing.homeYellowCards, awayYellowCards = existing.awayYellowCards,
                    homeScorers = existing.homeScorers, awayScorers = existing.awayScorers,
                    homeMissedPenalties = existing.homeMissedPenalties,
                    awayMissedPenalties = existing.awayMissedPenalties,
                    winnerTeam = existing.winnerTeam,
                    homeHeadedGoals = existing.homeHeadedGoals, awayHeadedGoals = existing.awayHeadedGoals,
                    hasSubGoal = existing.hasSubGoal
                )
            } else gen
        }

        val changed = preserved != existingDieciseisavos.sortedBy { it.id }
        if (!changed) return

        repository.insertMatches(preserved)
        cachedMatches = (cachedMatches.filter { it.id !in 73..88 } + preserved).sortedBy { it.id }
        refreshUpcomingMatches()
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

    private fun isFinishedByTime(match: MatchEntity): Boolean {
        val start = parseMadridInstant(match.dateTime) ?: return false
        return match.homeGoals != null && match.awayGoals != null &&
            Instant.now().isAfter(start.plusSeconds(MATCH_WINDOW_SECONDS))
    }

    private suspend fun fetchLiveResults(fullFetch: Boolean = false) {
        val wcMatches = filterMatchesForFetch(fullFetch) ?: return
        val scoreUpdates = fetchWithRetry { liveScoreService.fetchScoreUpdates(wcMatches) }.orEmpty()
        processScoreUpdates(scoreUpdates)
        tryGenerateDieciseisavos()
        notifyGoalEvents()
        recalculateAllPlayerGoals()
        refreshPoints()
        checkGoalNotifications()
        refreshUpcomingMatches()
    }

    private suspend fun filterMatchesForFetch(fullFetch: Boolean): List<MatchEntity>? {
        val todayMatches = getTodayMatchesWithDates()
        val staleMatches = if (fullFetch) emptyList() else getStaleMatches()
        val matchesForWc = if (fullFetch) {
            cachedMatches
        } else {
            (todayMatches + staleMatches).distinctBy { it.id }
        }
        if (matchesForWc.isEmpty()) return null

        val cachedIds = loadCachedScorers()
        val finishedByTimeIds = matchesForWc.filter { isFinishedByTime(it) }.map { it.id }.toSet()
        val skipApiIds = cachedIds.intersect(finishedByTimeIds)

        val wcMatches = if (skipApiIds.size == matchesForWc.size) emptyList()
        else matchesForWc.filter { it.id !in skipApiIds }

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
                if (start != null && start.isAfter(Instant.now())) return@forEach
            }
            updateMatchScores(update)
            updateGoalScorers(update)
            if (update.isFinished) {
                repository.updateMatchCards(update.matchId, update.homeRedCards, update.awayRedCards, update.homeYellowCards, update.awayYellowCards)
                repository.updateMatchMissedPenalties(update.matchId, update.homeMissedPenalties, update.awayMissedPenalties)
                repository.updateMatchHeadedGoals(update.matchId, update.homeHeadedGoals, update.awayHeadedGoals)
                repository.updateMatchSubGoal(update.matchId, update.hasSubGoal)
                if (update.winnerTeam != null) {
                    repository.updateMatchWinner(update.matchId, update.winnerTeam)
                }
            }
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
            if (update.isFinished) {
                repository.updateMatchScorers(update.matchId, homeJson, awayJson)
            }
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

        val allSorted = matches.sortedBy { it.dateTime.ifBlank { "zzz" } }
        val allDisplay = allSorted.map { match -> toDisplay(match) }

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
        val maxVisibleId = MatchScheduleProvider.getMaxVisibleRoundId()
        _dayKeys.value = allDisplay
            .filter { it.id <= maxVisibleId }
            .mapNotNull { d -> if (d.dayKey.isBlank()) null else d.dayKey }.distinct()
            .sortedBy { d -> allDisplay.firstOrNull { it.dayKey == d }?.sortKey ?: d }
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
        val start = parseMadridInstant(match.dateTime)
        if (start != null && start.isAfter(Instant.now())) return MatchStatus.UPCOMING
        val lm = liveMinutes[match.id]
        if (lm == "FINAL") return MatchStatus.FINISHED
        if (start != null) {
            val now = Instant.now()
            val end = start.plusSeconds(MATCH_WINDOW_SECONDS)
            if (now.isAfter(end)) return MatchStatus.FINISHED
        }
        if (lm != null) return MatchStatus.LIVE
        if (match.homeGoals != null || match.awayGoals != null) return MatchStatus.LIVE
        return MatchStatus.UPCOMING
    }

    private fun startBackgroundService() {
        val intent = Intent(context, LiveUpdateService::class.java)
        ContextCompat.startForegroundService(context, intent)
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
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale("es", "ES"))
        private val dayAbbrFormatter = DateTimeFormatter.ofPattern("EEE", Locale("es", "ES"))
        private val dayNumFormatter = DateTimeFormatter.ofPattern("dd", Locale.US)
        private val monthDayFormatter = DateTimeFormatter.ofPattern("MMdd", Locale.US)
        private val diacriticsRegex = Regex("\\p{M}")
        private val whitespaceRegex = Regex("\\s+")
        private val nameRegex = Regex(".*[a-zA-Z].*")
        private val gson = Gson()
        private val scorerListType = object : TypeToken<List<LiveScorer>>() {}.type
        private val madridZone = ZoneId.of("Europe/Madrid")
    }
}
