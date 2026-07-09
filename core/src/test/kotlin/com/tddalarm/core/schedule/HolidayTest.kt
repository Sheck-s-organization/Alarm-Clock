package com.tddalarm.core.schedule

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class HolidayTest {

    @Test
    fun `one-time holiday occurs only on its exact date`() {
        val goodFriday2026 = Holiday.OneTime("Good Friday", LocalDate.of(2026, 4, 3))
        assertTrue(goodFriday2026.occursOn(LocalDate.of(2026, 4, 3)))
        assertFalse(goodFriday2026.occursOn(LocalDate.of(2027, 4, 3)))
        assertFalse(goodFriday2026.occursOn(LocalDate.of(2026, 4, 4)))
    }

    @Test
    fun `annual holiday occurs on the same month and day every year`() {
        val christmas = Holiday.Annual("Christmas", Month.DECEMBER, 25)
        assertTrue(christmas.occursOn(LocalDate.of(2026, 12, 25)))
        assertTrue(christmas.occursOn(LocalDate.of(2030, 12, 25)))
        assertFalse(christmas.occursOn(LocalDate.of(2026, 12, 24)))
        assertFalse(christmas.occursOn(LocalDate.of(2026, 11, 25)))
    }

    @Test
    fun `annual holiday on leap day only occurs in leap years`() {
        val leapDay = Holiday.Annual("Leap day", Month.FEBRUARY, 29)
        assertTrue(leapDay.occursOn(LocalDate.of(2028, 2, 29)))
        assertFalse(leapDay.occursOn(LocalDate.of(2026, 2, 28)))
        assertFalse(leapDay.occursOn(LocalDate.of(2026, 3, 1)))
    }
}
