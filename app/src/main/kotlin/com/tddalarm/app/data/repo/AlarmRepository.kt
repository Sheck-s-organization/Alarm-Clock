package com.tddalarm.app.data.repo

import com.tddalarm.app.data.dao.AlarmDao
import com.tddalarm.app.data.toDomain
import com.tddalarm.app.data.toEntity
import com.tddalarm.core.alarm.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(private val dao: AlarmDao) : AlarmStore {

    override val alarms: Flow<List<Alarm>> =
        dao.all().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): Alarm? = dao.byId(id)?.toDomain()

    override suspend fun save(alarm: Alarm): Long {
        val rowId = dao.upsert(alarm.toEntity())
        return if (alarm.id != 0L) alarm.id else rowId
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    override suspend fun enabledAlarms(): List<Alarm> = dao.enabledOnce().map { it.toDomain() }
}
