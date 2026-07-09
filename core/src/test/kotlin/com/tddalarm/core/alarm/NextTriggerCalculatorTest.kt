package com.tddalarm.core.alarm

import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import com.tddalarm.core.schedule.WorkCalendar
import com.tddalarm.core.schedule.WorkSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class NextTriggerCalculatorTest {

    // 2026-07-06 is a Monday.
    private val mondayMorning = LocalDateTime.of(2026, 7, 6, 5, 0)

    private val monToFri = WorkCalendar(
        WorkSchedule(
            setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            )
        )
    )

    private val daily630 = Alarm(id = 1, label = "Work", hour = 6, minute = 30)

    // --- nextTrigger: pure clock arithmetic, ignores skip rules -------------

    @Test
    fun `alarm later today triggers today`() {
        assertEquals(
            LocalDateTime.of(2026, 7, 6, 6, 30),
            NextTriggerCalculator.nextTrigger(daily630, now = mondayMorning),
        )
    }

    @Test
    fun `alarm time already passed today triggers tomorrow`() {
        val now = LocalDateTime.of(2026, 7, 6, 7, 0)
        assertEquals(
            LocalDateTime.of(2026, 7, 7, 6, 30),
            NextTriggerCalculator.nextTrigger(daily630, now = now),
        )
    }

    @Test
    fun `alarm at exactly now triggers tomorrow, not now`() {
        val now = LocalDateTime.of(2026, 7, 6, 6, 30)
        assertEquals(
            LocalDateTime.of(2026, 7, 7, 6, 30),
            NextTriggerCalculator.nextTrigger(daily630, now = now),
        )
    }

    @Test
    fun `repeat-day alarm jumps to the next matching weekday`() {
        val sundayAlarm = daily630.copy(repeatDays = setOf(DayOfWeek.SUNDAY), hour = 8, minute = 0)
        assertEquals(
            LocalDateTime.of(2026, 7, 12, 8, 0), // the following Sunday
            NextTriggerCalculator.nextTrigger(sundayAlarm, now = mondayMorning),
        )
    }

    @Test
    fun `repeat-day alarm on its own day but past its time jumps a full week`() {
        val sundayAlarm = daily630.copy(repeatDays = setOf(DayOfWeek.SUNDAY), hour = 8, minute = 0)
        val sundayAfternoon = LocalDateTime.of(2026, 7, 12, 14, 0)
        assertEquals(
            LocalDateTime.of(2026, 7, 19, 8, 0),
            NextTriggerCalculator.nextTrigger(sundayAlarm, now = sundayAfternoon),
        )
    }

    @Test
    fun `next trigger crosses month boundary`() {
        val now = LocalDateTime.of(2026, 7, 31, 23, 0)
        assertEquals(
            LocalDateTime.of(2026, 8, 1, 6, 30),
            NextTriggerCalculator.nextTrigger(daily630, now = now),
        )
    }

    @Test
    fun `next trigger crosses year boundary`() {
        val now = LocalDateTime.of(2026, 12, 31, 23, 0)
        assertEquals(
            LocalDateTime.of(2027, 1, 1, 6, 30),
            NextTriggerCalculator.nextTrigger(daily630, now = now),
        )
    }

    // --- nextExpectedFiring: honours skip-on-days-off ------------------------

    @Test
    fun `next expected firing skips the weekend for a work alarm`() {
        val workAlarm = daily630.copy(skipOnDaysOff = true)
        val fridayEvening = LocalDateTime.of(2026, 7, 10, 20, 0)
        assertEquals(
            LocalDateTime.of(2026, 7, 13, 6, 30), // Monday
            NextTriggerCalculator.nextExpectedFiring(workAlarm, fridayEvening, monToFri),
        )
    }

    @Test
    fun `next expected firing skips PTO and holidays`() {
        val workAlarm = daily630.copy(skipOnDaysOff = true)
        val calendar = WorkCalendar(
            monToFri.schedule,
            // PTO Mon 2026-07-06 through Thu 2026-07-09
            ptoPeriods = listOf(DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 9))),
            // and Friday 2026-07-10 is a one-off holiday
            holidays = listOf(Holiday.OneTime("Company day", LocalDate.of(2026, 7, 10))),
        )
        assertEquals(
            LocalDateTime.of(2026, 7, 13, 6, 30), // next Monday
            NextTriggerCalculator.nextExpectedFiring(workAlarm, mondayMorning, calendar),
        )
    }

    @Test
    fun `next expected firing without the work rule is just the next trigger`() {
        val fridayEvening = LocalDateTime.of(2026, 7, 10, 20, 0)
        assertEquals(
            LocalDateTime.of(2026, 7, 11, 6, 30), // Saturday — no rule, no skipping
            NextTriggerCalculator.nextExpectedFiring(daily630, fridayEvening, monToFri),
        )
    }

    @Test
    fun `next expected firing for a disabled alarm is null`() {
        assertNull(
            NextTriggerCalculator.nextExpectedFiring(daily630.copy(enabled = false), mondayMorning, monToFri),
        )
    }

    @Test
    fun `next expected firing gives up when no workday exists within a year`() {
        val neverWorking = WorkCalendar(WorkSchedule(emptySet()))
        val workAlarm = daily630.copy(skipOnDaysOff = true)
        assertNull(NextTriggerCalculator.nextExpectedFiring(workAlarm, mondayMorning, neverWorking))
    }

    @Test
    fun `church alarm next expected firing lands on Sunday even across a holiday`() {
        val churchAlarm = daily630.copy(
            repeatDays = setOf(DayOfWeek.SUNDAY), hour = 8, minute = 0,
            skipOnDaysOff = false,
        )
        val calendar = WorkCalendar(
            monToFri.schedule,
            holidays = listOf(Holiday.Annual("Christmas", Month.DECEMBER, 25)),
        )
        val now = LocalDateTime.of(2026, 12, 24, 12, 0)
        assertEquals(
            LocalDateTime.of(2026, 12, 27, 8, 0), // Sunday after Christmas
            NextTriggerCalculator.nextExpectedFiring(churchAlarm, now, calendar),
        )
    }
}
