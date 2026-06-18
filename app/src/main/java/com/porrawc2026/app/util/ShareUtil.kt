package com.porrawc2026.app.util

import android.content.Context
import android.content.Intent

object ShareUtil {

    fun shareResults(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir resultados"))
    }

    fun buildShareText(
        totalPoints: Int,
        groupPoints: Int,
        knockoutPoints: Int,
        questionPoints: Int,
        playerPoints: Int,
        matchesCount: Int,
        questionsAnswered: Int,
        playersNamed: Int,
        knockoutPredicted: Int
    ): String {
        return buildString {
            appendLine("⚽ PORRA MUNDIAL 2026 ⚽")
            appendLine("USA · México · Canadá")
            appendLine()
            appendLine("── PUNTUACIÓN ──")
            appendLine("Total: $totalPoints pts")
            appendLine("Fase de grupos: $groupPoints pts ($matchesCount/72 partidos)")
            appendLine("Eliminatorias: $knockoutPoints pts ($knockoutPredicted/32 predicciones)")
            appendLine("Preguntas: $questionPoints pts ($questionsAnswered/50)")
            appendLine("Goleadores: $playerPoints pts ($playersNamed/3)")
            appendLine()
            appendLine("── MÁXIMO POSIBLE ──")
            val maxGroup = 72 * 50
            val maxKnockout = (16 * 20) + (8 * 40) + (4 * 80) + (2 * 160) + 250 + 500
            val maxQuestions = 50 * 20
            val maxPlayers = 1000
            appendLine("Total máximo: ${maxGroup + maxKnockout + maxQuestions + maxPlayers} pts")
        }
    }
}
