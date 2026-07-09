package com.tddalarm.core.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class WorkCalendarTest {

    // 2026-07-06 is a Monday.
    private val monday = LocalDate.of(2026, 7, 6)
    private val friday = LocalDate.of(2026, 7, 10)
    private val saturday = LocalDate.of(2026, 7, 11)
    private val sunday = LocalDate.of(2026, 7, 12)

    private val monToFri = WorkSchedule(
        setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
    )

    @Test
    fun `weekday on a Mon-Fri schedule is a workday`() {
        val calendar = WorkCalendar(monToFri)
        assertTrue(calendar.isWorkday(monday))
        assertNull(calendar.dayOffReason(monday))
    }

    @Test
    fun `weekend on a Mon-Fri schedule is not a working day`() {
        val calendar = WorkCalendar(monToFri)
        assertFalse(calendar.isWorkday(saturday))
        assertEquals(DayOffReason.NOT_A_WORKING_DAY, calendar.dayOffReason(saturday))
        assertEquals(DayOffReason.NOT_A_WORKING_DAY, calendar.dayOffReason(sunday))
    }

    @Test
    fun `date inside a PTO period is a day off even if it is a working day`() {
        val calendar = WorkCalendar(
            schedule = monToFri,
            ptoPeriods = listOf(DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10))),
        )
        assertFalse(calendar.isWorkday(monday))
        assertEquals(DayOffReason.PTO, calendar.dayOffReason(monday))
    }

    @Test
    fun `working day after the PTO period ends is a workday again`() {
        val calendar = WorkCalendar(
            schedule = monToFri,
            ptoPeriods = listOf(DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10))),
        )
        // 2026-07-13 is the Monday after the PTO week.
        assertTrue(calendar.isWorkday(LocalDate.of(2026, 7, 13)))
    }

    @Test
    fun `holiday on a working day is a day off`() {
        val calendar = WorkCalendar(
            schedule = monToFri,
            holidays = listOf(Holiday.Annual("Christmas", Month.DECEMBER, 25)),
        )
        // 2026-12-25 is a Friday.
        val christmas = LocalDate.of(2026, 12, 25)
        assertFalse(calendar.isWorkday(christmas))
        assertEquals(DayOffReason.HOLIDAY, calendar.dayOffReason(christmas))
    }

    @Test
    fun `non-working day is reported as NOT_A_WORKING_DAY even when it is also a holiday`() {
        val calendar = WorkCalendar(
            schedule = monToFri,
            // 2026-07-11 is a Saturday.
            holidays = listOf(Holiday.OneTime("Some Saturday festival", saturday)),
        )
        assertEquals(DayOffReason.NOT_A_WORKING_DAY, calendar.dayOffReason(saturday))
    }

    @Test
    fun `holiday takes precedence over PTO when both apply`() {
        val calendar = WorkCalendar(
            schedule = monToFri,
            ptoPeriods = listOf(DateRange(LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 31))),
            holidays = listOf(Holiday.Annual("Christmas", Month.DECEMBER, 25)),
        )
        assertEquals(DayOffReason.HOLIDAY, calendar.dayOffReason(LocalDate.of(2026, 12, 25)))
    }

    @Test
    fun `empty schedule means no day is ever a workday`() {
        val calendar = WorkCalendar(WorkSchedule(emptySet()))
        assertFalse(calendar.isWorkday(monday))
        assertFalse(calendar.isWorkday(saturday))
    }

    @Test
    fun `multiple PTO periods are all respected`() {
        val calendar = WorkCalendar(
            schedule = monToFri,
            ptoPeriods = listOf(
                DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 7)),
                DateRange(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)),
            ),
        )
        assertEquals(DayOffReason.PTO, calendar.dayOffReason(LocalDate.of(2026, 7, 6)))
        assertEquals(DayOffReason.PTO, calendar.dayOffReason(LocalDate.of(2026, 8, 5)))
        assertTrue(calendar.isWorkday(LocalDate.of(2026, 7, 8)))
    }
}
