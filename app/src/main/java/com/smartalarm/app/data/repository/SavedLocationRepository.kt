package com.smartalarm.app.data.repository

import com.smartalarm.app.data.dao.SavedLocationDao
import com.smartalarm.app.data.entities.SavedLocation
import kotlinx.coroutines.flow.Flow

class SavedLocationRepository(private val dao: SavedLocationDao) {

    val allLocations: Flow<List<SavedLocation>> get() = dao.getAll()

    suspend fun insert(location: SavedLocation): Long = dao.insert(location)

    suspend fun delete(location: SavedLocation) = dao.delete(location)

    suspend fun getAllNow(): List<SavedLocation> = dao.getAllNow()
}
