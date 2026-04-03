package com.smartalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartalarm.app.data.entities.SavedLocation
import com.smartalarm.app.data.repository.SavedLocationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedLocationViewModel(
    application: Application,
    private val repository: SavedLocationRepository
) : AndroidViewModel(application) {

    val allLocations: StateFlow<List<SavedLocation>> = repository.allLocations
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addLocation(name: String, latitude: Double, longitude: Double, radiusMeters: Float = 500f) {
        viewModelScope.launch {
            repository.insert(SavedLocation(name = name, latitude = latitude, longitude = longitude, radiusMeters = radiusMeters))
        }
    }

    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.delete(location)
        }
    }
}

class SavedLocationViewModelFactory(
    private val application: Application,
    private val repository: SavedLocationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedLocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedLocationViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
