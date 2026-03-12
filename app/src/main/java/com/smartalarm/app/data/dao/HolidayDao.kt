package com.smartalarm.app.data.dao

import androidx.room.*
import com.smartalarm.app.data.entities.Holiday
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays ORDER BY month ASC, day ASC")
    fun getAllHolidaysFlow(): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays WHERE id = :id")
    suspend fun getHolidayById(id: Long): Holiday?

    /**
     * Returns all holidays that could match a given date.
     * Handles ONE_TIME (year+month+day), ANNUAL (month+day), and RANGE types.
     */
    @Query("""
        SELECT * FROM holidays WHERE
        (holiday_type = 'ONE_TIME' AND year = :year AND month = :month AND day = :day)
        OR (holiday_type = 'ANNUAL' AND month = :month AND day = :day)
        OR (holiday_type = 'RANGE' AND (
            (year = :year OR year IS NULL) AND
            (month * 100 + day) <= (:month * 100 + :day) AND
            (end_month * 100 + end_day) >= (:month * 100 + :day)
        ))
    """)
    suspend fun getHolidaysForDate(year: Int, month: Int, day: Int): List<Holiday>

    @Query("SELECT * FROM holidays WHERE month = :month ORDER BY day ASC")
    fun getHolidaysForMonthFlow(month: Int): Flow<List<Holiday>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: Holiday): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHolidays(holidays: List<Holiday>): List<Long>

    @Update
    suspend fun updateHoliday(holiday: Holiday)

    @Delete
    suspend fun deleteHoliday(holiday: Holiday)

    @Query("DELETE FROM holidays WHERE is_imported = 1 AND country_code = :countryCode")
    suspend fun deleteImportedHolidays(countryCode: String)

    @Query("SELECT COUNT(*) FROM holidays")
    suspend fun getHolidayCount(): Int
}
