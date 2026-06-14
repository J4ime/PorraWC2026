package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.remote.LiveScoreService
import com.porrawc2026.app.data.remote.MatchScheduleProvider
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.GoalNotifier
import com.porrawc2026.app.util.PlayerPhotoDownloader
import com.porrawc2026.app.util.PrefsManager
import com.porrawc2026.app.util.UpdateManager
import com.porrawc2026.app.util.ValidationResult
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
    val awayScorers: List<GoalEvent> = emptyList(),
    val dayKey: String = "",
    val sortKey: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val liveScoreService: LiveScoreService,
    private val prefsManager: PrefsManager,
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
    private val _appVersion = MutableStateFlow(
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?" } catch (_: Exception) { "?" }
    )
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

    private var cachedMatches: List<MatchEntity> = emptyList()
    private var refreshJob: Job? = null
    private var livePollJob: Job? = null
    private val lastWrittenScores = mutableMapOf<Int, Pair<Int, Int>>()
    private val goalScorers = mutableMapOf<Int, Pair<List<GoalEvent>, List<GoalEvent>>>()
    private val seenScorers = mutableMapOf<Int, MutableSet<String>>()
    private val liveMinutes = mutableMapOf<Int, String>()
    private var isInForeground = false

    init {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        viewModelScope.launch {
            _excelFileName.value = prefsManager.getExcelFileNameSync()
            _autoRefreshEnabled.value = prefsManager.getAutoRefreshSync()
            _notificationsEnabled.value = prefsManager.getNotificationsSync()
            _userName.value = prefsManager.getUserNameSync()
        }
        refreshPoints(); loadPlayers(); preloadSchedule()
        forceCheckUpdate()
    }

    private suspend fun precachePhotosInBackground() {
        PlayerPhotoDownloader.precacheTopPlayers(context)
    }

    private fun preloadSchedule() {
        _isBusy.value = true
        loadHardcodedMatches()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbMatches = repository.getAllMatches().first()
                if (dbMatches.isNotEmpty()) {
                    cachedMatches = dbMatches
                    enrichSchedule()
                    recalcAllPoints()
                    refreshPoints()
                    loadPlayers()
                    downloadPlayerPhotos()
                    precachePhotosInBackground()
                    startAutoRefresh()
                    // Keep DB goals, re-check from API
                    lastWrittenScores.clear()
                    refreshUpcomingMatches()
                    fetchLiveResults()
                    refreshUpcomingMatches()
                    _hasData.value = true
                } else {
                    enrichSchedule()
                    refreshUpcomingMatches()
                    fetchLiveResults()
                    refreshUpcomingMatches()
                    _hasData.value = true
                }
                _isReady.value = true
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun loadHardcodedMatches() {
        cachedMatches = MatchScheduleProvider.buildMatchEntities()
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
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val displayName = it.getString(nameIndex)
                            _excelFileName.value = displayName
                            prefsManager.setExcelFileName(displayName)
                            val extracted = extractUserName(displayName)
                            if (extracted != null) {
                                _userName.value = extracted
                                prefsManager.setUserName(extracted)
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
                    refreshUpcomingMatches()
                    _hasData.value = true
                    loadPlayers()
                    downloadPlayerPhotos()
                    startAutoRefresh()
                    fetchLiveResults()
                    refreshUpcomingMatches()
                }
            } catch (e: Exception) {
                _errorMessage.emit("Error al cargar el Excel: ${e.message}")
            } finally {
                _isLoading.value = false
                _isBusy.value = false
            }
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
            try {
                fetchLiveResults()
                refreshUpcomingMatches()
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun refreshCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            try {
                fetchLiveResults()
                refreshUpcomingMatches()
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun loadPdfResult(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            try {
                val searchName = _userName.value?.takeIf { it.isNotBlank() } ?: "jaimede"
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                val position = findNamePosition(doc, searchName)
                doc.close()
                inputStream.close()
                _pdfResult.value = if (position > 0) "$position" else "No encontrado"
                _userPosition.value = position
            } catch (e: Exception) {
                _pdfResult.value = "Error: ${e.message?.take(25)}"
            } finally { _isBusy.value = false }
        }
    }

    private suspend fun findNamePosition(doc: com.tom_roush.pdfbox.pdmodel.PDDocument, searchName: String): Int {
        data class Cc(val x: Float, val y: Float, val ch: String, val pg: Int)

        val all = mutableListOf<Cc>()
        var currentPage = 0

        val stripper = object : com.tom_roush.pdfbox.text.PDFTextStripper() {
            override fun startPage(page: com.tom_roush.pdfbox.pdmodel.PDPage) {
                currentPage = currentPageNo
            }
            override fun writeString(text: String, textPositions: MutableList<com.tom_roush.pdfbox.text.TextPosition>) {
                for (tp in textPositions) {
                    val ch = tp.unicode
                    if (ch != null && ch.isNotBlank()) {
                        all.add(Cc(tp.xDirAdj, tp.yDirAdj, ch, currentPage))
                    }
                }
            }
        }
        stripper.sortByPosition = true
        stripper.getText(doc)

        // Normalize search name (remove accents, lowercase)
        fun normalize(s: String) = s.lowercase()
            .replace("í", "i").replace("é", "e").replace("á", "a")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
        val searchNorm = normalize(searchName)

        // Find the name by matching characters sequentially
        var targetPage = -1
        var targetY = -1f
        for (i in 0..all.size - searchName.length) {
            if (normalize(all[i].ch) == searchNorm[0].toString()) {
                var match = true
                for (j in 1 until searchName.length) {
                    if (i + j >= all.size || normalize(all[i + j].ch) != searchNorm[j].toString()) {
                        match = false
                        break
                    }
                }
                if (match) {
                    targetPage = all[i].pg
                    targetY = all[i].y
                    break
                }
            }
        }
        if (targetPage < 0) return 0

        // Name-area text: left side (X<100) OR right side (X in [450,600), excluding goal predictions with "/")
        fun nameText(chars: List<Cc>): String {
            val left = StringBuilder()
            val right = StringBuilder()
            for (cc in chars.sortedBy { it.x }) {
                if (cc.ch in "0123456789 ") continue
                if (cc.x < 100) left.append(cc.ch)
                else if (cc.x >= 450 && cc.x < 600) right.append(cc.ch)
            }
            val l = left.toString().trim()
            val r = right.toString().trim()
            return when {
                l.contains("/") && r.contains("/") -> ""
                r.contains("/") -> l
                l.contains("/") -> r
                l.length >= 3 -> l
                r.length >= 3 -> r
                else -> ""
            }
        }

        // Group by page, then Y (rounded)
        val pageYGroups = mutableMapOf<Int, MutableMap<Int, MutableList<Cc>>>()
        for (cc in all) {
            val yKey = Math.round(cc.y)
            pageYGroups.getOrPut(cc.pg) { mutableMapOf() }
                .getOrPut(yKey) { mutableListOf() }
                .add(cc)
        }

        // Count names across pages up to target
        val targetYKey = Math.round(targetY)
        var pos = 0
        for (pg in 1..targetPage) {
            val yg = pageYGroups[pg] ?: continue
            val sortedY = yg.keys.sorted()
            var prevY = -1000
            for (y in sortedY) {
                if (pg == targetPage && y > targetYKey) break
                val text = nameText(yg[y]!!)
                if (text.length < 3 || text.contains("/") || text.contains("---")) continue
                if (text.firstOrNull()?.isUpperCase() != true) continue

                // Always count the row where the name was found
                if (pg == targetPage && y == targetYKey) {
                    pos++
                    break
                }

                val diff = y - prevY
                if (diff < 3) continue // merge split names on same row
                pos++
                prevY = y
            }
        }
        return pos
    }

    fun installUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdating.value = true
            try {
                val info = UpdateManager.checkForUpdate(context)
                when {
                    info == null -> _errorMessage.emit("Error al comprobar actualizacion. Sin conexion?")
                    !info.isNewer -> _errorMessage.emit("Ya tienes la ultima version")
                    else -> {
                        val ok = UpdateManager.downloadAndInstall(context, info.downloadUrl)
                        if (!ok) _errorMessage.emit("Error al descargar la actualizacion")
                        else _appVersion.value = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?" } catch (_: Exception) { "?" }
                    }
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
        if (!_autoRefreshEnabled.value) { livePollJob?.cancel(); return }
        if (livePollJob?.isActive != true) startLivePolling()
    }

    fun parseMadridDate(dateTime: String): Date? {
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
        }
    }

    private suspend fun fetchLiveResults() {
        val (scoreUpdates, cardUpdates) = liveScoreService.fetchScoreUpdates(cachedMatches)

        scoreUpdates.forEach { update ->
            if (update.isFinished) liveMinutes[update.matchId] = "FINAL"
            else if (update.liveMinute != null) liveMinutes[update.matchId] = update.liveMinute
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
                val homeScr = update.homeScorers.map { GoalEvent(it.playerName, it.minute) }
                val awayScr = update.awayScorers.map { GoalEvent(it.playerName, it.minute) }
                if (homeScr.isNotEmpty() || awayScr.isNotEmpty()) {
                    goalScorers[update.matchId] = Pair(homeScr, awayScr)
                }
            }
        }

        cardUpdates.forEach { card ->
            cachedMatches = cachedMatches.map {
                if (it.id == card.matchId) it.copy(
                    homeRedCards = card.homeReds, awayRedCards = card.awayReds,
                    homeYellowCards = card.homeYellows, awayYellowCards = card.awayYellows
                ) else it
            }
            repository.updateMatchCards(card.matchId, card.homeReds, card.awayReds, card.homeYellows, card.awayYellows)
        }

        // Actualizar marcadores desde football-data.org
        try {
            val liveUpdates = liveScoreService.fetchLiveMatchDetails(cachedMatches)
            liveUpdates.forEach { update ->
                val match = cachedMatches.firstOrNull { it.id == update.matchId }
                if (match != null) {
                    val start = parseMadridDate(match.dateTime)
                    if (start != null && start.after(Date())) return@forEach
                }
                cachedMatches = cachedMatches.map {
                    if (it.id == update.matchId) it.copy(
                        homeGoals = update.homeGoals,
                        awayGoals = update.awayGoals
                    ) else it
                }
                recalcAllPoints()
            }
        } catch (_: Exception) { }

        // Actualizar minuto real desde api-sports.io (sobrescribe "LIVE")
        try {
            val minuteUpdates = liveScoreService.fetchApiFootballMinutes(cachedMatches)
            minuteUpdates.forEach { update ->
                val m = cachedMatches.firstOrNull { it.id == update.matchId }
                if (m != null) {
                    val start = parseMadridDate(m.dateTime)
                    if (start != null && start.after(Date())) return@forEach
                }
                if (update.liveMinute != null) {
                    liveMinutes[update.matchId] = update.liveMinute
                }
            }
        } catch (_: Exception) { }

        checkGoalNotifications()
        refreshUpcomingMatches()
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
        } ?: if (status == MatchStatus.LIVE && date != null) {
            val elapsed = ((Date().time - date.time) / 60000).toInt().coerceAtLeast(1)
            if (elapsed > 45) "${(elapsed + 15).coerceAtMost(120)}'" else "$elapsed'"
        } else null
        val s = goalScorers[match.id]
        val homeScr = s?.first ?: emptyList()
        val awayScr = s?.second ?: emptyList()
        val dayAbbrFmt = SimpleDateFormat("EEE", Locale("es", "ES")).apply { timeZone = madridTZ }
        val dayNumFmt = SimpleDateFormat("dd", Locale.US).apply { timeZone = madridTZ }
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
        if (lm != null && lm != "FINAL") return MatchStatus.LIVE
        if (match.homeGoals != null && match.awayGoals != null) return MatchStatus.FINISHED
        val parsed = start ?: return MatchStatus.UPCOMING
        val now = Date()
        val end = Date(parsed.time + 150L * 60 * 1000)
        return if (now.after(parsed) && now.before(end)) MatchStatus.LIVE else MatchStatus.UPCOMING
    }

    override fun onCleared() {
        refreshJob?.cancel()
        livePollJob?.cancel()
        super.onCleared()
    }

    fun setForegroundState(isForeground: Boolean) {
        isInForeground = isForeground
        if (isForeground) {
            startLiveMinutePolling()
        } else {
            stopLiveMinutePolling()
        }
    }

    private var liveMinutePollJob: Job? = null

    private fun startLiveMinutePolling() {
        liveMinutePollJob?.cancel()
        liveMinutePollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && isInForeground) {
                try {
                    fetchLiveResults()
                } catch (e: Exception) {
                    Log.d("HomeVM", "Live minute polling error: ${e.message}")
                }
                delay(5 * 60_000L)
            }
        }
    }

    private fun stopLiveMinutePolling() {
        liveMinutePollJob?.cancel()
        liveMinutePollJob = null
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
}
