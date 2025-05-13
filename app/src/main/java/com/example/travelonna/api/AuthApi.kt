package com.example.travelonna.api

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/google")
    fun googleLogin(@Body request: GoogleLoginRequest): Call<TokenResponse>
}

data class GoogleLoginRequest(
    val code: String
)

data class TokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    
    @SerializedName("refreshToken")
    val refreshToken: String,
    
    @SerializedName("tokenType")
    val tokenType: String,
    
    @SerializedName("expiresIn")
    val expiresIn: Int,
    
    @SerializedName("user_id")  // Use the exact field name from API response
    val userId: Int = 0,
    
    @SerializedName("scope")
    val scope: String = ""
) 