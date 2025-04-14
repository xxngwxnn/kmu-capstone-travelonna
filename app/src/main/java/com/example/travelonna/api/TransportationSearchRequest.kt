package com.example.travelonna.api

data class TransportationSearchRequest(
    val source: String,           // 출발지
    val destination: String,      // 목적지
    val departureDate: String,    // 출발날짜 (YYYY-MM-DD)
    val transportType: String     // 교통수단 유형 (car, bus, train, etc)
) 