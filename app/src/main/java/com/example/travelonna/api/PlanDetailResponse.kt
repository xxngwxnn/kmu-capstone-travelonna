package com.example.travelonna.api

import com.google.gson.annotations.SerializedName
import java.util.Date

data class PlanDetailResponse(
    val success: Boolean,
    val message: String,
    val data: PlanDetail
)

data class PlanDetail(
    val planId: Int,
    val userId: Int,
    val title: String,
    val location: String,
    val startDate: String,
    val endDate: String,
    val transportInfo: String,
    val isPublic: Boolean,
    val totalCost: Int,
    val memo: String,
    val createdAt: String,
    val updatedAt: String,
    val places: List<PlaceDetail>
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