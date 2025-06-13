package com.example.travelonna.api

data class LogDetailApiResponse(
    val success: Boolean,
    val message: String,
    val data: LogDetailData?
) 