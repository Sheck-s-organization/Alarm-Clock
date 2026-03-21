package com.smartalarm.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartalarm.app.data.entities.WorkSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: WorkSchedule): Long

    @Delete
    suspend fun delete(schedule: WorkSchedule)

    @Query("SELECT * FROM work_schedules ORDER BY name ASC")
    fun getAll(): Flow<List<WorkSchedule>>

    @Query("SELECT * FROM work_schedules WHERE id = :id")
    suspend fun getById(id: Long): WorkSchedule?
}
