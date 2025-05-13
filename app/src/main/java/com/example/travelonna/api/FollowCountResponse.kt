package com.example.travelonna.api

/**
 * Response model for follower and following count API endpoints.
 * Used for both /api/v1/follows/count/followers/{profileId} and
 * /api/v1/follows/count/followings/{profileId} endpoints.
 */
data class FollowCountResponse(
    val success: Boolean,
    val message: String,
    val data: FollowCountData
)

/**
 * Data class containing the count information.
 */
data class FollowCountData(
    val count: Int
) 