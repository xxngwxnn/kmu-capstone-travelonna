package com.example.travelonna

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.adapter.FollowAdapter
import com.example.travelonna.model.User
import com.google.android.material.tabs.TabLayout

class FollowListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var tvSectionTitle: TextView
    private lateinit var tvTitle: TextView
    
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_list)
        
        recyclerView = findViewById(R.id.recyclerView)
        tabLayout = findViewById(R.id.tabLayout)
        tvSectionTitle = findViewById(R.id.tvSectionTitle)
        tvTitle = findViewById(R.id.tvTitle)
        
        // 뒤로가기 버튼
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        // Intent에서 타입 가져오기 (followers 또는 following)
        val type = intent.getStringExtra("type") ?: "followers"
        isFollowerTab = type == "followers"
        
        // 초기 탭 선택
        tabLayout.getTabAt(if (isFollowerTab) 0 else 1)?.select()
        
        // 초기 데이터 설정
        if (isFollowerTab) {
            setupRecyclerView(followers)
            tvSectionTitle.text = "모든 팔로워"
        } else {
            setupRecyclerView(following)
            tvSectionTitle.text = "모든 팔로잉"
        }
        
        // 탭 선택 리스너
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        isFollowerTab = true
                        setupRecyclerView(followers)
                        tvSectionTitle.text = "모든 팔로워"
                    }
                    1 -> {
                        isFollowerTab = false
                        setupRecyclerView(following)
                        tvSectionTitle.text = "모든 팔로잉"
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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