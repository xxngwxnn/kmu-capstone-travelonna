package com.example.travelonna.api

import com.example.travelonna.model.LogDetailResponse
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
    
    @GET("api/v1/logs/{logId}/detail")
    fun getLogDetail(@Path("logId") logId: Int): Call<LogDetailApiResponse>
    
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
    
    // Search endpoint
    @GET("api/v1/search")
    fun search(@Query("keyword") keyword: String): Call<SearchResponse>
    
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
    
    // Multipart 요청을 위한 프로필 업데이트 API
    @Multipart
    @PUT("api/v1/profiles/{profileId}")
    fun updateUserProfileWithImage(
        @Path("profileId") profileId: Int,
        @Part("nickname") nickname: RequestBody,
        @Part("introduction") introduction: RequestBody,
        @Part profileImage: MultipartBody.Part?
    ): Call<ProfileResponse>
    
    // Follow count endpoints
    @GET("api/v1/follows/count/followers/{profileId}")
    fun getFollowersCount(@Path("profileId") profileId: Int): Call<FollowCountResponse>
    
    @GET("api/v1/follows/count/followings/{profileId}")
    fun getFollowingsCount(@Path("profileId") profileId: Int): Call<FollowCountResponse>
    
    // Follow list endpoints
    @GET("api/v1/follows/followers/{profileId}")
    fun getFollowersList(@Path("profileId") profileId: Int): Call<FollowListResponse>
    
    @GET("api/v1/follows/followings/{profileId}")
    fun getFollowingsList(@Path("profileId") profileId: Int): Call<FollowListResponse>
    
    // Travel log endpoints
    @GET("api/v1/places/{placeId}/detail")
    fun getPlaceDetail(@Path("placeId") placeId: Int): Call<PlaceDetailResponse>
    
    @POST("api/v1/logs")
    fun createTravelLog(@Body request: TravelLogCreateRequest): Call<TravelLogResponse>
    
    @GET("api/v1/logs/place/{placeId}")
    fun getTravelLogsByPlace(@Path("placeId") placeId: Int): Call<TravelLogListResponse>
    
    @PUT("api/v1/logs/{logId}")
    fun updateTravelLog(@Path("logId") logId: Int, @Body request: TravelLogUpdateRequest): Call<TravelLogResponse>
    
    @GET("api/v1/logs/user/{userId}")
    fun getTravelLogsByUser(@Path("userId") userId: Int): Call<TravelLogListResponse>
    
    // Plan update endpoint
    @PUT("api/v1/plans/{planId}")
    fun updatePlan(@Path("planId") planId: Int, @Body request: PlanUpdateRequest): Call<PlanResponse>
    
    // Recommendation endpoints
    @POST("api/v1/recommendations")
    fun getRecommendations(@Body request: RecommendationRequest): Call<RecommendationApiResponse>
    
    @GET("api/v1/recommendations/exists")
    fun checkRecommendationExists(@Query("userId") userId: Int, @Query("recType") recType: String): Call<RecommendationExistsResponse>
    
    @GET("api/v1/recommendations/count")
    fun getRecommendationCount(@Query("userId") userId: Int, @Query("recType") recType: String): Call<RecommendationCountResponse>
    
    // Like endpoints
    @POST("api/v1/logs/{logId}/like")
    fun toggleLogLike(@Path("logId") logId: Int): Call<LikeToggleResponse>
    
    // Follow endpoints
    @POST("api/v1/follows")
    fun followUser(@Body request: FollowRequest): Call<FollowResponse>
    
    @DELETE("api/v1/follows/{userId}")
    fun unfollowUser(@Path("userId") userId: Int): Call<UnfollowResponse>
    
    @GET("api/v1/follows/status/{userId}")
    fun getFollowStatus(@Path("userId") userId: Int): Call<FollowStatusResponse>
    
    // Comment endpoints
    @GET("api/v1/logs/{logId}/comments")
    fun getLogComments(@Path("logId") logId: Int): Call<CommentsResponse>
    
    @POST("api/v1/logs/{logId}/comments")
    fun createComment(@Path("logId") logId: Int, @Body request: CreateCommentRequest): Call<CommentData>
    
    @PUT("api/v1/comments/{commentId}")
    fun updateComment(@Path("commentId") commentId: Int, @Body request: CreateCommentRequest): Call<UpdateCommentResponse>
    
    @DELETE("api/v1/comments/{commentId}")
    fun deleteComment(@Path("commentId") commentId: Int): Call<DeleteCommentResponse>
    
    // User logs endpoints
    @GET("api/v1/logs/users/{userId}")
    fun getUserLogs(@Path("userId") userId: Int): Call<UserLogsResponse>
    
    @GET("api/v1/plans/{planId}/places/view")
    fun getPlanPlaces(@Path("planId") planId: Int): Call<PlanPlacesResponse>
}