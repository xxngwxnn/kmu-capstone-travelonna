package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.travelonna.api.AuthApi
import com.example.travelonna.api.GoogleLoginRequest
import com.example.travelonna.api.ProfileResponse
import com.example.travelonna.api.RetrofitClient
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
        .baseUrl("http://travelonna.shop/")  // 포트 번호 제거
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
        val androidClientId = BuildConfig.ANDROID_CLIENT_ID
        val webClientId = "57756654565-ilb27ab4881crfk0f3usde7cgkma3liv.apps.googleusercontent.com"
        
        Log.d(TAG, "Using Android Client ID: $androidClientId")
        Log.d(TAG, "Using Web Client ID: $webClientId")
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(webClientId)
            .requestIdToken(webClientId)
            .build()

        Log.d(TAG, "GoogleSignInOptions configured: ${gso.extensions}")
        Log.d(TAG, "Scopes requested: ${gso.scopeArray?.joinToString()}")

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d(TAG, "GoogleSignInClient initialized")

        // Get the background image and login button
        val loginDisplayImage = findViewById<ImageView>(R.id.loginDisplayImage)
        val customLoginButton = findViewById<ImageButton>(R.id.customLoginButton)
        
        // Hide both initially
        loginDisplayImage.alpha = 0f
        customLoginButton.visibility = View.INVISIBLE
        
        // Create fade-in animation for background image
        loginDisplayImage.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()

        // Show the button after 1 second with fade-in animation
        Handler(Looper.getMainLooper()).postDelayed({
            // Create fade-in animation for button
            val fadeIn = AlphaAnimation(0.0f, 1.0f)
            fadeIn.duration = 500 // 0.5 seconds duration
            fadeIn.fillAfter = true
            
            // Make button visible and start animation
            customLoginButton.visibility = View.VISIBLE
            customLoginButton.startAnimation(fadeIn)
        }, 1000) // 1 second delay

        // Set click listener for custom login button
        customLoginButton.setOnClickListener {
            Log.d(TAG, "=== Custom Login Button Clicked ===")
            triggerGoogleSignIn()
        }
    }

    private fun triggerGoogleSignIn() {
        // 먼저 로그아웃
        Log.d(TAG, "Signing out previous session...")
        googleSignInClient.signOut().addOnCompleteListener {
            // 모든 권한 해제
            googleSignInClient.revokeAccess().addOnCompleteListener { revokeTask ->
                Log.d(TAG, "Access revoked: ${revokeTask.isSuccessful}")
                
                // 로그아웃 후 로그인 시도
                val signInIntent = googleSignInClient.signInIntent
                Log.d(TAG, "Sign In Intent created: $signInIntent")
                Log.d(TAG, "Intent extras: ${signInIntent.extras}")
                Log.d(TAG, "Intent component: ${signInIntent.component}")
                startActivityForResult(signInIntent, RC_SIGN_IN)
                Log.d(TAG, "startActivityForResult called with RC_SIGN_IN: $RC_SIGN_IN")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "=== onActivityResult ===")
        Log.d(TAG, "Request code: $requestCode")
        Log.d(TAG, "Result code: $resultCode")
        Log.d(TAG, "Intent data: $data")
        Log.d(TAG, "Intent extras: ${data?.extras}")
        Log.d(TAG, "Activity is finishing: $isFinishing")

        // Google 로그인 결과 처리
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Processing Google Sign In result")
            
            if (resultCode != RESULT_OK) {
                Log.e(TAG, "Google Sign In result is not OK. Result code: $resultCode")
                
                // data에서 에러 정보 추출 시도
                data?.extras?.keySet()?.forEach { key ->
                    Log.d(TAG, "Intent extra key: $key, value: ${data.extras?.get(key)}")
                }
                
                // 안드로이드 버전에 따라 데이터 형식이 다를 수 있으므로 직접 파싱 대신 로그만 출력
                val googleSignInStatus = data?.extras?.get("googleSignInStatus")
                Log.e(TAG, "Google Sign In Status from extras: $googleSignInStatus")
                
                Toast.makeText(this, "Google Sign In failed with code: $resultCode", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Creating sign in task from intent")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Log.d(TAG, "Task created: $task")
            Log.d(TAG, "Task complete: ${task.isComplete}")
            Log.d(TAG, "Task successful: ${task.isSuccessful}")
            
            try {
                Log.d(TAG, "Getting account from task")
                val account = task.getResult(ApiException::class.java)
                
                // 구글 로그인 성공, 사용자 정보 출력
                Log.d(TAG, "Account ID: ${account.id}")
                Log.d(TAG, "Account Email: ${account.email}")
                Log.d(TAG, "Account Display Name: ${account.displayName}")
                Log.d(TAG, "Account Given Name: ${account.givenName}")
                Log.d(TAG, "Account Family Name: ${account.familyName}")
                Log.d(TAG, "Account Photo URL: ${account.photoUrl}")
                
                // 서버 인증 코득
                val serverAuthCode = account.serverAuthCode
                val idToken = account.idToken
                Log.d(TAG, "=== Google Sign In Success ===")
                Log.d(TAG, "Email: ${account.email}")
                Log.d(TAG, "Server Auth Code: $serverAuthCode")
                Log.d(TAG, "ID Token: $idToken")
                Log.d(TAG, "========================")
                
                // 서버에 인증 코득 전송
                if (serverAuthCode != null) {
                    sendAuthCodeToServer(serverAuthCode)
                } else {
                    Log.e(TAG, "Server auth code is null, cannot authenticate with backend")
                    Toast.makeText(this, "인증 코득를 받아오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                // 구글 로그인 실패
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
                
                Toast.makeText(this, "Google Sign In failed: ${e.status}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendAuthCodeToServer(serverAuthCode: String) {
        // 인증 코득를 백엔드로 전송하는 부분
        Log.d(TAG, "Creating request body: GoogleLoginRequest(code=$serverAuthCode)")
        val request = GoogleLoginRequest(serverAuthCode)
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
                    
                    // More detailed response logging
                    Log.d(TAG, "===== /auth/google Response =====")
                    Log.d(TAG, "Access Token: ${tokenResponse?.accessToken}")
                    Log.d(TAG, "Refresh Token: ${tokenResponse?.refreshToken}")
                    Log.d(TAG, "Token Type: ${tokenResponse?.tokenType}")
                    Log.d(TAG, "Expires In: ${tokenResponse?.expiresIn}")
                    
                    // Safely access userId and scope fields
                    try {
                        Log.d(TAG, "User ID: ${tokenResponse?.userId}")
                        Log.d(TAG, "Scope: ${tokenResponse?.scope}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error accessing userId or scope fields: ${e.message}")
                    }
                    
                    Log.d(TAG, "Complete Response JSON: ${tokenResponseToJson(tokenResponse)}")
                    Log.d(TAG, "==============================")
                    
                    // 토큰 저장
                    tokenResponse?.accessToken?.let { token ->
                        RetrofitClient.saveToken(token)
                        Log.d(TAG, "Token saved to RetrofitClient")
                        
                        // userId도 저장
                        tokenResponse.userId.let { userId ->
                            RetrofitClient.saveUserId(userId)
                            Log.d(TAG, "User ID saved to RetrofitClient: $userId")
                        }
                    }
                    
                    // Check if user profile exists
                    tokenResponse?.userId?.let { userId ->
                        Log.d(TAG, "Checking if user profile exists for userId: $userId")
                        checkUserProfile(userId)
                    } ?: run {
                        Log.e(TAG, "User ID is null, cannot check profile")
                        navigateToHomeActivity()
                    }
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
    }

    // Helper function to convert TokenResponse to a JSON string representation
    private fun tokenResponseToJson(response: TokenResponse?): String {
        if (response == null) return "null"
        return """
            {
              "accessToken": "${response.accessToken}",
              "refreshToken": "${response.refreshToken}",
              "tokenType": "${response.tokenType}",
              "expiresIn": ${response.expiresIn}
              ${if (response.userId > 0) """, "user_id": ${response.userId}""" else ""}
              ${if (response.scope.isNotEmpty()) """, "scope": "${response.scope}"""" else ""}
            }
        """.trimIndent()
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

    // Add a new method to check user profile
    private fun checkUserProfile(userId: Int) {
        Log.d(TAG, "Making API call to check user profile: userId=$userId")
        RetrofitClient.apiService.getUserProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                Log.d(TAG, "Profile check response code: ${response.code()}")
                
                when (response.code()) {
                    200 -> {
                        // Profile exists, navigate to HomeActivity
                        Log.d(TAG, "User profile exists, navigating to HomeActivity")
                        navigateToHomeActivity()
                    }
                    204 -> {
                        // 204 indicates profile doesn't exist
                        Log.d(TAG, "User profile doesn't exist (204), navigating to ProfileCreateActivity")
                        Toast.makeText(this@LoginActivity, "프로필을 생성해주세요", Toast.LENGTH_SHORT).show()
                        navigateToProfileCreateActivity()
                    }
                    else -> {
                        // Other error
                        Log.e(TAG, "Error checking profile: ${response.code()}, ${response.message()}")
                        Log.e(TAG, "Error body: ${response.errorBody()?.string()}")
                        // Navigate to plan activity anyway as fallback
                        navigateToHomeActivity()
                    }
                }
            }
            
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Log.e(TAG, "Network error when checking profile", t)
                // Navigate to plan activity as fallback
                navigateToHomeActivity()
            }
        })
    }
    
    // Helper method to navigate to HomeActivity
    private fun navigateToHomeActivity() {
        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    // Helper method to navigate to ProfileCreateActivity
    private fun navigateToProfileCreateActivity() {
        val intent = Intent(this@LoginActivity, ProfileCreateActivity::class.java)
        startActivity(intent)
        finish()
    }
} 