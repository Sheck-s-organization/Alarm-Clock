package com.smartalarm.app.data.dao

import androidx.room.*
import com.smartalarm.app.data.entities.WorkSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkScheduleDao {

    @Query("SELECT * FROM work_schedules ORDER BY name ASC")
    fun getAllSchedulesFlow(): Flow<List<WorkSchedule>>

    @Query("SELECT * FROM work_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): WorkSchedule?

    @Query("SELECT * FROM work_schedules WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveSchedule(): WorkSchedule?

    @Query("SELECT * FROM work_schedules WHERE is_active = 1 LIMIT 1")
    fun getActiveScheduleFlow(): Flow<WorkSchedule?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: WorkSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: WorkSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: WorkSchedule)

    @Query("UPDATE work_schedules SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE work_schedules SET is_active = 1 WHERE id = :id")
    suspend fun activateSchedule(id: Long)

    @Transaction
    suspend fun setActiveSchedule(id: Long) {
        deactivateAll()
        activateSchedule(id)
    }
}
