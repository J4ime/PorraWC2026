package com.porrawc2026.app.data.remote

import org.junit.Assert.*
import org.junit.Test

class MatchScheduleProviderTest {

    @Test
    fun `getHardcodedSchedule returns 104 matches`() {
        val schedule = MatchScheduleProvider.getHardcodedSchedule()
        assertEquals(104, schedule.size)
    }

    @Test
    fun `getHardcodedSchedule contains all match IDs from 1 to 104`() {
        val schedule = MatchScheduleProvider.getHardcodedSchedule()
        for (id in 1..104) {
            assertTrue("Match $id should exist", schedule.containsKey(id))
        }
    }

    @Test
    fun `buildMatchEntities creates 104 matches including all knockout rounds`() {
        val entities = MatchScheduleProvider.buildMatchEntities()
        assertEquals(104, entities.size)
        assertEquals(72, entities.count { it.id in 1..72 })
        assertEquals(16, entities.count { it.id in 73..88 })
        assertEquals(8, entities.count { it.id in 89..96 })
        assertEquals(4, entities.count { it.id in 97..100 })
        assertEquals(4, entities.count { it.id in 101..104 })
    }

    @Test
    fun `buildMatchEntities assigns correct groups and dieciseisavos`() {
        val entities = MatchScheduleProvider.buildMatchEntities()
        
        val groupA = entities.filter { it.id in 1..6 }
        assertTrue(groupA.all { it.groupName == "Grupo A" })
        
        val groupB = entities.filter { it.id in 7..12 }
        assertTrue(groupB.all { it.groupName == "Grupo B" })
        
        val groupL = entities.filter { it.id in 67..72 }
        assertTrue(groupL.all { it.groupName == "Grupo L" })
        
        val dieciseisavos = entities.filter { it.id in 73..88 }
        assertTrue(dieciseisavos.all { it.groupName == "Dieciseisavos" })
        assertTrue(dieciseisavos.all { it.isKnockout })
        val octavos = entities.filter { it.id in 89..96 }
        assertTrue(octavos.all { it.groupName == "Octavos" })
        assertTrue(octavos.all { it.isKnockout })
        val cuartos = entities.filter { it.id in 97..100 }
        assertTrue(cuartos.all { it.groupName == "Cuartos" })
        assertTrue(cuartos.all { it.isKnockout })
        val semis = entities.filter { it.id in 101..102 }
        assertTrue(semis.all { it.groupName == "Semifinales" })
        assertTrue(semis.all { it.isKnockout })
        val tercer = entities.filter { it.id == 103 }
        assertTrue(tercer.all { it.groupName == "Tercer puesto" })
        assertTrue(tercer.all { it.isKnockout })
        val final = entities.filter { it.id == 104 }
        assertTrue(final.all { it.groupName == "Final" })
        assertTrue(final.all { it.isKnockout })
    }

    @Test
    fun `buildMatchEntities assigns correct matchdays`() {
        val entities = MatchScheduleProvider.buildMatchEntities()
        
        val match1 = entities.find { it.id == 1 }
        assertEquals("J1", match1?.matchday)
        
        val match7 = entities.find { it.id == 7 }
        assertEquals("J1", match7?.matchday)
        
        val match2 = entities.find { it.id == 2 }
        assertEquals("J2", match2?.matchday)
    }

    @Test
    fun `buildMatchEntities sets knockout correctly`() {
        val entities = MatchScheduleProvider.buildMatchEntities()
        assertTrue(entities.filter { it.id <= 72 }.all { !it.isKnockout })
        assertTrue(entities.filter { it.id >= 73 }.all { it.isKnockout })
    }

    @Test
    fun `enrichSchedule updates dates and TV channels`() {
        val original = MatchScheduleProvider.buildMatchEntities()
        val enriched = MatchScheduleProvider.enrichSchedule(original)
        
        assertEquals(original.size, enriched.size)
        
        val match1 = enriched.find { it.id == 1 }
        assertNotNull(match1)
        assertTrue(match1!!.tvChannel.contains("RTVE") || match1.tvChannel.contains("DAZN"))
    }

    @Test
    fun `enrichSchedule marks Spain matches with RTVE`() {
        val original = MatchScheduleProvider.buildMatchEntities()
        val enriched = MatchScheduleProvider.enrichSchedule(original)
        
        val spainMatches = enriched.filter { 
            it.homeTeam == "España" || it.awayTeam == "España" 
        }
        
        assertTrue(spainMatches.all { it.tvChannel.contains("RTVE") })
    }

    @Test
    fun `first match is Mexico vs South Africa`() {
        val schedule = MatchScheduleProvider.getHardcodedSchedule()
        val match1 = schedule[1]
        
        assertNotNull(match1)
        assertEquals("México", match1!!.home)
        assertEquals("Sudáfrica", match1.away)
    }

    @Test
    fun `all matches have valid dates`() {
        val schedule = MatchScheduleProvider.getHardcodedSchedule()
        
        schedule.values.forEach { match ->
            assertTrue("Match ${match.id} should have a date", 
                      match.date.isNotBlank())
            assertTrue("Match ${match.id} date should match format",
                      match.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
        }
    }

    @Test
    fun `all matches have TV channels`() {
        val schedule = MatchScheduleProvider.getHardcodedSchedule()
        
        schedule.values.forEach { match ->
            assertTrue("Match ${match.id} should have a TV channel",
                      match.tv.isNotBlank())
        }
    }
}
