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
import com.example.travelonna.api.FollowListResponse
import com.example.travelonna.api.FollowRequest
import com.example.travelonna.api.FollowResponse
import com.example.travelonna.api.FollowUser
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.UnfollowResponse
import com.example.travelonna.model.User
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.app.AlertDialog
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
        
        // Intent에서 타입과 닉네임, 프로필 ID 가져오기
        val type = intent.getStringExtra("type") ?: "followers"
        val nickname = intent.getStringExtra("nickname") ?: "travel_on_me"
        profileId = intent.getIntExtra("profileId", RetrofitClient.getUserId())
        
        // 닉네임 설정
        tvTitle.text = nickname
        
        isFollowerTab = type == "followers"
        
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
            tvSectionTitle.text = "모든 팔로워"
            fetchFollowersList(profileId)
        } else {
            tvSectionTitle.text = "모든 팔로잉"
            fetchFollowingsList(profileId)
        }
    }
    
    private fun setupRecyclerView(users: List<User>) {
        currentList.clear()
        currentList.addAll(users)
        
        adapter = FollowAdapter(currentList) { user, position ->
            handleFollowToggle(user, position)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    // FollowUser를 User 모델로 변환하는 확장 함수
    private fun FollowUser.toUser(isFollowerList: Boolean): User {
        // 팔로워 목록인 경우: fromUser가 실제 사용자 ID
        // 팔로잉 목록인 경우: toUser가 실제 사용자 ID
        val actualUserId = if (isFollowerList) this.fromUser else this.toUser
        
        return User(
            id = actualUserId.toString(),
            username = "user_$actualUserId", // 실제로는 사용자명을 받아와야 함
            profileImageUrl = null,
            isFollowing = this.following
        )
    }

    private fun fetchFollowersList(profileId: Int) {
        RetrofitClient.apiService.getFollowersList(profileId).enqueue(object : Callback<FollowListResponse> {
            override fun onResponse(call: Call<FollowListResponse>, response: Response<FollowListResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val followListResponse = response.body()!!
                    
                    if (followListResponse.success) {
                        val userList = followListResponse.data.map { it.toUser(isFollowerList = true) }
                        setupRecyclerView(userList)
                        Log.d(TAG, "팔로워 목록 조회 성공: ${userList.size}명")
                    } else {
                        Log.w(TAG, "팔로워 목록 조회 실패: ${followListResponse.message}")
                        // 실패 시 더미 데이터 사용
                        setupRecyclerView(followers)
                    }
                } else {
                    Log.e(TAG, "팔로워 목록 HTTP 오류: ${response.code()}")
                    // HTTP 오류 시 더미 데이터 사용
                    setupRecyclerView(followers)
                }
            }
            
            override fun onFailure(call: Call<FollowListResponse>, t: Throwable) {
                Log.e(TAG, "팔로워 목록 네트워크 오류", t)
                // 네트워크 오류 시 더미 데이터 사용
                setupRecyclerView(followers)
                Toast.makeText(this@FollowListActivity, "네트워크 오류로 더미 데이터를 표시합니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchFollowingsList(profileId: Int) {
        RetrofitClient.apiService.getFollowingsList(profileId).enqueue(object : Callback<FollowListResponse> {
            override fun onResponse(call: Call<FollowListResponse>, response: Response<FollowListResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val followListResponse = response.body()!!
                    
                    if (followListResponse.success) {
                        val userList = followListResponse.data.map { it.toUser(isFollowerList = false) }
                        setupRecyclerView(userList)
                        Log.d(TAG, "팔로잉 목록 조회 성공: ${userList.size}명")
                    } else {
                        Log.w(TAG, "팔로잉 목록 조회 실패: ${followListResponse.message}")
                        // 실패 시 더미 데이터 사용
                        setupRecyclerView(following)
                    }
                } else {
                    Log.e(TAG, "팔로잉 목록 HTTP 오류: ${response.code()}")
                    // HTTP 오류 시 더미 데이터 사용
                    setupRecyclerView(following)
                }
            }
            
            override fun onFailure(call: Call<FollowListResponse>, t: Throwable) {
                Log.e(TAG, "팔로잉 목록 네트워크 오류", t)
                // 네트워크 오류 시 더미 데이터 사용
                setupRecyclerView(following)
                Toast.makeText(this@FollowListActivity, "네트워크 오류로 더미 데이터를 표시합니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * 팔로우 토글 처리
     */
    private fun handleFollowToggle(user: User, position: Int) {
        if (user.isFollowing) {
            // 팔로우 중인 상태에서 토글하면 언팔로우 확인 다이얼로그 표시
            showUnfollowConfirmDialog(user, position)
        } else {
            // 팔로우하지 않은 상태에서 토글하면 팔로우 실행
            followUser(user, position)
        }
    }

    /**
     * 언팔로우 확인 다이얼로그 표시
     */
    private fun showUnfollowConfirmDialog(user: User, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("팔로우 취소")
            .setMessage("${user.username}님의 팔로우를 취소하시겠습니까?")
            .setPositiveButton("취소") { _, _ ->
                // 언팔로우 실행
                unfollowUser(user, position)
            }
            .setNegativeButton("아니오") { dialog, _ ->
                // 토글 상태를 원래대로 되돌리기
                revertToggleState(position)
                dialog.dismiss()
            }
            .setOnCancelListener {
                // 다이얼로그 취소 시에도 토글 상태 되돌리기
                revertToggleState(position)
            }
            .show()
    }

    /**
     * 실제 언팔로우 API 호출
     */
    private fun unfollowUser(user: User, position: Int) {
        val userId = user.id.toIntOrNull()
        if (userId == null) {
            Toast.makeText(this, "사용자 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            revertToggleState(position)
            return
        }

        Log.d(TAG, "언팔로우 API 호출 - userId: $userId, username: ${user.username}")
        RetrofitClient.apiService.unfollowUser(userId).enqueue(object : Callback<UnfollowResponse> {
            override fun onResponse(call: Call<UnfollowResponse>, response: Response<UnfollowResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val unfollowResponse = response.body()!!
                    
                    if (unfollowResponse.success) {
                        // 언팔로우 성공 - 목록에서 제거하지 않고 상태만 변경
                        user.isFollowing = false
                        adapter.notifyItemChanged(position)
                        Toast.makeText(this@FollowListActivity, "${user.username}님의 팔로우를 취소했습니다.", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "언팔로우 성공: ${user.username}")
                        
                        // 팔로잉 수만 업데이트 (현재 탭이 팔로잉 탭인 경우)
                        if (!isFollowerTab) {
                            followingCount = maxOf(0, followingCount - 1)
                            setupTabText()
                        }
                    } else {
                        Log.w(TAG, "언팔로우 실패: ${unfollowResponse.message}")
                        Toast.makeText(this@FollowListActivity, "팔로우 취소에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        revertToggleState(position)
                    }
                } else {
                    Log.e(TAG, "언팔로우 HTTP 오류: ${response.code()}")
                    Toast.makeText(this@FollowListActivity, "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    revertToggleState(position)
                }
            }
            
            override fun onFailure(call: Call<UnfollowResponse>, t: Throwable) {
                Log.e(TAG, "언팔로우 네트워크 오류", t)
                Toast.makeText(this@FollowListActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                revertToggleState(position)
            }
        })
    }

    /**
     * 실제 팔로우 API 호출
     */
    private fun followUser(user: User, position: Int) {
        val userId = user.id.toIntOrNull()
        if (userId == null) {
            Toast.makeText(this, "사용자 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            revertToggleState(position)
            return
        }

        val currentUserId = RetrofitClient.getUserId()
        val request = FollowRequest(toUser = userId, fromUser = currentUserId)
        
        Log.d(TAG, "팔로우 API 호출 - toUser: $userId, fromUser: $currentUserId, username: ${user.username}")
        RetrofitClient.apiService.followUser(request).enqueue(object : Callback<FollowResponse> {
            override fun onResponse(call: Call<FollowResponse>, response: Response<FollowResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val followResponse = response.body()!!
                    
                    if (followResponse.success && followResponse.data != null) {
                        // 팔로우 성공
                        val isFollowing = followResponse.data.isFollowing
                        Toast.makeText(this@FollowListActivity, "${user.username}님을 팔로우했습니다.", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "팔로우 성공: ${user.username}, isFollowing: $isFollowing")
                        
                        // 팔로잉 수 업데이트 (현재 탭이 팔로잉 탭인 경우)
                        if (!isFollowerTab && isFollowing) {
                            followingCount += 1
                            setupTabText()
                        }
                        
                        // 팔로우 성공 후 페이지 업데이트 (최신 상태 반영)
                        refreshCurrentTab()
                    } else {
                        Log.w(TAG, "팔로우 실패: ${followResponse.message}")
                        Toast.makeText(this@FollowListActivity, "팔로우에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        revertToggleState(position)
                    }
                } else {
                    Log.e(TAG, "팔로우 HTTP 오류: ${response.code()}")
                    Toast.makeText(this@FollowListActivity, "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    revertToggleState(position)
                }
            }
            
            override fun onFailure(call: Call<FollowResponse>, t: Throwable) {
                Log.e(TAG, "팔로우 네트워크 오류", t)
                Toast.makeText(this@FollowListActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                revertToggleState(position)
            }
        })
    }

    /**
     * 현재 탭 새로고침
     */
    private fun refreshCurrentTab() {
        // 팔로워/팔로잉 수 다시 조회
        fetchFollowCounts(profileId)
        
        // 현재 탭의 목록 다시 조회
        updateContent()
    }

    /**
     * 토글 상태를 원래대로 되돌리기
     */
    private fun revertToggleState(position: Int) {
        if (position < currentList.size) {
            // 어댑터에 변경사항 알림하여 원래 상태로 되돌림
            adapter.notifyItemChanged(position)
        }
    }
} 