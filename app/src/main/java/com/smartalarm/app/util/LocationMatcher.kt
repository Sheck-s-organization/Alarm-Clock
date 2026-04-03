package com.smartalarm.app.util

import com.smartalarm.app.data.entities.LocationTimeOverride
import com.smartalarm.app.data.entities.SavedLocation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Determines which (if any) saved location the device is currently inside.
 * Extracted as an interface so tests can provide a fake implementation without
 * needing the Android location framework.
 */
interface LocationMatcher {
    fun findMatch(
        deviceLat: Double,
        deviceLng: Double,
        locations: List<SavedLocation>
    ): SavedLocation?
}

/**
 * Production implementation — uses the Haversine formula to compute the
 * great-circle distance between two coordinates and compares it against each
 * saved location's [SavedLocation.radiusMeters].
 */
object HaversineLocationMatcher : LocationMatcher {

    override fun findMatch(
        deviceLat: Double,
        deviceLng: Double,
        locations: List<SavedLocation>
    ): SavedLocation? = locations.firstOrNull { location ->
        haversineDistance(deviceLat, deviceLng, location.latitude, location.longitude) <=
            location.radiusMeters
    }

    /**
     * Returns the distance in metres between two WGS-84 coordinates using the
     * Haversine formula. Pure function — no Android framework dependencies.
     */
    fun haversineDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }
}

/**
 * Given the device's matched [SavedLocation] (or null) and the alarm's list of
 * [overrides], returns the effective [Pair] of (hour, minute) to use.
 * Falls back to [defaultHour]/[defaultMinute] when no override matches.
 *
 * Pure function — no Android dependencies, fully unit-testable.
 */
fun resolveAlarmTime(
    matchedLocation: SavedLocation?,
    overrides: List<LocationTimeOverride>,
    defaultHour: Int,
    defaultMinute: Int
): Pair<Int, Int> {
    if (matchedLocation == null || overrides.isEmpty()) return defaultHour to defaultMinute
    val override = overrides.firstOrNull { it.savedLocationId == matchedLocation.id }
        ?: return defaultHour to defaultMinute
    return override.hour to override.minute
}
