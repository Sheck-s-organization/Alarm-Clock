package com.tddalarm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tddalarm.app.data.repo.AlarmSchedulerPort
import com.tddalarm.app.data.repo.AlarmStore
import com.tddalarm.app.data.repo.WorkCalendarStore
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.NextTriggerCalculator
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlarmListItem(
    val alarm: Alarm,
    /** When the alarm is actually expected to ring next; null when never/disabled. */
    val nextFiring: LocalDateTime?,
)

class AlarmListViewModel(
    private val alarms: AlarmStore,
    calendarStore: WorkCalendarStore,
    private val scheduler: AlarmSchedulerPort,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    val items: StateFlow<List<AlarmListItem>> =
        combine(alarms.alarms, calendarStore.calendar) { list, calendar ->
            list.map { alarm ->
                AlarmListItem(
                    alarm = alarm,
                    nextFiring = NextTriggerCalculator.nextExpectedFiring(
                        alarm, LocalDateTime.now(clock), calendar,
                    ),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(alarm: Alarm) {
        viewModelScope.launch {
            val id = alarms.save(alarm)
            val saved = alarm.copy(id = id)
            if (saved.enabled) scheduler.scheduleNext(saved) else scheduler.cancel(saved.id)
        }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            alarms.setEnabled(alarm.id, enabled)
            if (enabled) {
                scheduler.scheduleNext(alarm.copy(enabled = true))
            } else {
                scheduler.cancel(alarm.id)
            }
        }
    }

    fun delete(alarm: Alarm) {
        viewModelScope.launch {
            alarms.delete(alarm.id)
            scheduler.cancel(alarm.id)
        }
    }
}
