package com.example.travelonna.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {
    @GET("api/v1/plans/{planId}/detail")
    fun getPlanDetail(@Path("planId") planId: Int): Call<PlanDetailResponse>
    
    @POST("api/v1/plans/{planId}/places")
    fun createPlace(@Path("planId") planId: Int, @Body request: PlaceCreateRequest): Call<PlaceCreateResponse>
    
    @POST("api/v1/plans")
    fun createPlan(@Body request: PlanCreateRequest): Call<PlanCreateResponse>
    
    @POST("api/v1/plans/transportation/search")
    fun searchTransportation(@Body request: TransportationRequest): Call<TransportationResponse>
    
    @POST("api/v1/groups")
    fun createGroupUrl(@Body request: GroupUrlRequest): Call<GroupUrlResponse>

    @GET("api/v1/plans")
    fun getPlans(@Header("Authorization") authorization: String): Call<PlanListResponse>
    
    @GET("api/v1/groups/plan/{planId}")
    fun getGroupInfo(@Path("planId") planId: Int): Call<GroupInfoResponse>
    
    @GET("api/v1/stations/search")
    fun searchStations(@Query("keyword") keyword: String): Call<StationSearchResponse>
} 