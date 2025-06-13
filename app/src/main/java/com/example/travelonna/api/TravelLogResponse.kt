package com.example.travelonna.api

data class TravelLogResponse(
    val success: Boolean,
    val message: String,
    val data: List<TravelLogData>
)

data class TravelLogData(
    val logId: Int,
    val userId: Int,
    val userName: String,
    val userProfileImage: String,
    val comment: String,
    val createdAt: String,
    val isPublic: Boolean,
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val plan: TravelLogPlan,
    val placeNames: List<String>
)

data class TravelLogPlan(
    val planId: Int,
    val startDate: String,
    val endDate: String,
    val transportInfo: String,
    val location: String,
    val title: String,
    val isPublic: Boolean,
    val totalCost: Int
)

// 추가 모델들
data class TravelLogCreateRequest(
    val placeId: Int,
    val title: String,
    val content: String,
    val visitDate: String,
    val isPublic: Boolean = true
)

data class TravelLogUpdateRequest(
    val title: String,
    val content: String,
    val visitDate: String,
    val isPublic: Boolean = true
)

data class TravelLogListResponse(
    val success: Boolean,
    val message: String,
    val data: List<TravelLogData>?
)