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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.travelonna.util.CustomToast

class HomeActivity : BaseActivity() {
    
    companion object {
        private const val TAG = "HomeActivity"
        const val REQUEST_POST_DETAIL = 1001
        private const val DEFAULT_PAGE_SIZE = 50
    }
    
    private lateinit var recommendationAdapter: RecommendationAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var recommendationTitle: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var postDetailLauncher: ActivityResultLauncher<Intent>
    
    private var currentPage = 1
    private var isLoading = false
    private var hasNextPage = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // 하단 네비게이션 바 설정
        setupBottomNavBar(R.id.navHome)
        
        // ActivityResultLauncher 초기화
        setupActivityResultLauncher()
        
        // 뷰 초기화
        initViews()
        
        // SwipeRefreshLayout 초기화
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            resetPagination()
            loadRecommendations()
        }
        
        // 기록 작성 아이콘 클릭 리스너 설정
        findViewById<ImageView>(R.id.writeIcon).setOnClickListener {
            val intent = Intent(this, LogActivity::class.java)
            intent.putExtra("from_write_memory", true)  // 기록작성 모드임을 알림
            startActivity(intent)
        }
        
        // RecyclerView 설정
        setupRecyclerView()
        
        // 초기 데이터 로드
        loadRecommendations()
    }
    
    private fun setupActivityResultLauncher() {
        postDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val needsRefresh = result.data?.getBooleanExtra(PostDetailActivity.RESULT_REFRESH_NEEDED, false) ?: false
                if (needsRefresh) {
                    Log.d(TAG, "PostDetail returned with refresh needed, reloading recommendations")
                    resetPagination()
                    loadRecommendations()
                }
            }
        }
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
        recyclerView = findViewById(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recommendationAdapter = RecommendationAdapter()
        
        // PostDetailActivity로 이동할 때 ActivityResultLauncher 사용
        recommendationAdapter.setOnPostClickListener(object : RecommendationAdapter.OnPostClickListener {
            override fun onPostClick(intent: Intent) {
                postDetailLauncher.launch(intent)
            }
        })
        
        recyclerView.adapter = recommendationAdapter
        
        // 스크롤 리스너 추가
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!isLoading && hasNextPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0) {
                        loadNextPage()
                    }
                }
            }
        })
    }
    
    private fun resetPagination() {
        currentPage = 1
        hasNextPage = true
        recommendationAdapter.updateData(emptyList())
    }
    
    private fun loadNextPage() {
        if (!isLoading && hasNextPage) {
            currentPage++
            loadRecommendations()
        }
    }
    
    private fun loadRecommendations() {
        val userId = RetrofitClient.getUserId()
        
        if (userId == 0) {
            Log.w(TAG, "User ID not found, showing empty state")
            showEmptyState()
            swipeRefreshLayout.isRefreshing = false
            return
        }
        
        if (isLoading) return
        
        isLoading = true
        Log.d(TAG, "Loading recommendations for user: $userId (page=$currentPage)")
        
        // 첫 페이지 로드시에만 프로그레스바 표시
        if (currentPage == 1) {
            progressBar.visibility = View.VISIBLE
        }
        
        RetrofitClient.apiService.getRecommendations(
            userId = userId,
            type = "log",
            page = currentPage,
            size = DEFAULT_PAGE_SIZE
        ).enqueue(object : Callback<RecommendationApiResponse> {
            override fun onResponse(call: Call<RecommendationApiResponse>, response: Response<RecommendationApiResponse>) {
                isLoading = false
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    
                    if (apiResponse.success && apiResponse.data != null) {
                        val recommendations = apiResponse.data.recommendations
                        val pageInfo = apiResponse.data.pageInfo
                        
                        Log.d(TAG, "Recommendations loaded: ${recommendations.size} items (page ${pageInfo.currentPage}/${pageInfo.totalPages})")
                        
                        if (currentPage == 1) {
                            recommendationAdapter.updateData(recommendations)
                        } else {
                            recommendationAdapter.appendData(recommendations)
                        }
                        
                        hasNextPage = pageInfo.hasNext
                        
                        if (recommendations.isEmpty() && currentPage == 1) {
                            showEmptyState()
                        }
                        
                        // 추천 개수 업데이트
                        updateRecommendationTitle(pageInfo.totalElements)
                    } else {
                        Log.w(TAG, "API success but business logic failed: ${apiResponse.message}")
                        handleApiError(response.code(), apiResponse.message)
                    }
                } else {
                    handleApiError(response.code(), "Failed to load recommendations")
                }
            }
            
            override fun onFailure(call: Call<RecommendationApiResponse>, t: Throwable) {
                isLoading = false
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                Log.e(TAG, "Network error while loading recommendations", t)
                CustomToast.error(this@HomeActivity, "네트워크 오류가 발생했습니다.")
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
        recommendationTitle.text = "추천 게시물"
    }
    
    private fun handleApiError(httpCode: Int, message: String) {
        val userMessage = when (httpCode) {
            400 -> "잘못된 요청입니다: $message"
            404 -> "추천 게시물을 찾을 수 없습니다: $message"
            else -> "오류가 발생했습니다: $message"
        }
        
        CustomToast.error(this, userMessage)
        
        // 모든 에러 상황에서 빈 상태 표시
        showEmptyState()
    }
    
    private fun showEmptyState() {
        Log.d(TAG, "Showing empty state")
        
        // 빈 추천 어댑터로 설정하여 아무것도 표시하지 않음
        recommendationAdapter.updateData(emptyList())
        
        // 사용자에게 알림 (한 번만 표시)
        CustomToast.info(this, "아직 추천할 게시물이 없습니다.")
    }
    


    private fun updateRecommendationCount(count: Int) {
        recommendationTitle.text = "추천 게시물"  // 숫자 제거
    }
} 