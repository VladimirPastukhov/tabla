package io.github.vladimirpastukhov.tabla

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EventTimeTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun startFor(hour: Int, minute: Int, second: Int = 0): LocalDateTime {
        val now = LocalDateTime.of(2026, 10, 24, hour, minute, second).atZone(zone).toInstant()
        return quarterHourStart(now, zone).toLocalDateTime()
    }

    @Test
    fun roundsDownWithinEachQuarter() {
        assertEquals(LocalDateTime.of(2026, 10, 24, 14, 0), startFor(14, 7))
        assertEquals(LocalDateTime.of(2026, 10, 24, 14, 15), startFor(14, 29, 59))
        assertEquals(LocalDateTime.of(2026, 10, 24, 14, 30), startFor(14, 44))
        assertEquals(LocalDateTime.of(2026, 10, 24, 14, 45), startFor(14, 59, 59))
    }

    @Test
    fun keepsExactQuarterBoundary() {
        assertEquals(LocalDateTime.of(2026, 10, 24, 14, 15), startFor(14, 15, 0))
    }

    @Test
    fun usesLocalTimeInZoneWithNonWholeHourOffset() {
        val india = ZoneId.of("Asia/Kolkata") // UTC+5:30
        val now = LocalDateTime.of(2026, 10, 24, 9, 22).atZone(india).toInstant()
        assertEquals(LocalDateTime.of(2026, 10, 24, 9, 15), quarterHourStart(now, india).toLocalDateTime())
    }
}
