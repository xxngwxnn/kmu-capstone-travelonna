package com.example.travelonna

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travelonna.api.FollowCountResponse
import com.example.travelonna.api.ProfileResponse
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.TravelLogData
import com.example.travelonna.api.TravelLogResponse
import com.example.travelonna.api.TravelLogListResponse
import com.example.travelonna.api.UserLogsResponse
import com.example.travelonna.api.UserLogItem
import com.example.travelonna.api.LogDetailApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.travelonna.util.ImageUtils

class ProfileActivity : BaseActivity() {
    
    private lateinit var backButton: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var postsCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var bioText: TextView
    private lateinit var airplaneProgress: ProgressBar
    private lateinit var profileEditButton: Button
    private lateinit var postsTab: TextView
    private lateinit var mapTab: TextView
    private lateinit var divider: View
    private lateinit var postsRecyclerView: RecyclerView
    
    private val TAG = "ProfileActivity"
    private var profileId: Int = 0
    private val travelLogs = mutableListOf<UserLogItem>()
    private lateinit var travelLogsAdapter: TravelLogsAdapter
    private var currentProfileData: ProfileResponse? = null
    private lateinit var postDetailLauncher: ActivityResultLauncher<Intent>
    
    companion object {
        const val ACTION_FOLLOW_UPDATED = "com.example.travelonna.FOLLOW_UPDATED"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_IS_FOLLOWING = "is_following"
    }
    
