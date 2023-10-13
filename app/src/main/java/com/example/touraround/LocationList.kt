package com.example.touraround

import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class LocationList :AppCompatActivity(){
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            setContentView(R.layout.location_list)
            // Retrieve the location from the intent
            val currentLocation = intent.getParcelableExtra<Location>("CurrentLocation")
            if (currentLocation != null) {
                // Log the location data
                Log.d("Translator", "Latitude: ${currentLocation.latitude}, Longitude: ${currentLocation.longitude}")
            }
    }
}