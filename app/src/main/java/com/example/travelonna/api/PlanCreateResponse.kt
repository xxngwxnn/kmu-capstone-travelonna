package com.example.travelonna.api

data class PlanCreateResponse(
    val success: Boolean,
    val message: String,
    val data: PlanCreateData
)

data class PlanCreateData(
    val planId: Int,           // 일정 ID (서버에서는 planId로 반환)
    val title: String,         // 일정 제목
    val startDate: String,     // 시작 날짜
    val endDate: String,       // 종료 날짜
    val location: String,      // 위치
    val memo: String,          // 메모
    val isGroupPlan: Boolean,  // 그룹 일정 여부
    val userId: Int = 0,       // 사용자 ID (선택적)
    val transportInfo: String = "", // 교통 정보 (선택적)
    val groupId: Int? = null,  // 그룹 ID (선택적)
    val isPublic: Boolean = false, // 공개 여부 (선택적)
    val totalCost: Int = 0,    // 총 비용 (선택적)
    val createdAt: String = "", // 생성 시간 (선택적)
    val updatedAt: String = ""  // 업데이트 시간 (선택적)
) 