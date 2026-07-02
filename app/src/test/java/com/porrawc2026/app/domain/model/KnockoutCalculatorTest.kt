package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.KnockoutTeamProgressEntity
import org.junit.Assert.*
import org.junit.Test

class KnockoutCalculatorTest {

    @Test
    fun `test Dieciseisavos - equipo confirmado en 1_16 debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 73,
                round = "Dieciseisavos",
                homeTeamRef = "España",
                awayTeamRef = "Alemania",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "Dieciseisavos",
            "Alemania" to "Dieciseisavos"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertEquals(20, points[73])
    }

    @Test
    fun `test Octavos - equipo confirmado en 1_8 debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 89,
                round = "Octavos",
                homeTeamRef = "España",
                awayTeamRef = "Francia",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "Octavos",
            "Francia" to "Octavos"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertEquals(40, points[89])
    }

    @Test
    fun `test Cuartos - equipos sin resolver NO debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 97,
                round = "Cuartos",
                homeTeamRef = "W89",
                awayTeamRef = "W90",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "Octavos",
            "Francia" to "Octavos"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertNull(points[97])
    }

    @Test
    fun `test Semis - equipos sin resolver NO debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 101,
                round = "Semifinales",
                homeTeamRef = "W97",
                awayTeamRef = "W98",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "Cuartos",
            "Francia" to "Cuartos"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertNull(points[101])
    }

    @Test
    fun `test Final - equipos sin resolver NO debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 104,
                round = "Final",
                homeTeamRef = "W101",
                awayTeamRef = "W102",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "Semifinales",
            "Francia" to "Semifinales"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertNull(points[104])
    }

    @Test
    fun `test Cuartos - equipos resueltos y confirmados debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 97,
                round = "Cuartos",
                homeTeamRef = "España",
                awayTeamRef = "Francia",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "Cuartos",
            "Francia" to "Cuartos"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertEquals(80, points[97])
    }

    @Test
    fun `test equipo no ha llegado a la ronda no debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 89,
                round = "Octavos",
                homeTeamRef = "España",
                awayTeamRef = "Francia",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "Dieciseisavos",
            "Francia" to "Dieciseisavos"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertNull(points[89])
    }

    @Test
    fun `test 3er puesto - equipo confirmado en 3er puesto debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 103,
                round = "3er puesto",
                homeTeamRef = "España",
                awayTeamRef = "Francia",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "3er puesto",
            "Francia" to "3er puesto"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertEquals(250, points[103])
    }

    @Test
    fun `test 3er puesto - equipo en semifinal NO debe dar puntos`() {
        val predictions = listOf(
            KnockoutPredictionEntity(
                matchNumber = 103,
                round = "3er puesto",
                homeTeamRef = "España",
                awayTeamRef = "Francia",
                winner = 1
            )
        )
        
        val advancement = mapOf(
            "España" to "Semifinales",
            "Francia" to "Semifinales"
        )
        
        val points = KnockoutCalculator.computePointsFromAdvancement(predictions, advancement)
        
        assertNull(points[103])
    }
}
