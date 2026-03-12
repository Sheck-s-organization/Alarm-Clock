package com.smartalarm.app.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.smartalarm.app.data.dao.SavedLocationDao
import com.smartalarm.app.data.entities.SavedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class LocationRepository(
    private val dao: SavedLocationDao,
    private val context: Context
) {

    val allLocationsFlow: Flow<List<SavedLocation>> = dao.getAllLocationsFlow()

    suspend fun getAllLocations(): List<SavedLocation> = dao.getAllLocations()

    suspend fun getLocationById(id: Long): SavedLocation? = dao.getLocationById(id)

    suspend fun insertLocation(location: SavedLocation): Long = dao.insertLocation(location)

    suspend fun updateLocation(location: SavedLocation) = dao.updateLocation(location)

    suspend fun deleteLocation(location: SavedLocation) = dao.deleteLocation(location)

    /**
     * Returns the current device location using a one-shot high-accuracy request.
     * Requires ACCESS_FINE_LOCATION permission to be already granted.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        return try {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Finds which saved location the user is currently inside (if any).
     * Returns the closest matching saved location within its geofence radius.
     */
    suspend fun getCurrentSavedLocation(): SavedLocation? {
        val current = getCurrentLocation() ?: return null
        val all = getAllLocations()

        return all
            .filter { saved ->
                val result = FloatArray(1)
                Location.distanceBetween(
                    current.latitude, current.longitude,
                    saved.latitude, saved.longitude,
                    result
                )
                result[0] <= saved.radiusMeters
            }
            .minByOrNull { saved ->
                val result = FloatArray(1)
                Location.distanceBetween(
                    current.latitude, current.longitude,
                    saved.latitude, saved.longitude,
                    result
                )
                result[0]
            }
    }

    /**
     * Checks whether the user is inside a specific saved location's geofence.
     */
    suspend fun isInsideLocation(savedLocationId: Long): Boolean {
        val saved = getLocationById(savedLocationId) ?: return false
        val current = getCurrentLocation() ?: return false
        val result = FloatArray(1)
        Location.distanceBetween(
            current.latitude, current.longitude,
            saved.latitude, saved.longitude,
            result
        )
        return result[0] <= saved.radiusMeters
    }
}
