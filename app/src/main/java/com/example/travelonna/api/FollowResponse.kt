package com.example.travelonna.api

import com.google.gson.annotations.SerializedName

data class FollowResponse(
    val success: Boolean,
    val message: String,
    val data: FollowData?
)

data class FollowData(
    val id: Int,
    val fromUser: Int,
    val toUser: Int,
    val profileId: Int,
    @SerializedName("following")
    val isFollowing: Boolean
) 