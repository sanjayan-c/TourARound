package com.example.touraround

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import java.util.concurrent.TimeUnit

object DirectionsUtils {

    private const val TAG = "DirectionsUtils"
    private const val API_KEY = "AIzaSyBZShPiUN_fogA26tFRvKK79owBT-BuW8c"

    fun getDirections(context: Context, origin: LatLng, destination: LatLng): DirectionsResult? {
        val geoApiContext = GeoApiContext.Builder()
            .apiKey(API_KEY)
            .queryRateLimit(3) // Requests per second
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        try {
            return DirectionsApi.newRequest(geoApiContext)
                .mode(TravelMode.WALKING)
                .origin("${origin.latitude},${origin.longitude}")
                .destination("${destination.latitude},${destination.longitude}")
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting directions: ${e.message}")
        }
        return null
    }
}
