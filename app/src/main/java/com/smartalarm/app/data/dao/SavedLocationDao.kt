package com.smartalarm.app.data.dao

import androidx.room.*
import com.smartalarm.app.data.entities.SavedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY name ASC")
    fun getAllLocationsFlow(): Flow<List<SavedLocation>>

    @Query("SELECT * FROM saved_locations ORDER BY name ASC")
    suspend fun getAllLocations(): List<SavedLocation>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getLocationById(id: Long): SavedLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocation): Long

    @Update
    suspend fun updateLocation(location: SavedLocation)

    @Delete
    suspend fun deleteLocation(location: SavedLocation)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Long)
}
