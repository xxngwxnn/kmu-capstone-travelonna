package com.example.travelonna.api.responses

import com.google.gson.annotations.SerializedName

data class LogDetailResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: LogDetail
)

data class LogDetail(
    @SerializedName("logId")
    val logId: Int,
    
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("userName")
    val userName: String,
    
    @SerializedName("userProfileImage")
    val userProfileImage: String?,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("imageUrl")
    val imageUrl: String?,
    
    @SerializedName("hasImage")
    val hasImage: Boolean,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("likeCount")
    val likeCount: Int,
    
    @SerializedName("commentCount")
    val commentCount: Int,
    
    @SerializedName("isLiked")
    val isLiked: Boolean,
    
    @SerializedName("plan")
    val plan: TravelPlan,
    
    @SerializedName("placeNames")
    val placeNames: List<String>
)

data class TravelPlan(
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