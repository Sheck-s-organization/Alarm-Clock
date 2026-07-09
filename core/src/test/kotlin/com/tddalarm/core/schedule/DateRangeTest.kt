package com.tddalarm.core.schedule

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateRangeTest {

    private val range = DateRange(
        start = LocalDate.of(2026, 7, 10),
        endInclusive = LocalDate.of(2026, 7, 14),
    )

    @Test
    fun `contains date strictly inside the range`() {
        assertTrue(LocalDate.of(2026, 7, 12) in range)
    }

    @Test
    fun `contains both boundary dates`() {
        assertTrue(LocalDate.of(2026, 7, 10) in range)
        assertTrue(LocalDate.of(2026, 7, 14) in range)
    }

    @Test
    fun `excludes dates outside the range`() {
        assertFalse(LocalDate.of(2026, 7, 9) in range)
        assertFalse(LocalDate.of(2026, 7, 15) in range)
    }

    @Test
    fun `single day range contains only that day`() {
        val singleDay = DateRange(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 24))
        assertTrue(LocalDate.of(2026, 12, 24) in singleDay)
        assertFalse(LocalDate.of(2026, 12, 25) in singleDay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `end before start is rejected`() {
        DateRange(LocalDate.of(2026, 7, 14), LocalDate.of(2026, 7, 10))
    }
}
