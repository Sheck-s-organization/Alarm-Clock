package com.tddalarm.core.alarm

import com.tddalarm.core.geo.GeoFence
import com.tddalarm.core.geo.GeoPoint
import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import com.tddalarm.core.schedule.WorkCalendar
import com.tddalarm.core.schedule.WorkSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class AlarmGateTest {

    private val gate = AlarmGate()

    // 2026-07-06 is a Monday; 2026-07-12 is a Sunday.
    private val monday = LocalDate.of(2026, 7, 6)
    private val sunday = LocalDate.of(2026, 7, 12)

    private val monToFri = WorkCalendar(
        WorkSchedule(
            setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            )
        )
    )

    private val home = GeoPoint(39.768403, -86.158068)
    private val farAway = GeoPoint(41.878113, -87.629799) // Chicago, ~260 km away
    private val homeTown = GeoFence(center = home, radiusMeters = 25_000.0)

    private val workAlarm = Alarm(
        id = 1, label = "Work", hour = 6, minute = 30,
        skipOnDaysOff = true,
    )

    private val churchAlarm = Alarm(
        id = 2, label = "Church", hour = 8, minute = 0,
        repeatDays = setOf(DayOfWeek.SUNDAY),
        locationRule = LocationRule(fence = homeTown),
    )

    // --- basics -----------------------------------------------------------

    @Test
    fun `disabled alarm never fires`() {
        val alarm = workAlarm.copy(enabled = false)
        assertEquals(
            FiringDecision.Skip(SkipReason.DISABLED),
            gate.evaluate(alarm, monday, monToFri, location = null),
        )
    }

    @Test
    fun `plain enabled alarm with no rules fires`() {
        val alarm = Alarm(id = 3, label = "Plain", hour = 7, minute = 0)
        assertEquals(FiringDecision.Fire, gate.evaluate(alarm, monday, calendar = null, location = null))
    }

    @Test
    fun `alarm with repeat days does not fire on other days`() {
        assertEquals(
            FiringDecision.Skip(SkipReason.NOT_A_REPEAT_DAY),
            gate.evaluate(churchAlarm, monday, monToFri, location = home),
        )
    }

    // --- work-schedule rule (PTO + holidays) ------------------------------

    @Test
    fun `work alarm fires on a normal working day`() {
        assertEquals(FiringDecision.Fire, gate.evaluate(workAlarm, monday, monToFri, location = null))
    }

    @Test
    fun `work alarm is skipped on the weekend`() {
        assertEquals(
            FiringDecision.Skip(SkipReason.NOT_A_WORKING_DAY),
            gate.evaluate(workAlarm, sunday, monToFri, location = null),
        )
    }

    @Test
    fun `work alarm is skipped during PTO`() {
        val calendar = WorkCalendar(
            monToFri.schedule,
            ptoPeriods = listOf(DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10))),
        )
        assertEquals(
            FiringDecision.Skip(SkipReason.PTO),
            gate.evaluate(workAlarm, monday, calendar, location = null),
        )
    }

    @Test
    fun `work alarm is skipped on a holiday`() {
        val calendar = WorkCalendar(
            monToFri.schedule,
            holidays = listOf(Holiday.Annual("Christmas", Month.DECEMBER, 25)),
        )
        // 2026-12-25 is a Friday.
        assertEquals(
            FiringDecision.Skip(SkipReason.HOLIDAY),
            gate.evaluate(workAlarm, LocalDate.of(2026, 12, 25), calendar, location = null),
        )
    }

    @Test
    fun `skipOnDaysOff without a configured calendar falls back to firing`() {
        assertEquals(FiringDecision.Fire, gate.evaluate(workAlarm, sunday, calendar = null, location = null))
    }

    @Test
    fun `alarm without the work rule still fires on PTO days`() {
        val calendar = WorkCalendar(
            monToFri.schedule,
            ptoPeriods = listOf(DateRange(monday, monday)),
        )
        val alarm = workAlarm.copy(skipOnDaysOff = false)
        assertEquals(FiringDecision.Fire, gate.evaluate(alarm, monday, calendar, location = null))
    }

    // --- location rule (church alarm) --------------------------------------

    @Test
    fun `church alarm fires on Sunday when at home`() {
        assertEquals(FiringDecision.Fire, gate.evaluate(churchAlarm, sunday, monToFri, location = home))
    }

    @Test
    fun `church alarm is skipped when out of town`() {
        assertEquals(
            FiringDecision.Skip(SkipReason.OUTSIDE_LOCATION),
            gate.evaluate(churchAlarm, sunday, monToFri, location = farAway),
        )
    }

    @Test
    fun `location-restricted alarm fires when location is unknown by default`() {
        // Fail-safe: better a spurious wake-up than silently missing church.
        assertEquals(FiringDecision.Fire, gate.evaluate(churchAlarm, sunday, monToFri, location = null))
    }

    @Test
    fun `location-restricted alarm can be configured to skip when location is unknown`() {
        val alarm = churchAlarm.copy(
            locationRule = LocationRule(fence = homeTown, fireWhenLocationUnknown = false),
        )
        assertEquals(
            FiringDecision.Skip(SkipReason.LOCATION_UNKNOWN),
            gate.evaluate(alarm, sunday, monToFri, location = null),
        )
    }

    // --- combined rules -----------------------------------------------------

    @Test
    fun `alarm with both rules must pass both`() {
        val alarm = workAlarm.copy(locationRule = LocationRule(fence = homeTown))
        // Working Monday but out of town -> location rule blocks it.
        assertEquals(
            FiringDecision.Skip(SkipReason.OUTSIDE_LOCATION),
            gate.evaluate(alarm, monday, monToFri, location = farAway),
        )
        // At home but on PTO -> work rule blocks it.
        val ptoCalendar = WorkCalendar(monToFri.schedule, ptoPeriods = listOf(DateRange(monday, monday)))
        assertEquals(
            FiringDecision.Skip(SkipReason.PTO),
            gate.evaluate(alarm, monday, ptoCalendar, location = home),
        )
        // At home on a working day -> fires.
        assertEquals(FiringDecision.Fire, gate.evaluate(alarm, monday, monToFri, location = home))
    }

    // --- invalid alarm times -------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `hour out of range is rejected`() {
        Alarm(id = 9, label = "bad", hour = 24, minute = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `minute out of range is rejected`() {
        Alarm(id = 9, label = "bad", hour = 23, minute = 60)
    }
}
