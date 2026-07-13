package com.tddalarm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tddalarm.app.data.repo.AlarmSchedulerPort
import com.tddalarm.app.data.repo.AlarmStore
import com.tddalarm.app.data.repo.PlaceStore
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

/** How the alarm editor was closed. */
enum class EditorResult {
    /** Explicit Save button. */
    SAVE,

    /** Explicit Cancel button — the only way to discard edits. */
    CANCEL,

    /**
     * Clicked off (tap outside / Back). Treated as save: a set alarm that was
     * never explicitly saved must still wake the user up.
     */
    DISMISS,

    /** Explicit Delete button. */
    DELETE,
}

data class AlarmListItem(
    val alarm: Alarm,
    /** When the alarm is actually expected to ring next; null when never/disabled. */
    val nextFiring: LocalDateTime?,
    /** Name of the saved place the alarm is restricted to, when it has one. */
    val placeName: String? = null,
)

class AlarmListViewModel(
    private val alarms: AlarmStore,
    calendarStore: WorkCalendarStore,
    placeStore: PlaceStore,
    private val scheduler: AlarmSchedulerPort,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    val items: StateFlow<List<AlarmListItem>> =
        combine(alarms.alarms, calendarStore.calendar, placeStore.places) { list, calendar, places ->
            val placeNames = places.associate { it.id to it.name }
            list.map { alarm ->
                AlarmListItem(
                    alarm = alarm,
                    nextFiring = NextTriggerCalculator.nextExpectedFiring(
                        alarm, LocalDateTime.now(clock), calendar,
                    ),
                    placeName = alarm.locationRule?.placeId?.let(placeNames::get),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onEditorClosed(draft: Alarm, result: EditorResult) {
        when (result) {
            EditorResult.SAVE, EditorResult.DISMISS -> save(draft)
            EditorResult.CANCEL -> Unit
            EditorResult.DELETE -> delete(draft)
        }
    }

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
