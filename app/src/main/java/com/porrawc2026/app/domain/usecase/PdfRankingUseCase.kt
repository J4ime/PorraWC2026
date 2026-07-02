package com.porrawc2026.app.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.porrawc2026.app.util.PdfRankingExtractor
import com.porrawc2026.app.util.RankingEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

sealed class PdfResult {
    data class Success(val entries: List<RankingEntry>, val userEntry: RankingEntry?) : PdfResult()
    data class Error(val message: String) : PdfResult()
}

@Singleton
class PdfRankingUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val pdfExtractor = PdfRankingExtractor()

    suspend fun loadPdf(uri: Uri, userName: String): PdfResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "loadPdf: uri=$uri")
            if (userName.isBlank()) {
                return@withContext PdfResult.Error("No hay nombre de usuario. Importa un Excel primero en Ajustes.")
            }
            val searchName = userName
            Log.i(TAG, "searchName=\"$searchName\"")

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext PdfResult.Error("No se pudo abrir el PDF")
            val doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
            Log.i(TAG, "PDF loaded, pages=${doc.numberOfPages}")

            val entries = pdfExtractor.extract(doc)
            Log.i(TAG, "extracted ${entries.size} entries")

            runCatching {
                val xlsmFile = File(context.cacheDir, "ClasificacionMundial_2026.xlsm")
                pdfExtractor.saveAsXlsm(entries, xlsmFile)
            }.onFailure { e ->
                Log.e(TAG, "Failed to save XLSM", e)
            }

            doc.close()
            inputStream.close()

            val sampleSize = minOf(10, entries.size)
            if (entries.isEmpty()) {
                Log.w(TAG, "No se extrajeron entradas del PDF")
            } else {
                for (i in 0 until sampleSize) {
                    val e = entries[i]
                    Log.i(TAG, "  entrada[$i]: \"${e.name}\" (pos=${e.position}, pts=${e.totalPoints})")
                }
            }

            val searchNorm = normalizeName(searchName)
            Log.i(TAG, "Buscando \"$searchName\" (normalized=\"$searchNorm\") en ${entries.size} entradas")

            val match = findUserInEntries(entries, searchName)
            if (match != null) {
                Log.i(TAG, "Encontrado en posición ${match.position}")
            } else {
                Log.w(TAG, "No encontrado: searchName=\"$searchName\" normalized=\"${normalizeName(searchName)}\"")
            }

            PdfResult.Success(entries, match)
        } catch (e: Exception) {
            Log.e(TAG, "loadPdfResult error", e)
            PdfResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun findUserInEntries(entries: List<RankingEntry>, searchName: String): RankingEntry? {
        val searchNorm = normalizeName(searchName)
        val searchFlat = searchNorm.replace(" ", "")

        for (entry in entries) {
            val nameNorm = normalizeName(entry.name)
            if (nameNorm.contains(searchNorm)) {
                Log.i(TAG, "Stage1 match: entry=\"${entry.name}\" contains \"$searchNorm\"")
                return entry
            }
        }

        for (entry in entries) {
            val nameNorm = normalizeName(entry.name)
            val flat = nameNorm.replace(" ", "")
            if (flat.contains(searchFlat)) {
                Log.i(TAG, "Stage2 match: entry=\"${entry.name}\" flat=\"$flat\" contains \"$searchFlat\"")
                return entry
            }
        }

        val searchLast = lastWord(searchNorm)
        if (searchLast.length >= 3 && searchLast != searchNorm) {
            for (entry in entries) {
                val nameNorm = normalizeName(entry.name)
                val nameLast = lastWord(nameNorm)
                if (nameLast == searchLast || nameNorm.contains(searchLast)) {
                    Log.i(TAG, "Stage3 match: entry=\"${entry.name}\" last=\"$nameLast\" searchLast=\"$searchLast\"")
                    return entry
                }
            }
        }

        return null
    }

    private fun normalizeName(name: String): String {
        return Normalizer.normalize(name.lowercase().trim(), Normalizer.Form.NFD)
            .replace(diacriticsRegex, "")
            .replace(".", "").replace("-", " ").replace("'", " ")
            .replace(whitespaceRegex, " ").trim()
    }

    private fun lastWord(name: String): String {
        val parts = name.split(" ")
        return if (parts.size > 1) parts.last() else name
    }

    companion object {
        private const val TAG = "PdfRankingUseCase"
        private val diacriticsRegex = Regex("\\p{M}")
        private val whitespaceRegex = Regex("\\s+")
    }
}
