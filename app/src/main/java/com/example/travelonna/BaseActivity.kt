package com.example.travelonna

import android.content.Intent
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.travelonna.api.ProfileResponse
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.util.ImageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * 모든 액티비티의 기본 클래스
 * 공통 네비게이션 바 설정 및 프로필 이미지 로딩 기능 제공
 */
open class BaseActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "BaseActivity"
    }

    /**
     * 하단 네비게이션 바 설정 및 프로필 이미지 로딩
     * @param selectedId 현재 선택된 네비게이션 버튼의 ID
     */
    protected fun setupBottomNavBar(selectedId: Int) {
        val navHome = findViewById<ImageButton>(R.id.navHome)
        val navMap = findViewById<ImageButton>(R.id.navMap)
        val navPlan = findViewById<ImageButton>(R.id.navPlan)
        val navSearch = findViewById<ImageButton>(R.id.navSearch)
        val navProfile = findViewById<ImageButton>(R.id.navProfile)

        val navButtons = listOf(navHome, navMap, navPlan, navSearch, navProfile)
        navButtons.forEach { it.isSelected = false }
        findViewById<ImageButton>(selectedId).isSelected = true

        // 네비게이션 클릭 이벤트 설정
        navHome.setOnClickListener {
            if (!it.isSelected) {
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
        navMap.setOnClickListener {
            if (!it.isSelected) {
                val intent = Intent(this, MyMapActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
        navPlan.setOnClickListener {
            if (!it.isSelected) {
                val intent = Intent(this, PlanActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
        navSearch.setOnClickListener {
            if (!it.isSelected) {
                val intent = Intent(this, SearchActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
        navProfile.setOnClickListener {
            if (!it.isSelected) {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
        
        // 프로필 버튼에 사용자 프로필 이미지 로드
        loadProfileImageToNavButton(navProfile)
    }
    
    /**
     * 네비게이션 바의 프로필 버튼에 현재 사용자의 프로필 이미지를 로드
     */
    private fun loadProfileImageToNavButton(navProfileButton: ImageButton) {
        val userId = RetrofitClient.getUserId()
        
        if (userId == 0) {
            Log.w(TAG, "User ID not found, using default profile image")
            // 기본 이미지 사용
            ImageUtils.loadProfileImage(navProfileButton, null)
            return
        }
        
        // 현재 사용자의 프로필 정보 조회
        RetrofitClient.apiService.getUserProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val profileData = response.body()!!
                    Log.d(TAG, "프로필 정보 조회 성공 - 이미지 URL: ${profileData.profileImage}")
                    
                    // 프로필 이미지를 네비게이션 버튼에 로드
                    ImageUtils.loadProfileImage(navProfileButton, profileData.profileImage)
                } else {
                    Log.e(TAG, "프로필 정보 조회 실패: ${response.code()}")
                    // 실패 시 기본 이미지 사용
                    ImageUtils.loadProfileImage(navProfileButton, null)
                }
            }
            
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Log.e(TAG, "프로필 정보 조회 네트워크 오류", t)
                // 네트워크 오류 시 기본 이미지 사용
                ImageUtils.loadProfileImage(navProfileButton, null)
            }
        })
    }
} 