package com.tddalarm.core.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoFenceTest {

    // Downtown Indianapolis (Monument Circle) and Indianapolis International Airport
    // are roughly 11.5 km apart — a stable real-world reference pair.
    private val monumentCircle = GeoPoint(39.768403, -86.158068)
    private val indyAirport = GeoPoint(39.717331, -86.294383)

    @Test
    fun `distance to self is zero`() {
        assertEquals(0.0, distanceMeters(monumentCircle, monumentCircle), 0.001)
    }

    @Test
    fun `distance is symmetric`() {
        assertEquals(
            distanceMeters(monumentCircle, indyAirport),
            distanceMeters(indyAirport, monumentCircle),
            0.001,
        )
    }

    @Test
    fun `distance between known landmarks is about 13 km`() {
        val d = distanceMeters(monumentCircle, indyAirport)
        assertTrue("expected 11–15 km, got $d m", d in 11_000.0..15_000.0)
    }

    @Test
    fun `one degree of latitude is about 111 km`() {
        val a = GeoPoint(39.0, -86.0)
        val b = GeoPoint(40.0, -86.0)
        val d = distanceMeters(a, b)
        assertTrue("expected ~111 km, got $d m", d in 110_000.0..112_500.0)
    }

    @Test
    fun `fence contains its own center and nearby points`() {
        val fence = GeoFence(center = monumentCircle, radiusMeters = 30_000.0)
        assertTrue(monumentCircle in fence)
        assertTrue(indyAirport in fence) // ~13 km away, inside a 30 km radius
    }

    @Test
    fun `fence excludes points beyond the radius`() {
        val fence = GeoFence(center = monumentCircle, radiusMeters = 5_000.0)
        assertFalse(indyAirport in fence) // ~13 km away, outside a 5 km radius
    }

    @Test
    fun `point exactly on the radius boundary is inside`() {
        val fence = GeoFence(center = monumentCircle, radiusMeters = distanceMeters(monumentCircle, indyAirport))
        assertTrue(indyAirport in fence)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative radius is rejected`() {
        GeoFence(monumentCircle, -1.0)
    }
}
