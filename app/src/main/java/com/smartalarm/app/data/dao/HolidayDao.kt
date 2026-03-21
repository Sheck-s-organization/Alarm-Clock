package com.smartalarm.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartalarm.app.data.entities.Holiday
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holiday: Holiday): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<Holiday>)

    @Delete
    suspend fun delete(holiday: Holiday)

    @Query("SELECT * FROM holidays ORDER BY month ASC, day ASC")
    fun getAll(): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays ORDER BY month ASC, day ASC")
    suspend fun getAllNow(): List<Holiday>
}
