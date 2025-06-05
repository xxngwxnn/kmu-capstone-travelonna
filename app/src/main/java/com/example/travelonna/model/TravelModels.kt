package com.example.travelonna.model

// 여행 장소 데이터 클래스
data class TravelPlace(
    val name: String,
    val address: String,
    val visitTime: String,
    val isLocked: Boolean = false
)

// 여행 로그 데이터 클래스
data class TravelLog(
    val title: String,
    val date: String,
    val type: String,
    val places: List<TravelPlace> = listOf(), // 장소 목록 추가
    val planId: Int = 0, // 계획 ID 추가
    val status: String = "완료" // 진행 상태 추가 (완료 또는 진행중)
) 