package com.smartalarm.app.data.dao

import androidx.room.*
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.data.entities.AlarmType
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarmsFlow(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabledAlarmsFlow(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE alarm_type = :type AND enabled = 1")
    suspend fun getEnabledAlarmsByType(type: AlarmType): List<Alarm>

    @Query("SELECT * FROM alarms WHERE work_schedule_id = :scheduleId AND enabled = 1")
    suspend fun getAlarmsForSchedule(scheduleId: Long): List<Alarm>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabledAlarms(): List<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET next_trigger_time = :triggerTime WHERE id = :id")
    suspend fun updateNextTriggerTime(id: Long, triggerTime: Long?)
}
