package com.tddalarm.core.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/** An inclusive range of calendar dates, e.g. a PTO period. */
data class DateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(!endInclusive.isBefore(start)) {
            "endInclusive ($endInclusive) must not be before start ($start)"
        }
    }

    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(endInclusive)
}

/** A day the user does not work, either once or every year. */
sealed interface Holiday {
    val name: String

    fun occursOn(date: LocalDate): Boolean

    data class OneTime(override val name: String, val date: LocalDate) : Holiday {
        override fun occursOn(date: LocalDate): Boolean = date == this.date
    }

    data class Annual(override val name: String, val month: Month, val dayOfMonth: Int) : Holiday {
        override fun occursOn(date: LocalDate): Boolean =
            date.month == month && date.dayOfMonth == dayOfMonth
    }
}

/** The weekdays the user normally works. */
data class WorkSchedule(val workingDays: Set<DayOfWeek>)

enum class DayOffReason { NOT_A_WORKING_DAY, HOLIDAY, PTO }

/**
 * Combines the weekly work schedule with PTO periods and holidays to decide
 * whether a given date is a workday.
 */
class WorkCalendar(
    val schedule: WorkSchedule,
    val ptoPeriods: List<DateRange> = emptyList(),
    val holidays: List<Holiday> = emptyList(),
) {

    /** Returns why [date] is a day off, or null if it is a workday. */
    fun dayOffReason(date: LocalDate): DayOffReason? = when {
        date.dayOfWeek !in schedule.workingDays -> DayOffReason.NOT_A_WORKING_DAY
        holidays.any { it.occursOn(date) } -> DayOffReason.HOLIDAY
        ptoPeriods.any { date in it } -> DayOffReason.PTO
        else -> null
    }

    fun isWorkday(date: LocalDate): Boolean = dayOffReason(date) == null
}
