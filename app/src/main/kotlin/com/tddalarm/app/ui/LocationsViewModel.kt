package com.tddalarm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tddalarm.app.R
import com.tddalarm.app.data.Place
import com.tddalarm.app.data.repo.PlaceStore
import com.tddalarm.app.location.AddressResolver
import com.tddalarm.app.location.LocationProvider
import com.tddalarm.core.geo.GeoFence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationsViewModel(
    private val store: PlaceStore,
    private val resolver: AddressResolver,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    val items: StateFlow<List<Place>> =
        store.places.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<Int?>(null)

    /** String resource id of a user-facing error, or null. */
    val message: StateFlow<Int?> = _message

    /**
     * Saves a place named [name]. A non-blank [address] is geocoded; a blank one
     * means "right here" and uses the device's current location instead.
     */
    fun addPlace(name: String, address: String, radiusKm: Double?) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _message.value = R.string.place_name_required
            return
        }
        viewModelScope.launch {
            val trimmedAddress = address.trim()
            val point = if (trimmedAddress.isEmpty()) {
                locationProvider.lastKnown()
                    ?: run { _message.value = R.string.location_unavailable; return@launch }
            } else {
                resolver.resolve(trimmedAddress)
                    ?: run { _message.value = R.string.address_not_found; return@launch }
            }
            val radius = radiusKm?.takeIf { it > 0 } ?: DEFAULT_RADIUS_KM
            store.save(Place(id = 0, name = trimmedName, fence = GeoFence(point, radius * 1000.0)))
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch { store.delete(id) }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        const val DEFAULT_RADIUS_KM = 25.0
    }
}
