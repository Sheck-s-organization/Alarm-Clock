package com.smartalarm.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Radius in metres; device must be within this distance to match. */
    val radiusMeters: Float = 500f
)
