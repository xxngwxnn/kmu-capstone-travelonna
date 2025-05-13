package com.example.travelonna.api

import retrofit2.Call
import retrofit2.http.GET

interface PlanApiService {
    @GET("/api/v1/plans")
    fun getPlans(): Call<PlanResponse>
}

data class PlanResponse(
    val success: Boolean,
    val message: String,
    val data: List<Plan>
)

data class Plan(
    val planId: Int,
    val userId: Int,
    val title: String,
    val startDate: String,
    val endDate: String,
    val location: String,
    val transportInfo: String,
    val groupId: Int?,
    val isPublic: Boolean,
    val totalCost: Int,
    val memo: String,
    val createdAt: String,
    val updatedAt: String
) 