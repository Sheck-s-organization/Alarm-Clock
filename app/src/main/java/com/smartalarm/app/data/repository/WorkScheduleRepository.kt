package com.smartalarm.app.data.repository

import com.smartalarm.app.data.dao.WorkScheduleDao
import com.smartalarm.app.data.entities.WorkSchedule
import kotlinx.coroutines.flow.Flow

class WorkScheduleRepository(private val dao: WorkScheduleDao) {

    val allSchedules: Flow<List<WorkSchedule>> get() = dao.getAll()

    suspend fun insert(schedule: WorkSchedule): Long = dao.insert(schedule)

    suspend fun delete(schedule: WorkSchedule) = dao.delete(schedule)

    suspend fun getById(id: Long): WorkSchedule? = dao.getById(id)
}
