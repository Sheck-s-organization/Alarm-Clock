package com.smartalarm.app.data.entities

/**
 * Stores a per-location alarm time override.
 * When the device is inside the radius of [savedLocationId], the alarm fires at
 * [hour]:[minute] instead of the alarm's default time.
 */
data class LocationTimeOverride(
    val savedLocationId: Long,
    val hour: Int,
    val minute: Int
)
