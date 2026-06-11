package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import com.porrawc2026.app.util.GoalNotifier
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
    val dayKey: String = ""
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
    private val _isBusy = MutableStateFlow(false)
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

    private var cachedMatches: List<MatchEntity> = emptyList()
    private var refreshJob: Job? = null
    private var livePollJob: Job? = null
    private val lastWrittenScores = mutableMapOf<Int, Pair<Int, Int>>()
    private val goalScorers = mutableMapOf<Int, Pair<List<GoalEvent>, List<GoalEvent>>>()
    private val seenScorers = mutableMapOf<Int, MutableSet<String>>()

    companion object {
        val WC_TEAMS = setOf("Mexico", "South Africa", "South Korea", "Czech Republic", "Canada",
            "Switzerland", "Qatar", "Bosnia-Herzegovina", "Brazil", "Morocco", "Haiti", "Scotland",
            "United States", "Paraguay", "Turkey", "Australia", "Germany", "Curacao", "Ivory Coast",
            "Ecuador", "Netherlands", "Japan", "Sweden", "Tunisia", "Belgium", "Egypt", "Iran",
            "New Zealand", "Spain", "Cape Verde", "Saudi Arabia", "Uruguay", "France", "Senegal",
            "Norway", "Iraq", "Argentina", "Jordan", "Austria", "Algeria", "Portugal", "Uzbekistan",
            "Colombia", "Congo DR", "England", "Croatia", "Panama", "Ghana")
    }

    init {
        val prefs = context.getSharedPreferences("porra_prefs", Context.MODE_PRIVATE)
        _excelFileName.value = prefs.getString("excel_filename", null)
        _autoRefreshEnabled.value = prefs.getBoolean("auto_refresh", true)
        refreshPoints(); loadPlayers(); preloadSchedule(); precachePhotos()
        forceCheckUpdate()
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
                            val name = it.getString(nameIndex)
                            _excelFileName.value = name
                            context.getSharedPreferences("porra_prefs", Context.MODE_PRIVATE).edit().putString("excel_filename", name).apply()
                        }
                    }
                }
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

    fun forceCheckUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = UpdateManager.checkForUpdate(context)
            _updateAvailable.value = info?.isNewer == true
        }
    }

    fun toggleNotifications() {
        _notificationsEnabled.value = !_notificationsEnabled.value
    }

    fun toggleAutoRefresh() {
        _autoRefreshEnabled.value = !_autoRefreshEnabled.value
        context.getSharedPreferences("porra_prefs", Context.MODE_PRIVATE).edit().putBoolean("auto_refresh", _autoRefreshEnabled.value).apply()
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
            context.getSharedPreferences("porra_prefs", Context.MODE_PRIVATE).edit().putString("excel_filename", null).apply()
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
        val allScorers = goalScorers.values.flatMap { it.first + it.second }
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
                    match.copy(dateTime = apiMatch.utcDate)
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
        if (!_autoRefreshEnabled.value) { livePollJob?.cancel(); return }
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
                    try {
                        val detail = apiService.getMatchDetail(matchId = fm.id)
                        val homeScr = detail.goals?.filter { g -> val t = g.team?.name; t == detail.homeTeam?.name || (t != null && detail.homeTeam?.name?.contains(t, true) == true) }
                            ?.mapNotNull { g -> val n = g.scorer?.name ?: return@mapNotNull null; val m = g.minute ?: 0; GoalEvent(n, m) } ?: emptyList()
                        val awayScr = detail.goals?.filter { g -> val t = g.team?.name; t == detail.awayTeam?.name || (t != null && detail.awayTeam?.name?.contains(t, true) == true) }
                            ?.mapNotNull { g -> val n = g.scorer?.name ?: return@mapNotNull null; val m = g.minute ?: 0; GoalEvent(n, m) } ?: emptyList()
                        if (homeScr.isNotEmpty() || awayScr.isNotEmpty()) {
                            goalScorers[entity.id] = Pair(homeScr.reversed(), awayScr.reversed())
                        }
                    } catch (_: Exception) {}
                    recalcAllPoints()
                    refreshPoints()
                    refreshUpcomingMatches()
                }
            }
        }
        val wcScraped = withContext(Dispatchers.IO) { LiveScoreScraper.fetchWcMatches() }
        wcScraped.forEach { sm ->
            val cm = cachedMatches.firstOrNull {
                it.homeTeam.contains(sm.homeTeam, true) || sm.homeTeam.contains(it.homeTeam, true)
            } ?: return@forEach
            val eId = sm.eventId
            if (eId > 0) {
                val (h, a) = withContext(Dispatchers.IO) { LiveScoreScraper.fetchGoalDetails(eId) }
                if (h.isNotEmpty() || a.isNotEmpty()) {
                    goalScorers[cm.id] = Pair(
                        h.map { GoalEvent(it.playerName, it.minute) },
                        a.map { GoalEvent(it.playerName, it.minute) }
                    )
                }
            }
        }
        checkGoalNotifications()
    }

    private fun refreshUpcomingMatches() {
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
        val nowCal = Calendar.getInstance(madridTZ)
        val todayDoy = nowCal.get(Calendar.DAY_OF_YEAR)
        val thisYear = nowCal.get(Calendar.YEAR)
        _dayKeys.value = allDisplay.mapNotNull { d ->
            if (d.dayKey.isBlank()) return@mapNotNull null
            d.dayKey
        }.distinct()
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

        val liveMin = when (status) {
            MatchStatus.FINISHED -> "FINAL"
            else -> null
        }
        val s = goalScorers[match.id]
        val homeScr = s?.first ?: emptyList()
        val awayScr = s?.second ?: emptyList()
        val dayAbbrFmt = SimpleDateFormat("EEE", Locale("es", "ES")).apply { timeZone = madridTZ }
        val dayNumFmt = SimpleDateFormat("dd", Locale.US).apply { timeZone = madridTZ }
        val dayKey = if (date != null) "${dayAbbrFmt.format(date).replace(".", "").uppercase()} ${dayNumFmt.format(date)}" else ""

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
            dayKey = dayKey
        )
    }

    private fun matchStatus(match: MatchEntity): MatchStatus {
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
            val hardcodedTv = fb?.getOrNull(1) ?: ""
            val scrapedTv = TvScraper.lookupTv(match.homeTeam, match.awayTeam, context.filesDir)
            val tv = when {
                match.tvChannel.isNotBlank() && scrapedTv == "DAZN" -> match.tvChannel
                scrapedTv != "DAZN" -> scrapedTv
                match.tvChannel.isNotBlank() -> match.tvChannel
                hardcodedTv.isNotBlank() -> hardcodedTv
                else -> "DAZN"
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
