package com.smartalarm.app.data.repository

import com.smartalarm.app.data.dao.HolidayDao
import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.HolidayType
import kotlinx.coroutines.flow.Flow

class HolidayRepository(private val dao: HolidayDao) {

    val allHolidays: Flow<List<Holiday>> get() = dao.getAll()

    suspend fun insert(holiday: Holiday): Long = dao.insert(holiday)

    suspend fun delete(holiday: Holiday) = dao.delete(holiday)

    suspend fun getAllNow(): List<Holiday> = dao.getAllNow()

    /** Inserts the fixed-date US federal holidays (annual, repeating). */
    suspend fun importUsHolidays() {
        dao.insertAll(US_FEDERAL_HOLIDAYS)
    }

    companion object {
        val US_FEDERAL_HOLIDAYS = listOf(
            Holiday(name = "New Year's Day",      month = 1,  day = 1,  type = HolidayType.ANNUAL),
            Holiday(name = "Independence Day",    month = 7,  day = 4,  type = HolidayType.ANNUAL),
            Holiday(name = "Veterans Day",        month = 11, day = 11, type = HolidayType.ANNUAL),
            Holiday(name = "Christmas Day",       month = 12, day = 25, type = HolidayType.ANNUAL)
            // Floating holidays (MLK Day, Presidents Day, Memorial Day, Labor Day,
            // Columbus Day, Thanksgiving) require last-workday logic — deferred to Phase 7.
        )
    }
}
