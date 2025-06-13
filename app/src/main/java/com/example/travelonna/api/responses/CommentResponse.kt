package com.example.travelonna.api.responses

import com.google.gson.annotations.SerializedName

data class CommentResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: List<Comment>
)

data class Comment(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("user")
    val user: UserProfile
)

data class UserProfile(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("nickname")
    val nickname: String,
    
    @SerializedName("introduction")
    val introduction: String?,
    
    @SerializedName("profileImageUrl")
    val profileImageUrl: String?,
    
    @SerializedName("isFollowing")
    val isFollowing: Boolean = false
) 