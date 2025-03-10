package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.travelonna.api.AuthApi
import com.example.travelonna.api.GoogleLoginRequest
import com.example.travelonna.api.TokenResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class LoginActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private val TAG = "LoginActivity"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://43.201.98.210:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    private val authApi = retrofit.create(AuthApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Google 로그인 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode("57756654565-ilb27ab4881crfk0f3usde7cgkma3liv.apps.googleusercontent.com", true)
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 로그인 버튼 클릭 시 먼저 로그아웃 수행
        findViewById<SignInButton>(R.id.googleSignInButton).setOnClickListener {
            // 먼저 로그아웃
            googleSignInClient.signOut().addOnCompleteListener {
                // 로그아웃 후 로그인 시도
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                
                val serverAuthCode = account.serverAuthCode
                Log.d(TAG, "=== Google Sign In Success ===")
                Log.d(TAG, "Email: ${account.email}")
                Log.d(TAG, "Server Auth Code: $serverAuthCode")
                Log.d(TAG, "Sending auth code to backend: $serverAuthCode")
                Log.d(TAG, "========================")

                Toast.makeText(this, "Auth Code: $serverAuthCode", Toast.LENGTH_LONG).show()

                authApi.googleLogin(GoogleLoginRequest(serverAuthCode!!)).enqueue(object : Callback<TokenResponse> {
                    override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                        Log.d(TAG, "=== Backend Response ===")
                        Log.d(TAG, "Response Code: ${response.code()}")
                        if (response.isSuccessful) {
                            val tokenResponse = response.body()
                            Log.d(TAG, "Success!")
                            Log.d(TAG, "Access Token: ${tokenResponse?.accessToken}")
                            Log.d(TAG, "Refresh Token: ${tokenResponse?.refreshToken}")
                            Log.d(TAG, "Token Type: ${tokenResponse?.tokenType}")
                            Log.d(TAG, "Expires In: ${tokenResponse?.expiresIn}")
                            Log.d(TAG, "=====================")
                            Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Log.e(TAG, "Error!")
                            Log.e(TAG, "Error Body: ${response.errorBody()?.string()}")
                            Log.e(TAG, "=====================")
                            Toast.makeText(this@LoginActivity, "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                        Log.e(TAG, "=== Network Error ===")
                        Log.e(TAG, "Error Message: ${t.message}")
                        Log.e(TAG, "Error Class: ${t.javaClass.simpleName}")
                        Log.e(TAG, "Request URL: ${call.request().url}")
                        Log.e(TAG, "Stack trace:")
                        t.printStackTrace()
                        Log.e(TAG, "===================")
                        Toast.makeText(this@LoginActivity, "네트워크 오류가 발생했습니다: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })

            } catch (e: ApiException) {
                Log.e(TAG, "=== Google Sign In Error ===")
                Log.e(TAG, "Status Code: ${e.statusCode}")
                Log.e(TAG, "Message: ${e.message}")
                Log.e(TAG, "Status: ${e.status}")
                Log.e(TAG, "=======================")
                Toast.makeText(this, "로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 