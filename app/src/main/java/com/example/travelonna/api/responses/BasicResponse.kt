package com.example.travelonna.api.responses

data class BasicResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
) 