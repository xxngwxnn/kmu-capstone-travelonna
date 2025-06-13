package com.example.travelonna.api

data class UnfollowResponse(
    val success: Boolean,
    val message: String,
    val data: UnfollowData?
)

data class UnfollowData(
    val isFollowing: Boolean,
    val followerId: Int,
    val followingId: Int
) 