package com.porrawc2026.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class TeamNameNormalizerTest {

    @Test
    fun `enToEs translates known team names`() {
        assertEquals("México", TeamNameNormalizer.enToEs("Mexico"))
        assertEquals("Sudáfrica", TeamNameNormalizer.enToEs("South Africa"))
        assertEquals("Corea del Sur", TeamNameNormalizer.enToEs("South Korea"))
        assertEquals("República Checa", TeamNameNormalizer.enToEs("Czech Republic"))
        assertEquals("Bosnia y Herzegovina", TeamNameNormalizer.enToEs("Bosnia-Herzegovina"))
        assertEquals("Estados Unidos", TeamNameNormalizer.enToEs("United States"))
        assertEquals("Países Bajos", TeamNameNormalizer.enToEs("Netherlands"))
        assertEquals("Costa de Marfil", TeamNameNormalizer.enToEs("Ivory Coast"))
        assertEquals("Arabia Saudita", TeamNameNormalizer.enToEs("Saudi Arabia"))
        assertEquals("Nueva Zelanda", TeamNameNormalizer.enToEs("New Zealand"))
    }

    @Test
    fun `enToEs returns original for unknown names`() {
        assertEquals("Unknown Team", TeamNameNormalizer.enToEs("Unknown Team"))
        assertEquals("Test", TeamNameNormalizer.enToEs("Test"))
    }

    @Test
    fun `normalize removes accents and spaces`() {
        assertEquals("mexico", TeamNameNormalizer.normalize("México"))
        assertEquals("espana", TeamNameNormalizer.normalize("España"))
        assertEquals("francia", TeamNameNormalizer.normalize("Francia"))
        assertEquals("alemania", TeamNameNormalizer.normalize("Alemania"))
    }

    @Test
    fun `normalize maps known variations`() {
        assertEquals("sudafrica", TeamNameNormalizer.normalize("South Africa"))
        assertEquals("coreadelsur", TeamNameNormalizer.normalize("South Korea"))
        assertEquals("coreadelsur", TeamNameNormalizer.normalize("Korea Republic"))
        assertEquals("republicacheca", TeamNameNormalizer.normalize("Czech Republic"))
        assertEquals("bosniayherzegovina", TeamNameNormalizer.normalize("Bosnia-Herzegovina"))
        assertEquals("estadosunidos", TeamNameNormalizer.normalize("United States"))
        assertEquals("estadosunidos", TeamNameNormalizer.normalize("USA"))
        assertEquals("paisesbajos", TeamNameNormalizer.normalize("Netherlands"))
        assertEquals("costademarfil", TeamNameNormalizer.normalize("Ivory Coast"))
    }

    @Test
    fun `matches returns true for equivalent names`() {
        assertTrue(TeamNameNormalizer.matches("México", "Mexico"))
        assertTrue(TeamNameNormalizer.matches("South Korea", "Corea del Sur"))
        assertTrue(TeamNameNormalizer.matches("United States", "USA"))
        assertTrue(TeamNameNormalizer.matches("España", "Spain"))
    }

    @Test
    fun `matches returns false for different names`() {
        assertFalse(TeamNameNormalizer.matches("México", "Argentina"))
        assertFalse(TeamNameNormalizer.matches("España", "Francia"))
        assertFalse(TeamNameNormalizer.matches("Brasil", "Portugal"))
    }

    @Test
    fun `normalize handles case insensitivity`() {
        assertEquals(TeamNameNormalizer.normalize("MEXICO"), TeamNameNormalizer.normalize("mexico"))
        assertEquals(TeamNameNormalizer.normalize("Spain"), TeamNameNormalizer.normalize("SPAIN"))
    }

    @Test
    fun `normalize handles hyphens and spaces`() {
        assertEquals(TeamNameNormalizer.normalize("Bosnia-Herzegovina"), 
                     TeamNameNormalizer.normalize("Bosnia Herzegovina"))
        assertEquals(TeamNameNormalizer.normalize("South Korea"), 
                     TeamNameNormalizer.normalize("SouthKorea"))
    }
}
