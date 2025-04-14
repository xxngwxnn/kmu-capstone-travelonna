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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.PlacesClient

class ScheduleDetailActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var addNewPlaceButton: TextView
    private lateinit var confirmButton: TextView
    
    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance()
    private var dayCount = 0
    private var planId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_detail)
        
        // 뷰 초기화
        viewPager = findViewById(R.id.viewPager)
        addNewPlaceButton = findViewById(R.id.addNewPlaceButton)
        confirmButton = findViewById(R.id.confirmButton)
        
            // 인텐트에서 날짜 데이터 가져오기
            val startDate = intent.getLongExtra("START_DATE", System.currentTimeMillis())
            val endDate = intent.getLongExtra("END_DATE", System.currentTimeMillis())
        val scheduleName = intent.getStringExtra("SCHEDULE_NAME") ?: "일정"
        planId = intent.getIntExtra("PLAN_ID", 0)
        
        // planId 확인을 위한 로그 추가
        Log.d("ScheduleDetail", "Received Plan ID: $planId")
            
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
    
        // 완료 버튼 리스너
        confirmButton.setOnClickListener {
            finish() // 현재 화면 종료하고 이전 화면으로 돌아가기
        }
        
        // 최초 로드 시 일정 정보 가져오기 (planId가 유효한 경우)
        if (planId > 0) {
            Log.d("ScheduleDetail", "Initial loading of plan details: planId=$planId, scheduleName=$scheduleName")
            fetchPlanDetail(planId)
        } else {
            Log.d("ScheduleDetail", "No valid planId provided ($planId), skipping plan detail fetch")
        }
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
    }
    
    // 드래그 앤 드롭 헬퍼 설정
    private fun setupItemTouchHelper(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // 드래그 방향
        0  // 스와이프 사용하지 않음
    ) {
        private var dragFrom = -1
        private var dragTo = -1

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
            // 스와이프 기능 사용하지 않음
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true  // 롱 프레스로 드래그 활성화
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            
            if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                // 드래그가 끝났을 때 전체 리스트 갱신
                recyclerView.adapter?.notifyDataSetChanged()
                }
                
                // 드래그 위치 초기화
                dragFrom = -1
                dragTo = -1
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
    
    // 장소 목록 어댑터
    inner class PlaceAdapter(
        internal val places: MutableList<PlaceItem>
    ) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {
        
        private lateinit var placesClient: PlacesClient
        
        init {
            // Places API 초기화
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, getString(R.string.google_maps_key))
            }
            placesClient = Places.createClient(applicationContext)
        }
        
        fun updatePlaces(newPlaces: List<PlaceItem>) {
            places.clear()
            places.addAll(newPlaces)
            notifyDataSetChanged()
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
                // 편집 기능 구현 (향후)
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
                        Log.d("DayPagerAdapter", "Creating PlaceItem - name: ${place.name}, googleId: ${place.googleId}")
                        
                        PlaceItem(
                            name = place.name,
                            address = place.address,
                            imageResId = R.drawable.dummy_place_1, // 기본 이미지 (실제 이미지 로드 실패 시 사용)
                            cost = place.cost.toString(),
                            memo = place.memo,
                            isPublic = place.isPublic,
                            googleId = place.googleId  // googleId 저장
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
        val googleId: String = ""  // googleId 필드 추가
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
            
            Toast.makeText(this, "일정 정보가 업데이트되었습니다", Toast.LENGTH_SHORT).show()
            Log.d("ScheduleDetail", "UI update completed for plan: ${detail.title}")
        } ?: run {
            Log.w("ScheduleDetail", "Cannot update UI: planDetail is null")
        }
    }
}