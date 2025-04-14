package com.example.travelonna.api

data class PlaceCreateResponse(
    val success: Boolean,
    val message: String,
    val data: PlaceData
)

data class PlaceData(
    val id: Int,
    val name: String,
    val address: String,
    val order: Int,
    val isPublic: Boolean,
    val visitDate: String,
    val day: Int,
    val cost: Int,
    val memo: String,
    val lat: String,
    val lon: String,
    val googleId: String
) 