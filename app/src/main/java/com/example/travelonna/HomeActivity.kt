package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.adapter.MainPostAdapter
import com.example.travelonna.model.Post
import com.example.travelonna.util.PostManager

class HomeActivity : AppCompatActivity(), PostManager.PostUpdateListener {
    
    private lateinit var adapter: MainPostAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // 검색 아이콘 클릭 리스너 설정
        findViewById<ImageView>(R.id.searchIcon).setOnClickListener {
            // 검색 화면으로 이동
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
        
        // 더미 데이터 초기화
        val dummyPosts = listOf(
            Post(
                id = 1L,
                imageResource = R.drawable.main_dummy_1,
                userName = "Pro_traveler_kr",
                isFollowing = true,
                description = "지난 주에 일본 가벼지만을 다녀왔어요~ 모던하면서도 자분하고 깔끔한...",
                date = "2025.08.17",
                isLiked = false,
                likeCount = 125,
                commentCount = 23
            ),
            Post(
                id = 2L,
                imageResource = R.drawable.main_dummy_2,
                userName = "Seoul_explorer",
                isFollowing = false,
                description = "서울 야경이 정말 아름다웠어요! 다음에는 남산타워도 가볼 예정입니다.",
                date = "2025.08.15",
                isLiked = true,
                likeCount = 87,
                commentCount = 15
            ),
            Post(
                id = 3L,
                imageResource = R.drawable.main_dummy_1, // 재사용
                userName = "Travel_with_me",
                isFollowing = false,
                description = "제주도 여행 추천 코스를 공유합니다. 숨겨진 명소들만 모아봤어요!",
                date = "2025.08.10",
                isLiked = false,
                likeCount = 203,
                commentCount = 42
            )
        )
        
        // PostManager에 데이터 초기화
        PostManager.initializePosts(dummyPosts)
        
        // RecyclerView 설정
        val recyclerView = findViewById<RecyclerView>(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        
        adapter = MainPostAdapter(PostManager.getAllPosts())
        recyclerView.adapter = adapter
        
        // PostManager 리스너 등록
        PostManager.addListener(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 메모리 누수 방지를 위해 리스너 제거
        PostManager.removeListener(this)
    }
    
    override fun onPostUpdated(post: Post) {
        // 게시물이 업데이트되면 어댑터에 반영
        runOnUiThread {
            adapter.updateData(PostManager.getAllPosts())
        }
    }
    
    override fun onCommentsUpdated(postId: Long, comments: List<com.example.travelonna.model.Comment>) {
        // 댓글이 업데이트되면 어댑터에 반영 (댓글 수가 변경됨)
        runOnUiThread {
            adapter.updateData(PostManager.getAllPosts())
        }
    }
} 