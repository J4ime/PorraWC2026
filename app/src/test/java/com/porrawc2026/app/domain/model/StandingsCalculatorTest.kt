package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.MatchEntity
import org.junit.Assert.*
import org.junit.Test

class StandingsCalculatorTest {

    @Test
    fun `calculateGroupStandings with no matches returns empty standings`() {
        val teams = listOf("Team A", "Team B", "Team C", "Team D")
        val standings = StandingsCalculator.calculateGroupStandings(emptyList(), teams)
        
        assertEquals(4, standings.size)
        assertTrue(standings.all { it.played == 0 })
        assertTrue(standings.all { it.points == 0 })
    }

    @Test
    fun `calculateGroupStandings home win awards 3 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "Team A", awayTeam = "Team B",
            homeGoals = 2, awayGoals = 1
        )
        val standings = StandingsCalculator.calculateGroupStandings(
            listOf(match), listOf("Team A", "Team B")
        )
        
        val teamA = standings.find { it.teamName == "Team A" }
        val teamB = standings.find { it.teamName == "Team B" }
        
        assertEquals(3, teamA?.points)
        assertEquals(0, teamB?.points)
        assertEquals(1, teamA?.won)
        assertEquals(1, teamB?.lost)
    }

    @Test
    fun `calculateGroupStandings draw awards 1 point each`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "Team A", awayTeam = "Team B",
            homeGoals = 1, awayGoals = 1
        )
        val standings = StandingsCalculator.calculateGroupStandings(
            listOf(match), listOf("Team A", "Team B")
        )
        
        val teamA = standings.find { it.teamName == "Team A" }
        val teamB = standings.find { it.teamName == "Team B" }
        
        assertEquals(1, teamA?.points)
        assertEquals(1, teamB?.points)
        assertEquals(1, teamA?.drawn)
        assertEquals(1, teamB?.drawn)
    }

    @Test
    fun `calculateGroupStandings away win awards 3 points`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "Team A", awayTeam = "Team B",
            homeGoals = 0, awayGoals = 2
        )
        val standings = StandingsCalculator.calculateGroupStandings(
            listOf(match), listOf("Team A", "Team B")
        )
        
        val teamA = standings.find { it.teamName == "Team A" }
        val teamB = standings.find { it.teamName == "Team B" }
        
        assertEquals(0, teamA?.points)
        assertEquals(3, teamB?.points)
    }

    @Test
    fun `calculateGroupStandings calculates goal difference correctly`() {
        val match = MatchEntity(
            id = 1, groupName = "A", matchday = "J1", dateTime = "",
            homeTeam = "Team A", awayTeam = "Team B",
            homeGoals = 3, awayGoals = 1
        )
        val standings = StandingsCalculator.calculateGroupStandings(
            listOf(match), listOf("Team A", "Team B")
        )
        
        val teamA = standings.find { it.teamName == "Team A" }
        val teamB = standings.find { it.teamName == "Team B" }
        
        assertEquals(2, teamA?.goalDifference)
        assertEquals(-2, teamB?.goalDifference)
        assertEquals(3, teamA?.goalsFor)
        assertEquals(1, teamA?.goalsAgainst)
    }

    @Test
    fun `calculateGroupStandings sorts by points then goal difference`() {
        val matches = listOf(
            MatchEntity(1, "A", "J1", "", "Team A", "Team B", homeGoals = 3, awayGoals = 0),
            MatchEntity(2, "A", "J1", "", "Team C", "Team D", homeGoals = 1, awayGoals = 0)
        )
        val standings = StandingsCalculator.calculateGroupStandings(
            matches, listOf("Team A", "Team B", "Team C", "Team D")
        )
        
        assertEquals("Team A", standings[0].teamName)
        assertEquals("Team C", standings[1].teamName)
    }

    @Test
    fun `calculateGroupStandings ignores matches without results`() {
        val matches = listOf(
            MatchEntity(1, "A", "J1", "", "Team A", "Team B", homeGoals = 2, awayGoals = 1),
            MatchEntity(2, "A", "J2", "", "Team C", "Team D", homeGoals = null, awayGoals = null)
        )
        val standings = StandingsCalculator.calculateGroupStandings(
            matches, listOf("Team A", "Team B", "Team C", "Team D")
        )
        
        val teamC = standings.find { it.teamName == "Team C" }
        val teamD = standings.find { it.teamName == "Team D" }
        
        assertEquals(0, teamC?.played)
        assertEquals(0, teamD?.played)
    }

    @Test
    fun `calculatePredictionStats with no matches returns zeros`() {
        val stats = StandingsCalculator.calculatePredictionStats(emptyList())
        
        assertEquals(0, stats.totalMatches)
        assertEquals(0, stats.matchesWithResults)
        assertEquals(0, stats.exactScoreHits)
        assertEquals(0, stats.totalPoints)
    }

    @Test
    fun `calculatePredictionStats counts exact score hits`() {
        val matches = listOf(
            MatchEntity(1, "A", "J1", "", "A", "B", 
                       predictedHomeGoals = 2, predictedAwayGoals = 1,
                       homeGoals = 2, awayGoals = 1)
        )
        val stats = StandingsCalculator.calculatePredictionStats(matches)
        
        assertEquals(1, stats.exactScoreHits)
        assertEquals(50, stats.totalPoints)
    }

    @Test
    fun `calculatePredictionStats counts result hits`() {
        val matches = listOf(
            MatchEntity(1, "A", "J1", "", "A", "B",
                       predictedHomeGoals = 3, predictedAwayGoals = 0,
                       homeGoals = 2, awayGoals = 1)
        )
        val stats = StandingsCalculator.calculatePredictionStats(matches)
        
        assertEquals(1, stats.resultHits)
        assertEquals(30, stats.totalPoints)
    }

    @Test
    fun `calculatePredictionStats calculates accuracy percentage`() {
        val matches = listOf(
            MatchEntity(1, "A", "J1", "", "A", "B",
                       predictedHomeGoals = 2, predictedAwayGoals = 1,
                       homeGoals = 2, awayGoals = 1),
            MatchEntity(2, "A", "J1", "", "C", "D",
                       predictedHomeGoals = 1, predictedAwayGoals = 0,
                       homeGoals = 0, awayGoals = 1)
        )
        val stats = StandingsCalculator.calculatePredictionStats(matches)
        
        assertEquals(50f, stats.accuracyPercent)
    }
}
