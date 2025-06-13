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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.adapter.RecommendationAdapter
import com.example.travelonna.api.RecommendationRequest
import com.example.travelonna.api.RecommendationApiResponse
import com.example.travelonna.api.RecommendationCountResponse
import com.example.travelonna.api.RecommendationExistsResponse
import com.example.travelonna.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "HomeActivity"
        const val REQUEST_POST_DETAIL = 1001
    }
    
    private lateinit var recommendationAdapter: RecommendationAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var recommendationTitle: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // 뷰 초기화
        initViews()
        
        // 검색 아이콘 클릭 리스너 설정
        findViewById<ImageView>(R.id.searchIcon).setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
        
        // RecyclerView 설정
        setupRecyclerView()
        
        // 추천 데이터 존재 여부 먼저 확인
        checkRecommendationsExist()
        
        // 추천 개수 로드
        loadRecommendationCount()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.postsRecyclerView)
        recommendationTitle = findViewById(R.id.recommendationTitle)
        progressBar = findViewById<ProgressBar?>(R.id.progressBar) ?: run {
            // ProgressBar가 없다면 더미 뷰 생성
            ProgressBar(this).apply { visibility = View.GONE }
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        
        // 초기에는 빈 어댑터로 설정
        recommendationAdapter = RecommendationAdapter(mutableListOf())
        recyclerView.adapter = recommendationAdapter
    }
    
    private fun checkRecommendationsExist() {
        val userId = RetrofitClient.getUserId()
        
        if (userId == 0) {
            Log.w(TAG, "User ID not found, showing empty state")
            showEmptyState()
            return
        }
        
        Log.d(TAG, "Checking if recommendations exist for user: $userId")
        
        // 로딩 표시
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.apiService.checkRecommendationExists(userId, "log").enqueue(object : Callback<RecommendationExistsResponse> {
            override fun onResponse(call: Call<RecommendationExistsResponse>, response: Response<RecommendationExistsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val existsResponse = response.body()!!
                    
                    if (existsResponse.success) {
                        if (existsResponse.data) {
                            // 추천 데이터가 존재하는 경우 - 추천 목록 로드 (로딩은 loadRecommendations에서 관리)
                            Log.d(TAG, "Recommendations exist, loading recommendations...")
                            loadRecommendations()
                        } else {
                            // 추천 데이터가 존재하지 않는 경우
                            progressBar.visibility = View.GONE
                            Log.d(TAG, "No recommendations exist for user")
                            showEmptyState()
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        Log.w(TAG, "Failed to check recommendation existence: ${existsResponse.message}")
                        // API 확인 실패 시 빈 상태 표시
                        showEmptyState()
                    }
                } else {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Failed to check recommendation existence: ${response.code()}")
                    // HTTP 에러 시 빈 상태 표시
                    showEmptyState()
                }
            }
            
            override fun onFailure(call: Call<RecommendationExistsResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e(TAG, "Network error while checking recommendation existence", t)
                // 네트워크 에러 시 빈 상태 표시
                showEmptyState()
            }
        })
    }
    
    private fun loadRecommendations() {
        val userId = RetrofitClient.getUserId()
        
        if (userId == 0) {
            Log.w(TAG, "User ID not found, showing empty state")
            showEmptyState()
            return
        }
        
        Log.d(TAG, "Loading recommendations for user: $userId")
        
        // 로딩 표시
        progressBar.visibility = View.VISIBLE
        
        val request = RecommendationRequest(
            userId = userId,
            recType = "log",
            recLimit = 20
        )
        
        RetrofitClient.apiService.getRecommendations(request).enqueue(object : Callback<RecommendationApiResponse> {
            override fun onResponse(call: Call<RecommendationApiResponse>, response: Response<RecommendationApiResponse>) {
                progressBar.visibility = View.GONE
                
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    
                    if (apiResponse.success && apiResponse.data != null) {
                        // 성공적으로 추천 데이터를 받은 경우
                        Log.d(TAG, "Recommendations loaded: ${apiResponse.data.recommendations.size} items")
                        recommendationAdapter.updateData(apiResponse.data.recommendations)
                        
                        if (apiResponse.data.recommendations.isEmpty()) {
                            showEmptyState()
                        }
                    } else {
                        // API는 성공했지만 비즈니스 로직 실패
                        Log.w(TAG, "API success but business logic failed: ${apiResponse.message}")
                        handleApiError(response.code(), apiResponse.message)
                    }
                } else {
                    // HTTP 에러 응답 처리
                    val errorMessage = when (response.code()) {
                        400 -> {
                            Log.e(TAG, "Bad request: Invalid recommendation type or parameters")
                            "잘못된 요청입니다. 지원하지 않는 추천 타입이거나 잘못된 매개변수입니다."
                        }
                        404 -> {
                            Log.w(TAG, "No recommendations found for user")
                            "추천 게시물을 찾을 수 없습니다."
                        }
                        else -> {
                            Log.e(TAG, "Failed to load recommendations: ${response.code()}")
                            "추천 게시물을 불러올 수 없습니다."
                        }
                    }
                    
                    Toast.makeText(this@HomeActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    
                    // 에러 응답 body도 확인해보기
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.d(TAG, "Error response body: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read error body", e)
                    }
                    
                    // 모든 에러 상황에서 빈 상태 표시
                    showEmptyState()
                }
            }
            
            override fun onFailure(call: Call<RecommendationApiResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e(TAG, "Network error while loading recommendations", t)
                Toast.makeText(this@HomeActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        })
    }
    
    private fun loadRecommendationCount() {
        val userId = RetrofitClient.getUserId()
        
        if (userId == 0) {
            Log.w(TAG, "User ID not found, cannot load recommendation count")
            return
        }
        
        Log.d(TAG, "Loading recommendation count for user: $userId")
        
        RetrofitClient.apiService.getRecommendationCount(userId, "log").enqueue(object : Callback<RecommendationCountResponse> {
            override fun onResponse(call: Call<RecommendationCountResponse>, response: Response<RecommendationCountResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val countResponse = response.body()!!
                    
                    if (countResponse.success) {
                        val count = countResponse.data
                        Log.d(TAG, "Recommendation count loaded: $count")
                        updateRecommendationTitle(count)
                    } else {
                        Log.w(TAG, "Failed to get recommendation count: ${countResponse.message}")
                        updateRecommendationTitle(null)
                    }
                } else {
                    Log.e(TAG, "Failed to load recommendation count: ${response.code()}")
                    updateRecommendationTitle(null)
                }
            }
            
            override fun onFailure(call: Call<RecommendationCountResponse>, t: Throwable) {
                Log.e(TAG, "Network error while loading recommendation count", t)
                updateRecommendationTitle(null)
            }
        })
    }
    
    private fun updateRecommendationTitle(count: Int?) {
        val title = if (count != null) {
            "추천 게시물 ($count)"
        } else {
            "추천 게시물"
        }
        recommendationTitle.text = title
    }
    
    private fun handleApiError(httpCode: Int, message: String) {
        val userMessage = when (httpCode) {
            400 -> "잘못된 요청입니다: $message"
            404 -> "추천 게시물을 찾을 수 없습니다: $message"
            else -> "오류가 발생했습니다: $message"
        }
        
        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show()
        
        // 모든 에러 상황에서 빈 상태 표시
        showEmptyState()
    }
    
    private fun showEmptyState() {
        Log.d(TAG, "Showing empty state")
        
        // 빈 추천 어댑터로 설정하여 아무것도 표시하지 않음
        recommendationAdapter.updateData(emptyList())
        
        // 사용자에게 알림 (한 번만 표시)
        Toast.makeText(this, "아직 추천할 게시물이 없습니다.", Toast.LENGTH_SHORT).show()
    }
} 