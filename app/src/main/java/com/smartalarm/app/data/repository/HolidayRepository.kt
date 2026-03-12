package com.smartalarm.app.data.repository

import com.smartalarm.app.data.dao.HolidayDao
import com.smartalarm.app.data.entities.Holiday
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HolidayRepository(private val dao: HolidayDao) {

    val allHolidaysFlow: Flow<List<Holiday>> = dao.getAllHolidaysFlow()

    fun getHolidaysForMonthFlow(month: Int): Flow<List<Holiday>> =
        dao.getHolidaysForMonthFlow(month)

    suspend fun getHolidaysForDate(year: Int, month: Int, day: Int): List<Holiday> =
        dao.getHolidaysForDate(year, month, day)

    /** Convenience: check if today is a holiday */
    suspend fun isTodayHoliday(): Boolean {
        val cal = Calendar.getInstance()
        return dao.getHolidaysForDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,   // Calendar months are 0-based
            cal.get(Calendar.DAY_OF_MONTH)
        ).isNotEmpty()
    }

    /** Check if a specific date (in millis) is a holiday */
    suspend fun isHoliday(timeMillis: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return dao.getHolidaysForDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        ).isNotEmpty()
    }

    suspend fun insertHoliday(holiday: Holiday): Long = dao.insertHoliday(holiday)

    suspend fun insertHolidays(holidays: List<Holiday>) = dao.insertHolidays(holidays)

    suspend fun updateHoliday(holiday: Holiday) = dao.updateHoliday(holiday)

    suspend fun deleteHoliday(holiday: Holiday) = dao.deleteHoliday(holiday)

    suspend fun replaceImportedHolidays(countryCode: String, holidays: List<Holiday>) {
        dao.deleteImportedHolidays(countryCode)
        dao.insertHolidays(holidays)
    }
}
