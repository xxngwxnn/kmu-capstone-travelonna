package com.example.travelonna.api

import com.google.gson.annotations.SerializedName

// 사용자 로그 목록 응답
data class UserLogsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: List<UserLogItem>
)

// 사용자 로그 아이템
data class UserLogItem(
    @SerializedName("logId")
    val logId: Int,
    
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("userName")
    val userName: String,
    
    @SerializedName("userProfileImage")
    val userProfileImage: String?,
    
    @SerializedName("comment")
    val comment: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("isPublic")
    val isPublic: Boolean,
    
    @SerializedName("imageUrls")
    val imageUrls: List<String>,
    
    @SerializedName("likeCount")
    val likeCount: Int,
    
    @SerializedName("commentCount")
    val commentCount: Int,
    
    @SerializedName("isLiked")
    val isLiked: Boolean,
    
    @SerializedName("plan")
    val plan: LogPlan,
    
    @SerializedName("placeNames")
    val placeNames: List<String>
)

// 로그 내 계획 정보
data class LogPlan(
    @SerializedName("planId")
    val planId: Int,
    
    @SerializedName("startDate")
    val startDate: String,
    
    @SerializedName("endDate")
    val endDate: String,
    
    @SerializedName("transportInfo")
    val transportInfo: String,
    
    @SerializedName("location")
    val location: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("isPublic")
    val isPublic: Boolean,
    
    @SerializedName("totalCost")
    val totalCost: Int
)

// 여행 장소 목록 응답
data class PlanPlacesResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: List<PlanPlace>
)

// 여행 장소 정보
data class PlanPlace(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("order")
    val order: Int,
    
    @SerializedName("isPublic")
    val isPublic: Boolean,
    
    @SerializedName("visitDate")
    val visitDate: String,
    
    @SerializedName("day")
    val day: Int,
    
    @SerializedName("cost")
    val cost: Int,
    
    @SerializedName("memo")
    val memo: String?,
    
    @SerializedName("lat")
    val lat: String,
    
    @SerializedName("lon")
    val lon: String,
    
    @SerializedName("googleId")
    val googleId: String?
) 