package com.tddalarm.core.alarm

import com.tddalarm.core.schedule.WorkCalendar
import java.time.LocalDateTime

object NextTriggerCalculator {

    private const val MAX_LOOKAHEAD_DAYS = 366L

    /**
     * The next wall-clock instant the alarm's time and repeat days match,
     * strictly after [now]. Skip rules (days off, location) are deliberately
     * ignored: they are evaluated at ring time, because PTO entries and the
     * device's location can change between scheduling and ringing.
     */
    fun nextTrigger(alarm: Alarm, now: LocalDateTime): LocalDateTime {
        var candidate = now.toLocalDate().atTime(alarm.hour, alarm.minute)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        while (alarm.repeatDays.isNotEmpty() && candidate.dayOfWeek !in alarm.repeatDays) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    /**
     * The next instant the alarm is actually expected to ring, taking the
     * skip-on-days-off rule into account — for display ("next: Mon 6:30") and
     * for scheduling past known days off. Returns null for a disabled alarm or
     * when nothing fires within [MAX_LOOKAHEAD_DAYS].
     */
    fun nextExpectedFiring(
        alarm: Alarm,
        now: LocalDateTime,
        calendar: WorkCalendar?,
    ): LocalDateTime? {
        if (!alarm.enabled) return null

        var candidate = nextTrigger(alarm, now)
        val horizon = now.plusDays(MAX_LOOKAHEAD_DAYS)
        while (candidate.isBefore(horizon)) {
            val skipsToday = alarm.skipOnDaysOff && calendar != null &&
                !calendar.isWorkday(candidate.toLocalDate())
            if (!skipsToday) return candidate
            candidate = nextTrigger(alarm, now = candidate)
        }
        return null
    }
}
