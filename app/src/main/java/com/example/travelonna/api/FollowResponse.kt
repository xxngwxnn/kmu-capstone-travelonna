package com.example.travelonna.api

data class FollowResponse(
    val success: Boolean,
    val message: String,
    val data: FollowData?
)

data class FollowData(
    val isFollowing: Boolean,
    val followerId: Int,
    val followingId: Int
) 