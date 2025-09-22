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
import com.example.travelonna.api.BasicResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.textfield.TextInputEditText
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat

class PlanActivity : AppCompatActivity() {
    
    private lateinit var planAdapter: PlanAdapter
    private val TAG = "PlanActivity"
    private val allPlans = mutableListOf<PlanData>() // 모든 일정을 저장할 리스트
    private lateinit var scheduleRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)
        
        // 하단 네비게이션 바 설정
        setupBottomNavBar(R.id.navPlan)

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

        // 일정 목록 RecyclerView 설정
        scheduleRecyclerView = findViewById<RecyclerView>(R.id.scheduleRecyclerView)
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
        
        // 아이템 삭제 리스너 설정
        planAdapter.setOnItemDeleteListener { plan, position ->
            deletePlan(plan.planId.toInt(), position)
        }
        
        // 스와이프-투-딜리트 설정
        setupSwipeToDelete()
        
        // API에서 일정 목록 가져오기
        loadAllPlans()
    }
    
    // 스와이프-투-딜리트 설정
    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, // 드래그 방향 (비활성화)
            ItemTouchHelper.LEFT // 왼쪽으로 스와이프하여 삭제
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }
            
            // 짧은 스와이프로도 삭제 다이얼로그가 표시되도록 설정
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.15F
            }
            
            override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                return defaultValue * 0.5f
            }
            
            override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
                return defaultValue * 0.5f
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val plan = allPlans[position]
                
                // 삭제 확인 다이얼로그 표시
                val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this@PlanActivity)
                    .setTitle("일정 삭제")
                    .setMessage("'${plan.title}' 일정을 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        // 삭제 요청
                        planAdapter.deleteSchedule(position)
                    }
                    .setNegativeButton("취소") { _, _ ->
                        // 스와이프 취소하고 아이템 복원
                        planAdapter.notifyItemChanged(position)
                    }
                    .setCancelable(false)
                    .create()
                    
                alertDialog.show()
            }
            
            // 고정된 짧은 거리만 스와이프되도록 제한
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    
                    // 스와이프 최대 거리 제한 (아이템 너비의 1/4로 제한)
                    val maxSwipeDistance = -(itemView.width / 4f)
                    val limitedDX = Math.max(dX, maxSwipeDistance)
                    
                    // 배경 색상 설정 (빨간색)
                    val background = Paint()
                    background.color = ContextCompat.getColor(this@PlanActivity, R.color.delete_red)
                    
                    // 삭제 아이콘 설정
                    val deleteIcon = ContextCompat.getDrawable(
                        this@PlanActivity, 
                        android.R.drawable.ic_menu_delete
                    )
                    
                    // 아이콘 위치와 크기 계산
                    val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                    val iconBottom = iconTop + deleteIcon.intrinsicHeight
                    
                    // 왼쪽으로 스와이프할 때
                    if (limitedDX < 0) {
                        // 아이콘 위치 재조정 - 스와이프 영역의 중앙에 배치
                        val swipeAreaWidth = -maxSwipeDistance // 스와이프 영역 너비
                        val iconWidth = deleteIcon.intrinsicWidth
                        
                        // 아이콘을 스와이프 영역 중앙에 배치
                        val iconLeft = itemView.right - (swipeAreaWidth / 2) - (iconWidth / 2)
                        val iconRight = iconLeft + iconWidth
                        
                        deleteIcon.setBounds(iconLeft.toInt(), iconTop, iconRight.toInt(), iconBottom)
                        
                        // 배경 그리기
                        c.drawRect(
                            itemView.right + limitedDX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(),
                            background
                        )
                        
                        // 아이콘 그리기
                        deleteIcon.draw(c)
                    }
                    
                    super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive)
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        })
        
        itemTouchHelper.attachToRecyclerView(scheduleRecyclerView)
    }
    
    // 일정 삭제 API 호출
    private fun deletePlan(planId: Int, position: Int) {
        val loadingToast = Toast.makeText(this, "삭제 중...", Toast.LENGTH_SHORT)
        loadingToast.show()
        
        RetrofitClient.apiService.deletePlan(planId)
            .enqueue(object : Callback<BasicResponse> {
                override fun onResponse(
                    call: Call<BasicResponse>,
                    response: Response<BasicResponse>
                ) {
                    loadingToast.cancel()
                    
                    if (response.isSuccessful) {
                        Log.d(TAG, "Successfully deleted plan: planId=$planId")
                        // 어댑터에서 아이템 삭제
                        planAdapter.removeItem(position)
                        // 로컬 리스트에서도 제거
                        if (position < allPlans.size) {
                            allPlans.removeAt(position)
                        }
                        Toast.makeText(this@PlanActivity, "일정이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Failed to delete plan: ${response.code()}, ${response.message()}")
                        Toast.makeText(this@PlanActivity, "일정 삭제에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        
                        // 삭제 실패 시 목록 갱신하여 항목 복원
                        planAdapter.notifyItemChanged(position)
                    }
                }
                
                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    loadingToast.cancel()
                    Log.e(TAG, "Network error when deleting plan", t)
                    Toast.makeText(this@PlanActivity, "네트워크 오류: 일정 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
                    
                    // 삭제 실패 시 목록 갱신하여 항목 복원
                    planAdapter.notifyItemChanged(position)
                }
            })
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
            
            // API 호출로 그룹 참여 처리
            joinGroup(groupUrl)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun joinGroup(urlCode: String) {
        RetrofitClient.apiService.joinGroup(urlCode)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PlanActivity, "그룹에 참여했습니다", Toast.LENGTH_SHORT).show()
                        loadAllPlans() // 일정 목록 새로고침
                    } else {
                        Toast.makeText(this@PlanActivity, "그룹 참여에 실패했습니다. 코드를 확인해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@PlanActivity, "네트워크 오류: 그룹 참여에 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            })
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
        findViewById<ImageButton>(selectedId).isSelected = true

        navHome.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, HomeActivity::class.java))
        }
        navMap.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, MyMapActivity::class.java))
        }
        navPlan.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, PlanActivity::class.java))
        }
        navSearch.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, SearchActivity::class.java))
        }
        navProfile.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
