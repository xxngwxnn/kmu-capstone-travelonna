package com.example.travelonna

import android.content.Intent
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var backButton: ImageButton
    private lateinit var notificationButton: ImageButton
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
    private lateinit var travelLogsAdapter: TravelLogsAdapter
    private var travelLogs: MutableList<TravelLogData> = mutableListOf()
    private var currentProfileData: ProfileResponse? = null
    
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
    
    override fun onResume() {
        super.onResume()
        // 프로필 편집 후 돌아왔을 때 데이터 새로고침
        fetchProfileData()
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.back_button)
        notificationButton = findViewById(R.id.notification_button)
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
        
        // API 호출
        RetrofitClient.apiService.getUserProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                showLoading(false)
                
                if (response.isSuccessful) {
                    val profileData = response.body()
                    if (profileData != null) {
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
    
    private fun setupRecyclerView() {
        travelLogsAdapter = TravelLogsAdapter(travelLogs)
        postsRecyclerView.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ProfileActivity) // 한 줄에 하나씩
            adapter = travelLogsAdapter
        }
    }
    
    private fun fetchUserTravelLogs() {
        val userId = RetrofitClient.getUserId()
        Log.d(TAG, "사용자별 기록 조회 시작 - userId: $userId")
        
        RetrofitClient.apiService.getTravelLogsByUser(userId).enqueue(object : Callback<TravelLogListResponse> {
            override fun onResponse(call: Call<TravelLogListResponse>, response: Response<TravelLogListResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val logs = response.body()?.data ?: emptyList()
                    Log.d(TAG, "사용자별 기록 조회 성공 - 기록 수: ${logs.size}")
                    
                    // 기록 수 업데이트
                    postsCount.text = logs.size.toString()
                    
                    // RecyclerView 업데이트
                    travelLogs.clear()
                    travelLogs.addAll(logs)
                    travelLogsAdapter.notifyDataSetChanged()
                    
                    if (logs.isEmpty()) {
                        // 기록이 없는 경우 메시지 표시 (필요시)
                        Log.d(TAG, "사용자 기록이 없습니다")
                    }
                } else {
                    Log.e(TAG, "사용자별 기록 조회 실패: ${response.code()}")
                    postsCount.text = "0"
                }
            }
            
            override fun onFailure(call: Call<TravelLogListResponse>, t: Throwable) {
                Log.e(TAG, "사용자별 기록 조회 네트워크 오류", t)
                postsCount.text = "0"
            }
        })
    }
}

// 사용자별 기록을 표시하는 어댑터
class TravelLogsAdapter(private val travelLogs: List<TravelLogData>) : 
    RecyclerView.Adapter<TravelLogsAdapter.TravelLogViewHolder>() {

    class TravelLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postImage: ImageView = view.findViewById(R.id.post_image)
        val placeName: TextView = view.findViewById(R.id.place_name)
        val placeAddress: TextView = view.findViewById(R.id.place_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_post, parent, false)
        return TravelLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: TravelLogViewHolder, position: Int) {
        val travelLog = travelLogs[position]
        
        // 장소명 표시 (placeNames에서 첫 번째 장소 사용)
        val placeName = if (travelLog.placeNames.isNotEmpty()) {
            travelLog.placeNames.first()
        } else {
            travelLog.plan.title // 장소명이 없으면 계획 제목 사용
        }
        holder.placeName.text = placeName
        
        // 주소 표시 (계획의 location 사용)
        holder.placeAddress.text = travelLog.plan.location
        
        // 이미지 표시 (imageUrls에서 첫 번째 이미지 사용)
        if (travelLog.imageUrls.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(travelLog.imageUrls.first())
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.postImage)
        } else {
            holder.postImage.setImageResource(R.drawable.ic_launcher_background)
        }
        
        // 아이템 클릭 이벤트 (기록 상세 보기)
        holder.itemView.setOnClickListener {
            // TODO: 기록 상세 화면으로 이동
            Log.d("TravelLogsAdapter", "기록 클릭 - logId: ${travelLog.logId}")
        }
    }

    override fun getItemCount() = travelLogs.size
} 