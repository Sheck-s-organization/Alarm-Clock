package com.tddalarm.core.alarm

import com.tddalarm.core.geo.GeoPoint
import com.tddalarm.core.schedule.DayOffReason
import com.tddalarm.core.schedule.WorkCalendar
import java.time.LocalDate

enum class SkipReason {
    DISABLED,
    NOT_A_REPEAT_DAY,
    NOT_A_WORKING_DAY,
    HOLIDAY,
    PTO,
    OUTSIDE_LOCATION,
    LOCATION_UNKNOWN,
}

sealed interface FiringDecision {
    data object Fire : FiringDecision
    data class Skip(val reason: SkipReason) : FiringDecision
}

/**
 * The single place that decides, at ring time, whether an alarm should
 * actually go off. Pure logic: date, work calendar and device location are
 * passed in, nothing is read from the environment.
 */
class AlarmGate {

    fun evaluate(
        alarm: Alarm,
        date: LocalDate,
        calendar: WorkCalendar?,
        location: GeoPoint?,
    ): FiringDecision {
        if (!alarm.enabled) return FiringDecision.Skip(SkipReason.DISABLED)

        if (alarm.repeatDays.isNotEmpty() && date.dayOfWeek !in alarm.repeatDays) {
            return FiringDecision.Skip(SkipReason.NOT_A_REPEAT_DAY)
        }

        if (alarm.skipOnDaysOff && calendar != null) {
            when (calendar.dayOffReason(date)) {
                DayOffReason.NOT_A_WORKING_DAY -> return FiringDecision.Skip(SkipReason.NOT_A_WORKING_DAY)
                DayOffReason.HOLIDAY -> return FiringDecision.Skip(SkipReason.HOLIDAY)
                DayOffReason.PTO -> return FiringDecision.Skip(SkipReason.PTO)
                null -> Unit
            }
        }

        val rule = alarm.locationRule
        if (rule != null) {
            if (location == null) {
                if (!rule.fireWhenLocationUnknown) {
                    return FiringDecision.Skip(SkipReason.LOCATION_UNKNOWN)
                }
            } else if (location !in rule.fence) {
                return FiringDecision.Skip(SkipReason.OUTSIDE_LOCATION)
            }
        }

        return FiringDecision.Fire
    }
}
