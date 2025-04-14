package com.example.travelonna.api

data class TransportationRequest(
    val source: String,
    val destination: String,
    val departureDate: String,
    val transportType: String
)

data class TransportationResponse(
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
    val type: String,
    val departureTime: String,
    val arrivalTime: String,
    val totalTime: Int,
    val price: Int,
    val routeInfo: String
) 