package com.example.travelonna

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.graphics.Typeface
import android.widget.Toast
import android.widget.LinearLayout
import com.example.travelonna.api.PlaceDetail
import com.example.travelonna.api.PlanDetail
import com.example.travelonna.api.PlanDetailResponse
import com.example.travelonna.api.RetrofitClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import androidx.core.content.ContextCompat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import kotlin.math.abs
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.example.travelonna.api.PlaceCreateRequest
import com.example.travelonna.api.BasicResponse

class TravelDetailActivity : AppCompatActivity() {

    private lateinit var recyclerViewPlaces: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var dateRangeTextView: TextView
    private lateinit var backButton: ImageView
    private lateinit var tabContainer: LinearLayout
    private lateinit var placesClient: PlacesClient
    
    private val dayTabs = mutableListOf<LinearLayout>()
    private val dayIndicators = mutableListOf<View>()
    private val dayTexts = mutableListOf<TextView>()
    
    private var currentTabIndex = 0
    private var planId: Int = 0
    private var planDetail: PlanDetail? = null
    
    private val TAG = "TravelDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel_detail)

        // UI 요소 초기화
        titleTextView = findViewById(R.id.titleTextView)
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        backButton = findViewById(R.id.backButton)
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces)
        tabContainer = findViewById(R.id.tabContainer)
        
        // Google Places API 초기화
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)
        
        // 탭 요소 초기화
        initializeTabs()

        // Intent에서 데이터 가져오기
        planId = intent.getIntExtra("PLAN_ID", 0)
        
        if (planId > 0) {
            // API를 통해 계획 상세 정보 가져오기
            fetchPlanDetail(planId)
        } else {
            // 테스트용 기본 데이터 표시
            val title = intent.getStringExtra("TRAVEL_TITLE") ?: "미미미누"
            val startDate = intent.getLongExtra("START_DATE", System.currentTimeMillis())
            val endDate = intent.getLongExtra("END_DATE", System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000)) // 기본값: 오늘부터 4일
            
            // 기본 정보 표시
            titleTextView.text = title
            setupDateTexts(startDate, endDate)
        }
        
        // RecyclerView 설정
        recyclerViewPlaces.layoutManager = LinearLayoutManager(this)
        
        // 아이템 간격 설정
        if (recyclerViewPlaces.itemDecorationCount == 0) {
            recyclerViewPlaces.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    // 첫 번째 아이템 상단 여백 추가
                    if (parent.getChildAdapterPosition(view) == 0) {
                        outRect.top = resources.getDimensionPixelSize(R.dimen.card_margin_vertical)
                    }
                    // 각 아이템의 하단 여백 설정
                    outRect.bottom = resources.getDimensionPixelSize(R.dimen.card_margin_vertical)
                }
            })
        }
        
        // 전체 화면에 스와이프 감지를 위한 터치 리스너 설정
        setupSwipeListener()
        
        // 뒤로가기 버튼 설정
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun fetchPlanDetail(planId: Int) {
        Log.d(TAG, "Fetching plan details for planId: $planId")
        
        RetrofitClient.apiService.getPlanDetail(planId).enqueue(object : Callback<PlanDetailResponse> {
            override fun onResponse(call: Call<PlanDetailResponse>, response: Response<PlanDetailResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val detailResponse = response.body()!!
                    if (detailResponse.success) {
                        planDetail = detailResponse.data
                        updateUIWithPlanData(planDetail!!)
                    } else {
                        Toast.makeText(this@TravelDetailActivity, "데이터를 가져오는데 실패했습니다: ${detailResponse.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = "API 호출 실패: ${response.code()}"
                    Log.e(TAG, errorMessage)
                    Toast.makeText(this@TravelDetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<PlanDetailResponse>, t: Throwable) {
                val errorMessage = "네트워크 오류: ${t.message}"
                Log.e(TAG, errorMessage, t)
                Toast.makeText(this@TravelDetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun updateUIWithPlanData(planDetail: PlanDetail) {
        // 기본 정보 업데이트
        titleTextView.text = planDetail.title
        
        // 날짜 설정
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val startDate = LocalDate.parse(planDetail.startDate, formatter)
            val endDate = LocalDate.parse(planDetail.endDate, formatter)
            
            // 날짜 텍스트 업데이트
            val displayFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            dateRangeTextView.text = "${startDate.format(displayFormat)} - ${endDate.format(displayFormat)}"
            
            // 일수 계산
            val dayCount = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            
            // 기존 탭 초기화
            createDynamicTabs(dayCount, startDate)
            
            // 첫 번째 탭 선택
            updateTabSelection(0)
        } catch (e: Exception) {
            Log.e(TAG, "날짜 파싱 오류", e)
            Toast.makeText(this, "날짜 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 여행 일수에 맞게 동적으로 탭 생성
    private fun createDynamicTabs(dayCount: Int, startDate: LocalDate) {
        // 기존 탭 컨테이너 초기화
        tabContainer.removeAllViews()
        dayTabs.clear()
        dayIndicators.clear()
        dayTexts.clear()
        
        // 날짜 포맷 설정
        val dayFormat = DateTimeFormatter.ofPattern("E", Locale.KOREA) // 요일
        val dayNumberFormat = DateTimeFormatter.ofPattern("dd") // 일
        
        // 각 일자별 탭 생성
        for (i in 0 until dayCount) {
            val currentDate = startDate.plusDays(i.toLong())
            val dayStr = "${currentDate.format(dayFormat)}.${currentDate.format(dayNumberFormat)}"
            
            // 탭 레이아웃 생성
            val tabLayout = layoutInflater.inflate(R.layout.item_day_tab, tabContainer, false) as LinearLayout
            tabLayout.id = View.generateViewId()
            
            // 탭 내부 뷰 가져오기
            val dayTextView = tabLayout.findViewById<TextView>(R.id.dayText)
            val dayIndicator = tabLayout.findViewById<View>(R.id.dayIndicator)
            val dateTextView = tabLayout.findViewById<TextView>(R.id.dayDateText)
            
            // 텍스트 설정
            dayTextView.text = String.format("DAY %02d", i + 1)
            dateTextView.text = dayStr
            
            // 첫 번째 탭이 아니면 기본 스타일 설정
            if (i > 0) {
                dayTextView.typeface = Typeface.DEFAULT
                dayTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                dayIndicator.visibility = View.INVISIBLE
            }
            
            // 마진 설정 (마지막 탭이 아니면 오른쪽 마진 추가)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (i < dayCount - 1) {
                layoutParams.marginEnd = resources.getDimensionPixelSize(R.dimen.tab_margin_end)
            }
            tabLayout.layoutParams = layoutParams
            
            // 클릭 리스너 설정
            val finalIndex = i
            tabLayout.setOnClickListener {
                val goingRight = finalIndex > currentTabIndex
                updateTabSelection(finalIndex, goingRight)
            }
            
            // 리스트에 추가
            dayTabs.add(tabLayout)
            dayTexts.add(dayTextView)
            dayIndicators.add(dayIndicator)
            
            // 컨테이너에 추가
            tabContainer.addView(tabLayout)
        }
    }
    
    private fun setupSwipeListener() {
        // 스와이프 감지를 위한 간단한 터치 리스너 구현
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 50  // 더 낮게 설정하여 민감도 향상
            private val SWIPE_VELOCITY_THRESHOLD = 50  // 더 낮게 설정하여 민감도 향상
            
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
            
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                var result = false
                try {
                    if (e1 == null) return false
                    
                    val diffX = e2.x - e1.x
                    val diffY = e2.y - e1.y
                    
                    if (abs(diffX) > abs(diffY) &&
                        abs(diffX) > SWIPE_THRESHOLD &&
                        abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        
                        if (diffX > 0) {
                            // 오른쪽으로 스와이프: 이전 날짜로 이동
                            onSwipeRight()
                        } else {
                            // 왼쪽으로 스와이프: 다음 날짜로 이동
                            onSwipeLeft()
                        }
                        result = true
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                
                return result
            }
        })
        
        // 루트 레이아웃에 터치 리스너 적용 (전체 화면에서 스와이프 감지)
        val rootLayout = findViewById<View>(android.R.id.content)
        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        
        // RecyclerView를 위한 별도의 터치 리스너 구현
        val recyclerViewTouchListener = object : RecyclerView.OnItemTouchListener {
            private var startX = 0f
            private var startY = 0f
            private val SWIPE_THRESHOLD = 50
            
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x
                        startY = e.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val diffX = e.x - startX
                        val diffY = e.y - startY
                        
                        // 수평 스와이프가 수직 스와이프보다 크면 인터셉트
                        if (abs(diffX) > abs(diffY) && abs(diffX) > SWIPE_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                            return true
                        }
                    }
                }
                return false
            }
            
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        }
        
        // RecyclerView에 새로운 터치 리스너 적용
        recyclerViewPlaces.addOnItemTouchListener(recyclerViewTouchListener)
    }
    
    private fun onSwipeLeft() {
        // 다음 탭으로 이동 (오른쪽으로)
        if (currentTabIndex < dayTabs.size - 1 && dayTabs[currentTabIndex + 1].visibility == View.VISIBLE) {
            updateTabSelection(currentTabIndex + 1, true)
        }
    }
    
    private fun onSwipeRight() {
        // 이전 탭으로 이동 (왼쪽으로)
        if (currentTabIndex > 0) {
            updateTabSelection(currentTabIndex - 1, false)
        }
    }
    
    private fun initializeTabs() {
        // 탭 컨테이너 초기화. 
        // 동적으로 탭을 생성할 것이므로 여기서는 최소한의 초기화만 수행
        tabContainer.removeAllViews()
        dayTabs.clear()
        dayIndicators.clear()
        dayTexts.clear()
    }
    
    private fun setupDateTexts(startDateMillis: Long, endDateMillis: Long) {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        
        // 날짜 범위 텍스트 설정
        val startDateStr = dateFormat.format(startDateMillis)
        val endDateStr = dateFormat.format(endDateMillis)
        dateRangeTextView.text = "$startDateStr - $endDateStr"
        
        // 캘린더 객체로 변환하여 일수 계산
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endDateMillis }
        
        val dayCount = ((endDateMillis - startDateMillis) / (24 * 60 * 60 * 1000)).toInt() + 1
        
        // 날짜 형식으로 변환하여 동적 탭 생성
        try {
            val localStartDate = LocalDate.of(
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH) + 1,
                startCal.get(Calendar.DAY_OF_MONTH)
            )
            createDynamicTabs(dayCount, localStartDate)
        } catch (e: Exception) {
            Log.e(TAG, "날짜 변환 오류", e)
            // 오류 발생 시 일단 탭만 생성 (날짜 정보 없이)
            createSimpleTabs(dayCount)
        }
        
        // 첫 번째 탭 선택
        updateTabSelection(0)
    }
    
    // 간단한 탭 생성 (날짜 정보 없이)
    private fun createSimpleTabs(dayCount: Int) {
        tabContainer.removeAllViews()
        dayTabs.clear()
        dayIndicators.clear()
        dayTexts.clear()
        
        for (i in 0 until dayCount) {
            val tabLayout = layoutInflater.inflate(R.layout.item_day_tab, tabContainer, false) as LinearLayout
            tabLayout.id = View.generateViewId()
            
            val dayTextView = tabLayout.findViewById<TextView>(R.id.dayText)
            val dayIndicator = tabLayout.findViewById<View>(R.id.dayIndicator)
            val dateTextView = tabLayout.findViewById<TextView>(R.id.dayDateText)
            
            dayTextView.text = String.format("DAY %02d", i + 1)
            dateTextView.text = "-.-"
            
            if (i > 0) {
                dayTextView.typeface = Typeface.DEFAULT
                dayTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                dayIndicator.visibility = View.INVISIBLE
            }
            
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (i < dayCount - 1) {
                layoutParams.marginEnd = resources.getDimensionPixelSize(R.dimen.tab_margin_end)
            }
            tabLayout.layoutParams = layoutParams
            
            val finalIndex = i
            tabLayout.setOnClickListener {
                val goingRight = finalIndex > currentTabIndex
                updateTabSelection(finalIndex, goingRight)
            }
            
            dayTabs.add(tabLayout)
            dayTexts.add(dayTextView)
            dayIndicators.add(dayIndicator)
            
            tabContainer.addView(tabLayout)
        }
    }
    
    private fun updateTabSelection(selectedTabIndex: Int, goingRight: Boolean = true) {
        // 선택 범위 확인
        if (selectedTabIndex < 0 || selectedTabIndex >= dayTabs.size) return
        
        // 모든 탭 초기화
        for (i in dayTabs.indices) {
            dayTexts[i].setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            dayTexts[i].typeface = Typeface.DEFAULT
            dayIndicators[i].visibility = View.INVISIBLE
        }
        
        // 선택된 탭 활성화
        dayTexts[selectedTabIndex].setTextColor(ContextCompat.getColor(this, android.R.color.black))
        dayTexts[selectedTabIndex].typeface = Typeface.DEFAULT_BOLD
        dayIndicators[selectedTabIndex].visibility = View.VISIBLE
        
        // 선택된 탭이 보이도록 스크롤
        val scrollView = findViewById<android.widget.HorizontalScrollView>(R.id.tabScrollView)
        scrollView.post {
            val tabLeft = dayTabs[selectedTabIndex].left
            scrollView.smoothScrollTo(tabLeft - 40, 0) // 왼쪽 여백 고려하여 스크롤
        }
        
        // 현재 날짜에 해당하는 장소 목록 가져오기
        val dayToShow = selectedTabIndex + 1 // API의 day는 1부터 시작함
        
        // API 데이터가 있으면 실제 데이터 사용, 없으면 더미 데이터
        val places = if (planDetail != null) {
            planDetail!!.places.filter { it.day == dayToShow }
        } else {
            // 더미 데이터
            when (selectedTabIndex) {
                0 -> getDummyPlaces()
                1 -> getDummyPlacesForDay2()
                2 -> getDummyPlacesForDay3()
                else -> getDummyPlaces()
            }.map { 
                PlaceDetail(
                    id = 0,
                    name = it.name,
                    address = it.address,
                    order = 0,
                    isPublic = true,  // 기본값을 true로 설정
                    visitDate = "",
                    day = selectedTabIndex + 1,
                    cost = 0,
                    memo = "",
                    lat = "0",
                    lon = "0",
                    googleId = ""
                )
            }
        }
        
        // 애니메이션과 함께 콘텐츠 전환
        animateContentChange(places, goingRight)
        
        // 현재 선택된 탭 인덱스 업데이트
        currentTabIndex = selectedTabIndex
    }
    
    private fun animateContentChange(newPlaces: List<PlaceDetail>, goingRight: Boolean) {
        // 슬라이드 애니메이션 로드
        val slideOut = if (goingRight) 
            AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        else 
            AnimationUtils.loadAnimation(this, R.anim.slide_out_right)
            
        val slideIn = if (goingRight) 
            AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        else 
            AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        
        // 애니메이션 속도 조정 (더 빠르게)
        slideOut.duration = 200
        slideIn.duration = 200
        
        // 기존 콘텐츠 슬라이드 아웃
        recyclerViewPlaces.startAnimation(slideOut)
        
        // 애니메이션이 끝나면 새 어댑터 설정 및 새 콘텐츠 슬라이드 인
        slideOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                // 애니메이션이 끝나면 새 어댑터 설정
                recyclerViewPlaces.adapter = PlaceAdapter(newPlaces, placesClient)
                
                // 새 콘텐츠 슬라이드 인
                recyclerViewPlaces.startAnimation(slideIn)
            }
            
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }
    
    // 임시 데이터 생성 메서드 - DAY 1
    private fun getDummyPlaces(): List<com.example.travelonna.model.TravelPlace> {
        return listOf(
            com.example.travelonna.model.TravelPlace("동대구역", "대구광역시 동구 동대구로 550", "10:00"),
            com.example.travelonna.model.TravelPlace("반월당", "대구광역시 중구 동성로", "12:30"),
            com.example.travelonna.model.TravelPlace("대구 수목원", "대구광역시 달서구 화암로 342", "15:00")
        )
    }
    
    // 임시 데이터 생성 메서드 - DAY 2
    private fun getDummyPlacesForDay2(): List<com.example.travelonna.model.TravelPlace> {
        return listOf(
            com.example.travelonna.model.TravelPlace("경주 불국사", "경상북도 경주시 불국로 385", "09:00"),
            com.example.travelonna.model.TravelPlace("첨성대", "경상북도 경주시 인왕동", "13:00"),
            com.example.travelonna.model.TravelPlace("안압지", "경상북도 경주시 원화로 102", "16:30")
        )
    }
    
    // 임시 데이터 생성 메서드 - DAY 3
    private fun getDummyPlacesForDay3(): List<com.example.travelonna.model.TravelPlace> {
        return listOf(
            com.example.travelonna.model.TravelPlace("해운대", "부산광역시 해운대구", "10:00"),
            com.example.travelonna.model.TravelPlace("광안리", "부산광역시 수영구", "15:00")
        )
    }

    fun getPlanId(): Int {
        return planId
    }

    fun refreshPlaceData(placeId: Int, newIsPublic: Boolean) {
        // planDetail이 null이 아닌 경우에만 처리
        planDetail?.let { detail ->
            // 해당 ID를 가진 장소 찾기
            val placeIndex = detail.places.indexOfFirst { it.id == placeId }
            
            if (placeIndex != -1) {
                // 새로운 장소 객체 생성 (불변 객체이므로 새로 생성해야 함)
                val updatedPlace = detail.places[placeIndex].copy(isPublic = newIsPublic)
                
                // 새로운 places 리스트 생성
                val updatedPlaces = detail.places.toMutableList().apply {
                    set(placeIndex, updatedPlace)
                }
                
                // 새로운 planDetail 객체 생성 (copy 메서드 대신 직접 필드 업데이트)
                // createdAt 필드가 null인 경우 NPE가 발생하므로 이 방식으로 변경
                val currentPlanDetail = detail
                val newPlanDetail = PlanDetail(
                    planId = currentPlanDetail.planId,
                    userId = currentPlanDetail.userId,
                    title = currentPlanDetail.title,
                    location = currentPlanDetail.location,
                    startDate = currentPlanDetail.startDate,
                    endDate = currentPlanDetail.endDate,
                    transportInfo = currentPlanDetail.transportInfo,
                    isPublic = currentPlanDetail.isPublic,
                    totalCost = currentPlanDetail.totalCost,
                    memo = currentPlanDetail.memo,
                    createdAt = currentPlanDetail.createdAt,
                    updatedAt = currentPlanDetail.updatedAt,
                    places = updatedPlaces,
                    isGroup = currentPlanDetail.isGroup,
                    isGroup2 = currentPlanDetail.isGroup2,
                    groupId = currentPlanDetail.groupId,
                    groupId2 = currentPlanDetail.groupId2
                )
                planDetail = newPlanDetail
                
                // 현재 표시 중인 탭이 업데이트된 장소의 날짜와 일치하면 UI 업데이트
                val dayToShow = currentTabIndex + 1
                if (updatedPlace.day == dayToShow) {
                    // 현재 탭에 해당하는 장소 목록 다시 가져와서 어댑터 업데이트
                    val places = planDetail!!.places.filter { it.day == dayToShow }
                    recyclerViewPlaces.adapter = PlaceAdapter(places, placesClient)
                }
                
                Log.d(TAG, "장소 ID: $placeId 공개 상태가 $newIsPublic 로 업데이트되었습니다.")
            } else {
                Log.e(TAG, "장소 ID: $placeId 를 찾을 수 없습니다.")
            }
        } ?: run {
            Log.e(TAG, "planDetail이 null입니다. 장소 상태를 업데이트할 수 없습니다.")
        }
    }
}

// 장소 어댑터
class PlaceAdapter(private val places: List<PlaceDetail>, private val placesClient: PlacesClient) : 
    RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val placeImage: ImageView = view.findViewById(R.id.placeImage)
        val nameText: TextView = view.findViewById(R.id.placeName)
        val addressText: TextView = view.findViewById(R.id.placeAddress)
        val lockIcon: ImageView = view.findViewById(R.id.lockIconView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.nameText.text = place.name
        holder.addressText.text = place.address
        
        // 잠금/열림 아이콘 설정
        holder.lockIcon.setImageResource(
            if (!place.isPublic) R.drawable.ic_circle_lock else R.drawable.ic_circle_open
        )
        
        // 아이콘 클릭 이벤트
        holder.lockIcon.setOnClickListener {
            // 현재 상태의 반대로 변경
            val newIsPublic = !place.isPublic
            
            // API 요청 생성
            val request = PlaceCreateRequest(
                place = place.address,
                isPublic = newIsPublic,
                visitDate = place.visitDate,
                placeCost = place.cost,
                memo = place.memo,
                lat = place.lat,
                lon = place.lon,
                name = place.name,
                googleId = place.googleId,
                order = place.order
            )
            
            // 현재 Activity에서 planId 가져오기
            val activity = holder.itemView.context as TravelDetailActivity
            val planId = activity.getPlanId()
            
            // API 호출
            RetrofitClient.apiService.updatePlace(
                planId = planId, 
                placeId = place.id, 
                request = request
            ).enqueue(object : Callback<BasicResponse> {
                override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                    if (response.isSuccessful) {
                        // UI 업데이트
                        holder.lockIcon.setImageResource(
                            if (!newIsPublic) R.drawable.ic_circle_lock else R.drawable.ic_circle_open
            )
            
                        // 데이터 모델 업데이트 - 변경 불가능한 데이터 클래스이므로 직접 수정 불가
                        // 대신 Activity에 알려서 필요시 데이터를 새로고침하도록 함
                        activity.refreshPlaceData(place.id, newIsPublic)
                        
                        Toast.makeText(holder.itemView.context, 
                            "${place.name} 장소가 ${if (!newIsPublic) "비공개" else "공개"}로 설정되었습니다", 
                            Toast.LENGTH_SHORT).show()
                    } else {
                        // 오류 처리
                        Log.e("PlaceAdapter", "API 오류: ${response.code()}, ${response.message()}")
                        Toast.makeText(holder.itemView.context, 
                            "설정 변경에 실패했습니다: ${response.message()}", 
                            Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    // 네트워크 오류 처리
                    Log.e("PlaceAdapter", "네트워크 오류", t)
            Toast.makeText(holder.itemView.context, 
                        "네트워크 오류: ${t.message}", 
                Toast.LENGTH_SHORT).show()
                }
            })
        }
        
        // Google Places API로 이미지 로드
        if (place.googleId.isNotEmpty()) {
            loadPlaceImage(place.googleId, holder.placeImage)
        } else {
            // 기본 이미지 표시
            holder.placeImage.setImageResource(R.drawable.ic_place_holder)
        }
        
        // 아이템 클릭 이벤트 다시 활성화
        holder.itemView.setOnClickListener {
            val activity = holder.itemView.context as TravelDetailActivity
            val intent = Intent(holder.itemView.context, PlaceMemoryActivity::class.java).apply {
                putExtra("PLACE_NAME", place.name)
                putExtra("PLACE_ADDRESS", place.address)
                putExtra("PLACE_ID", place.id)
                putExtra("PLAN_ID", activity.getPlanId())
                putExtra("IS_PUBLIC", place.isPublic)
                putExtra("GOOGLE_ID", place.googleId)
                putExtra("VISIT_DATE", place.visitDate)
                putExtra("COST", place.cost)
                putExtra("MEMO", place.memo)
                putExtra("LAT", place.lat)
                putExtra("LON", place.lon)
                putExtra("ORDER", place.order)
            }
            holder.itemView.context.startActivity(intent)
        }
    }
    
    private fun loadPlaceImage(placeId: String, imageView: ImageView) {
        try {
            // 먼저 장소 자체의 정보를 가져옴
            val placeFields = listOf(Place.Field.PHOTO_METADATAS)
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            
            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val place = response.place
                val photoMetadata = place.photoMetadatas
                
                if (photoMetadata != null && photoMetadata.isNotEmpty()) {
                    // 첫 번째 사진 메타데이터 사용
                    val firstPhoto = photoMetadata.first()
                    
                    // 사진 요청 생성
                    val photoRequest = FetchPhotoRequest.builder(firstPhoto)
                        .setMaxWidth(500) // 적당한 크기로 설정
                        .setMaxHeight(500)
                        .build()
                    
                    // 사진 가져오기
                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener { fetchPhotoResponse ->
                        val bitmap = fetchPhotoResponse.bitmap
                        imageView.setImageBitmap(bitmap)
                    }.addOnFailureListener { exception ->
                        Log.e("PlaceAdapter", "Photo fetch failed: ${exception.message}")
                        imageView.setImageResource(R.drawable.ic_place_holder)
                    }
                } else {
                    // 사진이 없는 경우 기본 이미지 표시
                    imageView.setImageResource(R.drawable.ic_place_holder)
                }
            }.addOnFailureListener { exception ->
                Log.e("PlaceAdapter", "Place fetch failed: ${exception.message}")
                imageView.setImageResource(R.drawable.ic_place_holder)
            }
        } catch (e: Exception) {
            Log.e("PlaceAdapter", "Error loading place image", e)
            imageView.setImageResource(R.drawable.ic_place_holder)
        }
    }

    override fun getItemCount() = places.size
} 