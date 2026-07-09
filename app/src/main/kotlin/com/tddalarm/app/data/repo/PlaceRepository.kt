package com.tddalarm.app.data.repo

import com.tddalarm.app.data.Place
import com.tddalarm.app.data.dao.SavedLocationDao
import com.tddalarm.app.data.toDomain
import com.tddalarm.app.data.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaceRepository(private val dao: SavedLocationDao) : PlaceStore {

    override val places: Flow<List<Place>> =
        dao.all().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): Place? = dao.byId(id)?.toDomain()

    override suspend fun save(place: Place): Long {
        val rowId = dao.upsert(place.toEntity())
        return if (place.id != 0L) place.id else rowId
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
