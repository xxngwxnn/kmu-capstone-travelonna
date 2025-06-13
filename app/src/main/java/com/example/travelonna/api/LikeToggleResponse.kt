package com.example.travelonna.api

data class LikeToggleResponse(
    val success: Boolean,
    val message: String,
    val data: LikeToggleData?
)

data class LikeToggleData(
    val isLiked: Boolean,
    val likeCount: Int
) 