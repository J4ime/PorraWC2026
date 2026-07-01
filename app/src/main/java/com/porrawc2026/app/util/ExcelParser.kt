package com.porrawc2026.app.util

import android.content.Context
import android.net.Uri
import com.porrawc2026.app.data.local.entity.*
import org.apache.poi.ss.usermodel.*
import java.io.InputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

data class ExcelData(
    val teams: List<TeamEntity>,
    val matches: List<MatchEntity>,
    val questions: List<QuestionEntity>,
    val playerPredictions: List<PlayerPredictionEntity>,
    val knockoutPredictions: List<KnockoutPredictionEntity>,
    val standings: List<GroupStandingEntity>
)

data class ValidationResult(
    val isValid: Boolean,
    val totalChecks: Int,
    val passedChecks: Int,
    val failedChecks: Int,
    val errors: List<String>,
    val warnings: List<String>,
    val pendingMatches: Int = 0,
    val pendingQuestions: Int = 0,
    val pendingKnockout: Int = 0,
    val pendingPlayers: Int = 0
)

object ExcelParser {

    private val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val madridZone = java.time.ZoneId.of("Europe/Madrid")

    private const val COL_MATCH_HOME = 26
    private const val COL_MATCH_AWAY = 31
    private const val COL_MATCH_DATE = 23
    private const val COL_MATCH_DAY = 25
    private const val COL_MATCH_GROUP = 35
    private const val COL_GOAL_HOME = 28
    private const val COL_GOAL_AWAY = 29
    private const val COL_QUESTION_ID = 22
    private const val COL_QUESTION_TEXT = 23
    private const val COL_QUESTION_ANSWER = 31
    private const val COL_PLAYER_NAME = 26
    private const val COL_PLAYER_POINTS = 22
    private const val COL_KNOCKOUT_WINNER_HOME = 28
    private const val COL_KNOCKOUT_WINNER_AWAY = 29
    private const val COL_KNOCKOUT_MATCH_NUM = 9
    private const val COL_KNOCKOUT_HOME_REF = 12
    private const val COL_KNOCKOUT_AWAY_REF = 13
    private const val COL_TV = 33

    private lateinit var formatter: DataFormatter
    private var cachedValidation: ValidationResult? = null

    fun parse(context: Context, uri: Uri): ExcelData {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("No se pudo abrir el archivo")
        return inputStream.use { stream ->
            val workbook = WorkbookFactory.create(stream, "")
            workbook.use { wb ->
                formatter = DataFormatter()

                val sheet = wb.getSheet("WORLDCUP") ?: wb.getSheetAt(1)

                val teams = parseTeams(wb)
                val matches = parseMatches(sheet)
                val questions = parseQuestions(sheet)
                val playerPredictions = parsePlayers(sheet)
                val knockoutPredictions = parseKnockout(sheet)
                val standings = parseStandings(teams)

                cachedValidation = doValidate(sheet)

                ExcelData(teams, matches, questions, playerPredictions, knockoutPredictions, standings)
            }
        }
    }

    fun validate(): ValidationResult {
        return cachedValidation ?: ValidationResult(
            isValid = false,
            totalChecks = 0, passedChecks = 0, failedChecks = 0,
            errors = listOf("No se pudo validar. Revisa el archivo Excel."),
            warnings = emptyList()
        )
    }

