package com.example.travelonna.api

import com.google.gson.annotations.SerializedName

data class TravelLogResponse(
    val success: Boolean,
    val message: String,
    val data: List<TravelLogData>
)

// 여행 기록 생성 응답용 (단일 객체)
data class TravelLogCreateResponse(
    val success: Boolean,
    val message: String,
    val data: TravelLogData
)

data class TravelLogData(
    val logId: Int,
    val userId: Int,
    val userName: String,
    val userProfileImage: String?,
    val comment: String,
    val createdAt: String,
    val isPublic: Boolean,
    val imageUrls: List<String>?,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val plan: TravelLogPlan,
    val placeNames: List<String>?
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
    @SerializedName("planId")
    val planId: Int,
    @SerializedName("placeId")
    val placeId: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("comment")
    val comment: String,
    @SerializedName("visitDate")
    val visitDate: String,
    @SerializedName("isPublic")
    val isPublic: Boolean = true
)

data class TravelLogUpdateRequest(
    @SerializedName("planId")
    val planId: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("comment")
    val comment: String,
    @SerializedName("visitDate")
    val visitDate: String,
    @SerializedName("isPublic")
    val isPublic: Boolean = true
)

data class TravelLogListResponse(
    val success: Boolean,
    val message: String,
    val data: List<TravelLogData>?
)