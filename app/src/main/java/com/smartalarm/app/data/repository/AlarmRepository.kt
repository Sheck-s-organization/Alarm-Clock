package com.smartalarm.app.data.repository

import com.smartalarm.app.data.dao.AlarmDao
import com.smartalarm.app.data.dao.AlarmLogDao
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.data.entities.AlarmEvent
import com.smartalarm.app.data.entities.AlarmLog
import com.smartalarm.app.data.entities.AlarmType
import com.smartalarm.app.data.entities.SkipReason
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val alarmLogDao: AlarmLogDao
) {

    val allAlarmsFlow: Flow<List<Alarm>> = alarmDao.getAllAlarmsFlow()
    val enabledAlarmsFlow: Flow<List<Alarm>> = alarmDao.getEnabledAlarmsFlow()

    suspend fun getAlarmById(id: Long): Alarm? = alarmDao.getAlarmById(id)

    suspend fun getAllEnabledAlarms(): List<Alarm> = alarmDao.getAllEnabledAlarms()

    suspend fun getEnabledAlarmsByType(type: AlarmType): List<Alarm> =
        alarmDao.getEnabledAlarmsByType(type)

    suspend fun getAlarmsForSchedule(scheduleId: Long): List<Alarm> =
        alarmDao.getAlarmsForSchedule(scheduleId)

    suspend fun insertAlarm(alarm: Alarm): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = alarmDao.deleteAlarm(alarm)

    suspend fun setAlarmEnabled(id: Long, enabled: Boolean) =
        alarmDao.setAlarmEnabled(id, enabled)

    suspend fun updateNextTriggerTime(id: Long, triggerTime: Long?) =
        alarmDao.updateNextTriggerTime(id, triggerTime)

    // ---- Logging ----

    fun getRecentLogsFlow(limit: Int = 50) = alarmLogDao.getRecentLogsFlow(limit)

    fun getLogsForAlarmFlow(alarmId: Long) = alarmLogDao.getLogsForAlarmFlow(alarmId)

    suspend fun logEvent(
        alarmId: Long,
        event: AlarmEvent,
        skipReason: SkipReason? = null,
        wasCharging: Boolean? = null,
        locationName: String? = null,
        isHoliday: Boolean = false,
        isWorkDay: Boolean? = null,
        dayOfMonth: Int? = null,
        snoozeCount: Int = 0
    ) {
        alarmLogDao.insertLog(
            AlarmLog(
                alarmId = alarmId,
                event = event,
                skipReason = skipReason,
                wasCharging = wasCharging,
                locationName = locationName,
                isHoliday = isHoliday,
                isWorkDay = isWorkDay,
                dayOfMonth = dayOfMonth,
                snoozeCount = snoozeCount
            )
        )
    }

    suspend fun pruneOldLogs(keepDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
        alarmLogDao.deleteOldLogs(cutoff)
    }
}
