package com.example.travelonna.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/google")
    fun googleLogin(@Body request: GoogleLoginRequest): Call<TokenResponse>
}

data class GoogleLoginRequest(
    val code: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int
) 