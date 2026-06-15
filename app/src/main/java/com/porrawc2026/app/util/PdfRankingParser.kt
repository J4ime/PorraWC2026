package com.porrawc2026.app.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

class PdfRankingParser {

    private data class TextPos(val x: Float, val y: Float, val text: String, val pg: Int)

    fun findNamePosition(doc: PDDocument, searchName: String): Int {
        val allItems = extractTextPositions(doc)
        val (targetPage, targetX) = findNameInItems(allItems, searchName) ?: return 0

        val pageYGroups = allItems.groupBy { it.pg }
            .mapValues { (_, items) ->
                items.groupBy { Math.round(it.y) }
            }

        var position = 0
        for (pg in 1 until targetPage) {
            val yg = pageYGroups[pg] ?: continue
            val posData = findPositionRow(yg) ?: continue
            position += posData.size
        }

        val targetYg = pageYGroups[targetPage] ?: return 0
        val targetPositions = findPositionRow(targetYg) ?: return 0

        val sortedPositions = targetPositions.sortedBy { it.second }
        val firstPosX = sortedPositions.first().second
        val lastPosX = sortedPositions.last().second
        val tableWidth = lastPosX - firstPosX
        if (tableWidth <= 0f) return 0
        val numPositions = sortedPositions.size
        val relativeX = ((targetX - firstPosX) / tableWidth).coerceIn(0f, 1f)
        val colIndex = Math.round(relativeX * (numPositions - 1)).coerceIn(0, numPositions - 1)
        val bestPos = sortedPositions[colIndex].first
        position += bestPos - sortedPositions.first().first + 1
        return position
    }

    private fun extractTextPositions(doc: PDDocument): List<TextPos> {
        val items = mutableListOf<TextPos>()
        var currentPage = 0
        val stripper = object : PDFTextStripper() {
            override fun startPage(page: com.tom_roush.pdfbox.pdmodel.PDPage) {
                currentPage = currentPageNo
            }
            override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                for (tp in textPositions) {
                    val ch = tp.unicode
                    if (ch != null && ch.isNotBlank()) {
                        items.add(TextPos(tp.xDirAdj, tp.yDirAdj, ch, currentPage))
                    }
                }
            }
        }
        stripper.sortByPosition = true
        stripper.getText(doc)
        return items
    }

    private fun findNameInItems(allItems: List<TextPos>, searchName: String): Pair<Int, Float>? {
        fun normalize(s: String) = s.lowercase()
            .replace("\u00ed", "i").replace("\u00e9", "e").replace("\u00e1", "a")
            .replace("\u00f3", "o").replace("\u00fa", "u").replace("\u00f1", "n")
        val searchNorm = normalize(searchName)

        fun isIgnored(c: String) = c.isBlank() || c in listOf("-", "_", ".", ",", "'", ":", ";", "(", ")", "\"", "!", "?")
        var targetPage = -1
        var targetX = -1f
        var searchOffset = -1
        var pdfIdx = 0
        var nameIdx = 0
        while (pdfIdx < allItems.size && nameIdx < searchNorm.length) {
            val pdfChar = normalize(allItems[pdfIdx].text)
            if (isIgnored(pdfChar)) {
                pdfIdx++
                continue
            }
            if (pdfChar == searchNorm[nameIdx].toString()) {
                if (nameIdx == 0) searchOffset = pdfIdx
                nameIdx++
                if (nameIdx == searchNorm.length) {
                    targetPage = allItems[searchOffset].pg
                    var sumX = 0f
                    for (k in searchOffset..pdfIdx) {
                        sumX += allItems[k].x
                    }
                    targetX = sumX / (pdfIdx - searchOffset + 1)
                    return targetPage to targetX
                }
                pdfIdx++
            } else {
                pdfIdx = (searchOffset + 1).coerceAtLeast(pdfIdx - nameIdx + 1)
                nameIdx = 0
                searchOffset = -1
            }
        }
        return null
    }

    private fun findPositionRow(yg: Map<Int, List<TextPos>>): List<Pair<Int, Float>>? {
        val allPositions = mutableListOf<Pair<Int, Float>>()
        for ((_, items) in yg) {
            if (items.size < 5) continue
            val digitItems = items.filter { it.text.trim().isNotEmpty() && it.text.trim().all { c -> c.isDigit() } }
                .sortedBy { it.x }
            if (digitItems.size < 5) continue
            val groups = mutableListOf<Pair<Int, Float>>()
            var current = StringBuilder()
            var firstX = 0f
            var prevX = 0f
            for (d in digitItems) {
                if (current.isEmpty()) {
                    current.append(d.text.trim())
                    firstX = d.x
                    prevX = d.x
                } else if (d.x - prevX < 3) {
                    current.append(d.text.trim())
                    prevX = d.x
                } else {
                    val n = current.toString().toIntOrNull()
                    if (n != null) groups.add(n to firstX)
                    current = StringBuilder(d.text.trim())
                    firstX = d.x
                    prevX = d.x
                }
            }
            val n = current.toString().toIntOrNull()
            if (n != null) groups.add(n to firstX)
            if (groups.size < 5) continue
            val sorted = groups.map { it.first }.sorted()
            var sequential = true
            for (i in 1 until sorted.size) {
                if (sorted[i] != sorted[i - 1] + 1) { sequential = false; break }
            }
            if (sequential) {
                allPositions.addAll(groups)
            }
        }
        return allPositions.sortedBy { it.first }.ifEmpty { null }
    }
}
