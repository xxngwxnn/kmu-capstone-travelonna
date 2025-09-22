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
import com.example.travelonna.api.UserLogsResponse
import com.example.travelonna.model.Post
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserLogListActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        private const val TAG = "UserLogListActivity"
    }

    private lateinit var backButton: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noResultsText: TextView
    private lateinit var adapter: PlacePostAdapter
    private lateinit var postDetailLauncher: ActivityResultLauncher<Intent>
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_log_list)

        // 하단 네비게이션 바 설정 (기본적으로 아무것도 선택되지 않음)
        setupBottomNavBar(-1)

        // ActivityResultLauncher 초기화
        setupActivityResultLauncher()

        initViews()
        setupRecyclerView()

        val userId = intent.getLongExtra(EXTRA_USER_ID, -1)
        if (userId == -1L) {
            Toast.makeText(this, "유저 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUserId = userId.toInt()
        loadUserLogs(currentUserId)
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        recyclerView = findViewById(R.id.userLogRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        noResultsText = findViewById(R.id.noResultsText)

        backButton.setOnClickListener { finish() }
    }

    private fun setupActivityResultLauncher() {
        postDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "ActivityResult received - resultCode: ${result.resultCode}, data: ${result.data}")
            if (result.resultCode == RESULT_OK) {
                val needsRefresh = result.data?.getBooleanExtra(PostDetailActivity.RESULT_REFRESH_NEEDED, false) ?: false
                Log.d(TAG, "Result OK - needsRefresh: $needsRefresh")
                if (needsRefresh) {
                    Log.d(TAG, "PostDetail returned with refresh needed, reloading user logs")
                    loadUserLogs(currentUserId)
                } else {
                    Log.d(TAG, "No refresh needed")
                }
            } else {
                Log.d(TAG, "Result not OK - resultCode: ${result.resultCode}")
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

    private fun loadUserLogs(userId: Int) {
        Log.d(TAG, "loadUserLogs called for userId: $userId")
        showLoading()
        RetrofitClient.apiService.getUserLogs(userId).enqueue(object : Callback<UserLogsResponse> {
            override fun onResponse(call: Call<UserLogsResponse>, response: Response<UserLogsResponse>) {
                hideLoading()
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success) {
                        val posts = apiResponse.data
                            .filter { it.isPublic }
                            .map { log ->
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
                            }
                        adapter.updateData(posts)
                        showNoResults(posts.isEmpty())
                        Log.d(TAG, "Loaded ${posts.size} public logs for user $userId")
                    } else {
                        Log.e(TAG, "API returned success=false: ${apiResponse.message}")
                        showError("게시물을 불러올 수 없습니다: ${apiResponse.message}")
                    }
                } else {
                    Log.e(TAG, "Failed to load user logs: ${response.code()}")
                    showError("게시물을 불러올 수 없습니다.")
                }
            }

            override fun onFailure(call: Call<UserLogsResponse>, t: Throwable) {
                hideLoading()
                Log.e(TAG, "Network error while loading user logs", t)
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