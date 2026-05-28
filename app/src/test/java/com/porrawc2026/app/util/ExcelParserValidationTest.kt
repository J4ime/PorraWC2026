package com.porrawc2026.app.util

import com.porrawc2026.app.data.local.entity.*
import org.junit.Assert.*
import org.junit.Test

class ExcelParserValidationTest {

    // ── ValidationResult ──────────────────────────────────────────

    @Test
    fun `validationResult is valid when zero errors`() {
        val result = ValidationResult(true, 157, 157, 0, emptyList(), emptyList())
        assertTrue(result.isValid)
        assertEquals(157, result.totalChecks)
        assertEquals(157, result.passedChecks)
        assertEquals(0, result.failedChecks)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `validationResult is invalid with errors`() {
        val result = ValidationResult(
            isValid = false,
            totalChecks = 157,
            passedChecks = 100,
            failedChecks = 57,
            errors = listOf("Falta predicción: México vs Sudáfrica"),
            warnings = emptyList()
        )
        assertFalse(result.isValid)
        assertEquals(57, result.failedChecks)
        assertEquals(1, result.errors.size)
    }

    // ── Group stage match validation ──────────────────────────────

    @Test
    fun `validate detects missing group predictions`() {
        val matches = listOf(
            MatchEntity(1, "Grupo A", "J1", "2026-06-11", "México", "Sudáfrica", predHome = 2, predAway = 1),
            MatchEntity(2, "Grupo A", "J1", "2026-06-11", "Corea del Sur", "República Checa", predHome = null, predAway = null),
        )
        val questions = (1..50).map {
            QuestionEntity(it, "Pregunta $it", predictedAnswer = true)
        }
        val players = listOf(
            PlayerPredictionEntity(1, "1er Goleador", predictedName = "Messi"),
            PlayerPredictionEntity(2, "2do Goleador", predictedName = "Mbappé"),
            PlayerPredictionEntity(3, "3er Goleador", predictedName = "Haaland")
        )
        val knockout = (73..104).map {
            KnockoutPredictionEntity(it, "Dieciseisavos", "W$it", "W${it + 1}", winner = 1)
        }

        val data = ExcelData(emptyList(), matches, questions, players, knockout, emptyList())
        val result = ExcelParser.validate(data)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Corea del Sur") })
    }

    @Test
    fun `validate all group matches predicted passes`() {
        val matches = (1..72).map {
            MatchEntity(it, "Grupo X", "J1", "2026-06-11", "EquipoA", "EquipoB", predHome = 1, predAway = 0)
        }
        val questions = (1..50).map {
            QuestionEntity(it, "Pregunta $it", predictedAnswer = true)
        }
        val players = listOf(
            PlayerPredictionEntity(1, "1er", predictedName = "A"),
            PlayerPredictionEntity(2, "2do", predictedName = "B"),
            PlayerPredictionEntity(3, "3er", predictedName = "C")
        )
        val knockout = (73..104).map {
            KnockoutPredictionEntity(it, "Dieciseisavos", "W$it", "W${it + 1}", winner = 1)
        }

        val data = ExcelData(emptyList(), matches, questions, players, knockout, emptyList())
        val result = ExcelParser.validate(data)
        assertTrue(result.isValid)
    }

    // ── Question validation ───────────────────────────────────────

    @Test
    fun `validate detects unanswered questions`() {
        val matches = (1..72).map {
            MatchEntity(it, "G", "J1", "2026-01-01", "A", "B", predHome = 1, predAway = 0)
        }
        val questions = (1..50).map { i ->
            QuestionEntity(i, "Pregunta $i", predictedAnswer = if (i <= 40) true else null)
        }
        val players = listOf(
            PlayerPredictionEntity(1, "1", predictedName = "X"),
            PlayerPredictionEntity(2, "2", predictedName = "Y"),
            PlayerPredictionEntity(3, "3", predictedName = "Z")
        )
        val knockout = (73..104).map {
            KnockoutPredictionEntity(it, "KO", "A", "B", winner = 1)
        }

        val data = ExcelData(emptyList(), matches, questions, players, knockout, emptyList())
        val result = ExcelParser.validate(data)

        assertFalse(result.isValid)
        assertEquals(10, result.errors.count { it.contains("sin responder") || it.contains("Pregunta") })
    }

    @Test
    fun `validate all questions answered passes`() {
        val matches = (1..72).map {
            MatchEntity(it, "G", "J1", "2026-01-01", "A", "B", predHome = 1, predAway = 0)
        }
        val questions = (1..50).map {
            QuestionEntity(it, "Pregunta $it", predictedAnswer = it % 2 == 0)
        }
        val players = listOf(
            PlayerPredictionEntity(1, "1", predictedName = "A"),
            PlayerPredictionEntity(2, "2", predictedName = "B"),
            PlayerPredictionEntity(3, "3", predictedName = "C")
        )
        val knockout = (73..104).map {
            KnockoutPredictionEntity(it, "KO", "A", "B", winner = 1)
        }

        val data = ExcelData(emptyList(), matches, questions, players, knockout, emptyList())
        val result = ExcelParser.validate(data)
        assertTrue(result.isValid)
    }

    // ── Player validation ─────────────────────────────────────────

    @Test
    fun `validate detects missing player names`() {
        val matches = (1..72).map {
            MatchEntity(it, "G", "J1", "2026-01-01", "A", "B", predHome = 1, predAway = 0)
        }
        val questions = (1..50).map {
            QuestionEntity(it, "Q$it", predictedAnswer = true)
        }
        val players = listOf(
            PlayerPredictionEntity(1, "1er", predictedName = "Messi"),
            PlayerPredictionEntity(2, "2do", predictedName = null),
            PlayerPredictionEntity(3, "3er", predictedName = "   ")
        )
        val knockout = (73..104).map {
            KnockoutPredictionEntity(it, "KO", "A", "B", winner = 1)
        }

        val data = ExcelData(emptyList(), matches, questions, players, knockout, emptyList())
        val result = ExcelParser.validate(data)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("jugador") })
    }

    // ── Knockout prediction validation ────────────────────────────

    @Test
    fun `validate detects missing knockout winner`() {
        val matches = (1..72).map {
            MatchEntity(it, "G", "J1", "2026-01-01", "A", "B", predHome = 1, predAway = 0)
        }
        val questions = (1..50).map {
            QuestionEntity(it, "Q$it", predictedAnswer = true)
        }
        val players = listOf(
            PlayerPredictionEntity(1, "1", predictedName = "A"),
            PlayerPredictionEntity(2, "2", predictedName = "B"),
            PlayerPredictionEntity(3, "3", predictedName = "C")
        )
        val knockout = (73..104).map { i ->
            KnockoutPredictionEntity(i, "Ronda", "A", "B", winner = if (i < 100) 1 else null)
        }

        val data = ExcelData(emptyList(), matches, questions, players, knockout, emptyList())
        val result = ExcelParser.validate(data)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Sin ganador") })
    }

    // ── Edge cases ────────────────────────────────────────────────

    @Test
    fun `validate returns all valid when completely empty data`() {
        val data = ExcelData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val result = ExcelParser.validate(data)
        assertTrue(result.isValid)
        assertEquals(0, result.totalChecks)
    }

    @Test
    fun `validation shows total checks matches expected count`() {
        val matches = (1..72).map {
            MatchEntity(it, "G", "J1", "2026-01-01", "A", "B", predHome = 1, predAway = 0)
        }
        val questions = (1..50).map { QuestionEntity(it, "Q$it", predictedAnswer = true) }
        val players = (1..3).map { PlayerPredictionEntity(it, "P$it", predictedName = "N$it") }
        val knockout = (1..32).map { KnockoutPredictionEntity(it, "R", "A", "B", winner = 1) }

        val data = ExcelData(emptyList(), matches, questions, players, knockout, emptyList())
        val result = ExcelParser.validate(data)

        assertEquals(72 + 50 + 3 + 32, result.totalChecks)
    }

    @Test
    fun `validation detects completely empty predictions`() {
        val matches = (1..72).map {
            MatchEntity(it, "G", "J1", "2026-01-01", "A", "B", predHome = null, predAway = null)
        }
        val questions = (1..50).map { QuestionEntity(it, "Q$it", predictedAnswer = null) }
        val players = (1..3).map { PlayerPredictionEntity(it, "P$it", predictedName = null) }
        val knockout = (1..32).map { KnockoutPredictionEntity(it, "R", "A", "B", winner = null) }

        val data = ExcelData(emptyList(), matches, questions, players, knockout, emptyList())
        val result = ExcelParser.validate(data)

        assertFalse(result.isValid)
        assertEquals(157, result.failedChecks)
    }
}
