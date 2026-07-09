package com.tddalarm.core.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on Earth in decimal degrees. */
data class GeoPoint(val latitude: Double, val longitude: Double)

private const val EARTH_RADIUS_METERS = 6_371_000.8

/** Great-circle distance between two points (haversine formula). */
fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val latA = Math.toRadians(a.latitude)
    val latB = Math.toRadians(b.latitude)

    val h = sin(dLat / 2) * sin(dLat / 2) + cos(latA) * cos(latB) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
}

/** A circular region: "within [radiusMeters] of [center]". */
data class GeoFence(val center: GeoPoint, val radiusMeters: Double) {
    init {
        require(radiusMeters >= 0) { "radiusMeters must be non-negative, was $radiusMeters" }
    }

    operator fun contains(point: GeoPoint): Boolean =
        distanceMeters(center, point) <= radiusMeters
}
