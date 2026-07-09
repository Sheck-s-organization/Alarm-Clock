package com.tddalarm.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tddalarm.app.data.entity.SavedLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY name")
    fun all(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations")
    suspend fun allOnce(): List<SavedLocationEntity>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun byId(id: Long): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedLocationEntity): Long

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun delete(id: Long)
}
