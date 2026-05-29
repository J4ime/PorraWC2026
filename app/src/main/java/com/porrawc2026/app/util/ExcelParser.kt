package com.porrawc2026.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.porrawc2026.app.data.local.entity.*
import org.apache.poi.ss.usermodel.*
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

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

    private const val COL_MATCH_HOME = 26
    private const val COL_MATCH_AWAY = 31
    private const val COL_MATCH_DATE = 23
    private const val COL_MATCH_DAY = 25
    private const val COL_MATCH_GROUP = 35
    private const val COL_GOAL_HOME = 28
    private const val COL_GOAL_AWAY = 29
    private const val COL_QUESTION_ID = 22
    private const val COL_QUESTION_TEXT = 23
    private const val COL_QUESTION_ANSWER = 31  // AF
    private const val COL_PLAYER_NAME = 32
    private const val COL_KNOCKOUT_WINNER_HOME = 28  // AC
    private const val COL_KNOCKOUT_WINNER_AWAY = 29  // AD
    private const val COL_KNOCKOUT_MATCH_NUM = 9
    private const val COL_KNOCKOUT_HOME_REF = 12
    private const val COL_KNOCKOUT_AWAY_REF = 13

    private lateinit var formatter: DataFormatter

    fun parse(context: Context, uri: Uri): ExcelData {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("No se pudo abrir el archivo")
        val workbook = WorkbookFactory.create(inputStream, "")
        formatter = DataFormatter()

        val sheet = workbook.getSheet("WORLDCUP")
            ?: workbook.getSheetAt(1)

        Log.d("ExcelParser", "WORLDCUP sheet: rows=${sheet.lastRowNum + 1}, protected=${sheet.protect}")

        val teams = parseTeams(workbook)
        val matches = parseMatches(sheet)
        val questions = parseQuestions(sheet)
        val playerPredictions = parsePlayers(sheet)
        val knockoutPredictions = parseKnockout(sheet)
        val standings = parseStandings(teams)

        Log.d("ExcelParser", "Parsed: teams=${teams.size}, matches=${matches.size} (group=${matches.count { !it.isKnockout }}, ko=${matches.count { it.isKnockout }}), questions=${questions.size} (answered=${questions.count { it.predictedAnswer != null }}), players=${playerPredictions.size} (named=${playerPredictions.count { !it.predictedName.isNullOrBlank() }}), knockoutPred=${knockoutPredictions.size} (picked=${knockoutPredictions.count { it.winner != null }})")

        workbook.close()
        inputStream.close()

        return ExcelData(teams, matches, questions, playerPredictions, knockoutPredictions, standings)
    }

    fun validate(data: ExcelData): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var total = 0
        var passed = 0

        val groupMatches = data.matches.filter { !it.isKnockout }
        total += groupMatches.size
        var predictedMatches = 0
        groupMatches.forEach { match ->
            if (match.predictedHomeGoals != null && match.predictedAwayGoals != null) {
                predictedMatches++
            } else {
                val label = if (match.groupName.isNotEmpty()) "(${match.groupName})" else ""
                errors.add("Falta predicción: ${match.homeTeam} vs ${match.awayTeam} $label")
            }
        }
        passed += predictedMatches

        total += data.questions.size
        val answeredQuestions = data.questions.count { it.predictedAnswer != null }
        passed += answeredQuestions
        data.questions.forEach { q ->
            if (q.predictedAnswer == null) {
                errors.add("Pregunta ${q.id} sin responder: ${q.text.take(80)}...")
            }
        }

        total += data.playerPredictions.size
        val namedPlayers = data.playerPredictions.count { !it.predictedName.isNullOrBlank() }
        passed += namedPlayers
        if (namedPlayers < 3) {
            errors.add("Faltan ${3 - namedPlayers} jugador(es) por nombrar")
        }

        val knockoutOnly = data.knockoutPredictions
        total += knockoutOnly.size
        val predictedKnockout = knockoutOnly.count { it.winner != null && it.winner in 1..2 }
        passed += predictedKnockout
        knockoutOnly.forEach { k ->
            if (k.winner == null || k.winner !in 1..2) {
                errors.add("Sin ganador en ${k.round}: ${k.homeTeamRef} vs ${k.awayTeamRef}")
            }
        }

        val totalPredicted = predictedMatches + answeredQuestions + namedPlayers + predictedKnockout
        val totalExpected = groupMatches.size + data.questions.size + 3 + knockoutOnly.size

        if (totalPredicted == 0) {
            errors.add(0, "EL EXCEL NO CONTIENE PREDICCIONES. ¿Has rellenado las casillas grises?")
        }

        val failed = total - passed
        val missingMatches = groupMatches.count { it.predictedHomeGoals == null || it.predictedAwayGoals == null }
        val missingQuestions = data.questions.count { it.predictedAnswer == null }
        val missingKnockout = knockoutOnly.count { it.winner == null || it.winner !in 1..2 }
        val missingPlayers = data.playerPredictions.count { it.predictedName.isNullOrBlank() }

        return ValidationResult(
            isValid = failed == 0,
            totalChecks = totalExpected,
            passedChecks = totalPredicted,
            failedChecks = failed,
            errors = errors,
            warnings = warnings,
            pendingMatches = missingMatches,
            pendingQuestions = missingQuestions,
            pendingKnockout = missingKnockout,
            pendingPlayers = missingPlayers
        )
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        val groupHeaders = listOf(2, 10, 18, 26, 34, 42, 50, 58, 66, 74, 82, 90)
        var loggedFirst = false

        for (baseRow in groupHeaders) {
            for (offset in 1..6) {
                val rowIdx = baseRow + offset
                val row = sheet.getRow(rowIdx) ?: continue

                val dateCell = getCellValue(row, COL_MATCH_DATE)
                val dateStr = when (dateCell) {
                    is Date -> dateFormat.format(dateCell)
                    is String -> dateCell
                    is Number -> dateCell.toString()
                    else -> ""
                }

                val homeCell = cellText(row, COL_MATCH_HOME) ?: getCellValue(row, COL_MATCH_HOME)?.toString()
                if (homeCell == null) { Log.d("ExcelParser", "Skip r$rowIdx: no home team"); continue }
                val awayCell = cellText(row, COL_MATCH_AWAY) ?: getCellValue(row, COL_MATCH_AWAY)?.toString()
                if (awayCell == null) { Log.d("ExcelParser", "Skip r$rowIdx: no away team"); continue }

                if (!loggedFirst) {
                    loggedFirst = true
                    Log.d("ExcelParser", "Match1(r$rowIdx): home=$homeCell, away=$awayCell, dateCell=$dateCell (${dateCell?.javaClass?.simpleName}), gH=${cellText(row, COL_GOAL_HOME)}, gA=${cellText(row, COL_GOAL_AWAY)}")
                }

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
                        isKnockout = false
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

                val dateCell = getCellValue(row, COL_MATCH_DATE)
                val dateStr = when (dateCell) {
                    is Date -> dateFormat.format(dateCell)
                    is String -> dateCell
                    is Number -> dateCell.toString()
                    else -> ""
                }

                matchId++
                matches.add(
                    MatchEntity(
                        id = matchId,
                        groupName = round,
                        matchday = round,
                        dateTime = dateStr,
                        homeTeam = "W$matchNumber",
                        awayTeam = "W$matchNumber",
                        isKnockout = true,
                        knockoutRound = round,
                        matchNumber = matchNumber
                    )
                )
            }
        }

        Log.d("ExcelParser", "parseMatches: found ${matches.size} (group=${matches.count { !it.isKnockout }}, ko=${matches.count { it.isKnockout }})")
        return matches
    }

    private fun parseQuestions(sheet: Sheet): List<QuestionEntity> {
        val questions = mutableListOf<QuestionEntity>()
        val sampleRows = listOf(158, 159, 160, 170, 180, 190, 200, 207)

        for (rowIdx in 158..207) {
            val row = sheet.getRow(rowIdx) ?: continue
            val id = cellInt(row, COL_QUESTION_ID) ?: continue
            val text = cellText(row, COL_QUESTION_TEXT) ?: continue

            val answerRaw = cellText(row, COL_QUESTION_ANSWER)
            val answer: Boolean? = when {
                answerRaw == null -> null
                answerRaw.uppercase() in listOf("TRUE", "VERDADERO", "V", "1", "YES", "SÍ", "SI") -> true
                answerRaw.uppercase() in listOf("FALSE", "FALSO", "F", "0", "NO") -> false
                answerRaw == "1.0" || answerRaw == "1,0" -> true
                answerRaw == "0.0" || answerRaw == "0,0" -> false
                else -> null
            }

            if (rowIdx in sampleRows) {
                val cell = row.getCell(COL_QUESTION_ANSWER)
                val raw = if (cell != null) {
                    when (cell.cellType) {
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.STRING -> cell.stringCellValue
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.FORMULA -> "F:${cell.cachedFormulaResultType}=${cell.numericCellValue}"
                        else -> formatter.formatCellValue(cell)
                    }
                } else "null"
                Log.d("ExcelParser", "Q$id(r$rowIdx): type=${cell?.cellType}, raw='$raw', txt='$answerRaw', ans=$answer")
            }

            questions.add(
                QuestionEntity(
                    id = id,
                    text = cleanText(text),
                    predictedAnswer = answer
                )
            )
        }

        Log.d("ExcelParser", "parseQuestions: found ${questions.size}, answered=${questions.count { it.predictedAnswer != null }}")
        return questions
    }

    private fun parsePlayers(sheet: Sheet): List<PlayerPredictionEntity> {
        val players = mutableListOf<PlayerPredictionEntity>()

        for (rank in 1..3) {
            val rowIdx = 152 + rank
            val row = sheet.getRow(rowIdx) ?: continue
            val name = cellText(row, COL_PLAYER_NAME)?.trim()?.takeIf { it.isNotEmpty() && it != "0" && it != "0.0" }

            players.add(
                PlayerPredictionEntity(
                    rank = rank,
                    playerName = when (rank) {
                        1 -> "1er Goleador"
                        2 -> "2do Goleador"
                        else -> "3er Goleador"
                    },
                    predictedName = name
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
                val homeRef = cellText(row, COL_KNOCKOUT_HOME_REF) ?: continue
                val awayRef = cellText(row, COL_KNOCKOUT_AWAY_REF) ?: continue

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

                if (matchNumber <= 76) {
                    val cAb = row.getCell(COL_KNOCKOUT_WINNER_HOME)
                    val cAe = row.getCell(COL_KNOCKOUT_WINNER_AWAY)
                    val rawAb = if (cAb != null) when (cAb.cellType) { CellType.NUMERIC -> cAb.numericCellValue.toString(); CellType.FORMULA -> "F:${cAb.cachedFormulaResultType}=${try { cAb.numericCellValue } catch(e:Exception) { cAb.stringCellValue }}"; else -> formatter.formatCellValue(cAb) } else "null"
                    val rawAe = if (cAe != null) when (cAe.cellType) { CellType.NUMERIC -> cAe.numericCellValue.toString(); CellType.FORMULA -> "F:${cAe.cachedFormulaResultType}=${try { cAe.numericCellValue } catch(e:Exception) { cAe.stringCellValue }}"; else -> formatter.formatCellValue(cAe) } else "null"
                    Log.d("ExcelParser", "KO$matchNumber(r$rowIdx): AB=$rawAb, AE=$rawAe, wh='$whRaw', wa='$waRaw', w=$winner")
                }

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
            predictions.add(KnockoutPredictionEntity(103, "3er puesto", "L101", "L102",
                when { wh == "1" || wh == "1.0" -> 1; wa == "1" || wa == "1.0" -> 2; else -> null }))
        }

        val finalRow = sheet.getRow(146)
        if (finalRow != null) {
            val wh = cellText(finalRow, COL_KNOCKOUT_WINNER_HOME)
            val wa = cellText(finalRow, COL_KNOCKOUT_WINNER_AWAY)
            predictions.add(KnockoutPredictionEntity(104, "Final", "W101", "W102",
                when { wh == "1" || wh == "1.0" -> 1; wa == "1" || wa == "1.0" -> 2; else -> null }))
        }

        Log.d("ExcelParser", "parseKnockout: ${predictions.size} predictions, picked=${predictions.count { it.winner != null }}")
        return predictions
    }

    private fun parseStandings(teams: List<TeamEntity>): List<GroupStandingEntity> {
        return teams.map { team -> GroupStandingEntity(teamId = team.id, groupLetter = team.groupLetter) }
    }

    // ── New helper functions that use DataFormatter ──────────────

    private fun cellText(row: Row, colIndex: Int): String? {
        val cell = row.getCell(colIndex) ?: return null
        val result = try {
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
        } catch (e: Exception) { null }
        if (result != null) return result.trim().takeIf { it.isNotEmpty() }
        // Fallback to DataFormatter
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
                try { cell.numericCellValue } catch (e: Exception) {
                    try { cell.stringCellValue } catch (e2: Exception) { null }
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

    private fun getFlagEmoji(teamName: String): String {
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
