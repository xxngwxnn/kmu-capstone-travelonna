package com.example.travelonna.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import android.content.SharedPreferences
import com.example.travelonna.App
import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    const val BASE_URL = "http://travelonna.shop/"
    private const val TOKEN_PREF = "auth_pref"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_ID_KEY = "user_id"
    
    // 저장된 토큰 가져오기
    private fun getToken(): String? {
        val sharedPref: SharedPreferences = App.getInstance().getSharedPreferences(TOKEN_PREF, Context.MODE_PRIVATE)
        val token = sharedPref.getString(TOKEN_KEY, null)
        Log.d(TAG, "Retrieved token: ${token?.take(15)}...${if(token?.length ?: 0 > 15) "..." else ""}")
        return token
    }
    
    // 저장된 사용자 ID 가져오기
    fun getUserId(): Int {
        val sharedPref = App.getInstance().getSharedPreferences(TOKEN_PREF, Context.MODE_PRIVATE)
        val userId = sharedPref.getInt(USER_ID_KEY, 0)
        Log.d(TAG, "Retrieved userId: $userId")
        return userId
    }
    
    // 인증 헤더 추가를 위한 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // 토큰이 있는 경우에만 헤더에 추가
        val token = getToken()
        val newRequest: Request = if (token != null) {
            Log.d(TAG, "Adding Authorization header with Bearer token")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Log.d(TAG, "No token available, proceeding without Authorization header")
            originalRequest
        }
        
        Log.d(TAG, "Request URL: ${newRequest.url}")
        Log.d(TAG, "Request Headers: ${newRequest.headers}")
        
        chain.proceed(newRequest)
    }
    
    // 로깅 인터셉터 추가
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // OkHttpClient에 인터셉터 추가
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    // 토큰 저장 메서드
    fun saveToken(token: String) {
        val sharedPref = App.getInstance().getSharedPreferences(TOKEN_PREF, Context.MODE_PRIVATE)
        sharedPref.edit().putString(TOKEN_KEY, token).apply()
        Log.d(TAG, "Token saved: ${token.take(15)}...")
    }
    
    // 사용자 ID 저장 메서드
    fun saveUserId(userId: Int) {
        val sharedPref = App.getInstance().getSharedPreferences(TOKEN_PREF, Context.MODE_PRIVATE)
        sharedPref.edit().putInt(USER_ID_KEY, userId).apply()
        Log.d(TAG, "User ID saved: $userId")
    }
    
    // 토큰 삭제 메서드
    fun clearToken() {
        val sharedPref = App.getInstance().getSharedPreferences(TOKEN_PREF, Context.MODE_PRIVATE)
        sharedPref.edit().remove(TOKEN_KEY).apply()
    }
    
    // 모든 사용자 데이터 삭제
    fun clearUserData() {
        val sharedPref = App.getInstance().getSharedPreferences(TOKEN_PREF, Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        Log.d(TAG, "All user data cleared")
    }
} 