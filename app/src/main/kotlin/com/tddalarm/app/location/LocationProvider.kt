package com.tddalarm.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.tddalarm.core.geo.GeoPoint
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

interface LocationProvider {
    /** Best known device position, or null when unavailable (no permission, no fix, error). */
    suspend fun lastKnown(): GeoPoint?
}

class FusedLocationProvider(private val context: Context) : LocationProvider {

    override suspend fun lastKnown(): GeoPoint? {
        if (!hasPermission()) return null
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                val location = LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation
                    .await()
                location?.let { GeoPoint(it.latitude, it.longitude) }
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val LOCATION_TIMEOUT_MS = 5_000L
    }
}
