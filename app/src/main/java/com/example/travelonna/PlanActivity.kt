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
import com.example.travelonna.api.PlanDetailResponse
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.GroupInfoResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText

class PlanActivity : AppCompatActivity() {
    
    private lateinit var planAdapter: PlanAdapter
    private val TAG = "PlanActivity"
    private val allPlans = mutableListOf<PlanData>() // 모든 일정을 저장할 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)

        // 뒤로가기 버튼 설정
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // 일정 작업 버튼 설정 (생성/참여)
        val editButton = findViewById<ImageButton>(R.id.editButton)
        editButton.setOnClickListener {
            // 일정 옵션 다이얼로그 표시
            showScheduleOptionsDialog()
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
        loadAllPlans()
    }
    
    // 모든 일정 데이터 로드 (내 일정 + 그룹 일정)
    private fun loadAllPlans() {
        allPlans.clear() // 기존 데이터 초기화
        
        // 내 일정과 참여 중인 그룹 일정을 모두 가져옴
        fetchPlans()
        fetchMyGroups()
    }
    
    // 내 일정 목록 가져오기
    private fun fetchPlans() {
        val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiZW9kdWVsY2hpMDVAZ21haWwuY29tIiwidXNlcl9pZCI6MTIsImlhdCI6MTc0NDY5MzM3OSwiZXhwIjoxNzQ0Njk2OTc5fQ.RehACK7RKot0bx0ZcF1MUUfPZ4OwxQaXkjZhhyqnX30"
        
        val authorization = "Bearer $token"
        RetrofitClient.apiService.getPlans(authorization).enqueue(object : Callback<PlanListResponse> {
            override fun onResponse(call: Call<PlanListResponse>, response: Response<PlanListResponse>) {
                if (response.isSuccessful) {
                    val planResponse = response.body()
                    if (planResponse != null && planResponse.success) {
                        val plans = planResponse.data
                        Log.d(TAG, "My plans loaded: ${plans.size}")
                        
                        // 내 일정 데이터 추가
                        allPlans.addAll(plans)
                        updatePlanAdapter()
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
    
    // 참여 중인 그룹 일정 가져오기
    private fun fetchMyGroups() {
        RetrofitClient.apiService.getMyGroups().enqueue(object : Callback<List<GroupInfoResponse>> {
            override fun onResponse(call: Call<List<GroupInfoResponse>>, response: Response<List<GroupInfoResponse>>) {
                if (response.isSuccessful) {
                    val groups = response.body()
                    if (groups != null && groups.isNotEmpty()) {
                        Log.d(TAG, "My group plans loaded: ${groups.size}")
                        
                        // 그룹 정보 로그에 출력
                        groups.forEach { group ->
                            Log.d(TAG, "Group: id=${group.id}, url=${group.url}, createdDate=${group.createdDate}")
                        }
                        
                        // 향후 백엔드에서 planId가 포함된 응답을 주면 이 부분을 수정할 예정
                        Toast.makeText(this@PlanActivity, "참여 중인 그룹: ${groups.size}개", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "No group plans found")
                    }
                } else {
                    Log.e(TAG, "Failed to fetch my groups: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading error body", e)
                    }
                }
            }
            
            override fun onFailure(call: Call<List<GroupInfoResponse>>, t: Throwable) {
                Log.e(TAG, "Failed to fetch my groups", t)
            }
        })
    }
    
    // 어댑터 업데이트
    private fun updatePlanAdapter() {
        // 최신순으로 정렬
        val sortedPlans = allPlans.sortedByDescending { it.createdAt }
        planAdapter.updateData(sortedPlans)
    }
    
    /**
     * 일정 옵션 다이얼로그를 표시합니다 (일정 생성/일정 참여 선택)
     */
    private fun showScheduleOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_schedule_options, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 원형 테두리 적용
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 일정 생성 버튼 클릭 이벤트
        dialogView.findViewById<LinearLayout>(R.id.btnCreateSchedule).setOnClickListener {
            dialog.dismiss()
            // 일정 생성 화면으로 이동
            val intent = Intent(this, ScheduleCreateActivity::class.java)
            startActivity(intent)
        }
        
        // 일정 참여 버튼 클릭 이벤트
        dialogView.findViewById<LinearLayout>(R.id.btnJoinSchedule).setOnClickListener {
            dialog.dismiss()
            // 그룹 URL 입력 다이얼로그 표시
            showGroupUrlInputDialog()
        }
        
        dialog.show()
    }
    
    /**
     * 그룹 URL 입력 다이얼로그를 표시합니다
     */
    private fun showGroupUrlInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_group_url_input, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // 원형 테두리 적용
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val edtGroupUrl = dialogView.findViewById<TextInputEditText>(R.id.edtGroupUrl)
        
        // 취소 버튼 클릭 이벤트
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        // 참여 버튼 클릭 이벤트
        dialogView.findViewById<Button>(R.id.btnJoin).setOnClickListener {
            val groupUrl = edtGroupUrl.text.toString().trim()
            
            if (groupUrl.isEmpty()) {
                Toast.makeText(this, "그룹 URL을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            dialog.dismiss()
            
            // URL 코드 추출 - URL 전체 또는 코드만 입력 가능
            val urlCode = extractUrlCode(groupUrl)
            
            if (urlCode.isNotEmpty()) {
                joinGroupSchedule(urlCode)
            } else {
                Toast.makeText(this, "유효하지 않은 그룹 URL입니다", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
    
    /**
     * URL에서 코드 부분만 추출합니다
     */
    private fun extractUrlCode(input: String): String {
        return try {
            // URL 전체가 입력된 경우 (https://travelonna.shop/group/9586836d)
            if (input.contains("/group/")) {
                val parts = input.split("/group/")
                parts.last()
            } 
            // "공유 URL: 9586836d" 형식으로 입력된 경우
            else if (input.contains("공유 URL:")) {
                val parts = input.split("공유 URL:")
                parts.last().trim()
            }
            // 코드만 입력된 경우
            else {
                input
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting URL code", e)
            ""
        }
    }
    
    /**
     * 그룹 일정에 참여합니다
     */
    private fun joinGroupSchedule(urlCode: String) {
        // 로딩 표시
        val loadingDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
            
        // 원형 테두리 적용
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.show()
        
        // API 호출로 그룹 참여
        Log.d(TAG, "Joining group with URL code: $urlCode")
        
        RetrofitClient.apiService.joinGroup(urlCode)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    loadingDialog.dismiss()
                    
                    Log.d(TAG, "Join group API response: ${response.code()}")
                    
                    if (response.isSuccessful) {
                        Log.d(TAG, "Successfully joined group with code: $urlCode")
                        
                        Toast.makeText(this@PlanActivity, "그룹 일정에 참여했습니다", Toast.LENGTH_SHORT).show()
                        
                        // 일정 목록 새로고침 (내 일정 + 참여 그룹 일정)
                        loadAllPlans()
                    } else {
                        val errorMsg = try {
                            response.errorBody()?.string() ?: "일정 참여에 실패했습니다"
                        } catch (e: Exception) {
                            "일정 참여에 실패했습니다"
                        }
                        
                        Log.e(TAG, "Failed to join group: $errorMsg")
                        Toast.makeText(this@PlanActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Network error when joining group", t)
                    Toast.makeText(this@PlanActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
