package com.tddalarm.app.scheduling

import com.tddalarm.app.FakeAlarmStore
import com.tddalarm.app.FakeLocationProvider
import com.tddalarm.app.FakeScheduler
import com.tddalarm.app.FakeWorkCalendarStore
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.LocationRule
import com.tddalarm.core.alarm.SkipReason
import com.tddalarm.core.geo.GeoFence
import com.tddalarm.core.geo.GeoPoint
import com.tddalarm.core.schedule.DateRange
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleAlarmTriggerTest {

    private val alarms = FakeAlarmStore()
    private val calendarStore = FakeWorkCalendarStore()
    private val locations = FakeLocationProvider()
    private val scheduler = FakeScheduler()

    // Fixed clock: Monday 2026-07-06 06:30 UTC.
    private val clock = Clock.fixed(
        LocalDate.of(2026, 7, 6).atTime(6, 30).toInstant(ZoneOffset.UTC),
        ZoneId.of("UTC"),
    )

    private val handler = HandleAlarmTrigger(alarms, calendarStore, locations, scheduler, clock = clock)

    private val home = GeoPoint(39.768403, -86.158068)
    private val chicago = GeoPoint(41.878113, -87.629799)

    @Test
    fun `unknown alarm id reports not found and schedules nothing`() = runTest {
        assertEquals(HandleAlarmTrigger.Outcome.NotFound, handler(999))
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `plain alarm rings and its next occurrence is chained`() = runTest {
        val id = alarms.save(Alarm(label = "Work", hour = 6, minute = 30))
        val outcome = handler(id)
        assertTrue(outcome is HandleAlarmTrigger.Outcome.Ring)
        assertEquals(listOf(id), scheduler.scheduled.map { it.id })
    }

    @Test
    fun `work alarm on a PTO day stays silent but still chains the next occurrence`() = runTest {
        calendarStore.addPto(DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10)))
        val id = alarms.save(Alarm(label = "Work", hour = 6, minute = 30, skipOnDaysOff = true))
        assertEquals(
            HandleAlarmTrigger.Outcome.Silent(SkipReason.PTO),
            handler(id),
        )
        assertEquals(listOf(id), scheduler.scheduled.map { it.id })
    }

    @Test
    fun `location-restricted alarm rings at home`() = runTest {
        locations.location = home
        val id = alarms.save(
            Alarm(
                label = "Church", hour = 6, minute = 30,
                locationRule = LocationRule(GeoFence(home, 25_000.0)),
            )
        )
        assertTrue(handler(id) is HandleAlarmTrigger.Outcome.Ring)
        assertTrue(locations.queried)
    }

    @Test
    fun `location-restricted alarm stays silent out of town`() = runTest {
        locations.location = chicago
        val id = alarms.save(
            Alarm(
                label = "Church", hour = 6, minute = 30,
                locationRule = LocationRule(GeoFence(home, 25_000.0)),
            )
        )
        assertEquals(
            HandleAlarmTrigger.Outcome.Silent(SkipReason.OUTSIDE_LOCATION),
            handler(id),
        )
    }

    @Test
    fun `location is not queried for alarms without a location rule`() = runTest {
        val id = alarms.save(Alarm(label = "Work", hour = 6, minute = 30))
        handler(id)
        assertFalse(locations.queried)
    }

    @Test
    fun `disabled alarm stays silent and is not rescheduled`() = runTest {
        val id = alarms.save(Alarm(label = "Old", hour = 6, minute = 30, enabled = false))
        assertEquals(
            HandleAlarmTrigger.Outcome.Silent(SkipReason.DISABLED),
            handler(id),
        )
        assertTrue(scheduler.scheduled.isEmpty())
    }
}
