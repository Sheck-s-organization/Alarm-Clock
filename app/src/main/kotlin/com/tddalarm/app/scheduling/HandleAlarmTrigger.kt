package com.tddalarm.app.scheduling

import com.tddalarm.app.data.repo.AlarmSchedulerPort
import com.tddalarm.app.data.repo.AlarmStore
import com.tddalarm.app.data.repo.WorkCalendarStore
import com.tddalarm.app.location.LocationProvider
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.AlarmGate
import com.tddalarm.core.alarm.FiringDecision
import com.tddalarm.core.alarm.SkipReason
import java.time.Clock
import java.time.LocalDate

/**
 * Everything that happens when an alarm's trigger time arrives, minus the
 * Android plumbing: decide whether to ring (work calendar + location rules)
 * and chain the alarm's next occurrence.
 */
class HandleAlarmTrigger(
    private val alarms: AlarmStore,
    private val calendarStore: WorkCalendarStore,
    private val locationProvider: LocationProvider,
    private val scheduler: AlarmSchedulerPort,
    private val gate: AlarmGate = AlarmGate(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    sealed interface Outcome {
        data class Ring(val alarm: Alarm) : Outcome
        data class Silent(val reason: SkipReason) : Outcome
        data object NotFound : Outcome
    }

    suspend operator fun invoke(alarmId: Long): Outcome {
        val alarm = alarms.getById(alarmId) ?: return Outcome.NotFound

        // Repeating alarms chain their next occurrence no matter what today's
        // decision is — a skipped day must not kill the series.
        if (alarm.enabled) scheduler.scheduleNext(alarm)

        val calendar = if (alarm.skipOnDaysOff) calendarStore.currentCalendar() else null
        val location = if (alarm.locationRule != null) locationProvider.lastKnown() else null

        return when (val decision = gate.evaluate(alarm, LocalDate.now(clock), calendar, location)) {
            is FiringDecision.Fire -> Outcome.Ring(alarm)
            is FiringDecision.Skip -> Outcome.Silent(decision.reason)
        }
    }
}
