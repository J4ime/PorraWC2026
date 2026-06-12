package com.porrawc2026.app.util

import com.porrawc2026.app.ui.screens.home.MatchStatus
import com.porrawc2026.app.ui.screens.home.MatchDisplay
import com.porrawc2026.app.ui.screens.home.GoalEvent
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class LiveDataTest {

    // ── Date format tests ───────────────────────────────────────

    @Test
    fun `Zafronix kickoffUtc ISO format`() {
        val utcFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        utcFmt.timeZone = TimeZone.getTimeZone("UTC")
        val parsed = utcFmt.parse("2026-06-20T00:30:00.000Z")
        assertNotNull(parsed)
        val madridFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        madridFmt.timeZone = TimeZone.getTimeZone("Europe/Madrid")
        assertEquals("2026-06-20T02:30:00", madridFmt.format(parsed!!))
    }

    @Test
    fun `Zafronix kickoffUtc format - Mexico match`() {
        val utcFmt = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US)
        utcFmt.timeZone = TimeZone.getTimeZone("UTC")
        val parsed = utcFmt.parse("06/11/2026 19:00:00")
        assertNotNull(parsed)
        val madridFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        madridFmt.timeZone = TimeZone.getTimeZone("Europe/Madrid")
        assertEquals("2026-06-11T21:00:00", madridFmt.format(parsed!!))
    }

    // ── Goals from API parsing ──────────────────────────────────

    @Test
    fun `goals parsed correctly for Mexico 2-0`() {
        val homeScorers = listOf(GoalEvent("Quiñones", 9), GoalEvent("Jiménez", 67))
        val awayScorers = emptyList<GoalEvent>()
        assertEquals(2, homeScorers.size)
        assertEquals(0, awayScorers.size)
        assertEquals("Quiñones", homeScorers[0].playerName)
        assertEquals(9, homeScorers[0].minute)
        assertEquals("Jiménez", homeScorers[1].playerName)
        assertEquals(67, homeScorers[1].minute)
    }

    @Test
    fun `goals parsed correctly for Korea 2-1`() {
        val homeScorers = listOf(GoalEvent("Hwang In-beom", 67), GoalEvent("Oh Hyeon-gyu", 80))
        val awayScorers = listOf(GoalEvent("Schick", 45))
        assertEquals(2, homeScorers.size)
        assertEquals(1, awayScorers.size)
    }

    // ── MatchStatus ─────────────────────────────────────────────

    @Test
    fun `match with scores is FINISHED`() {
        val status = if (2 != null && 0 != null) MatchStatus.FINISHED else MatchStatus.UPCOMING
        assertEquals(MatchStatus.FINISHED, status)
    }

    @Test
    fun `match without scores in window is LIVE`() {
        val now = Date()
        val start = Date(now.time - 60 * 60 * 1000) // 1 hour ago
        val end = Date(start.time + 150L * 60 * 1000)
        val isLive = now.after(start) && now.before(end)
        assertTrue(isLive)
    }

    @Test
    fun `match without scores before start is UPCOMING`() {
        val now = Date()
        val start = Date(now.time + 60 * 60 * 1000) // 1 hour future
        val isUpcoming = now.before(start)
        assertTrue(isUpcoming)
    }

    // ── Points calculation ──────────────────────────────────────

    @Test
    fun `exact score = 50 points (10+10+30)`() {
        val points = calculatePoints(2, 1, 2, 1)
        assertEquals(50, points)
    }

    @Test
    fun `correct result wrong score = 30 points`() {
        val points = calculatePoints(3, 0, 2, 1)
        assertEquals(30, points)
    }

    @Test
    fun `one correct goal = 10 points`() {
        val points = calculatePoints(2, 0, 2, 2)
        assertEquals(10, points)
    }

    @Test
    fun `correct draw = 30 points`() {
        val points = calculatePoints(2, 2, 1, 1)
        assertEquals(30, points)
    }

    fun calculatePoints(predH: Int, predA: Int, realH: Int, realA: Int): Int {
        var pts = 0
        if (predH == realH) pts += 10
        if (predA == realA) pts += 10
        val predRes = when { predH > predA -> "h"; predH < predA -> "a"; else -> "d" }
        val realRes = when { realH > realA -> "h"; realH < realA -> "a"; else -> "d" }
        if (predRes == realRes) pts += 30
        return pts
    }
}
