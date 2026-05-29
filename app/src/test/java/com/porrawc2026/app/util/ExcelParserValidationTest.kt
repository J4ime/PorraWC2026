package com.porrawc2026.app.util

import org.junit.Assert.*
import org.junit.Test

class ExcelParserValidationTest {

    @Test
    fun `validationResult is valid when zero errors`() {
        val result = ValidationResult(true, 200, 200, 0, emptyList(), emptyList())
        assertTrue(result.isValid)
        assertEquals(200, result.totalChecks)
        assertEquals(200, result.passedChecks)
        assertEquals(0, result.failedChecks)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `validationResult is invalid with errors`() {
        val result = ValidationResult(
            isValid = false,
            totalChecks = 200,
            passedChecks = 150,
            failedChecks = 50,
            errors = listOf("Grupo A, fila 5 — valor: 'ERROR'"),
            warnings = emptyList()
        )
        assertFalse(result.isValid)
        assertEquals(50, result.failedChecks)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `validationResult with warnings but no errors is valid`() {
        val result = ValidationResult(
            isValid = true,
            totalChecks = 200, passedChecks = 200, failedChecks = 0,
            errors = emptyList(),
            warnings = listOf("Columna AG validada: 200 celdas OK")
        )
        assertTrue(result.isValid)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `validate with no sheet returns error`() {
        val result = ExcelParser.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("columna AG") })
        assertEquals(0, result.totalChecks)
    }
}