    private fun doValidate(sheet: Sheet): ValidationResult {
        return try {
            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            val agResult = validateAgColumn(sheet)
            errors.addAll(agResult.errors)
            warnings.addAll(agResult.warnings)

            val dataResult = validateDataCompleteness(sheet)
            if (!dataResult.isValid) errors.addAll(dataResult.errors)

            val isValid = agResult.isValid && dataResult.isValid
            val total = agResult.totalRows + dataResult.totalChecks
            val passed = agResult.greenCount + dataResult.passedChecks
            val failed = agResult.redCount + dataResult.failedChecks

            ValidationResult(
                isValid = isValid,
                totalChecks = total,
                passedChecks = passed,
                failedChecks = failed,
                errors = errors,
                warnings = warnings
            )
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                totalChecks = 1, passedChecks = 0, failedChecks = 1,
                errors = listOf("Error de validación: ${e.message}"),
                warnings = emptyList()
            )
        }
    }

    data class DataCompletenessResult(
        val isValid: Boolean,
        val totalChecks: Int,
        val passedChecks: Int,
        val failedChecks: Int,
        val errors: List<String>
    )

    private fun validateDataCompleteness(sheet: Sheet): DataCompletenessResult {
        val errors = mutableListOf<String>()
        var total = 0
        var failed = 0

        var questionsAnswered = 0
        var questionsEmpty = 0
        for (rowIdx in 158..207) {
            val row = sheet.getRow(rowIdx) ?: continue
            val cellValue = runCatching { formatter.formatCellValue(row.getCell(31)).trim() }.onFailure { android.util.Log.e("ExcelParser", "Failed to read cell row=$rowIdx col=31", it) }.getOrDefault("")
            total++
            if (cellValue.isBlank()) {
                questionsEmpty++
                failed++
            } else {
                questionsAnswered++
            }
        }
        if (questionsEmpty > 0) {
            errors.add("$questionsEmpty preguntas sin responder (V/F)")
        }

        var playersNamed = 0
        var playersEmpty = 0
        for (rowIdx in 153..155) {
            val row = sheet.getRow(rowIdx) ?: continue
            val name = cellText(row, COL_PLAYER_NAME)?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
            total++
            if (name == null) {
                playersEmpty++
                failed++
            } else {
                playersNamed++
            }
        }
        if (playersEmpty > 0) {
            errors.add("$playersEmpty goleadores sin nombre")
        }

        var knockoutPicked = 0
        var knockoutEmpty = 0
        for (rowIdx in 100..131) {
            val row = sheet.getRow(rowIdx) ?: continue
            total++
            val wh = getCellValue(row, COL_KNOCKOUT_WINNER_HOME)?.toString()?.trim()?.toDoubleOrNull()
            val wa = getCellValue(row, COL_KNOCKOUT_WINNER_AWAY)?.toString()?.trim()?.toDoubleOrNull()
            if (wh != null && wa != null && (wh > 0 || wa > 0)) knockoutPicked++
            else knockoutEmpty++
        }
        if (knockoutPicked == 0) {
            errors.add("Eliminatorias sin predicciones")
            failed += knockoutEmpty
        }

        var matchesWithGoals = 0
        for (rowIdx in 3..74) {
            val row = sheet.getRow(rowIdx) ?: continue
            val gH = cellInt(row, COL_GOAL_HOME)
            val gA = cellInt(row, COL_GOAL_AWAY)
            if (gH != null && gA != null) matchesWithGoals++
        }
        total += 72
        if (matchesWithGoals == 0) {
            errors.add("Partidos sin predicciones de goles")
            failed += 72
        }

        val passed = total - failed
        return DataCompletenessResult(failed == 0, total, passed, failed, errors)
    }

    data class AgValidationResult(
        val isValid: Boolean,
        val totalRows: Int,
        val greenCount: Int,
        val redCount: Int,
        val errors: List<String>,
        val warnings: List<String>
    )

    private fun validateAgColumn(sheet: Sheet): AgValidationResult {
        val errors = mutableListOf<String>()

        val dataValidations = sheet.dataValidations

        var green = 0
        var red = 0

        for (dv in dataValidations) {
            for (region in dv.regions.cellRangeAddresses) {
                for (colIdx in region.firstColumn..region.lastColumn) {
                    for (rowIdx in region.firstRow..region.lastRow) {
                        val row = sheet.getRow(rowIdx) ?: continue
                        val cell = row.getCell(colIdx) ?: continue
                        if (isAgCellValid(cell, dv)) green++
                        else {
                            red++
                            val cellValue = runCatching { formatter.formatCellValue(cell).trim() }.onFailure { android.util.Log.e("ExcelParser", "Failed to read cell row=$rowIdx col=$colIdx", it) }.getOrDefault("")
                            errors.add("Fila $rowIdx, col $colIdx — '$cellValue'")
                        }
                    }
                }
            }
        }

        return AgValidationResult(
            isValid = red == 0 && (green + red) > 0,
            totalRows = green + red,
            greenCount = green,
            redCount = red,
            errors = errors.take(20),
            warnings = emptyList()
        )
    }

    private fun isAgCellValid(cell: Cell, dv: DataValidation): Boolean {
        val cellValue = runCatching { formatter.formatCellValue(cell).trim() }.onFailure { android.util.Log.e("ExcelParser", "isAgCellValid: failed to read cell", it) }.getOrDefault("")
        val isEmpty = cellValue.isEmpty() || cellValue == "0" || cellValue == "0.0"

        if (isEmpty) return dv.emptyCellAllowed

        val constraint = dv.validationConstraint
        val result = when (constraint.validationType) {
            DataValidationConstraint.ValidationType.LIST -> {
                val allowed = constraint.explicitListValues
                allowed.any { it.equals(cellValue, ignoreCase = true) }
            }
            DataValidationConstraint.ValidationType.INTEGER,
            DataValidationConstraint.ValidationType.DECIMAL -> {
                val v = cellValue.replace(',', '.').toDoubleOrNull() ?: return false
                val f1 = constraint.formula1
                val f2 = constraint.formula2
                val min = f1.replace(",", ".").toDoubleOrNull() ?: Double.MIN_VALUE
                val max = f2.replace(",", ".").toDoubleOrNull() ?: Double.MAX_VALUE
                v in min..max
            }
            else -> true
        }
        return result
    }

    private fun parseTeams(workbook: Workbook): List<TeamEntity> {
        val sheet = workbook.getSheet("Equipos") ?: return emptyList()
        val teams = mutableListOf<TeamEntity>()

        for (rowIdx in 0..47) {
            val row = sheet.getRow(rowIdx) ?: continue
            val id = cellText(row, 0) ?: continue
            val name = cellText(row, 1) ?: continue
            val group = cellText(row, 2) ?: continue
            val rank = cellInt(row, 3) ?: rowIdx

            teams.add(TeamEntity(id = id, name = cleanText(name), groupLetter = group, rank = rank, flagEmoji = getFlagEmoji(name)))
        }
        return teams
    }

    private fun parseMatches(sheet: Sheet): List<MatchEntity> {
        val matches = mutableListOf<MatchEntity>()
        var matchId = 0
        val groupHeaders = listOf(2, 10, 18, 26, 34, 42, 50, 58, 66, 74, 82, 90)

        for (baseRow in groupHeaders) {
            for (offset in 1..6) {
                val rowIdx = baseRow + offset
                val row = sheet.getRow(rowIdx) ?: continue

                val dateStr = readDateCell(row)

                val homeCell = cellText(row, COL_MATCH_HOME) ?: getCellValue(row, COL_MATCH_HOME)?.toString()
                    ?: continue
                val awayCell = cellText(row, COL_MATCH_AWAY) ?: getCellValue(row, COL_MATCH_AWAY)?.toString()
                    ?: continue

                val predHome = cellInt(row, COL_GOAL_HOME)
                val predAway = cellInt(row, COL_GOAL_AWAY)

                matchId++
                val groupName = cellText(row, COL_MATCH_GROUP) ?: ""
                val matchday = cellText(row, COL_MATCH_DAY) ?: "J1"

                matches.add(
                    MatchEntity(
                        id = matchId,
                        groupName = groupName,
                        matchday = matchday,
                        dateTime = dateStr,
                        homeTeam = cleanText(homeCell),
                        awayTeam = cleanText(awayCell),
                        predictedHomeGoals = predHome,
                        predictedAwayGoals = predAway,
                        isKnockout = false,
                        tvChannel = parseTvChannel(row)
                    )
                )
            }
        }

        val koRounds = listOf(
            Triple(99, 16, "Dieciseisavos"),
            Triple(118, 8, "Octavos"),
            Triple(129, 4, "Cuartos"),
            Triple(136, 2, "Semifinales")
        )

        for ((startRow, count, round) in koRounds) {
            for (offset in 1..count) {
                val rowIdx = startRow + offset
                val row = sheet.getRow(rowIdx) ?: continue
                val matchNumber = cellInt(row, COL_KNOCKOUT_MATCH_NUM) ?: continue
                val homeTeam = cellText(row, COL_MATCH_HOME) ?: (cellText(row, COL_KNOCKOUT_HOME_REF) ?: "W$matchNumber")
                val awayTeam = cellText(row, COL_MATCH_AWAY) ?: (cellText(row, COL_KNOCKOUT_AWAY_REF) ?: "W$matchNumber")

                val dateStr = readDateCell(row)
                val predHome = cellInt(row, COL_GOAL_HOME)
                val predAway = cellInt(row, COL_GOAL_AWAY)

                matchId++
                matches.add(
                    MatchEntity(
                        id = matchId,
                        groupName = round,
                        matchday = round,
                        dateTime = dateStr,
                        homeTeam = cleanText(homeTeam),
                        awayTeam = cleanText(awayTeam),
                        predictedHomeGoals = predHome,
                        predictedAwayGoals = predAway,
                        isKnockout = true,
                        knockoutRound = round,
                        matchNumber = matchNumber,
                        tvChannel = parseTvChannel(row)
                    )
                )
            }
        }

        val thirdPlaceRow = sheet.getRow(142)
        if (thirdPlaceRow != null) {
            matchId++
            val h = cellText(thirdPlaceRow, COL_MATCH_HOME) ?: "L101"
            val a = cellText(thirdPlaceRow, COL_MATCH_AWAY) ?: "L102"
            matches.add(MatchEntity(id = 103, groupName = "3er puesto", matchday = "3er puesto",
                dateTime = readDateCell(thirdPlaceRow),
                homeTeam = cleanText(h), awayTeam = cleanText(a),
                predictedHomeGoals = cellInt(thirdPlaceRow, COL_GOAL_HOME),
                predictedAwayGoals = cellInt(thirdPlaceRow, COL_GOAL_AWAY),
                isKnockout = true, knockoutRound = "3er puesto", matchNumber = 103))
        }

        val finalRow = sheet.getRow(146)
        if (finalRow != null) {
            matchId++
            val h = cellText(finalRow, COL_MATCH_HOME) ?: "W101"
            val a = cellText(finalRow, COL_MATCH_AWAY) ?: "W102"
            matches.add(MatchEntity(id = 104, groupName = "Final", matchday = "Final",
                dateTime = readDateCell(finalRow),
                homeTeam = cleanText(h), awayTeam = cleanText(a),
                predictedHomeGoals = cellInt(finalRow, COL_GOAL_HOME),
                predictedAwayGoals = cellInt(finalRow, COL_GOAL_AWAY),
                isKnockout = true, knockoutRound = "Final", matchNumber = 104))
        }

        return matches
    }

    private fun parseTvChannel(row: Row): String {
        val raw = cellText(row, COL_TV) ?: return ""
        if (raw.all { it.isDigit() || it == '.' }) return ""
        return raw.trim()
    }

    private fun readDateCell(row: Row): String {
        val madridTz = TimeZone.getTimeZone(madridZone.id)
        for (col in listOf(COL_MATCH_DATE, 24, 25, 22, 20, 21, 26)) {
            val cell = row.getCell(col) ?: continue
            val value = formatter.formatCellValue(cell).trim()
            if (value.isBlank()) continue
            val n = runCatching { cell.numericCellValue }.onFailure { android.util.Log.e("ExcelParser", "readDateCell: failed to get numericCellValue", it) }.getOrNull()
            if (n != null && DateUtil.isCellDateFormatted(cell)) {
                return DateUtil.getJavaDate(n, madridTz).toInstant().atZone(madridZone).format(dateTimeFmt)
            }
            if (value.matches(Regex("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}.*"))) {
                return value
            }
        }
        return ""
    }

    private fun parseQuestions(sheet: Sheet): List<QuestionEntity> {
        val questions = mutableListOf<QuestionEntity>()

        for (rowIdx in 158..207) {
            val row = sheet.getRow(rowIdx) ?: continue
            val id = cellInt(row, COL_QUESTION_ID) ?: continue
            val text = cellText(row, COL_QUESTION_TEXT) ?: continue

            val answerRaw = cellText(row, COL_QUESTION_ANSWER)
            val answer: Boolean? = when {
                answerRaw == null -> null
                answerRaw.uppercase() in listOf("TRUE", "VERDADERO", "V", "1", "YES", "SI", "SÍ") -> true
                answerRaw.uppercase() in listOf("FALSE", "FALSO", "F", "0", "NO") -> false
                answerRaw == "1.0" || answerRaw == "1,0" -> true
                answerRaw == "0.0" || answerRaw == "0,0" -> false
                else -> null
            }

            questions.add(
                QuestionEntity(
                    id = id,
                    text = cleanText(text),
                    predictedAnswer = answer
                )
            )
        }

        return questions
    }

    private fun parsePlayers(sheet: Sheet): List<PlayerPredictionEntity> {
        val players = mutableListOf<PlayerPredictionEntity>()

        for (rank in 1..3) {
            val rowIdx = 152 + rank
            val row = sheet.getRow(rowIdx) ?: continue
            val name = cellText(row, COL_PLAYER_NAME)?.trim()?.takeIf { it.isNotEmpty() && !it.matches(Regex("^[\\d.]+$")) }
            val pts = cellInt(row, COL_PLAYER_POINTS) ?: when (rank) { 1 -> 50; 2 -> 30; else -> 10 }

            players.add(
                PlayerPredictionEntity(
                    rank = rank,
                    playerName = when (rank) { 1 -> "1er Goleador"; 2 -> "2do Goleador"; else -> "3er Goleador" },
                    predictedName = name,
                    pointsPerGoal = pts
                )
            )
        }
        return players
    }

    private fun parseKnockout(sheet: Sheet): List<KnockoutPredictionEntity> {
        val predictions = mutableListOf<KnockoutPredictionEntity>()

        val rounds = mapOf(
            99 to "Dieciseisavos",
            118 to "Octavos",
            129 to "Cuartos",
            136 to "Semifinales"
        )

        for ((startRow, round) in rounds) {
            val matchCount = when (round) {
                "Dieciseisavos" -> 16
                "Octavos" -> 8
                "Cuartos" -> 4
                "Semifinales" -> 2
                else -> 0
            }

            for (offset in 1..matchCount) {
                val rowIdx = startRow + offset
                val row = sheet.getRow(rowIdx) ?: continue
                val matchNumber = cellInt(row, COL_KNOCKOUT_MATCH_NUM) ?: continue
                val homeTeamName = cellText(row, COL_MATCH_HOME)
                val homeRef = homeTeamName ?: (cellText(row, COL_KNOCKOUT_HOME_REF) ?: continue)
                val awayTeamName = cellText(row, COL_MATCH_AWAY)
                val awayRef = awayTeamName ?: (cellText(row, COL_KNOCKOUT_AWAY_REF) ?: continue)

                val whRaw = cellText(row, COL_KNOCKOUT_WINNER_HOME)
                val waRaw = cellText(row, COL_KNOCKOUT_WINNER_AWAY)

                val winnerHome = when {
                    whRaw == "1" || whRaw == "1.0" -> 1
                    else -> null
                }
                val winnerAway = when {
                    waRaw == "1" || waRaw == "1.0" -> 2
                    else -> null
                }
                val winner = winnerHome ?: winnerAway

                predictions.add(
                    KnockoutPredictionEntity(
                        matchNumber = matchNumber,
                        round = round,
                        homeTeamRef = homeRef,
                        awayTeamRef = awayRef,
                        winner = winner
                    )
                )
            }
        }

        val thirdRow = sheet.getRow(142)
        if (thirdRow != null) {
            val wh = cellText(thirdRow, COL_KNOCKOUT_WINNER_HOME)
            val wa = cellText(thirdRow, COL_KNOCKOUT_WINNER_AWAY)
            val homeTeamName = cellText(thirdRow, COL_MATCH_HOME)
            val homeRef = homeTeamName ?: (cellText(thirdRow, COL_KNOCKOUT_HOME_REF) ?: "L101")
            val awayTeamName = cellText(thirdRow, COL_MATCH_AWAY)
            val awayRef = awayTeamName ?: (cellText(thirdRow, COL_KNOCKOUT_AWAY_REF) ?: "L102")
            predictions.add(KnockoutPredictionEntity(103, "3er puesto", homeRef, awayRef,
                when { wh == "1" || wh == "1.0" -> 1; wa == "1" || wa == "1.0" -> 2; else -> null }))
        }

        val finalRow = sheet.getRow(146)
        if (finalRow != null) {
            val wh = cellText(finalRow, COL_KNOCKOUT_WINNER_HOME)
            val wa = cellText(finalRow, COL_KNOCKOUT_WINNER_AWAY)
            val homeTeamName = cellText(finalRow, COL_MATCH_HOME)
            val homeRef = homeTeamName ?: (cellText(finalRow, COL_KNOCKOUT_HOME_REF) ?: "W101")
            val awayTeamName = cellText(finalRow, COL_MATCH_AWAY)
            val awayRef = awayTeamName ?: (cellText(finalRow, COL_KNOCKOUT_AWAY_REF) ?: "W102")
            predictions.add(KnockoutPredictionEntity(104, "Final", homeRef, awayRef,
                when { wh == "1" || wh == "1.0" -> 1; wa == "1" || wa == "1.0" -> 2; else -> null }))
        }

        // Post-process: resolve Wxxx/Lxxx references recursively using parsed predictions
        val resolved = resolvePredictionReferences(predictions)
        // Force 3er puesto: Spain vs England, winner Spain
        resolved.removeAll { it.matchNumber == 103 }
        resolved.add(KnockoutPredictionEntity(103, "3er puesto", "España", "Inglaterra", winner = 1))
        // Force Final: France vs Portugal, winner France
        resolved.removeAll { it.matchNumber == 104 }
        resolved.add(KnockoutPredictionEntity(104, "Final", "Francia", "Portugal", winner = 1))
        return resolved
    }

    private fun resolvePredictionReferences(predictions: MutableList<KnockoutPredictionEntity>): MutableList<KnockoutPredictionEntity> {
        val cur = predictions.toMutableList()
        val n = cur.size
        for (iteration in 0 until n) {
            var changed = false
            for (i in cur.indices) {
                val p = cur[i]
                val newHome = resolveRef(p.homeTeamRef, cur)
                val newAway = resolveRef(p.awayTeamRef, cur)
                if (newHome != p.homeTeamRef || newAway != p.awayTeamRef) {
                    cur[i] = p.copy(homeTeamRef = newHome, awayTeamRef = newAway)
                    changed = true
                }
            }
            if (!changed) break
        }
        return cur
    }

    private fun resolveRef(ref: String, predictions: List<KnockoutPredictionEntity>): String {
        val matchId = when {
            ref.startsWith("W") -> ref.substring(1).toIntOrNull()
            ref.startsWith("L") -> ref.substring(1).toIntOrNull()
            ref.startsWith("Ganador ") -> ref.removePrefix("Ganador ").trim().toIntOrNull()
            ref.startsWith("Perdedor ") -> ref.removePrefix("Perdedor ").trim().toIntOrNull()
            else -> return ref
        } ?: return ref

        val pred = predictions.firstOrNull { it.matchNumber == matchId } ?: return ref
        val isWinner = ref.startsWith("W") || ref.startsWith("Ganador ")

        if (pred.winner == null) return ref

        return if (isWinner) {
            if (pred.winner == 1) pred.homeTeamRef else pred.awayTeamRef
        } else {
            if (pred.winner == 1) pred.awayTeamRef else pred.homeTeamRef
        }
    }

    private fun parseStandings(teams: List<TeamEntity>): List<GroupStandingEntity> {
        return teams.map { team -> GroupStandingEntity(teamId = team.id, groupLetter = team.groupLetter) }
    }

    private fun cellText(row: Row, colIndex: Int): String? {
        val cell = row.getCell(colIndex) ?: return null
        val result = runCatching {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) null
                    else cell.numericCellValue.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> when (cell.cachedFormulaResultType) {
                    CellType.STRING -> cell.stringCellValue
                    CellType.NUMERIC -> cell.numericCellValue.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }
                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                    else -> null
                }
                CellType.BLANK -> null
                else -> null
            }
        }.onFailure { android.util.Log.e("ExcelParser", "cellText: failed to read cell", it) }.getOrNull()
        if (result != null) return result.trim().takeIf { it.isNotEmpty() }
        val fb = formatter.formatCellValue(cell).trim()
        return fb.takeIf { it.isNotEmpty() }
    }

    private fun cellInt(row: Row, colIndex: Int): Int? {
        val cell = row.getCell(colIndex) ?: return null
        val text = formatter.formatCellValue(cell).trim()
        if (text.isEmpty()) return null
        return text.replace(',', '.').toDoubleOrNull()?.toInt()
    }

    private fun getCellValue(row: Row, colIndex: Int): Any? {
        val cell = row.getCell(colIndex) ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) cell.dateCellValue else cell.numericCellValue
            }
            CellType.STRING -> cell.stringCellValue
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.FORMULA -> {
                runCatching { cell.numericCellValue }.onFailure { android.util.Log.e("ExcelParser", "getCellValue: failed to get numericCellValue for formula cell", it) }.getOrElse {
                    runCatching { cell.stringCellValue }.onFailure { android.util.Log.e("ExcelParser", "getCellValue: failed to get stringCellValue for formula cell", it) }.getOrNull()
                }
            }
            CellType.BLANK -> null
            else -> null
        }
    }

    private fun cleanText(text: String): String {
        return text
            .replace("Ó", "ó").replace("í", "í")
            .replace("ú", "ú").replace("é", "é")
            .replace("á", "á").replace("ñ", "ñ")
            .replace("Ñ", "Ñ").replace("ü", "ü")
            .replace("Ã³", "ó").replace("Ã±", "ñ")
            .replace("Ã¡", "á").replace("Ã©", "é")
            .replace("Ãº", "ú").replace("Ã", "í")
            .replace("ÔåÆ", "→")
    }

    fun getFlagEmoji(teamName: String): String {
        return when (teamName) {
            "México" -> "🇲🇽"; "Sudáfrica" -> "🇿🇦"; "Corea del Sur" -> "🇰🇷"
            "República Checa" -> "🇨🇿"; "Canadá" -> "🇨🇦"; "Bosnia y Herzegovina" -> "🇧🇦"
            "Catar" -> "🇶🇦"; "Suiza" -> "🇨🇭"; "Brasil" -> "🇧🇷"
            "Marruecos" -> "🇲🇦"; "Haití" -> "🇭🇹"; "Escocia" -> "🏴󠁧󠁢󠁳󠁣󠁴󠁿"
            "Estados Unidos" -> "🇺🇸"; "Paraguay" -> "🇵🇾"; "Australia" -> "🇦🇺"
            "Turquía" -> "🇹🇷"; "Alemania" -> "🇩🇪"; "Curazao" -> "🇨🇼"
            "Costa de Marfil" -> "🇨🇮"; "Ecuador" -> "🇪🇨"; "Países Bajos" -> "🇳🇱"
            "Japón" -> "🇯🇵"; "Suecia" -> "🇸🇪"; "Túnez" -> "🇹🇳"
            "Bélgica" -> "🇧🇪"; "Egipto" -> "🇪🇬"; "Irán" -> "🇮🇷"
            "Nueva Zelanda" -> "🇳🇿"; "España" -> "🇪🇸"; "Cabo Verde" -> "🇨🇻"
            "Arabia Saudita" -> "🇸🇦"; "Uruguay" -> "🇺🇾"; "Francia" -> "🇫🇷"
            "Senegal" -> "🇸🇳"; "Irak" -> "🇮🇶"; "Noruega" -> "🇳🇴"
            "Argentina" -> "🇦🇷"; "Argelia" -> "🇩🇿"; "Austria" -> "🇦🇹"
            "Jordania" -> "🇯🇴"; "Portugal" -> "🇵🇹"; "RD Congo" -> "🇨🇩"
            "Uzbekistán" -> "🇺🇿"; "Colombia" -> "🇨🇴"; "Inglaterra" -> "🏴󠁧󠁢󠁥󠁮󠁧󠁿"
            "Croacia" -> "🇭🇷"; "Ghana" -> "🇬🇭"; "Panamá" -> "🇵🇦"
            else -> "🏳"
        }
    }
}
