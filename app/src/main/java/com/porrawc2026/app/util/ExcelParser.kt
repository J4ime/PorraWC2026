package com.porrawc2026.app.util

import android.content.Context
import android.net.Uri
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
    private const val COL_QUESTION_ANSWER = 35
    private const val COL_PLAYER_NAME = 32
    private const val COL_KNOCKOUT_WINNER_HOME = 27
    private const val COL_KNOCKOUT_WINNER_AWAY = 30
    private const val COL_KNOCKOUT_MATCH_NUM = 9
    private const val COL_KNOCKOUT_HOME_REF = 12
    private const val COL_KNOCKOUT_AWAY_REF = 13

    fun parse(context: Context, uri: Uri): ExcelData {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("No se pudo abrir el archivo")
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheet("WORLDCUP")
            ?: workbook.getSheetAt(1)

        val teams = parseTeams(workbook)
        val matches = parseMatches(sheet)
        val questions = parseQuestions(sheet)
        val playerPredictions = parsePlayers(sheet)
        val knockoutPredictions = parseKnockout(sheet)
        val standings = parseStandings(teams)

        workbook.close()
        inputStream.close()

        return ExcelData(teams, matches, questions, playerPredictions, knockoutPredictions, standings)
    }

    fun validate(data: ExcelData): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var total = 0
        var passed = 0

        // 1. Validate group stage matches (72 matches × 2 scores = 144 fields)
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

        // 2. Validate 50 questions
        total += data.questions.size
        val answeredQuestions = data.questions.count { it.predictedAnswer != null }
        passed += answeredQuestions
        data.questions.forEach { q ->
            if (q.predictedAnswer == null) {
                errors.add("Pregunta ${q.id} sin responder: ${q.text.take(80)}...")
            }
        }

        // 3. Validate 3 player names
        total += data.playerPredictions.size
        val namedPlayers = data.playerPredictions.count { !it.predictedName.isNullOrBlank() }
        passed += namedPlayers
        if (namedPlayers < 3) {
            errors.add("Faltan ${3 - namedPlayers} jugador(es) por nombrar")
        }

        // 4. Validate knockout predictions (32 matches)
        val knockoutOnly = data.knockoutPredictions
        total += knockoutOnly.size
        val predictedKnockout = knockoutOnly.count { it.winner != null && it.winner in 1..2 }
        passed += predictedKnockout
        knockoutOnly.forEach { k ->
            if (k.winner == null || k.winner !in 1..2) {
                errors.add("Sin ganador en ${k.round}: ${k.homeTeamRef} vs ${k.awayTeamRef}")
            }
        }

        // 5. Warnings (not blockers)
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
            val id = getCellValue(row, 0)?.toString() ?: continue
            val name = getCellValue(row, 1)?.toString() ?: continue
            val group = getCellValue(row, 2)?.toString() ?: continue
            val rank = (getCellValue(row, 3) as? Double)?.toInt() ?: rowIdx

            teams.add(TeamEntity(id = id, name = cleanText(name), groupLetter = group, rank = rank, flagEmoji = getFlagEmoji(name)))
        }
        return teams
    }

    private fun parseMatches(sheet: Sheet): List<MatchEntity> {
        val matches = mutableListOf<MatchEntity>()
        var matchId = 0
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        val groupHeaders = listOf(2, 10, 18, 26, 34, 42, 50, 58, 66, 74, 82, 90)

        for (baseRow in groupHeaders) {
            for (offset in 1..6) {
                val rowIdx = baseRow + offset
                val row = sheet.getRow(rowIdx) ?: continue

                val dateCell = getCellValue(row, COL_MATCH_DATE)
                val dateStr = when (dateCell) {
                    is Date -> dateFormat.format(dateCell)
                    is String -> dateCell
                    else -> null
                } ?: continue

                val homeCell = getCellValue(row, COL_MATCH_HOME)?.toString() ?: continue
                val awayCell = getCellValue(row, COL_MATCH_AWAY)?.toString() ?: continue

                val predHome = getCellValue(row, COL_GOAL_HOME)?.let {
                    when (it) {
                        is Double -> it.toInt()
                        is String -> it.toIntOrNull()
                        else -> null
                    }
                }
                val predAway = getCellValue(row, COL_GOAL_AWAY)?.let {
                    when (it) {
                        is Double -> it.toInt()
                        is String -> it.toIntOrNull()
                        else -> null
                    }
                }

                matchId++
                val groupName = getCellValue(row, COL_MATCH_GROUP)?.toString() ?: ""
                val matchday = getCellValue(row, COL_MATCH_DAY)?.toString() ?: "J1"

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

        // Knockout matches (rounds of 32, 16, 8, 4, 3rd, final)
        val koRounds = listOf(
            Triple(98, 16, "Dieciseisavos"),
            Triple(117, 8, "Octavos"),
            Triple(128, 4, "Cuartos"),
            Triple(135, 2, "Semifinales")
        )

        for ((startRow, count, round) in koRounds) {
            for (offset in 1..count) {
                val rowIdx = startRow + offset
                val row = sheet.getRow(rowIdx) ?: continue
                val matchNumber = (getCellValue(row, COL_KNOCKOUT_MATCH_NUM) as? Double)?.toInt() ?: continue

                val dateCell = getCellValue(row, COL_MATCH_DATE)
                val dateStr = when (dateCell) {
                    is Date -> dateFormat.format(dateCell)
                    is String -> dateCell
                    else -> null
                } ?: continue

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

        return matches
    }

    private fun parseQuestions(sheet: Sheet): List<QuestionEntity> {
        val questions = mutableListOf<QuestionEntity>()
        for (rowIdx in 158..207) {
            val row = sheet.getRow(rowIdx) ?: continue
            val id = (getCellValue(row, COL_QUESTION_ID) as? Double)?.toInt() ?: continue
            val text = getCellValue(row, COL_QUESTION_TEXT)?.toString() ?: continue

            val answerRaw = getCellValue(row, COL_QUESTION_ANSWER)
            val answer: Boolean? = when (answerRaw) {
                is Boolean -> answerRaw
                is String -> when (answerRaw.trim().uppercase()) {
                    "TRUE", "VERDADERO", "V", "1", "YES", "SÍ", "SI" -> true
                    "FALSE", "FALSO", "F", "0", "NO" -> false
                    else -> null
                }
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
            val name = getCellValue(row, COL_PLAYER_NAME)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

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
            98 to "Dieciseisavos",
            117 to "Octavos",
            128 to "Cuartos",
            135 to "Semifinales"
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
                val matchNumber = (getCellValue(row, COL_KNOCKOUT_MATCH_NUM) as? Double)?.toInt() ?: continue
                val homeRef = getCellValue(row, COL_KNOCKOUT_HOME_REF)?.toString() ?: continue
                val awayRef = getCellValue(row, COL_KNOCKOUT_AWAY_REF)?.toString() ?: continue

                val winnerHome = getCellValue(row, COL_KNOCKOUT_WINNER_HOME)?.let {
                    when (it) {
                        is Double -> if (it == 1.0) 1 else null
                        is String -> if (it.trim() == "1") 1 else null
                        else -> null
                    }
                }
                val winnerAway = getCellValue(row, COL_KNOCKOUT_WINNER_AWAY)?.let {
                    when (it) {
                        is Double -> if (it == 1.0) 2 else null
                        is String -> if (it.trim() == "1") 2 else null
                        else -> null
                    }
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

        // 3rd place
        val thirdRow = sheet.getRow(142)
        if (thirdRow != null) {
            val wHome = getCellValue(thirdRow, COL_KNOCKOUT_WINNER_HOME)?.let {
                when (it) { is Double -> if (it == 1.0) 1 else null; is String -> if (it.trim() == "1") 1 else null; else -> null }
            }
            val wAway = getCellValue(thirdRow, COL_KNOCKOUT_WINNER_AWAY)?.let {
                when (it) { is Double -> if (it == 1.0) 2 else null; is String -> if (it.trim() == "1") 2 else null; else -> null }
            }
            predictions.add(KnockoutPredictionEntity(103, "3er puesto", "L101", "L102", wHome ?: wAway))
        }

        // Final
        val finalRow = sheet.getRow(146)
        if (finalRow != null) {
            val wHome = getCellValue(finalRow, COL_KNOCKOUT_WINNER_HOME)?.let {
                when (it) { is Double -> if (it == 1.0) 1 else null; is String -> if (it.trim() == "1") 1 else null; else -> null }
            }
            val wAway = getCellValue(finalRow, COL_KNOCKOUT_WINNER_AWAY)?.let {
                when (it) { is Double -> if (it == 1.0) 2 else null; is String -> if (it.trim() == "1") 2 else null; else -> null }
            }
            predictions.add(KnockoutPredictionEntity(104, "Final", "W101", "W102", wHome ?: wAway))
        }

        return predictions
    }

    private fun parseStandings(teams: List<TeamEntity>): List<GroupStandingEntity> {
        return teams.map { team -> GroupStandingEntity(teamId = team.id, groupLetter = team.groupLetter) }
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
