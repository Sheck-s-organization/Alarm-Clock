package com.smartalarm.app.data.repository

import com.smartalarm.app.data.dao.HolidayDao
import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.HolidayType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HolidayRepository(private val dao: HolidayDao) {

    val allHolidays: Flow<List<Holiday>> get() = dao.getAll()

    suspend fun insert(holiday: Holiday): Long = dao.insert(holiday)

    suspend fun delete(holiday: Holiday) = dao.delete(holiday)

    suspend fun getAllNow(): List<Holiday> = dao.getAllNow()

    /**
     * Inserts all 11 US federal holidays.
     * Fixed-date holidays are inserted as ANNUAL (repeating every year).
     * Each floating holiday is inserted as a single ONE_TIME entry for its
     * next upcoming occurrence — this year if the date hasn't passed yet,
     * otherwise next year.
     */
    suspend fun importUsHolidays(today: Calendar = Calendar.getInstance()) {
        val holidays = FIXED_FEDERAL_HOLIDAYS + nextFloatingHolidays(today)
        dao.insertAll(holidays)
    }

    companion object {

        /** Fixed-date US federal holidays — same date every year. */
        val FIXED_FEDERAL_HOLIDAYS = listOf(
            Holiday(name = "New Year's Day",   month = 1,  day = 1,  type = HolidayType.ANNUAL),
            Holiday(name = "Juneteenth",       month = 6,  day = 19, type = HolidayType.ANNUAL),
            Holiday(name = "Independence Day", month = 7,  day = 4,  type = HolidayType.ANNUAL),
            Holiday(name = "Veterans Day",     month = 11, day = 11, type = HolidayType.ANNUAL),
            Holiday(name = "Christmas Day",    month = 12, day = 25, type = HolidayType.ANNUAL),
        )

        /**
         * Returns one ONE_TIME entry per floating holiday, each set to its next
         * upcoming occurrence relative to [today].
         */
        fun nextFloatingHolidays(today: Calendar = Calendar.getInstance()): List<Holiday> {
            val year = today.get(Calendar.YEAR)
            return floatingHolidaysForYear(year).map { holiday ->
                // If this year's date has already passed, bump to next year.
                val holidayCal = Calendar.getInstance().apply {
                    set(year, holiday.month - 1, holiday.day)
                }
                if (holidayCal.before(today)) {
                    floatingHolidaysForYear(year + 1)
                        .first { it.name == holiday.name }
                } else {
                    holiday
                }
            }
        }

        /** Returns the 6 floating US federal holidays as ONE_TIME entries for [year]. */
        fun floatingHolidaysForYear(year: Int): List<Holiday> = listOf(
            Holiday(
                name = "Martin Luther King Jr. Day",
                month = 1, day = nthWeekday(year, 1, Calendar.MONDAY, 3),
                year = year, type = HolidayType.ONE_TIME
            ),
            Holiday(
                name = "Presidents' Day",
                month = 2, day = nthWeekday(year, 2, Calendar.MONDAY, 3),
                year = year, type = HolidayType.ONE_TIME
            ),
            Holiday(
                name = "Memorial Day",
                month = 5, day = lastWeekday(year, 5, Calendar.MONDAY),
                year = year, type = HolidayType.ONE_TIME
            ),
            Holiday(
                name = "Labor Day",
                month = 9, day = nthWeekday(year, 9, Calendar.MONDAY, 1),
                year = year, type = HolidayType.ONE_TIME
            ),
            Holiday(
                name = "Columbus Day",
                month = 10, day = nthWeekday(year, 10, Calendar.MONDAY, 2),
                year = year, type = HolidayType.ONE_TIME
            ),
            Holiday(
                name = "Thanksgiving Day",
                month = 11, day = nthWeekday(year, 11, Calendar.THURSDAY, 4),
                year = year, type = HolidayType.ONE_TIME
            ),
        )

        /**
         * Returns the day-of-month for the [nth] occurrence of [weekday]
         * (a [Calendar.DAY_OF_WEEK] constant) in [month]/[year].
         */
        fun nthWeekday(year: Int, month: Int, weekday: Int, nth: Int): Int {
            val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
            var count = 0
            repeat(31) {
                if (cal.get(Calendar.MONTH) == month - 1) {
                    if (cal.get(Calendar.DAY_OF_WEEK) == weekday) {
                        count++
                        if (count == nth) return cal.get(Calendar.DAY_OF_MONTH)
                    }
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            error("nthWeekday($year, $month, $weekday, $nth) not found — invalid input")
        }

        /**
         * Returns the day-of-month for the last [weekday]
         * (a [Calendar.DAY_OF_WEEK] constant) in [month]/[year].
         */
        fun lastWeekday(year: Int, month: Int, weekday: Int): Int {
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, 1)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            while (cal.get(Calendar.DAY_OF_WEEK) != weekday) {
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            return cal.get(Calendar.DAY_OF_MONTH)
        }
    }
}
