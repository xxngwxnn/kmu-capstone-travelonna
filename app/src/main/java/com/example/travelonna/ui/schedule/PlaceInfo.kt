package com.example.travelonna.ui.schedule

data class PlaceInfo(
    val placeId: String,
    val name: String,
    val address: String,
    val rating: Float?,
    val latitude: Double,
    val longitude: Double,
    val websiteUri: String?,
    val phoneNumber: String?
) 