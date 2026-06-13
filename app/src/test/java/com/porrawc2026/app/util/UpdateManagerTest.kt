package com.porrawc2026.app.util

import org.junit.Assert.*
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun `compareVersions returns positive when v1 is newer`() {
        val result = compareVersions("2.0.0", "1.0.0")
        assertTrue(result > 0)
    }

    @Test
    fun `compareVersions returns negative when v1 is older`() {
        val result = compareVersions("1.0.0", "2.0.0")
        assertTrue(result < 0)
    }

    @Test
    fun `compareVersions returns zero when versions are equal`() {
        val result = compareVersions("1.0.0", "1.0.0")
        assertEquals(0, result)
    }

    @Test
    fun `compareVersions handles minor version differences`() {
        assertTrue(compareVersions("1.2.0", "1.1.0") > 0)
        assertTrue(compareVersions("1.1.0", "1.2.0") < 0)
    }

    @Test
    fun `compareVersions handles patch version differences`() {
        assertTrue(compareVersions("1.0.2", "1.0.1") > 0)
        assertTrue(compareVersions("1.0.1", "1.0.2") < 0)
    }

    @Test
    fun `compareVersions handles different length versions`() {
        assertTrue(compareVersions("1.0.1", "1.0") > 0)
        assertTrue(compareVersions("1.0", "1.0.1") < 0)
        assertEquals(0, compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun `compareVersions handles major version priority`() {
        assertTrue(compareVersions("2.0.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.9.9", "2.0.0") < 0)
    }

    @Test
    fun `compareVersions handles invalid version parts as zero`() {
        val result = compareVersions("1.0.abc", "1.0.0")
        assertEquals(0, result)
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a > b) return 1
            if (a < b) return -1
        }
        return 0
    }
}
