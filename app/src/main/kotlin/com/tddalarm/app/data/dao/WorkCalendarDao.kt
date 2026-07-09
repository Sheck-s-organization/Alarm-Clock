package com.tddalarm.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tddalarm.app.data.entity.HolidayEntity
import com.tddalarm.app.data.entity.PtoPeriodEntity
import com.tddalarm.app.data.entity.WorkScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkCalendarDao {

    @Query("SELECT * FROM work_schedule WHERE id = 1")
    fun schedule(): Flow<WorkScheduleEntity?>

    @Query("SELECT * FROM work_schedule WHERE id = 1")
    suspend fun scheduleOnce(): WorkScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(entity: WorkScheduleEntity)

    @Query("SELECT * FROM pto_periods ORDER BY startEpochDay")
    fun ptoPeriods(): Flow<List<PtoPeriodEntity>>

    @Query("SELECT * FROM pto_periods")
    suspend fun ptoPeriodsOnce(): List<PtoPeriodEntity>

    @Insert
    suspend fun insertPto(entity: PtoPeriodEntity): Long

    @Query("DELETE FROM pto_periods WHERE id = :id")
    suspend fun deletePto(id: Long)

    @Query("SELECT * FROM holidays ORDER BY name")
    fun holidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays")
    suspend fun holidaysOnce(): List<HolidayEntity>

    @Insert
    suspend fun insertHoliday(entity: HolidayEntity): Long

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteHoliday(id: Long)
}
