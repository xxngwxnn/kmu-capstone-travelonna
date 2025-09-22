package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.adapter.PlacePostAdapter
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.TravelLogListResponse
import com.example.travelonna.model.Post
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlacePostListActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "PlacePostListActivity"
    }

    private lateinit var backButton: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noResultsText: TextView
    private lateinit var adapter: PlacePostAdapter
    private lateinit var postDetailLauncher: ActivityResultLauncher<Intent>
    private var currentPlaceId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_post_list)

        // 하단 네비게이션 바 설정 (기본적으로 아무것도 선택되지 않음)
        setupBottomNavBar(-1)

        // ActivityResultLauncher 초기화
        setupActivityResultLauncher()

        initViews()
        setupRecyclerView()
        
        val placeId = intent.getLongExtra("placeId", -1)
        if (placeId == -1L) {
            Toast.makeText(this, "장소 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentPlaceId = placeId.toInt()
        loadPlacePosts(currentPlaceId)
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        recyclerView = findViewById(R.id.placePostRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        noResultsText = findViewById(R.id.noResultsText)

        backButton.setOnClickListener { finish() }
    }

    private fun setupActivityResultLauncher() {
        postDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val needsRefresh = result.data?.getBooleanExtra(PostDetailActivity.RESULT_REFRESH_NEEDED, false) ?: false
                if (needsRefresh) {
                    Log.d(TAG, "PostDetail returned with refresh needed, reloading place posts")
                    loadPlacePosts(currentPlaceId)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = PlacePostAdapter(listOf()) { post ->
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
            postDetailLauncher.launch(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadPlacePosts(placeId: Int) {
        showLoading()
        
        RetrofitClient.apiService.getTravelLogsByPlace(placeId).enqueue(object : Callback<TravelLogListResponse> {
            override fun onResponse(call: Call<TravelLogListResponse>, response: Response<TravelLogListResponse>) {
                hideLoading()
                
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    
                    if (apiResponse.success) {
                        val posts = apiResponse.data?.map { log ->
                            Post(
                                id = log.logId.toLong(),
                                userName = log.userName,
                                isFollowing = false,
                                description = log.comment,
                                imageResource = R.drawable.placeholder_image,
                                date = if (log.createdAt.isNotEmpty() && log.createdAt.length >= 10) log.createdAt.substring(0, 10) else "",
                                likeCount = log.likeCount,
                                commentCount = log.commentCount,
                                isLiked = log.isLiked
                            )
                        } ?: emptyList()
                        
                        adapter.updateData(posts)
                        showNoResults(posts.isEmpty())
                        Log.d(TAG, "Loaded ${posts.size} posts for place $placeId")
                    } else {
                        Log.e(TAG, "API returned success=false: ${apiResponse.message}")
                        showError("게시물을 불러올 수 없습니다: ${apiResponse.message}")
                    }
                } else {
                    Log.e(TAG, "Failed to load place posts: ${response.code()}")
                    showError("게시물을 불러올 수 없습니다.")
                }
            }
            
            override fun onFailure(call: Call<TravelLogListResponse>, t: Throwable) {
                hideLoading()
                Log.e(TAG, "Network error while loading place posts", t)
                showError("네트워크 오류가 발생했습니다.")
            }
        })
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noResultsText.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showNoResults(show: Boolean) {
        noResultsText.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        showNoResults(true)
    }

    // 하단 네비게이션 바 클릭 및 선택 상태 관리 함수 추가
    private fun setupBottomNavBar(selectedId: Int) {
        val navHome = findViewById<ImageButton>(R.id.navHome)
        val navMap = findViewById<ImageButton>(R.id.navMap)
        val navPlan = findViewById<ImageButton>(R.id.navPlan)
        val navSearch = findViewById<ImageButton>(R.id.navSearch)
        val navProfile = findViewById<ImageButton>(R.id.navProfile)

        val navButtons = listOf(navHome, navMap, navPlan, navSearch, navProfile)
        navButtons.forEach { it.isSelected = false }
        if (selectedId != -1) {
            findViewById<ImageButton>(selectedId).isSelected = true
        }

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
    }
} 