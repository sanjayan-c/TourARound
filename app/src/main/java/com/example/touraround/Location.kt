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
    val name: String? = null,
    val email: String? = null,
    val uid: String? = null,
    val emergencyContactName:String? = null,
    val emergencyContactNumber:String? = null,
    val details: String? = null
)
data class Comment
    (val commentId: String? = null, val uname: String? = null, val text: String? = null, val destination: String? = null,val uid: String? = null)


