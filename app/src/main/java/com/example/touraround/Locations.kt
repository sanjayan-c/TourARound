package com.example.touraround

data class Locations(
    val latitude: Double,
    val longitude: Double,
    val photos: List<String>, // List of photo URLs
    val name: String,
    val description: String
)