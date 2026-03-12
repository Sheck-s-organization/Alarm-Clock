package com.smartalarm.app.data.dao

import androidx.room.*
import com.smartalarm.app.data.entities.AlarmLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmLogDao {

    @Query("SELECT * FROM alarm_logs ORDER BY triggered_at DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int = 50): Flow<List<AlarmLog>>

    @Query("SELECT * FROM alarm_logs WHERE alarm_id = :alarmId ORDER BY triggered_at DESC LIMIT :limit")
    fun getLogsForAlarmFlow(alarmId: Long, limit: Int = 30): Flow<List<AlarmLog>>

    @Insert
    suspend fun insertLog(log: AlarmLog): Long

    @Query("DELETE FROM alarm_logs WHERE triggered_at < :beforeTime")
    suspend fun deleteOldLogs(beforeTime: Long)

    @Query("SELECT COUNT(*) FROM alarm_logs WHERE alarm_id = :alarmId AND event = 'SNOOZED' AND triggered_at > :sinceTime")
    suspend fun getSnoozeCountSince(alarmId: Long, sinceTime: Long): Int
}
