package com.tddalarm.app.data.repo

import com.tddalarm.app.data.dao.AlarmDao
import com.tddalarm.app.data.dao.SavedLocationDao
import com.tddalarm.app.data.entity.SavedLocationEntity
import com.tddalarm.app.data.toDomain
import com.tddalarm.app.data.toEntity
import com.tddalarm.core.alarm.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AlarmRepository(
    private val dao: AlarmDao,
    private val locationDao: SavedLocationDao,
) : AlarmStore {

    override val alarms: Flow<List<Alarm>> =
        combine(dao.all(), locationDao.all()) { entities, places ->
            val placesById = places.associateBy(SavedLocationEntity::id)
            entities.map { it.toDomain(placesById) }
        }

    override suspend fun getById(id: Long): Alarm? =
        dao.byId(id)?.toDomain(placesByIdOnce())

    override suspend fun save(alarm: Alarm): Long {
        val rowId = dao.upsert(alarm.toEntity())
        return if (alarm.id != 0L) alarm.id else rowId
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    override suspend fun enabledAlarms(): List<Alarm> {
        val placesById = placesByIdOnce()
        return dao.enabledOnce().map { it.toDomain(placesById) }
    }

    private suspend fun placesByIdOnce(): Map<Long, SavedLocationEntity> =
        locationDao.allOnce().associateBy(SavedLocationEntity::id)
}
