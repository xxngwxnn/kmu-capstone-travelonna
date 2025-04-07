package com.example.travelonna.ui.schedule

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.travelonna.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlaceInfoActivity : AppCompatActivity() {
    private lateinit var titleText: TextView
    private lateinit var dateRangeText: TextView
    private lateinit var placeNameInput: EditText
    private lateinit var placeAddressText: TextView
    private lateinit var estimatedCostInput: EditText
    private lateinit var memoInput: EditText
    private lateinit var searchPlaceButton: ImageButton
    private lateinit var cancelButton: Button
    private lateinit var confirmButton: Button
    
    private var selectedDay: Int = 0
    private var startDate: Long = 0
    private var endDate: Long = 0
    private var scheduleName: String = ""
    
    // 위치 정보
    private var placeId: String = ""
    private var placeName: String = ""
    private var placeAddress: String = ""
    private var placeLat: Double = 0.0
    private var placeLng: Double = 0.0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_info)
        
        // 인텐트에서 데이터 가져오기
        selectedDay = intent.getIntExtra("SELECTED_DAY", 0)
        startDate = intent.getLongExtra("START_DATE", System.currentTimeMillis())
        endDate = intent.getLongExtra("END_DATE", System.currentTimeMillis())
        scheduleName = intent.getStringExtra("SCHEDULE_NAME") ?: "일정"
        
        // 뷰 초기화
        initViews()
        
        // 상단 정보 표시
        setupHeader()
        
        // 탭 설정
        setupTabs()
        
        // 버튼 리스너 설정
        setupListeners()
    }
    
    private fun initViews() {
        titleText = findViewById(R.id.titleText)
        dateRangeText = findViewById(R.id.dateRangeText)
        placeNameInput = findViewById(R.id.placeNameInput)
        placeAddressText = findViewById(R.id.placeAddressText)
        estimatedCostInput = findViewById(R.id.estimatedCostInput)
        memoInput = findViewById(R.id.memoInput)
        searchPlaceButton = findViewById(R.id.searchPlaceButton)
        cancelButton = findViewById(R.id.cancelButton)
        confirmButton = findViewById(R.id.confirmButton)
        
        // 뒤로가기 버튼 설정
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    private fun setupHeader() {
        // 일정 이름 설정
        titleText.text = scheduleName
        
        // 날짜 정보 설정
        val startDateCalendar = Calendar.getInstance().apply { timeInMillis = startDate }
        val endDateCalendar = Calendar.getInstance().apply { timeInMillis = endDate }
        
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        val startDateStr = dateFormat.format(startDateCalendar.time)
        val endDateStr = dateFormat.format(endDateCalendar.time)
        dateRangeText.text = "$startDateStr ~ $endDateStr"
    }
    
    private fun setupTabs() {
        val tabContainer = findViewById<LinearLayout>(R.id.tabContainer)
        tabContainer.removeAllViews()
        
        // 날짜 계산
        val startDateCalendar = Calendar.getInstance().apply { timeInMillis = startDate }
        val endDateCalendar = Calendar.getInstance().apply { timeInMillis = endDate }
        val diffInMillis = endDateCalendar.timeInMillis - startDateCalendar.timeInMillis
        val dayCount = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt() + 1
        
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
            
            // 선택된 날짜 표시
            if (i == selectedDay) {
                selectionIndicator.visibility = View.VISIBLE
            } else {
                selectionIndicator.visibility = View.INVISIBLE
            }
            
            // 탭 클릭 비활성화 (이 화면에서는 탭 변경 불가)
            tabView.isClickable = false
            
            tabContainer.addView(tabView)
        }
    }
    
    private fun setupListeners() {
        // 위치 검색 버튼
        searchPlaceButton.setOnClickListener {
            val intent = Intent(this, AddPlaceActivity::class.java)
            startActivityForResult(intent, 100)
        }
        
        // 취소 버튼
        cancelButton.setOnClickListener {
            finish()
        }
        
        // 확인 버튼
        confirmButton.setOnClickListener {
            if (validateInputs()) {
                saveAndReturn()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        // 장소명 검증
        if (placeNameInput.text.isNullOrBlank()) {
            Toast.makeText(this, "장소명을 입력해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // 위치 검증
        if (placeAddress.isBlank()) {
            Toast.makeText(this, "위치를 선택해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun saveAndReturn() {
        val intent = Intent()
        intent.putExtra("PLACE_ID", placeId)
        intent.putExtra("PLACE_NAME", placeNameInput.text.toString())
        intent.putExtra("PLACE_ADDRESS", placeAddress)
        intent.putExtra("PLACE_LAT", placeLat)
        intent.putExtra("PLACE_LNG", placeLng)
        intent.putExtra("ESTIMATED_COST", estimatedCostInput.text.toString())
        intent.putExtra("MEMO", memoInput.text.toString())
        intent.putExtra("SELECTED_DAY", selectedDay)
        
        setResult(RESULT_OK, intent)
        finish()
    }
    
    // 위치 검색 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // AddPlaceActivity에서 선택한 위치 정보 가져오기
            placeName = data.getStringExtra("placeName") ?: ""
            placeAddress = data.getStringExtra("placeAddress") ?: ""
            placeLat = data.getDoubleExtra("placeLat", 0.0)
            placeLng = data.getDoubleExtra("placeLng", 0.0)
            
            // 주소에서 '대한민국' 제거
            placeAddress = removeCountryPrefix(placeAddress)
            
            // UI 업데이트
            placeNameInput.setText(placeName)
            placeAddressText.text = placeAddress
        }
    }
    
    // 주소에서 '대한민국' 접두어 제거
    private fun removeCountryPrefix(address: String): String {
        return address.replace("대한민국", "")
            .replace("^\\s+".toRegex(), "") // 앞쪽 공백 제거
            .trim()
    }
} 