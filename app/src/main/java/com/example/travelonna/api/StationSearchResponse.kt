package com.example.travelonna.api

data class StationSearchResponse(
    val success: Boolean,
    val message: String,
    val data: List<Station>
)

data class Station(
    val id: String,
    val name: String,
    val type: String,    // 역 유형 (기차역, 버스터미널, 공항 등)
    val location: String // 위치 (예: 서울시 강남구)
) 