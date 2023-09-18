package com.example.touraround

import android.content.Context
import android.location.Location
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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

    fun nearBylocations(
        context: Context,
        cameraView: CameraView,
        radius: Int,
        location: Location,
        callback: (List<com.example.touraround.Location>) -> Unit
    ){
        val nearbyLocations = ArrayList<com.example.touraround.Location>()

        val url = ("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                "${location.latitude},${location.longitude}" +
                "&radius=$radius&type=tourist_attraction&key=$API_KEY" +
                "&opennow=true")

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val results = response.optJSONArray("results")

                if (results != null) {
                    for (i in 0 until results.length()) {
                        val place = results.optJSONObject(i)
                        val name = place.optString("name")
                        val lat = place.getJSONObject("geometry").getJSONObject("location")
                            .optDouble("lat")
                        val lng = place.getJSONObject("geometry").getJSONObject("location")
                            .optDouble("lng")
                        // Check if the 'open_now' field exists and is true, and store it in a boolean variable
                        val isOpenNow = place.optBoolean("open_now", false)
                        val rating = place.optDouble("rating", 0.0)
                        val userRatingsTotal = place.optInt("user_ratings_total", 0)
                        val url = place.optString("url", "Not available")
                        val googlePlacePhotosBaseUrl = "https://maps.googleapis.com/maps/api/place/photo"
                        // Create a list to store complete image URLs
                        val imageUrls = mutableListOf<String>()

                        // Check if the 'photos' array is present in the JSON response
                        val photoArray = place.optJSONArray("photos")
                        if (photoArray != null) {
                            // Iterate through the 'photos' array
                            for (j in 0 until photoArray.length()) {
                                val photoObject = photoArray.optJSONObject(j)
                                val photoReference = photoObject.optString("photo_reference")

                                // Construct the complete image URL
                                val imageUrl = "$googlePlacePhotosBaseUrl?maxwidth=400&photo_reference=$photoReference&key=$API_KEY"

                                // Add the complete image URL to the list
                                imageUrls.add(imageUrl)

                                // Log the imageUrl for this location
                                Log.d(TAG, "Image URL for $name: $imageUrl")
                            }
                        }

                        // Calculate angle and distance using helper methods from CameraView
                        val angle = cameraView.angleFromCoordinate(
                            location.latitude,
                            location.longitude,
                            lat,
                            lng
                        )

                        val distance = cameraView.calculateDistance(
                            LatLng(location.latitude, location.longitude),
                            LatLng(lat, lng)
                        )
                        val locationData = Location(
                            lat,
                            lng,
                            name,
                            imageUrls,
                            isOpenNow,
                            rating,
                            userRatingsTotal,
                            url,
                            angle,
                            distance
                        )
                        nearbyLocations.add(locationData)
                    }
                    callback(nearbyLocations)
                    // Print the nearby locations
                    for (locationData in nearbyLocations) {
                        Log.d(TAG, "Latitude: ${locationData.latitude}")
                        Log.d(TAG, "Longitude: ${locationData.longitude}")
                        Log.d(TAG, "Name: ${locationData.name}")
                        Log.d(TAG, "Currently open: ${locationData.openNow}")
                        Log.d(TAG, "Average Rating: ${locationData.rating}")
                        Log.d(TAG, "Total Ratings: ${locationData.userRatingsTotal}")
                        Log.d(TAG, "Url link: ${locationData.url}")
                        Log.d(TAG, "Angle: ${locationData.angle}")
                        Log.d(TAG, "Distance: ${locationData.distance}")
                    }
                }
            },
            { error ->
                Log.e(TAG, "Error fetching nearby locations: ${error.message}")
            })
        // Add the request to the queue
        val queue = Volley.newRequestQueue(context)
        queue.add(request)
    }
}