package com.mesaviva.app.shell

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FestivalTest {

    @Test
    fun `easter sunday follows the gregorian calendar`() {
        assertEquals(LocalDate.of(2024, 3, 31), easterSunday(2024))
        assertEquals(LocalDate.of(2025, 4, 20), easterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), easterSunday(2026))
    }

    @Test
    fun `holy week carnival and corpus follow easter`() {
        assertEquals("semana-santa", festivalFor(LocalDate.of(2026, 4, 3), "VE").id)
        assertEquals("carnaval", festivalFor(LocalDate.of(2026, 2, 17), "VE").id)
        assertEquals("corpus", festivalFor(LocalDate.of(2026, 6, 4), "VE").id)
    }

    @Test
    fun `venezuelan festivals keep their dates`() {
        assertEquals("cruz-de-mayo", festivalFor(LocalDate.of(2026, 5, 3), "VE").id)
        assertEquals("san-juan", festivalFor(LocalDate.of(2026, 6, 24), "VE").id)
        assertEquals("independencia", festivalFor(LocalDate.of(2026, 7, 5), "VE").id)
        assertEquals("halloween", festivalFor(LocalDate.of(2026, 10, 31), "VE").id)
    }

    @Test
    fun `christmas and three kings overlap with the right priority`() {
        assertEquals("navidad", festivalFor(LocalDate.of(2026, 12, 25), "VE").id)
        assertEquals("reyes", festivalFor(LocalDate.of(2026, 1, 6), "VE").id)
    }

    @Test
    fun `independence day changes with the country`() {
        assertEquals("independencia", festivalFor(LocalDate.of(2026, 9, 16), "MX").id)
        assertEquals("independencia", festivalFor(LocalDate.of(2026, 7, 4), "US").id)
        assertEquals("independencia", festivalFor(LocalDate.of(2026, 9, 7), "BR").id)
        assertEquals("mesa", festivalFor(LocalDate.of(2026, 9, 16), "VE").id)
    }

    @Test
    fun `day of the dead wins over halloween in Mexico`() {
        assertEquals("dia-de-muertos", festivalFor(LocalDate.of(2026, 11, 2), "MX").id)
        assertEquals("halloween", festivalFor(LocalDate.of(2026, 11, 2), "VE").id)
    }

    @Test
    fun `ordinary days fall back to the creole table`() {
        assertEquals("mesa", festivalFor(LocalDate.of(2026, 8, 15), "VE").id)
    }
}
