package com.porrawc2026.app.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

data class RankingEntry(
    val position: Int,
    val name: String,
    val totalPoints: Int,
    val jornadaPoints: Int
)

class PdfRankingExtractor {

    private data class CharPos(val x: Float, val y: Float, val ch: String, val pg: Int)
    private data class WordItem(val text: String, val x: Float)

    fun extract(doc: PDDocument): List<RankingEntry> {
        val chars = extractCharPositions(doc)
        val allEntries = mutableListOf<RankingEntry>()

        for ((pg, pageChars) in chars.groupBy { it.pg }) {
            val yGroups = pageChars.groupBy { Math.round(it.y) }
            val sortedYs = yGroups.keys.sortedDescending()

            val yWords = mutableMapOf<Int, List<WordItem>>()
            for ((y, items) in yGroups) {
                yWords[y] = mergeToWords(items.sortedBy { it.x })
            }

            val rowTypes = mutableListOf<Triple<Int, String, List<WordItem>>>()
            for (y in sortedYs) {
                val words = yWords[y] ?: continue
                if (words.size < 3) continue
                val type = classifyRow(words)
                if (type != "other") {
                    rowTypes.add(Triple(y, type, words))
                }
            }

            val blocks = mutableListOf<List<Triple<Int, String, List<WordItem>>>>()
            var current = mutableListOf<Triple<Int, String, List<WordItem>>>()
            for (rt in rowTypes) {
                if (rt.second == "pos" && current.any { it.second == "pos" }) {
                    if (current.size >= 2) blocks.add(current.toList())
                    current = mutableListOf(rt)
                } else {
                    current.add(rt)
                }
            }
            if (current.size >= 2) blocks.add(current.toList())

            for (block in blocks) {
                val posRow = block.firstOrNull { it.second == "pos" } ?: continue
                val nameRow = block.firstOrNull { it.second == "name" }
                val numRows = block.filter { it.second == "num" }

                val numbers = posRow.third.mapNotNull { w ->
                    val t = w.text.trim()
                    if (t.all { c -> c.isDigit() } && t.length <= 3) t.toIntOrNull()?.let { it to w.x }
                    else null
                }
                if (numbers.size < 3) continue

                val colWidth = if (numbers.size >= 2) numbers[1].second - numbers[0].second else 30f
                val halfCol = (colWidth / 2f).coerceAtMost(40f)

                for ((pos, colX) in numbers) {
                    val name = nameRow?.let { findTextAtColumn(it.third, colX, halfCol) } ?: ""
                    val pts = numRows.getOrNull(0)?.let { findNumberAtColumn(it.third, colX, halfCol) } ?: 0
                    val jor = numRows.getOrNull(1)?.let { findNumberAtColumn(it.third, colX, halfCol) } ?: 0
                    allEntries.add(RankingEntry(pos, name, pts, jor))
                }
            }
        }

        return allEntries.sortedBy { it.position }
    }

    private fun classifyRow(words: List<WordItem>): String {
        val digitWords = words.count { it.text.all { c -> c.isDigit() } && it.text.length <= 3 }
        val letterWords = words.count { it.text.any { c -> c.isLetter() } }
        val total = words.size

        if (digitWords >= 3) {
            val nums = words.mapNotNull { w ->
                val t = w.text.trim()
                if (t.all { c -> c.isDigit() } && t.length <= 3) t.toIntOrNull()
                else null
            }.sorted()
            if (nums.size >= 3) {
                var seq = true
                for (i in 1 until nums.size) {
                    if (nums[i] != nums[i - 1] + 1) { seq = false; break }
                }
                if (seq) return "pos"
            }
        }

        if (letterWords >= 2 && digitWords <= letterWords) return "name"
        if (digitWords >= 2 && letterWords == 0) return "num"

        return "other"
    }

    private fun findTextAtColumn(items: List<WordItem>, colX: Float, halfCol: Float): String {
        val nearby = items.filter { kotlin.math.abs(it.x - colX) <= halfCol && it.text.any { c -> c.isLetter() } }
            .sortedBy { kotlin.math.abs(it.x - colX) }
        return nearby.joinToString(" ") { it.text }
    }

    private fun findNumberAtColumn(items: List<WordItem>, colX: Float, halfCol: Float): Int {
        val nearby = items.filter { kotlin.math.abs(it.x - colX) <= halfCol && it.text.all { c -> c.isDigit() } }
            .minByOrNull { kotlin.math.abs(it.x - colX) }
        return nearby?.text?.toIntOrNull() ?: 0
    }

    private fun mergeToWords(items: List<CharPos>): List<WordItem> {
        val words = mutableListOf<WordItem>()
        var sb = StringBuilder()
        var startX = 0f
        var prevX = 0f
        for (item in items) {
            if (sb.isEmpty()) {
                sb.append(item.ch)
                startX = item.x
                prevX = item.x
            } else if (item.x - prevX <= 3f) {
                sb.append(item.ch)
                prevX = item.x
            } else {
                words.add(WordItem(sb.toString(), startX))
                sb = StringBuilder(item.ch)
                startX = item.x
                prevX = item.x
            }
        }
        if (sb.isNotEmpty()) {
            words.add(WordItem(sb.toString(), startX))
        }
        return words
    }

    private fun extractCharPositions(doc: PDDocument): List<CharPos> {
        val items = mutableListOf<CharPos>()
        var currentPage = 0
        val stripper = object : PDFTextStripper() {
            override fun startPage(page: com.tom_roush.pdfbox.pdmodel.PDPage) {
                currentPage = currentPageNo
            }
            override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                for (tp in textPositions) {
                    val ch = tp.unicode
                    if (ch != null && ch.isNotBlank()) {
                        items.add(CharPos(tp.xDirAdj, tp.yDirAdj, ch, currentPage))
                    }
                }
            }
        }
        stripper.sortByPosition = true
        stripper.getText(doc)
        return items
    }

    fun saveAsXlsm(entries: List<RankingEntry>, file: File) {
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("Clasificacion")

        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Posición")
        header.createCell(1).setCellValue("Nombre")
        header.createCell(2).setCellValue("Puntos Totales")
        header.createCell(3).setCellValue("Puntos Jornada")

        sheet.setColumnWidth(0, 256 * 8)
        sheet.setColumnWidth(1, 256 * 35)
        sheet.setColumnWidth(2, 256 * 15)
        sheet.setColumnWidth(3, 256 * 15)

        entries.forEachIndexed { idx, entry ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(entry.position.toDouble())
            row.createCell(1).setCellValue(entry.name)
            row.createCell(2).setCellValue(entry.totalPoints.toDouble())
            row.createCell(3).setCellValue(entry.jornadaPoints.toDouble())
        }

        FileOutputStream(file).use { wb.write(it) }
        wb.close()
    }
}
