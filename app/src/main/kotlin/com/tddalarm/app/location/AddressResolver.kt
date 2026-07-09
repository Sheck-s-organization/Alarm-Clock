package com.tddalarm.app.location

import android.content.Context
import android.location.Geocoder
import com.tddalarm.core.geo.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AddressResolver {
    /** Geocodes a free-form address to a point, or null when it can't be resolved. */
    suspend fun resolve(query: String): GeoPoint?
}

class GeocoderAddressResolver(private val context: Context) : AddressResolver {

    override suspend fun resolve(query: String): GeoPoint? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent) return@withContext null
        try {
            @Suppress("DEPRECATION")
            Geocoder(context).getFromLocationName(query, 1)
                ?.firstOrNull()
                ?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }
}
