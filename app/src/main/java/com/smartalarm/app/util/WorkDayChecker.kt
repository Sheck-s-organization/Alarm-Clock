package com.smartalarm.app.util

import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.HolidayType
import com.smartalarm.app.data.entities.WorkSchedule
import java.util.Calendar

/**
 * Pure, Android-framework-free helper that decides whether an alarm should fire.
 *
 * Rules (in order):
 * 1. If [workSchedule] is null the alarm has no schedule filter → always fires.
 * 2. If today's day-of-week is not in [workSchedule.workDays] → skip.
 * 3. If today matches any entry in [holidays] → skip.
 * 4. Otherwise → fire.
 */
object WorkDayChecker {

    /**
     * Returns `true` if the alarm should fire on the day represented by [today].
     *
     * @param workSchedule the schedule linked to the alarm, or null if none.
     * @param holidays     all holidays currently stored; matching one causes a skip.
     * @param today        calendar representing the moment the alarm triggers (default = now).
     */
    fun shouldFire(
        workSchedule: WorkSchedule?,
        holidays: List<Holiday>,
        today: Calendar = Calendar.getInstance()
    ): Boolean {
        workSchedule ?: return true

        val dayOfWeek = today.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek !in workSchedule.workDays) return false

        val month = today.get(Calendar.MONTH) + 1   // Calendar uses 0-based months
        val day   = today.get(Calendar.DAY_OF_MONTH)
        val year  = today.get(Calendar.YEAR)

        return holidays.none { it.matches(month, day, year) }
    }

    private fun Holiday.matches(month: Int, day: Int, year: Int): Boolean = when (type) {
        HolidayType.ANNUAL   -> this.month == month && this.day == day
        HolidayType.ONE_TIME -> this.month == month && this.day == day && this.year == year
    }
}
