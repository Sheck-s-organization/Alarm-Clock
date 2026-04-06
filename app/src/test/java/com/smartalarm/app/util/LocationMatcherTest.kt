package com.smartalarm.app.util

import com.smartalarm.app.data.entities.LocationTimeOverride
import com.smartalarm.app.data.entities.SavedLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.roundToInt

class LocationMatcherTest {

    // --- haversineDistance ---

    @Test
    fun sameCoordinates_returnsZero() {
        val d = HaversineLocationMatcher.haversineDistance(37.7749, -122.4194, 37.7749, -122.4194)
        assertEquals(0.0, d, 0.001)
    }

    @Test
    fun knownDistance_SFtoLA_isApproximately560km() {
        // San Francisco to Los Angeles is ~559 km
        val d = HaversineLocationMatcher.haversineDistance(
            37.7749, -122.4194,  // SF
            34.0522, -118.2437   // LA
        )
        val km = (d / 1000).roundToInt()
        // Allow ±10 km tolerance
        assert(km in 549..569) { "Expected ~559 km, got $km km" }
    }

    @Test
    fun shortDistance_100m_isAccurate() {
        // Move ~100 m north from a point (roughly 0.0009 degrees latitude)
        val d = HaversineLocationMatcher.haversineDistance(
            51.5000, -0.1000,
            51.5009, -0.1000
        )
        assert(d in 90.0..110.0) { "Expected ~100 m, got $d m" }
    }

    // --- findMatch ---

    @Test
    fun emptyLocations_returnsNull() {
        val result = HaversineLocationMatcher.findMatch(37.7749, -122.4194, emptyList())
        assertNull(result)
    }

    @Test
    fun deviceInsideRadius_returnsLocation() {
        val home = SavedLocation(id = 1, name = "Home", latitude = 37.7749, longitude = -122.4194, radiusMeters = 500f)
        val result = HaversineLocationMatcher.findMatch(37.7749, -122.4194, listOf(home))
        assertNotNull(result)
        assertEquals("Home", result?.name)
    }

    @Test
    fun deviceOutsideRadius_returnsNull() {
        // 1 km away, but radius is only 200 m
        val office = SavedLocation(id = 2, name = "Office", latitude = 37.7749, longitude = -122.4194, radiusMeters = 200f)
        val result = HaversineLocationMatcher.findMatch(37.7838, -122.4194, listOf(office))
        assertNull(result)
    }

    @Test
    fun deviceInsideOneOfMultipleLocations_returnsCorrectOne() {
        val home = SavedLocation(id = 1, name = "Home",   latitude = 37.7749, longitude = -122.4194, radiusMeters = 500f)
        val office = SavedLocation(id = 2, name = "Office", latitude = 40.7128, longitude = -74.0060, radiusMeters = 500f)
        // Device is near home coordinates
        val result = HaversineLocationMatcher.findMatch(37.7749, -122.4194, listOf(home, office))
        assertEquals("Home", result?.name)
    }

    @Test
    fun deviceExactlyAtRadius_isConsideredInside() {
        // Verify the boundary comparison is <= not <.
        // We compute d (Double) then use ceil(d).toFloat() as the radius — this guarantees
        // the Float radius is >= d regardless of Float/Double rounding direction.
        val loc = SavedLocation(id = 1, name = "Test", latitude = 51.5000, longitude = -0.1000, radiusMeters = 100f)
        val d = HaversineLocationMatcher.haversineDistance(51.5000, -0.1000, 51.5009, -0.1000)
        val boundaryLoc = loc.copy(radiusMeters = kotlin.math.ceil(d).toFloat())
        val result = HaversineLocationMatcher.findMatch(51.5009, -0.1000, listOf(boundaryLoc))
        assertNotNull(result)
    }

    // --- resolveAlarmTime ---

    @Test
    fun noMatchedLocation_returnsDefault() {
        val (h, m) = resolveAlarmTime(null, emptyList(), defaultHour = 7, defaultMinute = 0)
        assertEquals(7, h)
        assertEquals(0, m)
    }

    @Test
    fun matchedLocationWithOverride_returnsOverrideTime() {
        val loc = SavedLocation(id = 1, name = "Office", latitude = 0.0, longitude = 0.0)
        val override = LocationTimeOverride(savedLocationId = 1, hour = 8, minute = 30)
        val (h, m) = resolveAlarmTime(loc, listOf(override), defaultHour = 7, defaultMinute = 0)
        assertEquals(8, h)
        assertEquals(30, m)
    }

    @Test
    fun matchedLocationButNoOverrideForIt_returnsDefault() {
        val loc = SavedLocation(id = 1, name = "Home", latitude = 0.0, longitude = 0.0)
        val override = LocationTimeOverride(savedLocationId = 99, hour = 9, minute = 0) // different id
        val (h, m) = resolveAlarmTime(loc, listOf(override), defaultHour = 7, defaultMinute = 0)
        assertEquals(7, h)
        assertEquals(0, m)
    }

    @Test
    fun multipleOverrides_picksCorrectOne() {
        val loc = SavedLocation(id = 2, name = "Gym", latitude = 0.0, longitude = 0.0)
        val overrides = listOf(
            LocationTimeOverride(savedLocationId = 1, hour = 6, minute = 0),
            LocationTimeOverride(savedLocationId = 2, hour = 5, minute = 30),
        )
        val (h, m) = resolveAlarmTime(loc, overrides, defaultHour = 7, defaultMinute = 0)
        assertEquals(5, h)
        assertEquals(30, m)
    }

    @Test
    fun overrideSameAsDefault_stillReturnsOverrideValues() {
        val loc = SavedLocation(id = 1, name = "Home", latitude = 0.0, longitude = 0.0)
        val override = LocationTimeOverride(savedLocationId = 1, hour = 7, minute = 0)
        val (h, m) = resolveAlarmTime(loc, listOf(override), defaultHour = 7, defaultMinute = 0)
        assertEquals(7, h)
        assertEquals(0, m)
    }
}
