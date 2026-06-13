package com.porrawc2026.app.util

import org.junit.Assert.*
import org.junit.Test

class ShareUtilTest {

    @Test
    fun `buildShareText contains all sections`() {
        val text = ShareUtil.buildShareText(
            totalPoints = 1500,
            groupPoints = 800,
            knockoutPoints = 300,
            questionPoints = 200,
            playerPoints = 200,
            matchesCount = 50,
            questionsAnswered = 40,
            playersNamed = 3,
            knockoutPredicted = 20
        )
        
        assertTrue(text.contains("PORRA MUNDIAL 2026"))
        assertTrue(text.contains("1500 pts"))
        assertTrue(text.contains("800 pts"))
        assertTrue(text.contains("300 pts"))
        assertTrue(text.contains("200 pts"))
        assertTrue(text.contains("50/72"))
        assertTrue(text.contains("40/50"))
        assertTrue(text.contains("3/3"))
        assertTrue(text.contains("20/32"))
    }

    @Test
    fun `buildShareText calculates maximum possible points`() {
        val text = ShareUtil.buildShareText(
            totalPoints = 0,
            groupPoints = 0,
            knockoutPoints = 0,
            questionPoints = 0,
            playerPoints = 0,
            matchesCount = 0,
            questionsAnswered = 0,
            playersNamed = 0,
            knockoutPredicted = 0
        )
        
        assertTrue(text.contains("MÁXIMO POSIBLE"))
        assertTrue(text.contains("Total máximo:"))
    }

    @Test
    fun `buildShareText handles zero values`() {
        val text = ShareUtil.buildShareText(
            totalPoints = 0,
            groupPoints = 0,
            knockoutPoints = 0,
            questionPoints = 0,
            playerPoints = 0,
            matchesCount = 0,
            questionsAnswered = 0,
            playersNamed = 0,
            knockoutPredicted = 0
        )
        
        assertTrue(text.contains("0 pts"))
        assertTrue(text.contains("0/72"))
        assertTrue(text.contains("0/50"))
        assertTrue(text.contains("0/3"))
        assertTrue(text.contains("0/32"))
    }

    @Test
    fun `buildShareText includes country flags`() {
        val text = ShareUtil.buildShareText(
            totalPoints = 100,
            groupPoints = 50,
            knockoutPoints = 20,
            questionPoints = 20,
            playerPoints = 10,
            matchesCount = 10,
            questionsAnswered = 10,
            playersNamed = 2,
            knockoutPredicted = 5
        )
        
        assertTrue(text.contains("USA"))
        assertTrue(text.contains("México"))
        assertTrue(text.contains("Canadá"))
    }
}
