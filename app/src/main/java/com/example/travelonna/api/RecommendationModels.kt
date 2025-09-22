package com.example.travelonna.api

import com.google.gson.annotations.SerializedName

// 추천 요청 모델
data class RecommendationRequest(
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("rec_type")
    val recType: String,
    
    @SerializedName("rec_limit")
    val recLimit: Int = 20
)

// 추천 API 응답 래퍼 (현재 서버 형식에 맞게 수정)
data class RecommendationApiResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: RecommendationResponse?
)

// 추천 응답 모델
data class RecommendationResponse(
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("itemType")
    val itemType: String,
    
    @SerializedName("recommendations")
    val recommendations: List<RecommendationItem>,
    
    @SerializedName("pageInfo")
    val pageInfo: PageInfo
)

// 추천 개수 조회 응답 모델
data class RecommendationCountResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: Int
)

// 추천 데이터 존재 여부 확인 응답 모델
data class RecommendationExistsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: Boolean
)

// 여행기록 상세 조회 응답 모델
data class LogDetailApiResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: LogDetailData?
)

// 여행기록 상세 데이터 모델
data class LogDetailData(
    @SerializedName("logId")
    val logId: Int,
    
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("userName")
    val userName: String,
    
    @SerializedName("userProfileImage")
    val userProfileImage: String?,
    
    @SerializedName("comment")
    val comment: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("isPublic")
    val isPublic: Boolean,
    
    @SerializedName("imageUrls")
    val imageUrls: List<String>,
    
    @SerializedName("likeCount")
    val likeCount: Int,
    
    @SerializedName("commentCount")
    val commentCount: Int,
    
    @SerializedName("isLiked")
    val isLiked: Boolean,
    
    @SerializedName("plan")
    val plan: LogPlanData,
    
    @SerializedName("placeId")
    val placeId: Int,
    
    @SerializedName("placeName")
    val placeName: String,
    
    @SerializedName("placeIds")
    val placeIds: List<Int>?,
    
    @SerializedName("placeNames")
    val placeNames: List<String>
)

// 로그 상세에서 사용되는 여행 계획 데이터 모델
data class LogPlanData(
    @SerializedName("planId")
    val planId: Int,
    
    @SerializedName("startDate")
    val startDate: String,
    
    @SerializedName("endDate")
    val endDate: String,
    
    @SerializedName("transportInfo")
    val transportInfo: String,
    
    @SerializedName("location")
    val location: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("isPublic")
    val isPublic: Boolean,
    
    @SerializedName("totalCost")
    val totalCost: Int
)

// 추천 아이템 모델
data class RecommendationItem(
    @SerializedName("itemId")
    val itemId: Int,
    
    @SerializedName("score")
    val score: Double,
    
    @SerializedName("logId")
    val logId: Int,
    
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("planId")
    val planId: Int,
    
    @SerializedName("comment")
    val comment: String,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("isPublic")
    val isPublic: Boolean
)

// 댓글 목록 조회 응답 모델
data class CommentsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: List<CommentData>
)

// 댓글 데이터 모델
data class CommentData(
    @SerializedName("commentId")
    val commentId: Int,
    
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("userName")
    val userName: String?,
    
    @SerializedName("userProfileImage")
    val userProfileImage: String?,
    
    @SerializedName("comment")
    val comment: String?,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("parentId")
    val parentId: Int?,
    
    @SerializedName("replies")
    val replies: List<String>?
)

// 댓글 생성 요청 모델
data class CreateCommentRequest(
    val comment: String,
    val parentId: Int? = null // null이면 JSON에 포함되지 않음
)

// 댓글 생성 에러 응답 모델 (400, 404의 경우)
data class CreateCommentErrorResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: CommentData
)

// 댓글 수정 응답 모델
data class UpdateCommentResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: CommentData
)

// 댓글 삭제 응답 모델
data class DeleteCommentResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: Map<String, Any> = emptyMap() // 빈 객체
)

// 페이지네이션 정보 모델
data class PageInfo(
    @SerializedName("currentPage")
    val currentPage: Int,
    
    @SerializedName("pageSize")
    val pageSize: Int,
    
    @SerializedName("totalElements")
    val totalElements: Int,
    
    @SerializedName("totalPages")
    val totalPages: Int,
    
    @SerializedName("hasNext")
    val hasNext: Boolean,
    
    @SerializedName("hasPrevious")
    val hasPrevious: Boolean,
    
    @SerializedName("isFirst")
    val isFirst: Boolean,
    
    @SerializedName("isLast")
    val isLast: Boolean
) 