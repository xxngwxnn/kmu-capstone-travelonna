package com.example.travelonna.api

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    val success: Boolean,
    val message: String,
    val data: ProfileData?
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