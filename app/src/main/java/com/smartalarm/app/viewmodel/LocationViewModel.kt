package com.smartalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.SavedLocation
import com.smartalarm.app.util.Constants
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SmartAlarmApplication
    private val repo = app.locationRepository

    val allLocations: LiveData<List<SavedLocation>> = repo.allLocationsFlow.asLiveData()

    private val _result = MutableLiveData<OperationResult>()
    val result: LiveData<OperationResult> = _result

    private val _currentLocation = MutableLiveData<android.location.Location?>()
    val currentLocation: LiveData<android.location.Location?> = _currentLocation

    fun refreshCurrentLocation() = viewModelScope.launch {
        _currentLocation.value = repo.getCurrentLocation()
    }

    fun saveCurrentAsLocation(name: String, radiusMeters: Float = Constants.DEFAULT_GEOFENCE_RADIUS_METERS) =
        viewModelScope.launch {
            val loc = repo.getCurrentLocation()
            if (loc == null) {
                _result.value = OperationResult.Error("Could not get current location")
                return@launch
            }
            val saved = SavedLocation(
                name = name,
                latitude = loc.latitude,
                longitude = loc.longitude,
                radiusMeters = radiusMeters
            )
            repo.insertLocation(saved)
            _result.value = OperationResult.Success("Location '$name' saved")
        }

    fun saveManualLocation(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = Constants.DEFAULT_GEOFENCE_RADIUS_METERS,
        address: String? = null
    ) = viewModelScope.launch {
        val saved = SavedLocation(
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            address = address
        )
        repo.insertLocation(saved)
        _result.value = OperationResult.Success("Location '$name' saved")
    }

    fun updateLocation(location: SavedLocation) = viewModelScope.launch {
        repo.updateLocation(location)
        _result.value = OperationResult.Success("Location updated")
    }

    fun deleteLocation(location: SavedLocation) = viewModelScope.launch {
        repo.deleteLocation(location)
        _result.value = OperationResult.Success("Location deleted")
    }
}
