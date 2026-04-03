package com.smartalarm.app.util

import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.HolidayType
import com.smartalarm.app.data.entities.WorkSchedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WorkDayCheckerTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun calendarFor(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)   // Calendar is 0-based
            set(Calendar.DAY_OF_MONTH, day)
        }

    private val monToFri = WorkSchedule(
        id = 1L, name = "Mon–Fri",
        workDays = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY
        )
    )

    // ── 5.1: no schedule → always fire ───────────────────────────────────────

    @Test
    fun `no schedule fires on any day`() {
        // Saturday
        val saturday = calendarFor(2026, 3, 21)
        assertTrue(WorkDayChecker.shouldFire(null, emptyList(), saturday))
    }

    @Test
    fun `no schedule fires even with holidays present`() {
        val newYearsDay = Holiday(name = "New Year's Day", month = 1, day = 1, type = HolidayType.ANNUAL)
        val jan1 = calendarFor(2026, 1, 1)
        assertTrue(WorkDayChecker.shouldFire(null, listOf(newYearsDay), jan1))
    }

    // ── 5.2: work days filter ────────────────────────────────────────────────

    @Test
    fun `fires on a configured work day`() {
        // 2026-03-16 is a Monday
        val monday = calendarFor(2026, 3, 16)
        assertTrue(WorkDayChecker.shouldFire(monToFri, emptyList(), monday))
    }

    @Test
    fun `skips on Saturday when not a work day`() {
        // 2026-03-21 is a Saturday
        val saturday = calendarFor(2026, 3, 21)
        assertFalse(WorkDayChecker.shouldFire(monToFri, emptyList(), saturday))
    }

    @Test
    fun `skips on Sunday when not a work day`() {
        // 2026-03-22 is a Sunday
        val sunday = calendarFor(2026, 3, 22)
        assertFalse(WorkDayChecker.shouldFire(monToFri, emptyList(), sunday))
    }

    @Test
    fun `fires on Saturday when Saturday is a work day`() {
        val sixDaySchedule = monToFri.copy(workDays = monToFri.workDays + Calendar.SATURDAY)
        val saturday = calendarFor(2026, 3, 21)
        assertTrue(WorkDayChecker.shouldFire(sixDaySchedule, emptyList(), saturday))
    }

    // ── 5.3: annual holiday skip ─────────────────────────────────────────────

    @Test
    fun `skips on annual holiday even if it is a work day`() {
        val christmas = Holiday(name = "Christmas", month = 12, day = 25, type = HolidayType.ANNUAL)
        // 2026-12-25 is a Friday
        val xmas = calendarFor(2026, 12, 25)
        assertFalse(WorkDayChecker.shouldFire(monToFri, listOf(christmas), xmas))
    }

    @Test
    fun `annual holiday does not block a different day`() {
        val christmas = Holiday(name = "Christmas", month = 12, day = 25, type = HolidayType.ANNUAL)
        // 2026-12-24 Thursday
        val xmasEve = calendarFor(2026, 12, 24)
        assertTrue(WorkDayChecker.shouldFire(monToFri, listOf(christmas), xmasEve))
    }

    @Test
    fun `annual holiday repeats in every year`() {
        val newYear = Holiday(name = "New Year's Day", month = 1, day = 1, type = HolidayType.ANNUAL)
        // 2027-01-01 is a Friday
        val jan1future = calendarFor(2027, 1, 1)
        assertFalse(WorkDayChecker.shouldFire(monToFri, listOf(newYear), jan1future))
    }

    // ── 5.4: one-time holiday skip ───────────────────────────────────────────

    @Test
    fun `skips on one-time holiday in the correct year`() {
        val offDay = Holiday(name = "Company Day Off", month = 6, day = 15, year = 2026, type = HolidayType.ONE_TIME)
        // 2026-06-15 is a Monday
        val day = calendarFor(2026, 6, 15)
        assertFalse(WorkDayChecker.shouldFire(monToFri, listOf(offDay), day))
    }

    @Test
    fun `one-time holiday does not block the same date in a different year`() {
        val offDay = Holiday(name = "Company Day Off", month = 6, day = 15, year = 2026, type = HolidayType.ONE_TIME)
        // 2027-06-15 is a Tuesday
        val nextYear = calendarFor(2027, 6, 15)
        assertTrue(WorkDayChecker.shouldFire(monToFri, listOf(offDay), nextYear))
    }

    // ── 5.5: edge — empty work days set ─────────────────────────────────────

    @Test
    fun `schedule with no work days always skips`() {
        val noWorkDays = monToFri.copy(workDays = emptySet())
        val monday = calendarFor(2026, 3, 16)
        assertFalse(WorkDayChecker.shouldFire(noWorkDays, emptyList(), monday))
    }

    // ── 5.6: multiple holidays ───────────────────────────────────────────────

    @Test
    fun `skips when one of several holidays matches today`() {
        val holidays = listOf(
            Holiday(name = "New Year's Day", month = 1, day = 1, type = HolidayType.ANNUAL),
            Holiday(name = "Christmas Day",  month = 12, day = 25, type = HolidayType.ANNUAL)
        )
        // 2026-01-01 is a Thursday
        val newYear = calendarFor(2026, 1, 1)
        assertFalse(WorkDayChecker.shouldFire(monToFri, holidays, newYear))
    }
}
