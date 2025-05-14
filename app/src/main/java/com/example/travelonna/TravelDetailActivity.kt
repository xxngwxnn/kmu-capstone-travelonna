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
import com.example.travelonna.model.TravelPlace
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import androidx.core.content.ContextCompat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import kotlin.math.abs

class TravelDetailActivity : AppCompatActivity() {

    private lateinit var recyclerViewPlaces: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var dateRangeTextView: TextView
    private lateinit var backButton: ImageView
    private lateinit var tabContainer: LinearLayout
    
    private val dayTabs = mutableListOf<LinearLayout>()
    private val dayIndicators = mutableListOf<View>()
    private val dayTexts = mutableListOf<TextView>()
    
    private var currentTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel_detail)

        // UI 요소 초기화
        titleTextView = findViewById(R.id.titleTextView)
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        backButton = findViewById(R.id.backButton)
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces)
        tabContainer = findViewById(R.id.tabContainer)
        
        // 탭 요소 초기화
        initializeTabs()

        // Intent에서 데이터 가져오기
        val title = intent.getStringExtra("TRAVEL_TITLE") ?: "미미미누"
        val startDate = intent.getLongExtra("START_DATE", System.currentTimeMillis())
        val endDate = intent.getLongExtra("END_DATE", System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000)) // 기본값: 오늘부터 3일
        
        // 날짜 포맷 설정
        setupDateTexts(startDate, endDate)
        
        // 기본 정보 표시
        titleTextView.text = title

        // 장소 목록 가져오기
        val places = getDummyPlaces() // 임시 데이터 생성 메서드 호출

        // RecyclerView 설정
        recyclerViewPlaces.layoutManager = LinearLayoutManager(this)
        recyclerViewPlaces.adapter = PlaceAdapter(places)
        
        // 전체 화면에 스와이프 감지를 위한 터치 리스너 설정
        setupSwipeListener()
        
        // 뒤로가기 버튼 설정
        backButton.setOnClickListener {
            finish()
        }
        
        // 첫 번째 탭 선택하여 표시
        updateTabSelection(0)
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
        
        // RecyclerView에도 터치 리스너 적용 (RecyclerView 내에서 스와이프 감지)
        recyclerViewPlaces.setOnTouchListener { v, event ->
            // 스크롤 중이 아닐 때만 스와이프 제스처 처리
            if (!recyclerViewPlaces.canScrollVertically(-1) || !recyclerViewPlaces.canScrollVertically(1)) {
                gestureDetector.onTouchEvent(event)
            }
            false // RecyclerView의 원래 터치 이벤트도 처리되도록 함
        }
    }
    
    private fun onSwipeLeft() {
        // 다음 탭으로 이동 (오른쪽으로)
        if (currentTabIndex < dayTabs.size - 1) {
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
        // 기존에 정의된 탭들을 가져옴
        dayTabs.add(findViewById(R.id.tab1))
        dayTabs.add(findViewById(R.id.tab2))
        dayTabs.add(findViewById(R.id.tab3))
        
        // 인디케이터 뷰 가져오기
        dayIndicators.add(findViewById(R.id.day1Indicator))
        dayIndicators.add(findViewById(R.id.day2Indicator))
        dayIndicators.add(findViewById(R.id.day3Indicator))
        
        // 탭 텍스트뷰 가져오기
        dayTexts.add(findViewById(R.id.day1Text))
        dayTexts.add(findViewById(R.id.day2Text))
        dayTexts.add(findViewById(R.id.day3Text))
        
        // 탭 클릭 이벤트 설정
        for (i in dayTabs.indices) {
            dayTabs[i].setOnClickListener {
                val goingRight = i > currentTabIndex
                updateTabSelection(i, goingRight)
            }
        }
    }
    
    private fun setupDateTexts(startDateMillis: Long, endDateMillis: Long) {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("E", Locale.KOREA) // 요일
        val dayNumberFormat = SimpleDateFormat("dd", Locale.getDefault()) // 일
        
        // 날짜 범위 텍스트 설정
        val startDateStr = dateFormat.format(startDateMillis)
        val endDateStr = dateFormat.format(endDateMillis)
        dateRangeTextView.text = "$startDateStr - $endDateStr"
        
        // 각 날짜 탭 텍스트 설정
        val cal = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
        }
        
        // 각 날짜 탭에 날짜 표시
        for (i in 0 until dayTabs.size) {
            if (i > 0) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            val dayStr = "${dayFormat.format(cal.time)}.${dayNumberFormat.format(cal.time)}"
            val dateTextView = dayTabs[i].findViewById<TextView>(getDayDateTextId(i))
            dateTextView.text = dayStr
        }
    }
    
    private fun getDayDateTextId(tabIndex: Int): Int {
        return when (tabIndex) {
            0 -> R.id.day1Date
            1 -> R.id.day2Date
            2 -> R.id.day3Date
            else -> throw IllegalArgumentException("Invalid tab index: $tabIndex")
        }
    }
    
    private fun updateTabSelection(selectedTabIndex: Int, goingRight: Boolean = true) {
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
            scrollView.smoothScrollTo(dayTabs[selectedTabIndex].left - 50, 0)
        }
        
        // 장소 데이터 로드 (여기서는 더미 데이터)
        val places = when (selectedTabIndex) {
            0 -> getDummyPlaces()
            1 -> getDummyPlacesForDay2()
            2 -> getDummyPlacesForDay3()
            else -> getDummyPlaces()
        }
        
        // 애니메이션과 함께 콘텐츠 전환
        animateContentChange(places, goingRight)
        
        // 현재 선택된 탭 인덱스 업데이트
        currentTabIndex = selectedTabIndex
    }
    
    private fun animateContentChange(newPlaces: List<TravelPlace>, goingRight: Boolean) {
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
                recyclerViewPlaces.adapter = PlaceAdapter(newPlaces)
                
                // 새 콘텐츠 슬라이드 인
                recyclerViewPlaces.startAnimation(slideIn)
            }
            
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }
    
    // 임시 데이터 생성 메서드 - DAY 1
    private fun getDummyPlaces(): List<TravelPlace> {
        return listOf(
            TravelPlace("동대구역", "대구광역시 동구 동대구로 550", "10:00"),
            TravelPlace("반월당", "대구광역시 중구 동성로", "12:30"),
            TravelPlace("대구 수목원", "대구광역시 달서구 화암로 342", "15:00")
        )
    }
    
    // 임시 데이터 생성 메서드 - DAY 2
    private fun getDummyPlacesForDay2(): List<TravelPlace> {
        return listOf(
            TravelPlace("경주 불국사", "경상북도 경주시 불국로 385", "09:00"),
            TravelPlace("첨성대", "경상북도 경주시 인왕동", "13:00"),
            TravelPlace("안압지", "경상북도 경주시 원화로 102", "16:30")
        )
    }
    
    // 임시 데이터 생성 메서드 - DAY 3
    private fun getDummyPlacesForDay3(): List<TravelPlace> {
        return listOf(
            TravelPlace("해운대", "부산광역시 해운대구", "10:00"),
            TravelPlace("광안리", "부산광역시 수영구", "15:00")
        )
    }
}

// 장소 어댑터
class PlaceAdapter(private val places: List<TravelPlace>) : 
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
            if (place.isLocked) R.drawable.ic_circle_lock else R.drawable.ic_circle_open
        )
        
        // 아이콘 클릭 이벤트
        holder.lockIcon.setOnClickListener {
            // 잠금/열림 상태 토글 로직 (실제 구현에서는 데이터 업데이트 필요)
            val newStatus = if ((it as ImageView).tag == "locked") "opened" else "locked"
            it.tag = newStatus
            
            it.setImageResource(
                if (newStatus == "locked") R.drawable.ic_circle_lock else R.drawable.ic_circle_open
            )
            
            Toast.makeText(holder.itemView.context, 
                "${place.name} 장소가 ${if (newStatus == "locked") "비공개" else "공개"}로 설정되었습니다", 
                Toast.LENGTH_SHORT).show()
        }
        
        // 아이템 클릭 이벤트
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PlaceMemoryActivity::class.java).apply {
                putExtra("PLACE_NAME", place.name)
                putExtra("PLACE_ADDRESS", place.address)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = places.size
} 