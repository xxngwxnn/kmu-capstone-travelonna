package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.travelonna.api.AuthApi
import com.example.travelonna.api.GoogleLoginRequest
import com.example.travelonna.api.TokenResponse
import com.example.travelonna.BuildConfig
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
        .baseUrl("http://travelonna.shop:8080/")  // 실제 Mac의 IP 주소
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    private val authApi = retrofit.create(AuthApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d(TAG, "=== LoginActivity onCreate ===")
        Log.d(TAG, "Device: ${android.os.Build.MODEL}")
        Log.d(TAG, "Android Version: ${android.os.Build.VERSION.RELEASE}")
        Log.d(TAG, "App Version: ${BuildConfig.VERSION_NAME}")

        // Google 로그인 설정
        val androidClientId = "57756654565-8uu2toq17sidffisck9d4r3iaicvfghc.apps.googleusercontent.com"
        val webClientId = "57756654565-ilb27ab4881crfk0f3usde7cgkma3liv.apps.googleusercontent.com"
        
        Log.d(TAG, "Using Android Client ID: $androidClientId")
        Log.d(TAG, "Using Web Client ID: $webClientId")
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(webClientId, true)
            .requestIdToken(webClientId)
            .build()

        Log.d(TAG, "GoogleSignInOptions configured: ${gso.extensions}")
        Log.d(TAG, "Scopes requested: ${gso.scopeArray?.joinToString()}")

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d(TAG, "GoogleSignInClient initialized")

        // 로그인 버튼 클릭 시 먼저 로그아웃 수행
        findViewById<SignInButton>(R.id.googleSignInButton).setOnClickListener {
            Log.d(TAG, "=== Sign In Button Clicked ===")
            // 먼저 로그아웃
            Log.d(TAG, "Signing out previous session...")
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "Previous session sign out complete, starting sign in flow")
                // 로그아웃 후 로그인 시도
                val signInIntent = googleSignInClient.signInIntent
                Log.d(TAG, "Sign In Intent created: $signInIntent")
                Log.d(TAG, "Intent extras: ${signInIntent.extras}")
                startActivityForResult(signInIntent, RC_SIGN_IN)
                Log.d(TAG, "startActivityForResult called with RC_SIGN_IN: $RC_SIGN_IN")
            }
        }
        Log.d(TAG, "=== LoginActivity onCreate complete ===")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "=== onActivityResult ===")
        Log.d(TAG, "Request code: $requestCode")
        Log.d(TAG, "Result code: $resultCode")
        Log.d(TAG, "Intent data: $data")
        Log.d(TAG, "Intent extras: ${data?.extras}")

        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Processing Google Sign In result")
            try {
                Log.d(TAG, "Creating sign in task from intent")
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                Log.d(TAG, "Task created: $task")
                Log.d(TAG, "Task complete: ${task.isComplete}")
                Log.d(TAG, "Task successful: ${task.isSuccessful}")
                
                Log.d(TAG, "Getting account from task")
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Account retrieved successfully: $account")
                Log.d(TAG, "Account ID: ${account.id}")
                Log.d(TAG, "Account Display Name: ${account.displayName}")
                Log.d(TAG, "Account Email: ${account.email}")
                Log.d(TAG, "Account Photo URL: ${account.photoUrl}")
                
                // 서버 인증 코드 획득
                val serverAuthCode = account.serverAuthCode
                Log.d(TAG, "=== Google Sign In Success ===")
                Log.d(TAG, "Email: ${account.email}")
                Log.d(TAG, "Server Auth Code: $serverAuthCode")
                Log.d(TAG, "ID Token: ${account.idToken}")
                Log.d(TAG, "Sending auth code to backend: $serverAuthCode")
                Log.d(TAG, "========================")

                Toast.makeText(this, "Auth Code: $serverAuthCode", Toast.LENGTH_LONG).show()

                // ID 토큰 로그 추가
                val idToken = account.idToken
                Log.d(TAG, "ID Token: $idToken")

                // 인증 코드를 백엔드로 전송하는 부분
                Log.d(TAG, "Creating request body: GoogleLoginRequest(token=$serverAuthCode)")
                val request = GoogleLoginRequest(serverAuthCode!!)
                Log.d(TAG, "Calling API: authApi.googleLogin")
                val call = authApi.googleLogin(request)
                Log.d(TAG, "Call created: $call")
                Log.d(TAG, "Request URL: ${call.request().url}")
                Log.d(TAG, "Request method: ${call.request().method}")
                Log.d(TAG, "Request headers: ${call.request().headers}")
                Log.d(TAG, "Enqueuing call...")
                
                call.enqueue(object : Callback<TokenResponse> {
                    override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                        Log.d(TAG, "=== Backend Response ===")
                        Log.d(TAG, "Response Code: ${response.code()}")
                        Log.d(TAG, "Response Message: ${response.message()}")
                        Log.d(TAG, "Response Headers: ${response.headers()}")
                        Log.d(TAG, "Raw response: ${response.raw()}")
                        
                        if (response.isSuccessful) {
                            val tokenResponse = response.body()
                            Log.d(TAG, "Success!")
                            Log.d(TAG, "Access Token: ${tokenResponse?.accessToken}")
                            Log.d(TAG, "Refresh Token: ${tokenResponse?.refreshToken}")
                            Log.d(TAG, "Token Type: ${tokenResponse?.tokenType}")
                            Log.d(TAG, "Expires In: ${tokenResponse?.expiresIn}")
                            Log.d(TAG, "Full token response: $tokenResponse")
                            Log.d(TAG, "=====================")
                            Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Log.e(TAG, "Error!")
                            Log.e(TAG, "Error Code: ${response.code()}")
                            val errorBody = response.errorBody()?.string() ?: "No error body"
                            Log.e(TAG, "Error Body: $errorBody")
                            Log.e(TAG, "Headers: ${response.headers()}")
                            Log.e(TAG, "=====================")
                            Toast.makeText(this@LoginActivity, "서버 오류가 발생했습니다. (${response.code()}): $errorBody", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                        Log.e(TAG, "=== Network Error ===")
                        Log.e(TAG, "Error Message: ${t.message}")
                        Log.e(TAG, "Error Class: ${t.javaClass.simpleName}")
                        Log.e(TAG, "Request URL: ${call.request().url}")
                        Log.e(TAG, "Request Method: ${call.request().method}")
                        Log.e(TAG, "Request Headers: ${call.request().headers}")
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
                Log.e(TAG, "Status Message: ${e.status.statusMessage}")
                Log.e(TAG, "Status Resolution: ${e.status.resolution}")
                Log.e(TAG, "Has Resolution: ${e.status.hasResolution()}")
                Log.e(TAG, "Exception Cause: ${e.cause}")
                Log.e(TAG, "Stack trace:")
                e.printStackTrace()
                Log.e(TAG, "=======================")
                Toast.makeText(this, "로그인 실패: ${e.statusCode} - ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Unknown request code: $requestCode, ignoring")
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "=== onStart ===")
        
        // Check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)
        Log.d(TAG, "Last signed in account: $account")
        if (account != null) {
            Log.d(TAG, "User is already signed in: ${account.email}")
            Log.d(TAG, "ID: ${account.id}")
            Log.d(TAG, "Display Name: ${account.displayName}")
        } else {
            Log.d(TAG, "No user is signed in")
        }
    }
} 