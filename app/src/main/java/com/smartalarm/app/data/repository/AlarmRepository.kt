package com.smartalarm.app.data.repository

import com.smartalarm.app.data.dao.AlarmDao
import com.smartalarm.app.data.entities.Alarm
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {

    val allAlarms: Flow<List<Alarm>> get() = alarmDao.getAll()

    suspend fun insert(alarm: Alarm): Long = alarmDao.insert(alarm)

    suspend fun delete(alarm: Alarm) = alarmDao.delete(alarm)

    suspend fun getById(id: Long): Alarm? = alarmDao.getById(id)
}
