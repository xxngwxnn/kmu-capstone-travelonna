package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.travelonna.api.ProfileResponse
import com.example.travelonna.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var backButton: ImageButton
    private lateinit var notificationButton: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var postsCount: TextView
    private lateinit var distanceCount: TextView
    private lateinit var placesCount: TextView
    private lateinit var bioText: TextView
    private lateinit var airplaneProgress: ProgressBar
    private lateinit var profileEditButton: Button
    private lateinit var postsTab: TextView
    private lateinit var mapTab: TextView
    private lateinit var divider: View
    
    private val TAG = "ProfileActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize views
        initViews()
        // Setup listeners
        setupListeners()
        // Load profile data from API
        fetchProfileData()
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.back_button)
        notificationButton = findViewById(R.id.notification_button)
        profileImage = findViewById(R.id.profile_image)
        usernameText = findViewById(R.id.username_text)
        postsCount = findViewById(R.id.posts_count)
        distanceCount = findViewById(R.id.distance_count)
        placesCount = findViewById(R.id.places_count)
        bioText = findViewById(R.id.bio_text)
        airplaneProgress = findViewById(R.id.airplane_progress)
        profileEditButton = findViewById(R.id.profile_edit_button)
        postsTab = findViewById(R.id.posts_tab)
        mapTab = findViewById(R.id.map_tab)
        divider = findViewById(R.id.divider)
    }
    
    private fun setupListeners() {
        // Back button - finish activity
        backButton.setOnClickListener {
            finish()
        }
        
        // Notification button
        notificationButton.setOnClickListener {
            // TODO: Implement notification screen navigation
        }
        
        // Profile edit button
        profileEditButton.setOnClickListener {
            // TODO: Navigate to profile edit screen
        }
        
        // Tab switching
        postsTab.setOnClickListener {
            switchToPostsTab()
        }
        
        mapTab.setOnClickListener {
            switchToMapTab()
        }
        
        // 팔로워/팔로잉 텍스트 클릭 이벤트
        distanceCount.setOnClickListener {
            val intent = Intent(this, FollowListActivity::class.java)
            intent.putExtra("type", "followers")
            startActivity(intent)
        }
        
        placesCount.setOnClickListener {
            val intent = Intent(this, FollowListActivity::class.java)
            intent.putExtra("type", "following")
            startActivity(intent)
        }
    }
    
    private fun fetchProfileData() {
        // 로딩 상태 표시
        showLoading(true)
        
        // 현재 유저 ID 가져오기 (API 호출용)
        val userId = RetrofitClient.getUserId()
        
        // API 호출
        RetrofitClient.apiService.getUserProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                showLoading(false)
                
                if (response.isSuccessful) {
                    val profileData = response.body()
                    if (profileData != null) {
                        // 프로필 데이터를 UI에 설정
                        updateProfileUI(profileData)
                    } else {
                        // 응답은 성공했지만 데이터가 null인 경우
                        Toast.makeText(this@ProfileActivity, "프로필 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 오류 응답 처리
                    val errorMessage = response.errorBody()?.string() ?: "프로필 정보를 가져오는데 실패했습니다."
                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "API Error: $errorMessage")
                    
                    // 임시 더미 데이터로 UI 업데이트
                    loadDummyData()
                }
            }
            
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@ProfileActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Network Error: ${t.message}")
                
                // 실패 시 임시 더미 데이터 사용
                loadDummyData()
            }
        })
    }
    
    private fun updateProfileUI(profileData: ProfileResponse) {
        // 닉네임 설정
        usernameText.text = profileData.nickname
        
        // 소개글 설정
        bioText.text = profileData.introduction ?: "소개글이 없습니다."
        
        // 프로필 이미지 설정 (Glide 라이브러리 사용)
        if (!profileData.profileImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(profileData.profileImage)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.ic_launcher_background)
        }
        
        // 임시 통계 데이터 (API에서 제공하지 않는 값)
        postsCount.text = "25"
        distanceCount.text = "220M"
        placesCount.text = "77"
        
        // 진행 상태 설정 (임시)
        airplaneProgress.progress = 75
    }
    
    private fun loadDummyData() {
        // 임시 데이터로 UI 업데이트
        usernameText.text = "travel_on_me"
        postsCount.text = "25"
        distanceCount.text = "220M"
        placesCount.text = "77"
        bioText.text = "여행을 하기 위해 살아가는 사나이"
        
        // 프로필 이미지 설정 - Glide 사용하여 더 나은 로딩 처리
        Glide.with(this)
            .load(R.drawable.ic_launcher_background)
            .centerCrop()
            .into(profileImage)
            
        airplaneProgress.progress = 75
    }
    
    private fun showLoading(isLoading: Boolean) {
        // 로딩 상태 표시 (필요에 따라 구현)
        // 로딩 표시용 ProgressBar가 있다면 여기서 처리
    }
    
    private fun switchToPostsTab() {
        postsTab.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        postsTab.setTypeface(null, android.graphics.Typeface.BOLD)
        
        mapTab.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        mapTab.setTypeface(null, android.graphics.Typeface.NORMAL)
        
        // TODO: Show posts content, hide map content
    }
    
    private fun switchToMapTab() {
        mapTab.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        mapTab.setTypeface(null, android.graphics.Typeface.BOLD)
        
        postsTab.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        postsTab.setTypeface(null, android.graphics.Typeface.NORMAL)
        
        // TODO: Show map content, hide posts content
    }
} 