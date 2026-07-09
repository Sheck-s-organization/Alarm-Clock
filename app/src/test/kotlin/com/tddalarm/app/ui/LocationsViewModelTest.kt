package com.tddalarm.app.ui

import com.tddalarm.app.FakeAddressResolver
import com.tddalarm.app.FakeLocationProvider
import com.tddalarm.app.FakePlaceStore
import com.tddalarm.app.R
import com.tddalarm.core.geo.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val home = GeoPoint(39.768403, -86.158068)

    private val store = FakePlaceStore()
    private val resolver = FakeAddressResolver(mapOf("1 Monument Cir, Indianapolis" to home))
    private val locations = FakeLocationProvider()

    private fun viewModel() = LocationsViewModel(store, resolver, locations)

    @Test
    fun `adding a place by address geocodes it and converts km to meters`() = runTest {
        val vm = viewModel()
        vm.addPlace(name = "Home", address = "1 Monument Cir, Indianapolis", radiusKm = 25.0)
        advanceUntilIdle()

        val place = store.places.first().single()
        assertEquals("Home", place.name)
        assertEquals(home, place.fence.center)
        assertEquals(25_000.0, place.fence.radiusMeters, 0.0)
        assertNull(vm.message.value)
    }

    @Test
    fun `unresolvable address reports an error and saves nothing`() = runTest {
        val vm = viewModel()
        vm.addPlace(name = "Home", address = "nowhere, atlantis", radiusKm = 25.0)
        advanceUntilIdle()

        assertTrue(store.places.first().isEmpty())
        assertEquals(R.string.address_not_found, vm.message.value)
    }

    @Test
    fun `blank name reports an error and saves nothing`() = runTest {
        val vm = viewModel()
        vm.addPlace(name = "   ", address = "1 Monument Cir, Indianapolis", radiusKm = 25.0)
        advanceUntilIdle()

        assertTrue(store.places.first().isEmpty())
        assertEquals(R.string.place_name_required, vm.message.value)
    }

    @Test
    fun `blank address falls back to the current device location`() = runTest {
        locations.location = home
        val vm = viewModel()
        vm.addPlace(name = "Here", address = "", radiusKm = 10.0)
        advanceUntilIdle()

        val place = store.places.first().single()
        assertEquals(home, place.fence.center)
        assertEquals(10_000.0, place.fence.radiusMeters, 0.0)
    }

    @Test
    fun `blank address with no device location reports an error`() = runTest {
        locations.location = null
        val vm = viewModel()
        vm.addPlace(name = "Here", address = "", radiusKm = 10.0)
        advanceUntilIdle()

        assertTrue(store.places.first().isEmpty())
        assertEquals(R.string.location_unavailable, vm.message.value)
    }

    @Test
    fun `missing or non-positive radius falls back to the default`() = runTest {
        val vm = viewModel()
        vm.addPlace(name = "Home", address = "1 Monument Cir, Indianapolis", radiusKm = null)
        advanceUntilIdle()

        assertEquals(
            LocationsViewModel.DEFAULT_RADIUS_KM * 1000.0,
            store.places.first().single().fence.radiusMeters,
            0.0,
        )
    }

    @Test
    fun `removing a place deletes it`() = runTest {
        val vm = viewModel()
        vm.addPlace(name = "Home", address = "1 Monument Cir, Indianapolis", radiusKm = 25.0)
        advanceUntilIdle()
        val id = store.places.first().single().id

        vm.remove(id)
        advanceUntilIdle()
        assertTrue(store.places.first().isEmpty())
    }

    @Test
    fun `clearing the message resets it to null`() = runTest {
        val vm = viewModel()
        vm.addPlace(name = "", address = "x", radiusKm = 1.0)
        advanceUntilIdle()
        vm.clearMessage()
        assertNull(vm.message.value)
    }
}
