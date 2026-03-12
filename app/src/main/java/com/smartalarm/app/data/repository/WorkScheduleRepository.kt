package com.smartalarm.app.data.repository

import com.smartalarm.app.data.dao.WorkScheduleDao
import com.smartalarm.app.data.entities.WorkSchedule
import kotlinx.coroutines.flow.Flow

class WorkScheduleRepository(private val dao: WorkScheduleDao) {

    val allSchedulesFlow: Flow<List<WorkSchedule>> = dao.getAllSchedulesFlow()
    val activeScheduleFlow: Flow<WorkSchedule?> = dao.getActiveScheduleFlow()

    suspend fun getScheduleById(id: Long): WorkSchedule? = dao.getScheduleById(id)

    suspend fun getActiveSchedule(): WorkSchedule? = dao.getActiveSchedule()

    suspend fun insertSchedule(schedule: WorkSchedule): Long = dao.insertSchedule(schedule)

    suspend fun updateSchedule(schedule: WorkSchedule) = dao.updateSchedule(schedule)

    suspend fun deleteSchedule(schedule: WorkSchedule) = dao.deleteSchedule(schedule)

    suspend fun setActiveSchedule(id: Long) = dao.setActiveSchedule(id)
}
