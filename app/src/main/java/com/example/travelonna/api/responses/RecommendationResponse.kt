package com.example.travelonna.api.responses

import com.google.gson.annotations.SerializedName

data class RecommendationResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: List<RecommendationItem>
)

data class RecommendationItem(
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
    val isLiked: Boolean
) 