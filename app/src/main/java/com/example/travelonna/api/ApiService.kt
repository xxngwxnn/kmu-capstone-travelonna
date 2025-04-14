package com.example.travelonna.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body

interface ApiService {
    @GET("api/v1/plans/{planId}/detail")
    fun getPlanDetail(@Path("planId") planId: Int): Call<PlanDetailResponse>
    
    @POST("api/v1/plans/{planId}/places")
    fun createPlace(@Path("planId") planId: Int, @Body request: PlaceCreateRequest): Call<PlaceCreateResponse>
    
    @POST("api/v1/plans")
    fun createPlan(@Body request: PlanCreateRequest): Call<PlanCreateResponse>
} 