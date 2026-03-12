package com.smartalarm.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named geographic location used for location-based alarm rules.
 *
 * The user saves locations such as "Home", "Office", "Gym".
 * An alarm with AlarmType.LOCATION can reference one or more of these
 * and specify whether it should fire only when INSIDE or OUTSIDE
 * the geofence radius.
 */
@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val latitude: Double,
    val longitude: Double,

    // Geofence radius in metres (default 200m)
    @ColumnInfo(name = "radius_meters")
    val radiusMeters: Float = 200f,

    // Optional street address for display purposes
    val address: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Inline rule embedded in an Alarm's locationRuleJson field.
 * Describes how a saved location affects alarm firing.
 */
data class AlarmLocationRule(
    val savedLocationId: Long,
    val locationName: String,           // Denormalised for display
    val triggerWhen: LocationTrigger = LocationTrigger.INSIDE,
    // Alarm time override when this location matches (null = use default alarm time)
    val overrideHour: Int? = null,
    val overrideMinute: Int? = null,
    // Optional: only apply on specific days of week (empty = all days)
    val activeDays: Set<Int> = emptySet()
)

enum class LocationTrigger {
    INSIDE,     // Alarm fires (or uses override time) when user is inside this geofence
    OUTSIDE     // Alarm fires (or uses override time) when user is outside this geofence
}
