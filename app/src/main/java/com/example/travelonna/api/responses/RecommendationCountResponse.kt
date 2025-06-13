package com.example.travelonna.api.responses

import com.google.gson.annotations.SerializedName

data class RecommendationCountResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: Int
) 