    private val followUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_FOLLOW_UPDATED) {
                // 팔로우 상태가 변경되었을 때 팔로워/팔로잉 수 새로고침
                refreshFollowCounts()
                Log.d(TAG, "팔로우 상태 변경 감지 - 팔로워/팔로잉 수 새로고침")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        // 하단 네비게이션 바 설정
        setupBottomNavBar(R.id.navProfile)
        
        // ActivityResultLauncher 초기화
        setupActivityResultLauncher()
        
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
    
    override fun onResume() {
        super.onResume()
        // 프로필 편집 후 돌아왔을 때 데이터 새로고침
        fetchProfileData()
        // 팔로워/팔로잉 수도 새로고침 (다른 화면에서 팔로우/언팔로우 후 돌아왔을 때)
        refreshFollowCounts()
        
        // 브로드캐스트 리시버 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(
            followUpdateReceiver,
            IntentFilter(ACTION_FOLLOW_UPDATED)
        )
    }
    
    override fun onPause() {
        super.onPause()
        // 브로드캐스트 리시버 해제
        LocalBroadcastManager.getInstance(this).unregisterReceiver(followUpdateReceiver)
    }
    
    private fun setupActivityResultLauncher() {
        postDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(TAG, "PostDetail에서 돌아옴 - resultCode: ${result.resultCode}")
            if (result.resultCode == RESULT_OK) {
                val needsRefresh = result.data?.getBooleanExtra(PostDetailActivity.RESULT_REFRESH_NEEDED, false) ?: false
                Log.d(TAG, "새로고침 필요: $needsRefresh")
                if (needsRefresh) {
                    Log.d(TAG, "게시물 데이터 변경됨, 프로필 새로고침 시작")
                    // 사용자 게시물 목록 새로고침
                    fetchUserTravelLogs()
                }
            }
        }
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.back_button)
        profileImage = findViewById(R.id.profile_image)
        usernameText = findViewById(R.id.username_text)
        postsCount = findViewById(R.id.posts_count)
        followersCount = findViewById(R.id.followers_count)
        followingCount = findViewById(R.id.following_count)
        bioText = findViewById(R.id.bio_text)
        airplaneProgress = findViewById(R.id.airplane_progress)
        profileEditButton = findViewById(R.id.profile_edit_button)
        postsTab = findViewById(R.id.posts_tab)
        mapTab = findViewById(R.id.map_tab)
        divider = findViewById(R.id.divider)
        postsRecyclerView = findViewById(R.id.posts_recycler_view)
        
        // RecyclerView 설정
        setupRecyclerView()
        
        // 기본적으로 게시물 탭 선택
        switchToPostsTab()
    }
    
    private fun setupListeners() {
        // Back button - finish activity
        backButton.setOnClickListener {
            finish()
        }
        
        // Profile edit button
        profileEditButton.setOnClickListener {
            val intent = Intent(this, ProfileCreateActivity::class.java)
            // 편집 모드임을 알리는 플래그
            intent.putExtra("isEditMode", true)
            // 현재 프로필 데이터 전달
            currentProfileData?.let { profileData ->
                intent.putExtra("nickname", profileData.nickname)
                intent.putExtra("introduction", profileData.introduction ?: "")
                intent.putExtra("profileImageUrl", profileData.profileImage ?: "")
                intent.putExtra("userId", profileData.userId)
                intent.putExtra("profileId", profileData.profileId)
            }
            startActivity(intent)
        }
        
        // Tab switching
        postsTab.setOnClickListener {
            switchToPostsTab()
        }
        
        mapTab.setOnClickListener {
            switchToMapTab()
        }
        
        // 팔로워/팔로잉 텍스트 클릭 이벤트
        followersCount.setOnClickListener {
            val intent = Intent(this, FollowListActivity::class.java)
            intent.putExtra("type", "followers")
            intent.putExtra("nickname", usernameText.text.toString())
            intent.putExtra("profileId", profileId)
            startActivity(intent)
        }
        
        followingCount.setOnClickListener {
            val intent = Intent(this, FollowListActivity::class.java)
            intent.putExtra("type", "following")
            intent.putExtra("nickname", usernameText.text.toString())
            intent.putExtra("profileId", profileId)
            startActivity(intent)
        }
    }
    
    private fun fetchProfileData() {
        // 로딩 상태 표시
        showLoading(true)
        
        // 현재 유저 ID 가져오기 (API 호출용)
        val userId = RetrofitClient.getUserId()
        Log.d(TAG, "프로필 데이터 조회 시작 - userId: $userId")
        
        // API 호출
        RetrofitClient.apiService.getUserProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                showLoading(false)
                Log.d(TAG, "프로필 API 응답 수신 - 성공: ${response.isSuccessful}, 코드: ${response.code()}")
                
                if (response.isSuccessful) {
                    val profileData = response.body()
                    if (profileData != null) {
                        Log.d(TAG, "프로필 데이터 수신 성공 - 닉네임: ${profileData.nickname}, 이미지URL: ${profileData.profileImage}")
                        
                        // 현재 프로필 데이터 저장
                        currentProfileData = profileData
                        // 프로필 데이터를 UI에 설정
                        profileId = profileData.profileId
                        updateProfileUI(profileData)
                        
                        // 팔로워 및 팔로잉 수 조회
                        fetchFollowCounts(profileId)
                        
                        // 사용자별 기록 조회
                        fetchUserTravelLogs()
                    } else {
                        Log.w(TAG, "프로필 데이터가 null입니다")
                        // 응답은 성공했지만 데이터가 null인 경우
                        Toast.makeText(this@ProfileActivity, "프로필 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 오류 응답 처리
                    val errorMessage = response.errorBody()?.string() ?: "프로필 정보를 가져오는데 실패했습니다."
                    Log.e(TAG, "프로필 API 오류 - 코드: ${response.code()}, 메시지: $errorMessage")
                    Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    
                    // 임시 더미 데이터로 UI 업데이트
                    loadDummyData()
                }
            }
            
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                showLoading(false)
                Log.e(TAG, "프로필 API 네트워크 오류: ${t.message}", t)
                Toast.makeText(this@ProfileActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                
                // 실패 시 임시 더미 데이터 사용
                loadDummyData()
            }
        })
    }
    
    private fun fetchFollowCounts(profileId: Int) {
        // 팔로워 수 조회
        fetchFollowersCount(profileId)
        
        // 팔로잉 수 조회
        fetchFollowingsCount(profileId)
    }
    
    private fun fetchFollowersCount(profileId: Int) {
        RetrofitClient.apiService.getFollowersCount(profileId).enqueue(object : Callback<FollowCountResponse> {
            override fun onResponse(call: Call<FollowCountResponse>, response: Response<FollowCountResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val count = response.body()!!.data.count
                    // Display formatted count (e.g., 1000 -> 1K)
                    followersCount.text = formatCount(count)
                    Log.d(TAG, "팔로워 수: $count")
                } else {
                    Log.e(TAG, "팔로워 수 조회 실패: ${response.errorBody()?.string()}")
                    // Set default value on error
                    followersCount.text = "0"
                }
            }
            
            override fun onFailure(call: Call<FollowCountResponse>, t: Throwable) {
                Log.e(TAG, "팔로워 수 조회 네트워크 오류", t)
                // Set default value on network error
                followersCount.text = "0"
            }
        })
    }
    
    private fun fetchFollowingsCount(profileId: Int) {
        RetrofitClient.apiService.getFollowingsCount(profileId).enqueue(object : Callback<FollowCountResponse> {
            override fun onResponse(call: Call<FollowCountResponse>, response: Response<FollowCountResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val count = response.body()!!.data.count
                    // Display formatted count (e.g., 1000 -> 1K)
                    followingCount.text = formatCount(count)
                    Log.d(TAG, "팔로잉 수: $count")
                } else {
                    Log.e(TAG, "팔로잉 수 조회 실패: ${response.errorBody()?.string()}")
                    // Set default value on error
                    followingCount.text = "0"
                }
            }
            
            override fun onFailure(call: Call<FollowCountResponse>, t: Throwable) {
                Log.e(TAG, "팔로잉 수 조회 네트워크 오류", t)
                // Set default value on network error
                followingCount.text = "0"
            }
        })
    }
    
    /**
     * Format large numbers for display (e.g., 1000 -> 1K, 1000000 -> 1M)
     */
    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
    
    private fun updateProfileUI(profileData: ProfileResponse) {
        // 닉네임 설정
        usernameText.text = profileData.nickname
        
        // 소개글 설정
        bioText.text = profileData.introduction ?: "소개글이 없습니다."
        
        // 프로필 이미지 설정 (ImageUtils 사용)
        Log.d(TAG, "프로필 이미지 로딩 - URL: ${profileData.profileImage}")
        ImageUtils.loadProfileImage(profileImage, profileData.profileImage)
        
        // 게시물 수는 fetchUserTravelLogs에서 설정됨
        
        // 진행 상태 설정 (임시)
        airplaneProgress.progress = 75
    }
    
    private fun loadDummyData() {
        // 임시 데이터로 UI 업데이트
        usernameText.text = "travel_on_me"
        postsCount.text = "0" // 기본값으로 0 설정
        followersCount.text = "0"
        followingCount.text = "0"
        bioText.text = "여행을 하기 위해 살아가는 사나이"
        
        // 프로필 이미지 설정 - ImageUtils 사용
        ImageUtils.loadProfileImage(profileImage, null)
            
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
        
        // 게시물 RecyclerView 보이기, 맵 숨기기
        postsRecyclerView.visibility = View.VISIBLE
        // TODO: 맵 뷰가 있다면 여기서 숨기기
        
        Log.d(TAG, "게시물 탭 선택됨 - 게시물 수: ${travelLogs.size}")
    }
    
    private fun switchToMapTab() {
        Log.d(TAG, "맵 탭 선택됨")
        
        // UserLogMapActivity로 이동
        val intent = Intent(this, UserLogMapActivity::class.java)
        startActivity(intent)
    }
    
    private fun setupRecyclerView() {
        travelLogsAdapter = TravelLogsAdapter(travelLogs) { travelLog ->
            // 게시물 클릭 시 PostDetailActivity로 이동
            onTravelLogClick(travelLog)
        }
        postsRecyclerView.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ProfileActivity)
            adapter = travelLogsAdapter
            setHasFixedSize(true) // 성능 향상
        }
        Log.d(TAG, "RecyclerView 설정 완료 - 초기 어댑터 아이템 수: ${travelLogsAdapter.itemCount}")
    }
    
    private fun fetchUserTravelLogs() {
        val userId = RetrofitClient.getUserId()
        Log.d(TAG, "사용자별 기록 조회 시작 - userId: $userId")
        RetrofitClient.apiService.getUserLogs(userId).enqueue(object : Callback<UserLogsResponse> {
            override fun onResponse(call: Call<UserLogsResponse>, response: Response<UserLogsResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val logs = response.body()?.data ?: emptyList()
                    Log.d(TAG, "사용자별 기록 조회 성공 - 기록 수: ${logs.size}")
                    postsCount.text = logs.size.toString()
                    
                    // 각 기록에 대해 상세 조회를 통해 실제 이미지 URL 가져오기
                    fetchDetailedTravelLogs(logs)
                } else {
                    Log.e(TAG, "사용자별 기록 조회 실패: ${response.code()}")
                    postsCount.text = "0"
                }
            }
            override fun onFailure(call: Call<UserLogsResponse>, t: Throwable) {
                Log.e(TAG, "사용자별 기록 조회 네트워크 오류", t)
                postsCount.text = "0"
            }
        })
    }
    
    private fun fetchDetailedTravelLogs(basicLogs: List<UserLogItem>) {
        Log.d(TAG, "상세 기록 조회 시작 - 총 ${basicLogs.size}개 기록")
        travelLogs.clear()
        
        var completedCount = 0
        val totalCount = basicLogs.size
        
        if (totalCount == 0) {
            updateRecyclerView()
            return
        }
        
        basicLogs.forEach { basicLog ->
            Log.d(TAG, "기록 상세 조회 - logId: ${basicLog.logId}")
            RetrofitClient.apiService.getLogDetail(basicLog.logId).enqueue(object : Callback<LogDetailApiResponse> {
                override fun onResponse(call: Call<LogDetailApiResponse>, response: Response<LogDetailApiResponse>) {
                    completedCount++
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val detailedLog = response.body()?.data
                        if (detailedLog != null) {
                            Log.d(TAG, "상세 조회 성공 - logId: ${detailedLog.logId}, 이미지 수: ${detailedLog.imageUrls.size}")
                            if (detailedLog.imageUrls.isNotEmpty()) {
                                Log.d(TAG, "실제 이미지 URL들: ${detailedLog.imageUrls}")
                            }
                            
                            // 상세 정보로 기본 정보 업데이트 (특정 장소 정보 포함)
                            val updatedLog = basicLog.copy(
                                imageUrls = detailedLog.imageUrls,
                                comment = detailedLog.comment,
                                likeCount = detailedLog.likeCount,
                                commentCount = detailedLog.commentCount,
                                isLiked = detailedLog.isLiked,
                                placeNames = listOf(detailedLog.placeName) // 특정 장소 이름으로 업데이트
                            )
                            travelLogs.add(updatedLog)
                        } else {
                            Log.w(TAG, "상세 조회 응답 데이터가 null - logId: ${basicLog.logId}")
                            travelLogs.add(basicLog) // 기본 정보 사용
                        }
                    } else {
                        Log.e(TAG, "상세 조회 실패 - logId: ${basicLog.logId}, code: ${response.code()}")
                        travelLogs.add(basicLog) // 기본 정보 사용
                    }
                    
                    // 모든 상세 조회가 완료되면 UI 업데이트
                    if (completedCount == totalCount) {
                        Log.d(TAG, "모든 상세 조회 완료 - 총 ${travelLogs.size}개 기록")
                        // logId 순서대로 정렬 (최신순)
                        travelLogs.sortByDescending { it.logId }
                        updateRecyclerView()
                    }
                }
                
                override fun onFailure(call: Call<LogDetailApiResponse>, t: Throwable) {
                    completedCount++
                    Log.e(TAG, "상세 조회 네트워크 오류 - logId: ${basicLog.logId}", t)
                    travelLogs.add(basicLog) // 기본 정보 사용
                    
                    // 모든 상세 조회가 완료되면 UI 업데이트
                    if (completedCount == totalCount) {
                        Log.d(TAG, "모든 상세 조회 완료 (일부 실패) - 총 ${travelLogs.size}개 기록")
                        travelLogs.sortByDescending { it.logId }
                        updateRecyclerView()
                    }
                }
            })
        }
    }
    
    private fun updateRecyclerView() {
        Log.d(TAG, "어댑터에 데이터 업데이트 - 총 ${travelLogs.size}개 아이템")
        Log.d(TAG, "어댑터 아이템 수: ${travelLogsAdapter.itemCount}")
        travelLogsAdapter.notifyDataSetChanged()
        Log.d(TAG, "notifyDataSetChanged() 호출 완료")
        
        // 데이터 로드 후 게시물이 보이도록 확인
        if (postsRecyclerView.visibility != View.VISIBLE) {
            postsRecyclerView.visibility = View.VISIBLE
            Log.d(TAG, "RecyclerView를 VISIBLE로 변경")
        }
        
        if (travelLogs.isEmpty()) {
            Log.d(TAG, "사용자 기록이 없습니다")
        } else {
            Log.d(TAG, "첫 번째 기록: ${travelLogs.first().comment}")
            Log.d(TAG, "RecyclerView 상태 - visibility: ${postsRecyclerView.visibility}, adapter: ${postsRecyclerView.adapter}, layoutManager: ${postsRecyclerView.layoutManager}")
        }
    }



    /**
     * 게시물 클릭 시 PostDetailActivity로 이동
     */
    private fun onTravelLogClick(travelLog: UserLogItem) {
        Log.d(TAG, "게시물 클릭됨 - logId: ${travelLog.logId}, 제목: ${travelLog.comment}")
        
        val intent = Intent(this, PostDetailActivity::class.java).apply {
            putExtra(PostDetailActivity.EXTRA_POST_ID, travelLog.logId.toLong())
        }
        
        Log.d(TAG, "PostDetailActivity 시작 - postId: ${travelLog.logId}")
        postDetailLauncher.launch(intent)
    }

    /**
     * 팔로워/팔로잉 수만 새로고침하는 메서드
     * 다른 화면에서 팔로우/언팔로우 후 돌아왔을 때 실시간 업데이트를 위해 사용
     */
    private fun refreshFollowCounts() {
        if (profileId > 0) {
            fetchFollowCounts(profileId)
        }
    }
}

