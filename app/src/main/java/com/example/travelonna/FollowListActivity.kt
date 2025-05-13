package com.example.travelonna

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.adapter.FollowAdapter
import com.example.travelonna.api.FollowCountResponse
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.model.User
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FollowListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var tvSectionTitle: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnNotification: ImageButton
    
    private val TAG = "FollowListActivity"
    
    // 더미 데이터
    private val followers = listOf(
        User("1", "lov.ely_01", null, true),
        User("2", "lov.ely_01", null, true),
        User("3", "lov.ely_01", null, true),
        User("4", "lov.ely_01", null, true),
        User("5", "lov.ely_01", null, true),
        User("6", "lov.ely_01", null, true),
        User("7", "lov.ely_01", null, true),
        User("8", "lov.ely_01", null, true),
        User("9", "lov.ely_01", null, true)
    )
    
    private val following = listOf(
        User("10", "travel_lover", null, true),
        User("11", "photo_journey", null, true),
        User("12", "wanderlust", null, true),
        User("13", "globe_explorer", null, true),
        User("14", "scenic_spots", null, true)
    )
    
    private var currentList = mutableListOf<User>()
    private var isFollowerTab = true
    private var profileId = 0
    private var followerCount = 0
    private var followingCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_follow_list)
        
        // Set up window insets like in ProfileActivity
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.follow_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initViews()
        setupListeners()
        
        // Intent에서 타입과 닉네임 가져오기
        val type = intent.getStringExtra("type") ?: "followers"
        val nickname = intent.getStringExtra("nickname") ?: "travel_on_me"
        
        // 닉네임 설정
        tvTitle.text = nickname
        
        isFollowerTab = type == "followers"
        
        // 프로필 ID를 가져오기 (API 호출용)
        profileId = RetrofitClient.getUserId()
        
        // 팔로워 및 팔로잉 수 가져오기
        fetchFollowCounts(profileId)
        
        // 초기 탭 선택
        tabLayout.getTabAt(if (isFollowerTab) 0 else 1)?.select()
        
        // 초기 데이터 설정
        updateContent()
    }
    
    private fun fetchFollowCounts(profileId: Int) {
        // 팔로워 수 가져오기
        RetrofitClient.apiService.getFollowersCount(profileId).enqueue(object : Callback<FollowCountResponse> {
            override fun onResponse(call: Call<FollowCountResponse>, response: Response<FollowCountResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    followerCount = response.body()!!.data.count
                    Log.d(TAG, "팔로워 수: $followerCount")
                    
                    // 팔로잉 수도 가져온 후 탭 텍스트 업데이트
                    fetchFollowingCount(profileId)
                } else {
                    Log.e(TAG, "팔로워 수 조회 실패: ${response.errorBody()?.string()}")
                    // 팔로잉 수도 가져온 후 탭 텍스트 업데이트 (오류 시 더미 데이터 사용)
                    followerCount = followers.size
                    fetchFollowingCount(profileId)
                }
            }
            
            override fun onFailure(call: Call<FollowCountResponse>, t: Throwable) {
                Log.e(TAG, "팔로워 수 조회 네트워크 오류", t)
                // 네트워크 오류 시 더미 데이터 사용
                followerCount = followers.size
                fetchFollowingCount(profileId)
            }
        })
    }
    
    private fun fetchFollowingCount(profileId: Int) {
        RetrofitClient.apiService.getFollowingsCount(profileId).enqueue(object : Callback<FollowCountResponse> {
            override fun onResponse(call: Call<FollowCountResponse>, response: Response<FollowCountResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    followingCount = response.body()!!.data.count
                    Log.d(TAG, "팔로잉 수: $followingCount")
                } else {
                    Log.e(TAG, "팔로잉 수 조회 실패: ${response.errorBody()?.string()}")
                    // 오류 시 더미 데이터 사용
                    followingCount = following.size
                }
                
                // 카운트 조회 완료 후 탭 텍스트 업데이트
                setupTabText()
            }
            
            override fun onFailure(call: Call<FollowCountResponse>, t: Throwable) {
                Log.e(TAG, "팔로잉 수 조회 네트워크 오류", t)
                // 네트워크 오류 시 더미 데이터 사용
                followingCount = following.size
                
                // 카운트 조회 완료 후 탭 텍스트 업데이트
                setupTabText()
            }
        })
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        tabLayout = findViewById(R.id.tabLayout)
        tvSectionTitle = findViewById(R.id.tvSectionTitle)
        tvTitle = findViewById(R.id.tvTitle)
        btnBack = findViewById(R.id.btnBack)
        btnNotification = findViewById(R.id.btnNotification)
    }
    
    private fun setupListeners() {
        // 뒤로가기 버튼
        btnBack.setOnClickListener {
            finish()
        }
        
        // 알림 버튼
        btnNotification.setOnClickListener {
            // TODO: 알림 화면으로 이동 또는 알림 기능 구현
            Toast.makeText(this, "알림 기능은 아직 구현되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
        
        // 탭 선택 리스너
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        isFollowerTab = true
                        updateContent()
                    }
                    1 -> {
                        isFollowerTab = false
                        updateContent()
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupTabText() {
        // 포맷팅된 카운트 문자열 생성 (예: 1000 -> 1K)
        val formattedFollowerCount = formatCount(followerCount)
        val formattedFollowingCount = formatCount(followingCount)
        
        // 탭 텍스트 설정
        tabLayout.getTabAt(0)?.text = "$formattedFollowerCount 팔로워"
        tabLayout.getTabAt(1)?.text = "$formattedFollowingCount 팔로잉"
    }
    
    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
    
    private fun updateContent() {
        if (isFollowerTab) {
            setupRecyclerView(followers)
            tvSectionTitle.text = "모든 팔로워"
        } else {
            setupRecyclerView(following)
            tvSectionTitle.text = "모든 팔로잉"
        }
    }
    
    private fun setupRecyclerView(users: List<User>) {
        currentList.clear()
        currentList.addAll(users)
        
        adapter = FollowAdapter(currentList) { user, position ->
            // 팔로우 상태가 변경될 때 메시지 표시
            val message = if (user.isFollowing) 
                "${user.username}님을 팔로우합니다." 
            else 
                "${user.username}님 팔로우를 취소했습니다."
            
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
} 