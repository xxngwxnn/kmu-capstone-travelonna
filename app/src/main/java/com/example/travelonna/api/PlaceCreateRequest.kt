package com.example.travelonna.api

data class PlaceCreateRequest(
    val place: String,        // 주소
    val isPublic: Boolean,    // 공개 여부
    val visitDate: String,    // 방문 날짜 (YYYY-MM-DD)
    val placeCost: Int,       // 비용
    val memo: String,         // 메모
    val lat: String,          // 위도
    val lon: String,          // 경도
    val name: String,         // 장소명
    val order: Int,           // 순서
    val googleId: String      // 구글 장소 ID
) 