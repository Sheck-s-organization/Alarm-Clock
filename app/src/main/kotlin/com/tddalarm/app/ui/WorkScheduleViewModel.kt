package com.tddalarm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tddalarm.app.data.HolidayEntry
import com.tddalarm.app.data.PtoEntry
import com.tddalarm.app.data.repo.WorkCalendarStore
import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkScheduleViewModel(private val store: WorkCalendarStore) : ViewModel() {

    val workingDays: StateFlow<Set<DayOfWeek>> =
        store.workingDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val ptoEntries: StateFlow<List<PtoEntry>> =
        store.ptoEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val holidayEntries: StateFlow<List<HolidayEntry>> =
        store.holidayEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleDay(day: DayOfWeek) {
        viewModelScope.launch {
            val current = store.workingDays.first()
            store.setWorkingDays(if (day in current) current - day else current + day)
        }
    }

    fun addPto(first: LocalDate, second: LocalDate) {
        viewModelScope.launch {
            val (start, end) = if (second.isBefore(first)) second to first else first to second
            store.addPto(DateRange(start, end))
        }
    }

    fun removePto(id: Long) {
        viewModelScope.launch { store.removePto(id) }
    }

    fun addOneTimeHoliday(name: String, date: LocalDate) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { store.addHoliday(Holiday.OneTime(trimmed, date)) }
    }

    fun addAnnualHoliday(name: String, date: LocalDate) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { store.addHoliday(Holiday.Annual(trimmed, date.month, date.dayOfMonth)) }
    }

    fun removeHoliday(id: Long) {
        viewModelScope.launch { store.removeHoliday(id) }
    }
}
