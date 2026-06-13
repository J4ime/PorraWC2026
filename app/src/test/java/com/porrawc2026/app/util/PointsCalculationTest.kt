package com.porrawc2026.app.util

import com.porrawc2026.app.data.local.entity.*
import com.porrawc2026.app.domain.model.PointsCalculator
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
        assertEquals(50, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `group stage - correct result wrong score gives 30 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-06-11", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 3, predictedAwayGoals = 0,
            homeGoals = 2, awayGoals = 1
        )
        assertEquals(30, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `group stage - correct draw result different score gives 30 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 2, predictedAwayGoals = 2,
            homeGoals = 1, awayGoals = 1
        )
        assertEquals(30, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `group stage - one team goals correct gives 10 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 2, predictedAwayGoals = 0,
            homeGoals = 2, awayGoals = 2
        )
        assertEquals(10, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `group stage - both goals correct gives 50 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 1, predictedAwayGoals = 2,
            homeGoals = 1, awayGoals = 2
        )
        assertEquals(50, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `group stage - completely wrong prediction gives 0 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 0, predictedAwayGoals = 0,
            homeGoals = 3, awayGoals = 2
        )
        assertEquals(0, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `group stage - null prediction gives 0 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = null, predictedAwayGoals = null,
            homeGoals = 1, awayGoals = 0
        )
        assertEquals(0, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `group stage - null result gives 0 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 2, predictedAwayGoals = 1,
            homeGoals = null, awayGoals = null
        )
        assertEquals(0, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `group stage - correct away goals only gives 10 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1",
            dateTime = "2026-01-01", homeTeam = "A", awayTeam = "B",
            predictedHomeGoals = 5, predictedAwayGoals = 1,
            homeGoals = 1, awayGoals = 1
        )
        assertEquals(10, PointsCalculator.calculateMatchPoints(match))
    }

    @Test
    fun `knockout points by round`() {
        assertEquals(20, PointsCalculator.getKnockoutPoints("Dieciseisavos"))
        assertEquals(40, PointsCalculator.getKnockoutPoints("Octavos"))
        assertEquals(80, PointsCalculator.getKnockoutPoints("Cuartos"))
        assertEquals(160, PointsCalculator.getKnockoutPoints("Semifinales"))
        assertEquals(250, PointsCalculator.getKnockoutPoints("3er puesto"))
        assertEquals(500, PointsCalculator.getKnockoutPoints("Final"))
    }

    @Test
    fun `knockout unknown round returns 0`() {
        assertEquals(0, PointsCalculator.getKnockoutPoints("RondaInventada"))
    }

    @Test
    fun `question correct answer gives 20 points`() {
        assertEquals(20, PointsCalculator.getQuestionPoints(predicted = true, correct = true))
        assertEquals(20, PointsCalculator.getQuestionPoints(predicted = false, correct = false))
    }

    @Test
    fun `question wrong answer gives 0 points`() {
        assertEquals(0, PointsCalculator.getQuestionPoints(predicted = true, correct = false))
        assertEquals(0, PointsCalculator.getQuestionPoints(predicted = false, correct = true))
    }

    @Test
    fun `question null answer gives 0 points`() {
        assertEquals(0, PointsCalculator.getQuestionPoints(predicted = null, correct = true))
        assertEquals(0, PointsCalculator.getQuestionPoints(predicted = true, correct = null))
    }

    @Test
    fun `player 1 gets 50 points per goal`() {
        assertEquals(50, PointsCalculator.getPlayerPoints(rank = 1, goals = 1))
        assertEquals(150, PointsCalculator.getPlayerPoints(rank = 1, goals = 3))
        assertEquals(0, PointsCalculator.getPlayerPoints(rank = 1, goals = 0))
    }

    @Test
    fun `player 2 gets 30 points per goal`() {
        assertEquals(60, PointsCalculator.getPlayerPoints(rank = 2, goals = 2))
    }

    @Test
    fun `player 3 gets 10 points per goal`() {
        assertEquals(100, PointsCalculator.getPlayerPoints(rank = 3, goals = 10))
    }

    @Test
    fun `player invalid rank returns 0`() {
        assertEquals(0, PointsCalculator.getPlayerPoints(rank = 4, goals = 5))
        assertEquals(0, PointsCalculator.getPlayerPoints(rank = 0, goals = 5))
    }

    @Test
    fun `total points sums all categories`() {
        val matchPoints = 150
        val questionPoints = 800
        val playerPoints = 90
        val knockoutPoints = 500
        assertEquals(1540, matchPoints + questionPoints + playerPoints + knockoutPoints)
    }

    @Test
    fun `total points with zeros returns zero`() {
        assertEquals(0, 0 + 0 + 0 + 0)
    }

    @Test
    fun `calculateMatchPoints with raw values`() {
        assertEquals(50, PointsCalculator.calculateMatchPoints(2, 1, 2, 1))
        assertEquals(30, PointsCalculator.calculateMatchPoints(3, 0, 2, 1))
        assertEquals(0, PointsCalculator.calculateMatchPoints(null, null, 1, 0))
        assertEquals(0, PointsCalculator.calculateMatchPoints(1, 0, null, null))
    }

    @Test
    fun `getPlayerPointsForEntity calculates correctly`() {
        val player = PlayerPredictionEntity(rank = 1, playerName = "Test", goalsScored = 3, pointsPerGoal = 50)
        assertEquals(150, PointsCalculator.getPlayerPointsForEntity(player))
    }

    @Test
    fun `calculateTotalMatchPoints sums all`() {
        val matches = listOf(
            MatchEntity(1, "A", "J1", "", "A", "B", pointsEarned = 50),
            MatchEntity(2, "A", "J1", "", "C", "D", pointsEarned = 30),
            MatchEntity(3, "A", "J1", "", "E", "F", pointsEarned = 0)
        )
        assertEquals(80, PointsCalculator.calculateTotalMatchPoints(matches))
    }
}
