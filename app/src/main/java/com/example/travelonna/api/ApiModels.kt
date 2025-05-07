package com.example.travelonna.api

// 그룹 URL 생성 요청 모델
data class GroupUrlRequest(
    val isGroup: Boolean = true
)

// 그룹 URL 생성 응답 모델 - 서버에서 오는 응답 형식에 맞게 수정
data class GroupUrlResponse(
    val id: Int = 0,
    val url: String = "",
    val isGroup: Boolean = false,
    val createdDate: String = "",
    val hostId: Int = 0
)

// 기본 응답 모델 - 성공/실패 메시지만 포함
data class BasicResponse(
    val success: Boolean = false,
    val message: String = "",
    val data: Any? = null
)

// 참고: 일정 관련 모델은 PlanResponse.kt 파일에 정의되어 있습니다. 