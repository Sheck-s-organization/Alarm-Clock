package com.tddalarm.app.ui

import com.tddalarm.app.FakeAlarmStore
import com.tddalarm.app.FakePlaceStore
import com.tddalarm.app.FakeScheduler
import com.tddalarm.app.FakeWorkCalendarStore
import com.tddalarm.app.data.Place
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.LocationRule
import com.tddalarm.core.geo.GeoFence
import com.tddalarm.core.geo.GeoPoint
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val alarms = FakeAlarmStore()
    private val calendarStore = FakeWorkCalendarStore()
    private val places = FakePlaceStore()
    private val scheduler = FakeScheduler()

    // Friday 2026-07-10, 20:00.
    private val clock = Clock.fixed(
        LocalDate.of(2026, 7, 10).atTime(20, 0).toInstant(ZoneOffset.UTC),
        ZoneId.of("UTC"),
    )

    private fun viewModel() = AlarmListViewModel(alarms, calendarStore, places, scheduler, clock)

    @Test
    fun `saving a new alarm stores it and schedules its next trigger`() = runTest {
        val vm = viewModel()
        vm.save(Alarm(label = "Work", hour = 6, minute = 30))
        advanceUntilIdle()

        assertEquals(1, alarms.enabledAlarms().size)
        val scheduled = scheduler.scheduled.single()
        assertEquals("Work", scheduled.label)
        assertTrue("saved alarm must be scheduled with its DB id", scheduled.id != 0L)
    }

    @Test
    fun `saving a disabled alarm cancels instead of scheduling`() = runTest {
        val vm = viewModel()
        vm.save(Alarm(id = 5, label = "Off", hour = 7, minute = 0, enabled = false))
        advanceUntilIdle()

        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf(5L), scheduler.cancelled)
    }

    @Test
    fun `disabling an alarm cancels it, re-enabling reschedules it`() = runTest {
        val id = alarms.save(Alarm(label = "Work", hour = 6, minute = 30))
        val alarm = requireNotNull(alarms.getById(id))
        val vm = viewModel()

        vm.setEnabled(alarm, false)
        advanceUntilIdle()
        assertEquals(listOf(id), scheduler.cancelled)
        assertEquals(false, alarms.getById(id)?.enabled)

        vm.setEnabled(alarm.copy(enabled = false), true)
        advanceUntilIdle()
        assertEquals(id, scheduler.scheduled.single().id)
        assertEquals(true, alarms.getById(id)?.enabled)
    }

    @Test
    fun `deleting an alarm removes it and cancels its trigger`() = runTest {
        val id = alarms.save(Alarm(label = "Work", hour = 6, minute = 30))
        val vm = viewModel()

        vm.delete(requireNotNull(alarms.getById(id)))
        advanceUntilIdle()

        assertTrue(alarms.enabledAlarms().isEmpty())
        assertEquals(listOf(id), scheduler.cancelled)
    }

    @Test
    fun `list items expose the next expected firing honouring the work calendar`() = runTest {
        // Work alarm on a Friday evening: next expected firing skips the weekend.
        alarms.save(Alarm(label = "Work", hour = 6, minute = 30, skipOnDaysOff = true))
        val vm = viewModel()

        val collector = launch { vm.items.collect {} }
        advanceUntilIdle()

        val item = vm.items.value.single()
        assertEquals(LocalDateTime.of(2026, 7, 13, 6, 30), item.nextFiring) // Monday
        collector.cancel()
    }

    @Test
    fun `list items expose the saved place name for location-restricted alarms`() = runTest {
        val fence = GeoFence(GeoPoint(39.768403, -86.158068), 25_000.0)
        val placeId = places.save(Place(0, "Home", fence))
        alarms.save(
            Alarm(
                label = "Church", hour = 8, minute = 0,
                locationRule = LocationRule(fence, placeId = placeId),
            )
        )
        alarms.save(Alarm(label = "Work", hour = 6, minute = 30))
        val vm = viewModel()

        val collector = launch { vm.items.collect {} }
        advanceUntilIdle()

        val byLabel = vm.items.value.associateBy { it.alarm.label }
        assertEquals("Home", byLabel.getValue("Church").placeName)
        assertEquals(null, byLabel.getValue("Work").placeName)
        collector.cancel()
    }
}
