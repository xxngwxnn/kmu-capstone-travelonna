package com.example.travelonna.model

data class TravelMemoryResponse(
    val id: Int,
    val planId: Int,
    val comment: String,
    val isPublic: Boolean,
    val imageUrls: List<String>,
    val createdAt: String
) 