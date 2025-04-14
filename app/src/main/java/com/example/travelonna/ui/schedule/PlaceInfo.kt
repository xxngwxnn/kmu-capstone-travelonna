package com.example.travelonna.ui.schedule

import android.net.Uri

data class PlaceInfo(
    val placeId: String,
    val name: String,
    val address: String,
    val rating: Double?,
    val latitude: Double,
    val longitude: Double,
    val websiteUri: Uri?,
    val phoneNumber: String?
) 