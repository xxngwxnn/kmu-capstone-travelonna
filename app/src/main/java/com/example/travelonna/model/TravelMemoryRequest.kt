package com.example.travelonna.model

data class TravelMemoryRequest(
    val planId: Int,
    val comment: String,
    val isPublic: Boolean,
    val imageUrls: List<String>
) 