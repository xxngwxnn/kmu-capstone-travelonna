package com.example.travelonna.api

data class PlanCreateRequest(
    val title: String,          // 일정 제목
    val startDate: String,      // 시작 날짜 (YYYY-MM-DD)
    val endDate: String,        // 종료 날짜 (YYYY-MM-DD)
    val location: String,       // 위치
    val memo: String,           // 메모
    val isGroupPlan: Boolean,   // 그룹 일정 여부
    val transportInfo: String = "car", // 교통 정보 (기본값: 차량)
    val groupId: Int? = null    // 그룹 ID (null이면 일반 일정)
) 