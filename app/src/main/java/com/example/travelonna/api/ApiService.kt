package com.example.travelonna.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.HeaderMap
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import java.util.HashMap

interface ApiService {
    @GET("api/v1/plans/{planId}/detail")
    fun getPlanDetail(@Path("planId") planId: Int): Call<PlanDetailResponse>
    
    @POST("api/v1/plans/{planId}/places")
    fun createPlace(@Path("planId") planId: Int, @Body request: PlaceCreateRequest): Call<PlaceCreateResponse>
    
    @PUT("api/v1/plans/{planId}/places/{placeId}")
    fun updatePlace(@Path("planId") planId: Int, @Path("placeId") placeId: Int, @Body request: PlaceCreateRequest): Call<BasicResponse>
    
    @POST("api/v1/plans")
    fun createPlan(@Body request: PlanCreateRequest): Call<PlanCreateResponse>
    
    @POST("api/v1/plans/transportation/search")
    fun searchTransportation(@Body request: TransportationRequest): Call<TransportationResponse>
    
    @POST("api/v1/groups")
    fun createGroupUrl(@Body request: GroupUrlRequest): Call<GroupUrlResponse>

    @GET("api/v1/plans")
    fun getPlans(@Header("Authorization") authorization: String): Call<PlanListResponse>
    
    @GET("api/v1/groups/my")
    fun getMyGroups(): Call<List<GroupInfoResponse>>
    
    @GET("api/v1/groups/plan/{planId}")
    fun getGroupInfo(@Path("planId") planId: Int): Call<GroupInfoResponse>
    
    @POST("api/v1/groups/join/{url}")
    fun joinGroup(@Path("url") urlCode: String): Call<Void>
    
    @GET("api/v1/stations/search")
    fun searchStations(@Query("keyword") keyword: String): Call<StationSearchResponse>
    
    @GET("api/v1/stations/region")
    fun searchStationsByRegion(@Query("region") region: String): Call<StationSearchResponse>
    
    @DELETE("api/v1/plans/{planId}/places/{placeId}")
    fun deletePlace(@Path("planId") planId: Int, @Path("placeId") placeId: Int): Call<BasicResponse>
    
    @DELETE("api/v1/plans/{planId}")
    fun deletePlan(@Path("planId") planId: Int): Call<BasicResponse>
    
    // User profile endpoints
    @GET("api/v1/profiles/user/{userId}")
    fun getUserProfile(@Path("userId") userId: Int): Call<ProfileResponse>
    
    @POST("api/v1/profiles")
    fun createUserProfile(@Body request: ProfileCreateRequest): Call<ProfileResponse>
    
    // Multipart 요청을 위한 프로필 생성 API
    @Multipart
    @POST("api/v1/profiles")
    fun createUserProfileWithImage(
        @Part("userId") userId: RequestBody,
        @Part("nickname") nickname: RequestBody,
        @Part("introduction") introduction: RequestBody,
        @Part profileImage: MultipartBody.Part?
    ): Call<ProfileResponse>
    
    // Follow count endpoints
    @GET("api/v1/follows/count/followers/{profileId}")
    fun getFollowersCount(@Path("profileId") profileId: Int): Call<FollowCountResponse>
    
    @GET("api/v1/follows/count/followings/{profileId}")
    fun getFollowingsCount(@Path("profileId") profileId: Int): Call<FollowCountResponse>
    
    // 여행 로그 생성 API
    @POST("api/v1/logs")
    fun createTravelLog(@Body requestBody: HashMap<String, Any>): Call<BasicResponse>
    
    // 여행 로그 조회 API (특정 계획의 기록들)
    @GET("api/v1/logs/plans/{planId}")
    fun getTravelLogsByPlan(@Path("planId") planId: Int): Call<TravelLogResponse>
    
    // 여행 로그 수정 API
    @PUT("api/v1/logs/{logId}")
    fun updateTravelLog(@Path("logId") logId: Int, @Body requestBody: HashMap<String, Any>): Call<BasicResponse>
} 