// 사용자별 기록을 표시하는 어댑터
class TravelLogsAdapter(
    private val travelLogs: List<UserLogItem>,
    private val onItemClick: (UserLogItem) -> Unit
) : RecyclerView.Adapter<TravelLogsAdapter.TravelLogViewHolder>() {

    private val TAG = "TravelLogsAdapter"
    
    class TravelLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postImage: ImageView = view.findViewById(R.id.post_image)
        val placeName: TextView = view.findViewById(R.id.place_name)
        val placeAddress: TextView = view.findViewById(R.id.place_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelLogViewHolder {
        Log.d(TAG, "onCreateViewHolder 호출됨")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_post, parent, false)
        return TravelLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: TravelLogViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder 호출됨 - position: $position")
        val travelLog = travelLogs[position]
        
        // 장소명 표시 (특정 장소명 우선, 없으면 전체 장소 목록에서 첫 번째)
        val placeName = if (travelLog.placeNames.isNotEmpty()) {
            travelLog.placeNames.first()
        } else {
            travelLog.plan.title // 장소명이 없으면 계획 제목 사용
        }
        holder.placeName.text = placeName
        Log.d(TAG, "바인딩된 장소명: $placeName (from ${travelLog.placeNames.size} places)")
        
        // 주소 표시 (계획의 location 사용)
        holder.placeAddress.text = travelLog.plan.location
        Log.d(TAG, "바인딩된 주소: ${travelLog.plan.location}")
        
        // 이미지 표시 개선
        Log.d(TAG, "이미지 처리 시작 - imageUrls 크기: ${travelLog.imageUrls.size}")
        if (travelLog.imageUrls.isNotEmpty()) {
            Log.d(TAG, "이미지 URL 목록: ${travelLog.imageUrls}")
        }
        
        if (travelLog.imageUrls.isNotEmpty()) {
            // 실제 이미지 URL 찾기 (유효한 HTTP URL)
            val validImageUrl = travelLog.imageUrls.find { url ->
                url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
            }
            
            if (validImageUrl != null) {
                Log.d(TAG, "실제 이미지 로드: $validImageUrl")
                Glide.with(holder.itemView.context)
                    .load(validImageUrl)
                    .placeholder(R.drawable.ic_place_holder)
                    .error(R.drawable.ic_place_holder)
                    .centerCrop()
                    .override(600, 300) // 더 큰 해상도로 로드
                    .into(holder.postImage)
            } else {
                // 실제 이미지가 없으면 장소 기반 기본 이미지
                Log.d(TAG, "유효하지 않은 이미지 URL들 - 장소 기반 기본 이미지 사용")
                Log.d(TAG, "무효한 URL들: ${travelLog.imageUrls}")
                setDefaultImageBasedOnLocation(holder.postImage, travelLog.plan.location)
            }
        } else {
            Log.d(TAG, "이미지 URL 배열이 비어있음 - 장소 기반 기본 이미지 사용")
            setDefaultImageBasedOnLocation(holder.postImage, travelLog.plan.location)
        }
        
        // 아이템 클릭 이벤트 (기록 상세 보기)
        holder.itemView.setOnClickListener {
            Log.d(TAG, "기록 클릭 - logId: ${travelLog.logId}")
            onItemClick(travelLog)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount 호출됨 - 아이템 수: ${travelLogs.size}")
        return travelLogs.size
    }
    
    private fun setDefaultImageBasedOnLocation(imageView: ImageView, location: String) {
        val imageResource = getDefaultImageForLocation(location)
        
        Glide.with(imageView.context)
            .load(imageResource)
            .centerCrop()
            .into(imageView)
        
        Log.d(TAG, "장소 '$location'에 대한 기본 이미지 설정됨: $imageResource")
    }
    
    private fun getDefaultImageForLocation(location: String?): Int {
        return when {
            location?.contains("서울", ignoreCase = true) == true -> R.drawable.img_namsan
            location?.contains("부산", ignoreCase = true) == true -> R.drawable.img_haeundae
            location?.contains("경주", ignoreCase = true) == true -> R.drawable.img_bulguksa
            location?.contains("제주", ignoreCase = true) == true -> R.drawable.img_hallasan
            location?.contains("광안", ignoreCase = true) == true -> R.drawable.img_gwangan
            location?.contains("경복궁", ignoreCase = true) == true -> R.drawable.img_gyeongbokgung
            location?.contains("첨성대", ignoreCase = true) == true -> R.drawable.img_cheomseongdae
            location?.contains("성산", ignoreCase = true) == true -> R.drawable.img_seongsan
            else -> R.drawable.ic_place_holder
        }
    }
} 