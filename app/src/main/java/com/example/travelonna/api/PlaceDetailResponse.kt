package com.example.travelonna.api

data class PlaceDetailResponse(
    val success: Boolean,
    val message: String,
    val data: PlaceDetail?
)

data class PlaceDetail(
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