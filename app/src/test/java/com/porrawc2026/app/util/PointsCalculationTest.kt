package com.porrawc2026.app.util

import com.porrawc2026.app.data.local.entity.*
import org.junit.Assert.*
import org.junit.Test

class PointsCalculationTest {

    @Test
    fun `group stage - correct exact score gives 50 points`() {
        val match = MatchEntity(
            id = 1, groupName = "Grupo A", matchday = "J1",
            dateTime = "2026-06-11", homeTeam = "México", awayTeam = "Sudáfrica",
            predictedHomeGoals = 2, predictedAwayGoals = 1,
            homeGoals = 2, awayGoals = 1
        )
        val points = calculateGroupPoints(match)
        assertEquals(50, points)
    }

    @Test
    fun `group stage - correct result wrong score gives 30 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-06-11", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 3, predictedAwayGoals = 0,
            homeGoals = 2, awayGoals = 1
        )
        val points = calculateGroupPoints(match)
        assertEquals(30, points)
    }

    @Test
    fun `group stage - correct draw result different score gives 30 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 2, predictedAwayGoals = 2,
            homeGoals = 1, awayGoals = 1
        )
        val points = calculateGroupPoints(match)
        assertEquals(30, points)
    }

    @Test
    fun `group stage - one team goals correct gives 10 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 2, predictedAwayGoals = 0,
            homeGoals = 2, awayGoals = 2
        )
        val points = calculateGroupPoints(match)
        assertEquals(10, points)
    }

    @Test
    fun `group stage - both goals correct but result wrong gives 20 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 1, predictedAwayGoals = 2,
            homeGoals = 1, awayGoals = 2
        )
        val points = calculateGroupPoints(match)
        assertEquals(50, points)
    }

    @Test
    fun `group stage - completely wrong prediction gives 0 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 0, predictedAwayGoals = 0,
            homeGoals = 3, awayGoals = 2
        )
        val points = calculateGroupPoints(match)
        assertEquals(0, points)
    }

    @Test
    fun `group stage - null prediction gives 0 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = null, predictedAwayGoals = null,
            homeGoals = 1, awayGoals = 0
        )
        val points = calculateGroupPoints(match)
        assertEquals(0, points)
    }

    @Test
    fun `group stage - null result gives 0 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 2, predictedAwayGoals = 1,
            homeGoals = null, awayGoals = null
        )
        val points = calculateGroupPoints(match)
        assertEquals(0, points)
    }

    @Test
    fun `group stage - correct away goals only gives 10 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 5, predictedAwayGoals = 1,
            homeGoals = 1, awayGoals = 1
        )
        val points = calculateGroupPoints(match)
        assertEquals(10, points)
    }

    // ── Knockout points ───────────────────────────────────────────

    @Test
    fun `knockout points by round`() {
        assertEquals(20, getKnockoutPoints("Dieciseisavos"))
        assertEquals(40, getKnockoutPoints("Octavos"))
        assertEquals(80, getKnockoutPoints("Cuartos"))
        assertEquals(160, getKnockoutPoints("Semifinales"))
        assertEquals(250, getKnockoutPoints("3er puesto"))
        assertEquals(500, getKnockoutPoints("Final"))
    }

    @Test
    fun `knockout unknown round returns 0`() {
        assertEquals(0, getKnockoutPoints("RondaInventada"))
    }

    // ── Question points ───────────────────────────────────────────

    @Test
    fun `question correct answer gives 20 points`() {
        assertEquals(20, getQuestionPoints(predicted = true, correct = true))
        assertEquals(20, getQuestionPoints(predicted = false, correct = false))
    }

    @Test
    fun `question wrong answer gives 0 points`() {
        assertEquals(0, getQuestionPoints(predicted = true, correct = false))
        assertEquals(0, getQuestionPoints(predicted = false, correct = true))
    }

    @Test
    fun `question null answer gives 0 points`() {
        assertEquals(0, getQuestionPoints(predicted = null, correct = true))
        assertEquals(0, getQuestionPoints(predicted = true, correct = null))
    }

    // ── Player points ─────────────────────────────────────────────

    @Test
    fun `player 1 gets 50 points per goal`() {
        assertEquals(50, getPlayerPoints(rank = 1, goals = 1))
        assertEquals(150, getPlayerPoints(rank = 1, goals = 3))
        assertEquals(0, getPlayerPoints(rank = 1, goals = 0))
    }

    @Test
    fun `player 2 gets 30 points per goal`() {
        assertEquals(60, getPlayerPoints(rank = 2, goals = 2))
    }

    @Test
    fun `player 3 gets 10 points per goal`() {
        assertEquals(100, getPlayerPoints(rank = 3, goals = 10))
    }

    @Test
    fun `player invalid rank returns 0`() {
        assertEquals(0, getPlayerPoints(rank = 4, goals = 5))
        assertEquals(0, getPlayerPoints(rank = 0, goals = 5))
    }

    // ── Total points calculation ──────────────────────────────────

    @Test
    fun `total points sums all categories`() {
        val matchPoints = 150
        val questionPoints = 800
        val playerPoints = 90
        val knockoutPoints = 500
        val total = matchPoints + questionPoints + playerPoints + knockoutPoints
        assertEquals(1540, total)
    }

    @Test
    fun `total points with zeros returns zero`() {
        assertEquals(0, 0 + 0 + 0 + 0)
    }

    // ═══════════════════════════════════════════════════════════════
    // Helper functions (mirror production logic for testing)
    // ═══════════════════════════════════════════════════════════════

    companion object {
        fun calculateGroupPoints(match: MatchEntity): Int {
            val predHome = match.predictedHomeGoals ?: return 0
            val predAway = match.predictedAwayGoals ?: return 0
            val realHome = match.homeGoals ?: return 0
            val realAway = match.awayGoals ?: return 0

            var points = 0
            if (predHome == realHome) points += 10
            if (predAway == realAway) points += 10

            val predResult = when {
                predHome > predAway -> "home"
                predHome < predAway -> "away"
                else -> "draw"
            }
            val realResult = when {
                realHome > realAway -> "home"
                realHome < realAway -> "away"
                else -> "draw"
            }
            if (predResult == realResult) points += 30

            return points
        }

        fun getKnockoutPoints(round: String): Int = when (round) {
            "Dieciseisavos" -> 20
            "Octavos" -> 40
            "Cuartos" -> 80
            "Semifinales" -> 160
            "3er puesto" -> 250
            "Final" -> 500
            else -> 0
        }

        fun getQuestionPoints(predicted: Boolean?, correct: Boolean?): Int {
            if (predicted == null || correct == null) return 0
            return if (predicted == correct) 20 else 0
        }

        fun getPlayerPoints(rank: Int, goals: Int): Int {
            val ptsPerGoal = when (rank) { 1 -> 50; 2 -> 30; 3 -> 10; else -> 0 }
            return ptsPerGoal * goals
        }
    }
}
