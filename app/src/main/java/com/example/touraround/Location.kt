package com.example.touraround

data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val photoReferences: List<String>,
    val openNow: Boolean,
    val rating: Double,
    val userRatingsTotal: Int,
    val url: String,
    val angle: Double,
    val distance: Double
)

data class User(
    val name: String,
    val email: String,
    val uid: String,
    val number:String
)

