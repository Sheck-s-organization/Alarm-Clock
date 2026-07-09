package com.tddalarm.app.ui

import com.tddalarm.app.FakeWorkCalendarStore
import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkScheduleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = FakeWorkCalendarStore()
    private fun viewModel() = WorkScheduleViewModel(store)

    @Test
    fun `toggling a working day off removes it from the schedule`() = runTest {
        val vm = viewModel()
        vm.toggleDay(DayOfWeek.FRIDAY)
        advanceUntilIdle()
        assertFalse(DayOfWeek.FRIDAY in store.workingDays.first())
    }

    @Test
    fun `toggling a non-working day on adds it to the schedule`() = runTest {
        val vm = viewModel()
        vm.toggleDay(DayOfWeek.SATURDAY)
        advanceUntilIdle()
        assertTrue(DayOfWeek.SATURDAY in store.workingDays.first())
    }

    @Test
    fun `adding PTO with reversed dates normalizes them`() = runTest {
        val vm = viewModel()
        vm.addPto(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 6))
        advanceUntilIdle()
        assertEquals(
            DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10)),
            store.ptoEntries.first().single().range,
        )
    }

    @Test
    fun `removing a PTO entry deletes it`() = runTest {
        val vm = viewModel()
        vm.addPto(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10))
        advanceUntilIdle()
        val id = store.ptoEntries.first().single().id

        vm.removePto(id)
        advanceUntilIdle()
        assertTrue(store.ptoEntries.first().isEmpty())
    }

    @Test
    fun `blank holiday names are rejected`() = runTest {
        val vm = viewModel()
        vm.addOneTimeHoliday("   ", LocalDate.of(2026, 4, 3))
        advanceUntilIdle()
        assertTrue(store.holidayEntries.first().isEmpty())
    }

    @Test
    fun `one-time and annual holidays are stored`() = runTest {
        val vm = viewModel()
        vm.addOneTimeHoliday("Good Friday", LocalDate.of(2026, 4, 3))
        vm.addAnnualHoliday("Christmas", LocalDate.of(2026, 12, 25))
        advanceUntilIdle()

        val holidays = store.holidayEntries.first().map { it.holiday }
        assertTrue(Holiday.OneTime("Good Friday", LocalDate.of(2026, 4, 3)) in holidays)
        assertTrue(Holiday.Annual("Christmas", Month.DECEMBER, 25) in holidays)
    }

    @Test
    fun `removing a holiday deletes it`() = runTest {
        val vm = viewModel()
        vm.addAnnualHoliday("Christmas", LocalDate.of(2026, 12, 25))
        advanceUntilIdle()
        val id = store.holidayEntries.first().single().id

        vm.removeHoliday(id)
        advanceUntilIdle()
        assertTrue(store.holidayEntries.first().isEmpty())
    }
}
