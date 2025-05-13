package com.example.travelonna.api

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("profileId") val profileId: Int,
    @SerializedName("userId") val userId: Int,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String?,
    @SerializedName("introduction") val introduction: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("errorMessage") val errorMessage: String?
)

data class ProfileData(
    val id: Int,
    val nickname: String,
    val profileImage: String?,
    val introduction: String?,
    val userId: Int
)

data class ProfileCreateRequest(
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("nickname")
    val nickname: String,
    
    @SerializedName("profileImageUrl")
    val profileImageUrl: String? = null,
    
    @SerializedName("introduction")
    val introduction: String? = null
) 