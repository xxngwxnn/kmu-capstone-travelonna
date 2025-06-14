package com.example.travelonna.api

/**
 * Response model for followers and followings list API endpoints.
 * Used for both /api/v1/follows/followers/{profileId} and
 * /api/v1/follows/followings/{profileId} endpoints.
 */
data class FollowListResponse(
    val success: Boolean,
    val message: String,
    val data: List<FollowUser>
)

/**
 * Data class representing a user in the follow list.
 * Based on the API response structure.
 */
data class FollowUser(
    val id: Int,
    val fromUser: Int,
    val toUser: Int,
    val profileId: Int,
    val following: Boolean
) 