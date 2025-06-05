package com.example.travelonna

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.widget.HorizontalScrollView
import android.content.Intent
import android.widget.Toast
import com.example.travelonna.ui.schedule.AddPlaceActivity
import com.example.travelonna.ui.schedule.PlaceInfoActivity
import androidx.recyclerview.widget.ItemTouchHelper
import android.util.Log
import com.example.travelonna.api.PlanDetailResponse
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.GroupInfoResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.PlacesClient
import android.widget.ImageButton
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.app.Dialog

class ScheduleDetailActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var addNewPlaceButton: TextView
    private lateinit var confirmButton: TextView
    private lateinit var shareButton: ImageButton
    
    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance()
    private var dayCount = 0
    private var planId = 0
    
    // 액티비티 레벨에서 Places 클라이언트를 공유
    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_detail)
        
        // Places API 초기화 - 한 번만 실행
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)
        
        // 뷰 초기화
        viewPager = findViewById(R.id.viewPager)
        addNewPlaceButton = findViewById(R.id.addNewPlaceButton)
        confirmButton = findViewById(R.id.confirmButton)
        shareButton = findViewById(R.id.shareButton)
        
        // 인텐트에서 날짜 데이터 가져오기
        val startDate = intent.getLongExtra("START_DATE", System.currentTimeMillis())
        val endDate = intent.getLongExtra("END_DATE", System.currentTimeMillis())
        val scheduleName = intent.getStringExtra("SCHEDULE_NAME") ?: "일정"
        planId = intent.getIntExtra("PLAN_ID", 0)
        
        // planId 확인을 위한 로그 추가
        Log.d("ScheduleDetail", "Received Plan ID: $planId from intent with extras: ${intent.extras}")
        if (planId <= 0) {
            Log.w("ScheduleDetail", "Invalid Plan ID received: $planId")
        }
            
        startDateCalendar.timeInMillis = startDate
        endDateCalendar.timeInMillis = endDate
            
        // 일정 이름과 날짜 표시
        val titleText = findViewById<TextView>(R.id.titleText)
        val dateRangeText = findViewById<TextView>(R.id.dateRangeText)
        
        titleText.text = scheduleName
        
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
            val startDateStr = dateFormat.format(startDateCalendar.time)
            val endDateStr = dateFormat.format(endDateCalendar.time)
        dateRangeText.text = "$startDateStr - $endDateStr"
        
        // 뒤로가기 버튼 설정
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
                                
        // 총 일수 계산
        val diffInMillis = endDateCalendar.timeInMillis - startDateCalendar.timeInMillis
        dayCount = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt() + 1
        
        // ViewPager 설정
        viewPager.adapter = DayPagerAdapter(this, dayCount, startDateCalendar)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabSelection(position)
            }
        })
        
        // 커스텀 탭 설정
        setupCustomTabs()
        
        // 장소 추가 버튼 리스너
        addNewPlaceButton.setOnClickListener {
            try {
                val intent = Intent(this@ScheduleDetailActivity, PlaceInfoActivity::class.java)
                intent.putExtra("SELECTED_DAY", viewPager.currentItem)
                intent.putExtra("START_DATE", startDateCalendar.timeInMillis)
                intent.putExtra("END_DATE", endDateCalendar.timeInMillis)
                intent.putExtra("SCHEDULE_NAME", scheduleName)
                intent.putExtra("PLAN_ID", planId)
                
                // 디버그를 위한 로그 추가
                Log.d("ScheduleDetail", "Sending Plan ID: $planId to PlaceInfoActivity")
                
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                Toast.makeText(this, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
        
        // 최초 로드 시 일정 정보 가져오기 (planId가 유효한 경우)
        if (planId > 0) {
            Log.d("ScheduleDetail", "Initial loading of plan details: planId=$planId, scheduleName=$scheduleName")
            fetchPlanDetail(planId)
        } else {
            Log.d("ScheduleDetail", "No valid planId provided ($planId), skipping plan detail fetch")
        }
        
        // 완료 버튼 리스너
        confirmButton.setOnClickListener {
            // 일정 완료 화면으로 이동
            val intent = Intent(this, ScheduleCompleteActivity::class.java)
            startActivity(intent)
            finish() // 현재 화면 종료
        }

        // 헤더 클릭 리스너 설정
        setupHeaderClickListener()
    }
    
    // 결과 처리를 위한 onActivityResult 수정
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val selectedDay = data.getIntExtra("SELECTED_DAY", 0)
            
            // 현재 뷰페이저 위치가 선택한 날짜와 다르면 해당 날짜로 이동
            if (viewPager.currentItem != selectedDay) {
                viewPager.currentItem = selectedDay
            }
            
            // API를 통해 장소가 추가된 경우
            if (data.getBooleanExtra("PLACE_ADDED", false)) {
                Toast.makeText(this, "장소가 추가되었습니다", Toast.LENGTH_SHORT).show()
                // 장소 추가 후 일정 상세 정보 다시 로드
                if (planId > 0) {
                    Log.d("ScheduleDetail", "Refreshing plan details after adding new place: planId=$planId, selectedDay=$selectedDay")
                    fetchPlanDetail(planId)
                        } else {
                    Log.d("ScheduleDetail", "Cannot refresh plan details: Invalid planId=$planId")
                }
                return
            }
            
            // 선택된 장소 정보 처리 (로컬 로직) - API 사용 시 제거 가능
            val placeName = data.getStringExtra("PLACE_NAME") ?: ""
            val placeAddress = data.getStringExtra("PLACE_ADDRESS") ?: ""
            val placeLat = data.getDoubleExtra("PLACE_LAT", 0.0)
            val placeLng = data.getDoubleExtra("PLACE_LNG", 0.0)
            val estimatedCost = data.getStringExtra("ESTIMATED_COST") ?: "0"
            val memo = data.getStringExtra("MEMO") ?: ""
            val isPublic = data.getBooleanExtra("IS_PUBLIC", false)
            
            // 새로운 장소 아이템 생성
            val newPlace = PlaceItem(
                name = placeName,
                address = placeAddress,
                imageResId = R.drawable.dummy_place_1, // 임시 이미지
                cost = estimatedCost,
                memo = memo,
                isPublic = isPublic
            )
            
            // 현재 표시 중인 날짜의 RecyclerView에 장소 추가
            val recyclerView = findViewById<ViewPager2>(R.id.viewPager)
                .getChildAt(0) as RecyclerView
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(selectedDay) as? DayPagerAdapter.DayPageHolder
            
            viewHolder?.let {
                val adapter = it.recyclerView.adapter as PlaceAdapter
                val placesList = adapter.places
                placesList.add(newPlace)
                adapter.notifyItemInserted(placesList.size - 1)
            }
            
            Toast.makeText(this, "장소가 추가되었습니다: $placeName", Toast.LENGTH_SHORT).show()
        }
        // 장소 편집 결과 처리 (요청 코드 200)
        else if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            // API를 통해 장소가 수정된 경우
            if (data.getBooleanExtra("PLACE_UPDATED", false) || data.getBooleanExtra("PLACE_ADDED", false)) {
                val selectedDay = data.getIntExtra("SELECTED_DAY", viewPager.currentItem)
                
                // 현재 뷰페이저 위치가 선택한 날짜와 다르면 해당 날짜로 이동
                if (viewPager.currentItem != selectedDay) {
                    viewPager.currentItem = selectedDay
                }
                
                Toast.makeText(this, "장소 정보가 업데이트되었습니다", Toast.LENGTH_SHORT).show()
                
                // 장소 정보 업데이트 후 일정 상세 정보 다시 로드
                if (planId > 0) {
                    Log.d("ScheduleDetail", "Refreshing plan details after updating place: planId=$planId")
                    fetchPlanDetail(planId)
                } else {
                    Log.d("ScheduleDetail", "Cannot refresh plan details: Invalid planId=$planId")
                }
            }
        }
    }
    
    // 드래그 앤 드롭 헬퍼 설정
    private fun setupItemTouchHelper(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // 드래그 방향
        ItemTouchHelper.LEFT  // 왼쪽으로 스와이프하여 삭제
    ) {
        private var dragFrom = -1
        private var dragTo = -1
        
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

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val adapter = recyclerView.adapter as PlaceAdapter
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition

            if (dragFrom == -1) {
                dragFrom = fromPosition
            }
            dragTo = toPosition

            adapter.moveItem(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val adapter = recyclerView.adapter as PlaceAdapter
            val place = adapter.places[position]
            
            // 삭제 확인 다이얼로그 표시
            val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this@ScheduleDetailActivity)
                .setTitle("장소 삭제")
                .setMessage("'${place.name}' 장소를 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    // 실제 삭제 수행
                    adapter.removeItem(position)
                }
                .setNegativeButton("취소") { _, _ ->
                    // 스와이프 취소하고 아이템 복원
                    adapter.notifyItemChanged(position)
                }
                .setCancelable(false)
                .create()
                
            alertDialog.show()
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true  // 롱 프레스로 드래그 활성화
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            
            if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                // 드래그가 끝났을 때 전체 리스트 갱신
                val adapter = recyclerView.adapter as PlaceAdapter
                
                // 순서가 변경된 경우 서버에 업데이트 요청
                if (planId > 0) {
                    adapter.updatePlaceOrdersOnServer()
                    Log.d("ItemTouchHelper", "Drag completed from $dragFrom to $dragTo, updating orders on server")
                } else {
                    Log.d("ItemTouchHelper", "Drag completed locally, plan ID not valid: $planId")
                }
                
                recyclerView.adapter?.notifyDataSetChanged()
                }
                
                // 드래그 위치 초기화
                dragFrom = -1
                dragTo = -1
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
                background.color = ContextCompat.getColor(this@ScheduleDetailActivity, R.color.delete_red)
                
                // 삭제 아이콘 설정
                val deleteIcon = ContextCompat.getDrawable(
                    this@ScheduleDetailActivity, 
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
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
    
    // 장소 목록 어댑터
    inner class PlaceAdapter(
        internal val places: MutableList<PlaceItem>
    ) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {
        
        init {
            // Places 초기화 코드 제거 - 액티비티 수준에서 관리
        }
        
        fun updatePlaces(newPlaces: List<PlaceItem>) {
            places.clear()
            places.addAll(newPlaces)
            notifyDataSetChanged()
        }
        
        // 서버에 모든 장소의 순서를 업데이트하는 메서드
        fun updatePlaceOrdersOnServer() {
            if (planId <= 0) return  // 유효한 planId가 없으면 실행 안함
            
            val selectedDay = viewPager.currentItem + 1  // 현재 선택된 날짜 (1부터 시작)
            
            // 로딩 표시
            val loadingToast = Toast.makeText(this@ScheduleDetailActivity, "순서 업데이트 중...", Toast.LENGTH_SHORT)
            loadingToast.show()
            
            var successCount = 0
            var failCount = 0
            val totalItems = places.size
            
            // 각 장소마다 순서 업데이트
            places.forEachIndexed { index, place ->
                // ID가 유효한 경우만 업데이트
                if (place.id <= 0) {
                    Log.w("PlaceAdapter", "Skip updating order for place without valid ID: ${place.name}")
                    if (++failCount + successCount >= totalItems) {
                        loadingToast.cancel()
                        showUpdateResult(successCount, failCount)
                    }
                    return@forEachIndexed
                }
                
                // 새 순서는 인덱스+1로 설정 (1부터 시작)
                val newOrder = index + 1
                
                // 업데이트 API 호출을 위한 요청 객체 생성
                val request = com.example.travelonna.api.PlaceCreateRequest(
                    place = place.address,
                    isPublic = place.isPublic,
                    visitDate = "",  // 기존 방문 날짜 유지
                    placeCost = if (place.cost.isEmpty()) 0 else place.cost.toIntOrNull() ?: 0,
                    memo = place.memo,
                    lat = "",  // 기존 위도 유지
                    lon = "",  // 기존 경도 유지
                    name = place.name,
                    googleId = place.googleId,
                    order = newOrder  // 새로운 순서
                )
                
                Log.d("PlaceAdapter", "Updating place order - id: ${place.id}, name: ${place.name}, newOrder: $newOrder")
                
                // API 호출
                RetrofitClient.apiService.updatePlace(planId, place.id, request)
                    .enqueue(object : Callback<com.example.travelonna.api.BasicResponse> {
                        override fun onResponse(
                            call: Call<com.example.travelonna.api.BasicResponse>,
                            response: Response<com.example.travelonna.api.BasicResponse>
                        ) {
                            if (response.isSuccessful) {
                                Log.d("PlaceAdapter", "Successfully updated order for place ${place.id} to $newOrder")
                                successCount++
                            } else {
                                Log.e("PlaceAdapter", "Failed to update order for place ${place.id}: ${response.code()}, ${response.message()}")
                                failCount++
                            }
                            
                            // 모든 업데이트가 완료되면 결과 표시
                            if (successCount + failCount >= totalItems) {
                                loadingToast.cancel()
                                showUpdateResult(successCount, failCount)
                            }
                        }
                        
                        override fun onFailure(call: Call<com.example.travelonna.api.BasicResponse>, t: Throwable) {
                            Log.e("PlaceAdapter", "Network error when updating order for place ${place.id}", t)
                            failCount++
                            
                            // 모든 업데이트가 완료되면 결과 표시
                            if (successCount + failCount >= totalItems) {
                                loadingToast.cancel()
                                showUpdateResult(successCount, failCount)
                            }
                        }
                    })
            }
            
            // 장소가 없는 경우
            if (places.isEmpty()) {
                loadingToast.cancel()
            }
        }
        
        // 업데이트 결과를 표시하는 메서드
        private fun showUpdateResult(successCount: Int, failCount: Int) {
            if (failCount == 0) {
                Toast.makeText(this@ScheduleDetailActivity, "모든 장소의 순서가 업데이트되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ScheduleDetailActivity, 
                    "장소 순서 업데이트: $successCount 성공, $failCount 실패", 
                    Toast.LENGTH_SHORT).show()
            }
            
            // UI 다시 로드 (선택적)
            if (successCount > 0) {
                fetchPlanDetail(planId)
            }
        }
        
        // 아이템 삭제 메서드 추가
        fun removeItem(position: Int) {
            if (position >= 0 && position < places.size) {
                // 삭제할 장소
                val removedPlace = places[position]
                
                // API를 통해 서버에서도 삭제 (planId가 유효한 경우)
                if (planId > 0) {
                    // 장소 ID가 필요한 경우, 모델에 ID 필드 추가 필요
                    val placeId = removedPlace.id
                    
                    if (placeId > 0) {
                        // 삭제중 로딩 표시
                        val loadingToast = Toast.makeText(this@ScheduleDetailActivity, "삭제 중...", Toast.LENGTH_SHORT)
                        loadingToast.show()
                        
                        // API 호출로 서버에서 장소 삭제 
                        RetrofitClient.apiService.deletePlace(planId, placeId)
                            .enqueue(object : Callback<com.example.travelonna.api.BasicResponse> {
                                override fun onResponse(
                                    call: Call<com.example.travelonna.api.BasicResponse>,
                                    response: Response<com.example.travelonna.api.BasicResponse>
                                ) {
                                    loadingToast.cancel()
                                    
                                    if (response.isSuccessful) {
                                        Log.d("PlaceAdapter", "Successfully deleted place from server: placeId=$placeId")
                                        // 로컬 리스트에서도 제거
                                        places.removeAt(position)
                                        notifyItemRemoved(position)
                                        
                                        // 번호 재지정을 위해 이후 아이템들 갱신
                                        notifyItemRangeChanged(position, places.size - position)
                                        
                                        // 순서 업데이트 (삭제 후 남은 항목들의 순서를 조정)
                                        updatePlaceOrdersOnServer()
                                        
                                        Toast.makeText(this@ScheduleDetailActivity, "${removedPlace.name} 장소가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Log.e("PlaceAdapter", "Failed to delete place: ${response.code()}, ${response.message()}")
                                        Toast.makeText(this@ScheduleDetailActivity, "장소 삭제에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                        
                                        // 삭제 실패 시 목록 갱신하여 항목 복원
                                        notifyItemChanged(position)
                                    }
                                }
                                
                                override fun onFailure(call: Call<com.example.travelonna.api.BasicResponse>, t: Throwable) {
                                    loadingToast.cancel()
                                    Log.e("PlaceAdapter", "Network error when deleting place", t)
                                    Toast.makeText(this@ScheduleDetailActivity, "네트워크 오류: 장소 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
                                    
                                    // 삭제 실패 시 목록 갱신하여 항목 복원
                                    notifyItemChanged(position)
                                }
                            })
                    } else {
                        Log.w("PlaceAdapter", "Cannot delete place: invalid placeId")
                        Toast.makeText(this@ScheduleDetailActivity, "유효하지 않은 장소입니다.", Toast.LENGTH_SHORT).show()
                        notifyItemChanged(position)
                    }
                } else {
                    // API 연동이 없는 경우 (로컬에서만 삭제)
                    Log.d("PlaceAdapter", "Local deletion only: planId is not valid")
                    places.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, places.size - position)
                    Toast.makeText(this@ScheduleDetailActivity, "${removedPlace.name} 장소가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val numberCircle: TextView = itemView.findViewById(R.id.numberCircle)
            val placeImage: ImageView = itemView.findViewById(R.id.placeImage)
            val placeName: TextView = itemView.findViewById(R.id.placeName)
            val placeAddress: TextView = itemView.findViewById(R.id.placeAddress)
            val placeCost: TextView = itemView.findViewById(R.id.placeCost)
            val placeMemo: TextView = itemView.findViewById(R.id.placeMemo)
            val editButton: TextView = itemView.findViewById(R.id.editButton)
            val privacyStatus: TextView = itemView.findViewById(R.id.privacyStatus)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place, parent, false)
            return PlaceViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
            val place = places[position]
            holder.numberCircle.text = (position + 1).toString()
            
            // 기본 이미지 먼저 설정
            holder.placeImage.setImageResource(place.imageResId)
            
            // googleId가 있으면 Google Places API를 사용해 실제 이미지 로드
            if (place.googleId.isNotEmpty()) {
                loadPlaceImage(place.googleId, holder.placeImage)
            }
            
            // 장소명 표시 (name 필드)
            holder.placeName.text = place.name
            
            // 주소 표시 (address 필드)
            holder.placeAddress.text = place.address
            
            // 가격과 메모 표시
            val costLayout = holder.itemView.findViewById<LinearLayout>(R.id.costLayout)
            if (place.cost.isNotEmpty() && place.cost != "0") {
                holder.placeCost.text = "${place.cost}원"
                costLayout.visibility = View.VISIBLE
            } else {
                costLayout.visibility = View.GONE
            }
            
            if (place.memo.isNotEmpty()) {
                holder.placeMemo.text = place.memo
                holder.placeMemo.visibility = View.VISIBLE
            } else {
                holder.placeMemo.visibility = View.GONE
            }
            
            // 공개/비공개 상태 표시
            holder.privacyStatus.text = if (place.isPublic) "공개" else "비공개"
            holder.privacyStatus.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (place.isPublic) R.color.blue else R.color.gray_text
                )
            )
            
            holder.editButton.setOnClickListener {
                // 편집 기능 구현
                val intent = Intent(this@ScheduleDetailActivity, PlaceInfoActivity::class.java)
                
                // 필요한 기본 데이터 전달
                intent.putExtra("SELECTED_DAY", viewPager.currentItem)
                intent.putExtra("START_DATE", startDateCalendar.timeInMillis)
                intent.putExtra("END_DATE", endDateCalendar.timeInMillis)
                intent.putExtra("SCHEDULE_NAME", findViewById<TextView>(R.id.titleText).text.toString())
                intent.putExtra("PLAN_ID", planId)
                
                // 장소 정보 전달
                intent.putExtra("IS_EDIT_MODE", true)  // 편집 모드 활성화
                intent.putExtra("PLACE_ID", place.id)  // 장소 ID
                intent.putExtra("PLACE_NAME", place.name)
                intent.putExtra("PLACE_ADDRESS", place.address)
                intent.putExtra("GOOGLE_PLACE_ID", place.googleId) // Google Place ID
                intent.putExtra("ESTIMATED_COST", place.cost)
                intent.putExtra("MEMO", place.memo)
                intent.putExtra("IS_PUBLIC", place.isPublic)
                
                // PlaceInfoActivity 실행
                startActivityForResult(intent, 200) // 수정용 요청코드 200 사용
            }
        }
        
        // Google Places API를 사용하여 장소 이미지 로드
        private fun loadPlaceImage(googleId: String, imageView: ImageView) {
            if (googleId.isEmpty()) return
            
            try {
                Log.d("PlaceAdapter", "Loading image for Google Place ID: $googleId")
                
                // Places Photo API를 사용하여 사진 요청
                val placeFields = listOf(Place.Field.PHOTO_METADATAS)
                val request = FetchPlaceRequest.builder(googleId, placeFields).build()
                
                // 액티비티의 공유된 placesClient 사용
                placesClient.fetchPlace(request).addOnSuccessListener { response ->
                    val place = response.place
                    val metadatas = place.photoMetadatas
                    
                    if (metadatas == null || metadatas.isEmpty()) {
                        Log.d("PlaceAdapter", "No photos found for place ID: $googleId")
                        return@addOnSuccessListener
                    }
                    
                    // 첫 번째 사진 가져오기
                    val photoMetadata = metadatas[0]
                    
                    // 사진 가져오기 요청 생성
                    val photoRequest = FetchPhotoRequest.builder(photoMetadata)
                        .setMaxWidth(500) // 이미지 최대 폭 설정
                        .setMaxHeight(300) // 이미지 최대 높이 설정
                        .build()
                    
                    // 사진 가져오기
                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener { fetchPhotoResponse ->
                        val bitmap = fetchPhotoResponse.bitmap
                        imageView.setImageBitmap(bitmap)
                        Log.d("PlaceAdapter", "Successfully loaded image for place ID: $googleId")
                    }.addOnFailureListener { exception ->
                        Log.e("PlaceAdapter", "Failed to fetch photo for place ID: $googleId", exception)
                    }
                }.addOnFailureListener { exception ->
                    Log.e("PlaceAdapter", "Failed to fetch place details for ID: $googleId", exception)
                }
            } catch (e: Exception) {
                Log.e("PlaceAdapter", "Error loading place image for ID: $googleId", e)
            }
        }

        fun moveItem(fromPosition: Int, toPosition: Int) {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    places.swap(i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    places.swap(i, i - 1)
                }
            }
            notifyItemMoved(fromPosition, toPosition)
        }

        private fun MutableList<PlaceItem>.swap(index1: Int, index2: Int) {
            val tmp = this[index1]
            this[index1] = this[index2]
            this[index2] = tmp
        }
        
        override fun getItemCount() = places.size
    }
    
    // Day별 페이지 어댑터
    inner class DayPagerAdapter(
        private val activity: AppCompatActivity, 
        private val dayCount: Int, 
        private val startDate: Calendar
    ) : RecyclerView.Adapter<DayPagerAdapter.DayPageHolder>() {
        
        // 날짜별 장소 데이터 저장
        private val dayPlaces: MutableMap<Int, List<PlaceItem>> = mutableMapOf()
        
        // 장소 데이터 업데이트 메서드
        fun updatePlaces(planDetail: com.example.travelonna.api.PlanDetail?) {
            planDetail?.let { detail ->
                // 기존 데이터 초기화
                dayPlaces.clear()
                
                // API에서 받은 장소 목록을 날짜별로 분류
                val placesByDay = detail.places.groupBy { it.day }
                
                // 각 날짜별 장소 목록 저장
                for (dayIndex in 1..dayCount) {
                    val places = placesByDay[dayIndex] ?: listOf()
                    
                    // PlaceItem으로 변환
                    val placeItems = places.map { place ->
                        Log.d("DayPagerAdapter", "Creating PlaceItem - name: ${place.name}, id: ${place.id}, googleId: ${place.googleId}")
                        
                        PlaceItem(
                            name = place.name,
                            address = place.address,
                            imageResId = R.drawable.dummy_place_1, // 기본 이미지 (실제 이미지 로드 실패 시 사용)
                            cost = place.cost.toString(),
                            memo = place.memo,
                            isPublic = place.isPublic,
                            googleId = place.googleId,  // googleId 저장
                            id = place.id  // 서버 ID 저장
                        )
                    }
                    
                    dayPlaces[dayIndex - 1] = placeItems
                }
                
                // 어댑터 갱신
                notifyDataSetChanged()
            }
        }
        
        inner class DayPageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val recyclerView: RecyclerView = itemView.findViewById(R.id.dayRecyclerView)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayPageHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.day_page_layout, parent, false)
            return DayPageHolder(view)
        }
        
        override fun onBindViewHolder(holder: DayPageHolder, position: Int) {
            // 날짜별 데이터 설정
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDate.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, position)
            
            // RecyclerView 설정
            holder.recyclerView.layoutManager = LinearLayoutManager(activity)
            
            // 해당 날짜의 장소 목록 가져오기
            val places = dayPlaces[position]?.toMutableList() ?: mutableListOf()
            
            // PlaceAdapter 설정
            val adapter = PlaceAdapter(places)
            holder.recyclerView.adapter = adapter
            setupItemTouchHelper(holder.recyclerView)
        }
        
        override fun getItemCount(): Int = dayCount
    }
    
    // 장소 아이템 데이터 클래스
    data class PlaceItem(
        val name: String, 
        val address: String, 
        val imageResId: Int, 
        val cost: String, 
        val memo: String,
        val isPublic: Boolean = false,
        val googleId: String = "",  // googleId 필드 추가
        val id: Int = 0  // 서버 ID 필드 추가
    )
    
    // 커스텀 탭 설정
    private fun setupCustomTabs() {
        val tabContainer = findViewById<LinearLayout>(R.id.tabContainer)
        tabContainer.removeAllViews()
        
        for (i in 0 until dayCount) {
            val tabView = LayoutInflater.from(this).inflate(R.layout.tab_day_item, tabContainer, false)
            val dayNumber = tabView.findViewById<TextView>(R.id.dayNumber)
            val dayDate = tabView.findViewById<TextView>(R.id.dayDate)
            val selectionIndicator = tabView.findViewById<View>(R.id.selectionIndicator)
            
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDateCalendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, i)
            
            // 요일과 일자 형식으로 변경
            val dayOfWeekFormat = SimpleDateFormat("E", Locale.KOREA) // 요일
            val dayOfMonthFormat = SimpleDateFormat("dd", Locale.KOREA) // 일
            val dayOfWeek = dayOfWeekFormat.format(calendar.time)
            val dayOfMonth = dayOfMonthFormat.format(calendar.time)
            
            // Day N 형식
            val dayNum = i + 1
            val dayNumStr = String.format("%02d", dayNum) // 01, 02, ... 형식으로
            dayNumber.text = "DAY $dayNumStr"
            
            // 요일.일 형식
            dayDate.text = "$dayOfWeek.$dayOfMonth"
            
            // 선택 표시자 색상을 오렌지로 설정
            selectionIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.orange))
            
            // 첫 번째 탭을 선택된 상태로 표시
            if (i == 0) {
                dayNumber.setTextColor(ContextCompat.getColor(this, R.color.black))
                selectionIndicator.visibility = View.VISIBLE
            } else {
                selectionIndicator.visibility = View.INVISIBLE
            }
            
            tabView.tag = i
            tabView.setOnClickListener { v ->
                // 모든 탭 초기화
                for (j in 0 until tabContainer.childCount) {
                    val tab = tabContainer.getChildAt(j)
                    tab.findViewById<TextView>(R.id.dayNumber).setTextColor(ContextCompat.getColor(this, R.color.black))
                    tab.findViewById<View>(R.id.selectionIndicator).visibility = View.INVISIBLE
                }
                
                // 선택된 탭 하이라이트
                v.findViewById<TextView>(R.id.dayNumber).setTextColor(ContextCompat.getColor(this, R.color.black))
                v.findViewById<View>(R.id.selectionIndicator).visibility = View.VISIBLE
                
                // ViewPager 페이지 변경
                viewPager.currentItem = v.tag as Int
            }
            
            tabContainer.addView(tabView)
        }
    }
    
    // 페이지 변경 시 탭 선택 상태 업데이트
    private fun updateTabSelection(position: Int) {
        val tabContainer = findViewById<LinearLayout>(R.id.tabContainer)
        
        // 모든 탭 초기화
        for (i in 0 until tabContainer.childCount) {
            val tab = tabContainer.getChildAt(i)
            tab.findViewById<TextView>(R.id.dayNumber).setTextColor(ContextCompat.getColor(this, R.color.black))
            tab.findViewById<View>(R.id.selectionIndicator).visibility = View.INVISIBLE
        }
        
        // 선택된 탭 하이라이트
        val selectedTab = tabContainer.getChildAt(position)
        selectedTab.findViewById<TextView>(R.id.dayNumber).setTextColor(ContextCompat.getColor(this, R.color.black))
        selectedTab.findViewById<View>(R.id.selectionIndicator).visibility = View.VISIBLE
        
        // 선택된 탭이 보이도록 스크롤
        val scrollView = findViewById<HorizontalScrollView>(R.id.tabScrollView)
        scrollView.smoothScrollTo(selectedTab.left - 50, 0)
    }
    
    // 일정 상세 정보를 가져오는 함수 수정
    private fun fetchPlanDetail(planId: Int) {
        Log.d("ScheduleDetail", "Fetching plan detail for ID: $planId")
        Log.d("ScheduleDetail", "API Call Started: getPlanDetail($planId)")
        
        // 로딩 표시 (필요한 경우)
        // showLoading()
        
        RetrofitClient.apiService.getPlanDetail(planId)
            .enqueue(object : Callback<PlanDetailResponse> {
                override fun onResponse(call: Call<PlanDetailResponse>, response: Response<PlanDetailResponse>) {
                    Log.d("ScheduleDetail", "API Response Received: status code ${response.code()}")
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val planDetail = response.body()?.data
                        // JSON 응답 전체를 로그로 출력
                        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                        val jsonResponse = gson.toJson(response.body())
                        Log.d("ScheduleDetail", "Full JSON response: $jsonResponse")
                        
                        // is_group 값 확인
                        Log.d("ScheduleDetail", "Is group plan?: ${planDetail?.isGroup}")
                        Log.d("ScheduleDetail", "Group ID: ${planDetail?.groupId}")
                        
                        Log.d("ScheduleDetail", "Plan detail fetched successfully: ${response.body()}")
                        Log.d("ScheduleDetail", "Plan Title: ${planDetail?.title}")
                        Log.d("ScheduleDetail", "Places count: ${planDetail?.places?.size ?: 0}")
                        
                        // 장소 정보 로깅
                        planDetail?.places?.forEachIndexed { index, place ->
                            Log.d("ScheduleDetail", "Place $index: ${place.name}, Day: ${place.day}")
                        }
                        
                        // UI 업데이트
                        updateUIWithPlanDetail(planDetail)
                    } else {
                        Log.e("ScheduleDetail", "Failed to fetch plan detail: ${response.code()}")
                        Log.e("ScheduleDetail", "Error body: ${response.errorBody()?.string()}")
                        Toast.makeText(this@ScheduleDetailActivity, "일정 정보를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                    
                    // 로딩 숨기기 (필요한 경우)
                    // hideLoading()
                }
                
                override fun onFailure(call: Call<PlanDetailResponse>, t: Throwable) {
                    Log.e("ScheduleDetail", "Network error when fetching plan detail: ${t.message}")
                    Log.e("ScheduleDetail", "Exception details:", t)
                    Toast.makeText(this@ScheduleDetailActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    
                    // 로딩 숨기기 (필요한 경우)
                    // hideLoading()
                }
            })
    }
    
    // 일정 상세 정보로 UI 업데이트
    private fun updateUIWithPlanDetail(planDetail: com.example.travelonna.api.PlanDetail?) {
        planDetail?.let { detail ->
            Log.d("ScheduleDetail", "Updating UI with plan details: title=${detail.title}, placeCount=${detail.places.size}")
            
            // 일정 제목 업데이트
            findViewById<TextView>(R.id.titleText).text = detail.title
            
            // ViewPager 어댑터 업데이트
            val pagerAdapter = viewPager.adapter as DayPagerAdapter
            pagerAdapter.updatePlaces(detail)
            Log.d("ScheduleDetail", "ViewPager adapter updated with new places data")
            
            // 현재 일자에 맞게 탭 선택 유지
            updateTabSelection(viewPager.currentItem)
            
            // 그룹 일정인지 확인하고 공유 버튼 설정
            if (detail.isGroup) {
                // 그룹 일정인 경우 공유 버튼 표시
                shareButton.visibility = View.VISIBLE
                // 일정 ID를 사용하여 그룹 정보 가져오기
                val planId = detail.planId
                Log.d("ScheduleDetail", "This is a group plan with planId: $planId")
                fetchGroupInfo(planId)
            } else {
                // 그룹이 아닌 경우 공유 버튼 숨김
                shareButton.visibility = View.GONE
                Log.d("ScheduleDetail", "This is not a group plan, hiding share button")
            }
            
            Toast.makeText(this, "일정 정보가 업데이트되었습니다", Toast.LENGTH_SHORT).show()
            Log.d("ScheduleDetail", "UI update completed for plan: ${detail.title}")
        } ?: run {
            Log.w("ScheduleDetail", "Cannot update UI: planDetail is null")
        }
    }

    // 그룹 기능 설정
    private fun setupGroupFunctionality(urlCode: String) {
        // 공유 버튼 클릭 리스너 설정
        shareButton.setOnClickListener {
            // 공유 기능 구현
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "'여행온나'에서 여행 일정에 참여하세요! 공유 URL: $urlCode")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "공유 방법 선택"))
        }
    }

    // 그룹 정보를 가져오는 함수 추가
    private fun fetchGroupInfo(planId: Int) {
        Log.d("ScheduleDetail", "Fetching group info for plan ID: $planId")
        
        RetrofitClient.apiService.getGroupInfo(planId)
            .enqueue(object : Callback<GroupInfoResponse> {
                override fun onResponse(call: Call<GroupInfoResponse>, response: Response<GroupInfoResponse>) {
                    Log.d("ScheduleDetail", "Group Info API Response: status code ${response.code()}")
                    
                    if (response.isSuccessful) {
                        val groupInfo = response.body()
                        Log.d("ScheduleDetail", "Group info fetched successfully: $groupInfo")
                        
                        if (groupInfo != null) {
                            // URL 값만 사용 (앞부분 제거)
                            val urlCode = groupInfo.url
                            Log.d("ScheduleDetail", "Group URL code: $urlCode")
                            
                            // 그룹 기능 설정
                            setupGroupFunctionality(urlCode)
                        } else {
                            // 그룹 정보가 없는 경우 공유 버튼 숨김
                            shareButton.visibility = View.GONE
                            Log.w("ScheduleDetail", "No group info returned from API")
                        }
                    } else {
                        // API 호출 실패
                        Log.e("ScheduleDetail", "Failed to fetch group info: ${response.code()}")
                        Log.e("ScheduleDetail", "Error body: ${response.errorBody()?.string()}")
                        
                        // 공유 버튼 숨김
                        shareButton.visibility = View.GONE
                    }
                }
                
                override fun onFailure(call: Call<GroupInfoResponse>, t: Throwable) {
                    Log.e("ScheduleDetail", "Network error when fetching group info: ${t.message}")
                    Log.e("ScheduleDetail", "Exception details:", t)
                    
                    // 네트워크 오류 발생 시 공유 버튼 숨김
                    shareButton.visibility = View.GONE
                }
            })
    }

    private fun setupHeaderClickListener() {
        val headerLayout = findViewById<View>(R.id.headerLayout)
        headerLayout.setOnClickListener {
            showPlanDetailDialog()
        }
    }

    private fun showPlanDetailDialog() {
        val planId = intent.getIntExtra("PLAN_ID", -1)
        if (planId == -1) {
            Toast.makeText(this, "일정 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // API 호출
        RetrofitClient.apiService.getPlanDetail(planId).enqueue(object : Callback<PlanDetailResponse> {
            override fun onResponse(call: Call<PlanDetailResponse>, response: Response<PlanDetailResponse>) {
                if (response.isSuccessful && response.body()?.data != null) {
                    showPlanInfoDialog(response.body()?.data!!)
                } else {
                    Toast.makeText(this@ScheduleDetailActivity, "일정 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PlanDetailResponse>, t: Throwable) {
                Toast.makeText(this@ScheduleDetailActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPlanInfoDialog(planDetail: com.example.travelonna.api.PlanDetail) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_schedule_info)

        // 다이얼로그 크기 설정
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 뷰 참조
        val titleTextView = dialog.findViewById<TextView>(R.id.titleTextView)
        val transportTextView = dialog.findViewById<TextView>(R.id.transportTextView)
        val totalCostTextView = dialog.findViewById<TextView>(R.id.totalCostTextView)
        val memoTextView = dialog.findViewById<TextView>(R.id.memoTextView)

        // 데이터 설정
        titleTextView.text = planDetail.title
        
        // 교통수단 한글로 변환
        val transportText = when (planDetail.transportInfo.lowercase()) {
            "train" -> "기차"
            "bus" -> "버스"
            "car" -> "차량"
            "etc" -> "기타"
            else -> "미지정"
        }
        transportTextView.text = transportText
        
        // 총 비용 (천 단위 콤마)
        val formattedCost = String.format("%,d원", planDetail.totalCost)
        totalCostTextView.text = formattedCost
        
        // 메모
        memoTextView.text = planDetail.memo.ifEmpty { "메모가 없습니다." }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Places API 클라이언트를 명시적으로 종료하여 채널 누수 방지
        try {
            val field = Places::class.java.getDeclaredField("zza")
            field.isAccessible = true
            val instance = field.get(null)
            
            val shutdownMethod = instance.javaClass.getDeclaredMethod("shutdown")
            shutdownMethod.isAccessible = true
            shutdownMethod.invoke(instance)
            
            Log.d("ScheduleDetail", "Successfully shut down Places API client")
        } catch (e: Exception) {
            Log.e("ScheduleDetail", "Failed to shut down Places API client", e)
        }
    }
}