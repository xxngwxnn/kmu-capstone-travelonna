package com.example.travelonna.api

data class LogDetailData(
    val logId: Int = 0,
    val userId: Int = 0,
    val userName: String,
    val userProfileImage: String? = null,
    val planId: Int = 0,
    val comment: String = "",
    val createdAt: String = "",
    val isLiked: Boolean,
    val likeCount: Int,
    val commentCount: Int,
    val imageUrls: List<String>,
    val imageUrl: String? = null,
    val hasImage: Boolean = false
) 