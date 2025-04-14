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
import com.google.android.gms.common.api.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.google.android.gms.auth.api.signin.SignInAccount
import com.example.travelonna.api.RetrofitClient

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
        .baseUrl("http://travelonna.shop:8080/")  // 포트 번호 다시 추가
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

        // 로그인 버튼 클릭 시 먼저 로그아웃
        findViewById<SignInButton>(R.id.googleSignInButton).setOnClickListener {
            Log.d(TAG, "=== Sign In Button Clicked ===")
            
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
        Log.d(TAG, "=== LoginActivity onCreate complete ===")
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
                
                // 서버에 인증 코드 전송
                if (serverAuthCode != null) {
                    sendAuthCodeToServer(serverAuthCode)
                } else {
                    Log.e(TAG, "Server auth code is null, cannot authenticate with backend")
                    Toast.makeText(this, "인증 코드를 받아오지 못했습니다.", Toast.LENGTH_SHORT).show()
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
        // 인증 코드를 백엔드로 전송하는 부분
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
                    Log.d(TAG, "Access Token: ${tokenResponse?.accessToken}")
                    Log.d(TAG, "Refresh Token: ${tokenResponse?.refreshToken}")
                    Log.d(TAG, "Token Type: ${tokenResponse?.tokenType}")
                    Log.d(TAG, "Expires In: ${tokenResponse?.expiresIn}")
                    Log.d(TAG, "Full token response: $tokenResponse")
                    Log.d(TAG, "=====================")
                    
                    // 토큰 저장
                    tokenResponse?.accessToken?.let { token ->
                        RetrofitClient.saveToken(token)
                        Log.d(TAG, "Token saved to RetrofitClient")
                    }
                    
                    Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    
                    // PlanActivity로 이동
                    val intent = Intent(this@LoginActivity, PlanActivity::class.java)
                    startActivity(intent)
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