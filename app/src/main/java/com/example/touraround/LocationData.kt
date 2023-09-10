package com.example.touraround

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationData {
    companion object {
        val placesList: List<Locations> = listOf(
            Locations(
                latitude = 6.916365825999852,
                longitude =  79.97241351967669,
                photos = listOf(
                    "https://archives1.dailynews.lk/sites/default/files/news/2022/03/29/Untitled-31.jpg",
                    "https://www.fat.lk/datauploads/3892_si.jpg"
                ),
                name = "Pizza Hut\n",
                description = "Taste delight"
            ),
            Locations(
                latitude = 6.912095622652169,
                longitude = 79.97231683923837,
                photos = listOf(
                    "https://lh5.googleusercontent.com/p/AF1QipPhaaX0bX__3Sy9oWv9jI4LRA0Rt6uf5M3G8dxS=w408-h306-k-no",
                    "https://lh5.googleusercontent.com/p/AF1QipNC-0SHsaBXeLnym4OMdlwuGOHlzp-pkaAu_dY2=w449-h336-p-k-no"
                ),
                name = "SPAR super market \n",
                description = "The nonstop restaurant is a second-generation Extension of Pathum Bakers ... Nonstop is a one-stop solution for delicious food and the most"
            )
        )
        init {
            // Log the contents of placesList
            for ((index, location) in placesList.withIndex()) {
                Log.d("LocationData", "Location $index: $location")
            }
        }
    }
}