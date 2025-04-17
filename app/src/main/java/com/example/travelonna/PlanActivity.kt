package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Rect
import com.example.travelonna.adapter.PlanAdapter
import com.example.travelonna.adapter.PlaceRecommendAdapter
import com.example.travelonna.adapter.PlaceRecommendItem
import com.example.travelonna.api.PlanListResponse
import com.example.travelonna.api.PlanData
import com.example.travelonna.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class PlanActivity : AppCompatActivity() {
    
    private lateinit var planAdapter: PlanAdapter
    private val TAG = "PlanActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)

        // 뒤로가기 버튼 설정
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // 추천 장소 데이터 설정
        val recommendPlaces = listOf(
            PlaceRecommendItem(R.drawable.dummy_place_1, "장소 이름"),
            PlaceRecommendItem(R.drawable.dummy_place_1, "장소 이름"),
            PlaceRecommendItem(R.drawable.dummy_place_1, "장소 이름")
        )

        // 추천 장소 RecyclerView 설정
        val placeRecommendRecyclerView = findViewById<RecyclerView>(R.id.placeRecommendRecyclerView)
        placeRecommendRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        placeRecommendRecyclerView.adapter = PlaceRecommendAdapter(recommendPlaces)

        // 일정 목록 RecyclerView 설정
        val scheduleRecyclerView = findViewById<RecyclerView>(R.id.scheduleRecyclerView)
        scheduleRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // 아이템 간 간격 없애기
        scheduleRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.set(0, 0, 0, 0)
            }
        })
        
        // 어댑터 초기화 (빈 목록으로 시작)
        planAdapter = PlanAdapter(emptyList())
        scheduleRecyclerView.adapter = planAdapter
        
        // 아이템 클릭 리스너 설정
        planAdapter.setOnItemClickListener { plan ->
            val intent = Intent(this, ScheduleDetailActivity::class.java)
            intent.putExtra("PLAN_ID", plan.planId.toInt())
            
            // 날짜 정보도 전달
            val startDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = startDateFormat.parse(plan.startDate)?.time ?: System.currentTimeMillis()
            val endDate = startDateFormat.parse(plan.endDate)?.time ?: System.currentTimeMillis()
            
            intent.putExtra("START_DATE", startDate)
            intent.putExtra("END_DATE", endDate)
            intent.putExtra("SCHEDULE_NAME", plan.title)
            
            startActivity(intent)
        }
        
        // API에서 일정 목록 가져오기
        fetchPlans()
    }
    
    private fun fetchPlans() {
        // 하드코딩된 토큰을 사용 (실제로는 저장된 토큰을 사용해야 함)
        val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiZW9kdWVsY2hpMDVAZ21haWwuY29tIiwidXNlcl9pZCI6MTIsImlhdCI6MTc0NDY5MzM3OSwiZXhwIjoxNzQ0Njk2OTc5fQ.RehACK7RKot0bx0ZcF1MUUfPZ4OwxQaXkjZhhyqnX30"
        
        val authorization = "Bearer $token"
        RetrofitClient.apiService.getPlans(authorization).enqueue(object : Callback<PlanListResponse> {
            override fun onResponse(call: Call<PlanListResponse>, response: Response<PlanListResponse>) {
                if (response.isSuccessful) {
                    val planResponse = response.body()
                    if (planResponse != null && planResponse.success) {
                        val plans = planResponse.data
                        Log.d(TAG, "Plans loaded: ${plans.size}")
                        planAdapter.updateData(plans)
                    } else {
                        Log.e(TAG, "Response not successful: ${response.code()}")
                        Toast.makeText(this@PlanActivity, "일정을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Response error: ${response.code()} - ${response.message()}")
                    Toast.makeText(this@PlanActivity, "오류: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<PlanListResponse>, t: Throwable) {
                Log.e(TAG, "API call failed", t)
                Toast.makeText(this@PlanActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
