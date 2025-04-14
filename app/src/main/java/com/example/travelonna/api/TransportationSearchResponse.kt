package com.example.travelonna.api

data class TransportationSearchResponse(
    val success: Boolean,
    val message: String,
    val data: TransportationData
)

data class TransportationData(
    val source: String,
    val destination: String,
    val departureDate: String,
    val transportType: String,
    val options: List<TransportOption>
)

data class TransportOption(
    val type: String,           // 교통수단 세부 유형 (KTX, SRT, 고속버스 등)
    val departureTime: String,  // 출발 시간
    val arrivalTime: String,    // 도착 시간
    val totalTime: Int,         // 총 소요시간(분)
    val price: Int,             // 가격
    val routeInfo: String       // 경로 정보
) 