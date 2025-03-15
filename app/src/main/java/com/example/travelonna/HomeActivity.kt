package com.example.travelonna

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.adapter.PostAdapter
import com.example.travelonna.model.Post

class HomeActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // 더미 데이터 초기화
        val dummyPosts = listOf(
            Post(
                id = 1L,
                imageResource = R.drawable.main_dummy_1,
                userName = "Pro_traveler_kr",
                isFollowing = true,
                description = "지난 주에 일본 가벼지만을 다녀왔어요~ 모던하면서도 자분하고 깔끔한...",
                date = "2025.08.17"
            ),
            Post(
                id = 2L,
                imageResource = R.drawable.main_dummy_2,
                userName = "Seoul_explorer",
                isFollowing = false,
                description = "서울 야경이 정말 아름다웠어요! 다음에는 남산타워도 가볼 예정입니다.",
                date = "2025.08.15"
            ),
            Post(
                id = 3L,
                imageResource = R.drawable.main_dummy_1, // 재사용
                userName = "Travel_with_me",
                isFollowing = false,
                description = "제주도 여행 추천 코스를 공유합니다. 숨겨진 명소들만 모아봤어요!",
                date = "2025.08.10"
            )
        )
        
        // RecyclerView 설정
        val recyclerView = findViewById<RecyclerView>(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = PostAdapter(dummyPosts)
    }
} 