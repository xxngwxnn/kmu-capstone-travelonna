package com.example.travelonna.api

data class FollowStatusResponse(
    val success: Boolean,
    val message: String,
    val data: FollowStatusData?
)

data class FollowStatusData(
    val isFollowing: Boolean,
    val followerId: Int,
    val followingId: Int
) 