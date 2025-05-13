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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.bumptech.glide.Glide
import android.util.Log
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPhotoResponse
import com.example.travelonna.api.PlaceCreateRequest
import com.example.travelonna.api.PlaceCreateResponse
import com.example.travelonna.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
    private lateinit var privacyToggle: com.example.travelonna.view.CustomToggleButton
    private lateinit var privacyPrivateText: TextView
    private lateinit var privacyPublicText: TextView
    
    // Places API
    private lateinit var placesClient: PlacesClient
    
    private var selectedDay: Int = 0
    private var startDate: Long = 0
    private var endDate: Long = 0
    private var scheduleName: String = ""
    private var isPublic: Boolean = true  // 기본값을 true(공개)로 변경
    
    // 위치 정보
    private var placeId: String = ""
    private var placeName: String = ""
    private var placeAddress: String = ""
    private var placeLat: Double = 0.0
    private var placeLng: Double = 0.0
    
    // 로그 태그
    private val TAG = "PlaceInfoActivity"
    
    private var planId: Int = 0
    
    // 편집 모드 관련 변수
    private var isEditMode: Boolean = false
    private var existingPlaceId: Int = 0  // 서버에서의 장소 ID (편집 시 사용)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_info)
        
        // Places API 초기화
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)
        
        // 인텐트에서 데이터 가져오기
        selectedDay = intent.getIntExtra("SELECTED_DAY", 0)
        startDate = intent.getLongExtra("START_DATE", System.currentTimeMillis())
        endDate = intent.getLongExtra("END_DATE", System.currentTimeMillis())
        scheduleName = intent.getStringExtra("SCHEDULE_NAME") ?: "일정"
        planId = intent.getIntExtra("PLAN_ID", 0)
        
        // 편집 모드 확인
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        
        // 뷰 초기화
        initViews()
        
        // 상단 정보 표시
        setupHeader()
        
        // 탭 설정
        setupTabs()
        
        // 편집 모드인 경우 기존 데이터 로드
        if (isEditMode) {
            loadExistingPlaceData()
        }
        
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
        privacyToggle = findViewById(R.id.privacyToggle)
        privacyPrivateText = findViewById(R.id.privacyPrivateText)
        privacyPublicText = findViewById(R.id.privacyPublicText)
        
        // 뒤로가기 버튼 설정
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // 공개 여부 토글 설정 - 기본값은 공개(true)
        privacyToggle.setChecked(isPublic)
        updatePrivacyTextColors(isPublic)
        
        privacyToggle.setOnCheckedChangeListener { isChecked ->
            isPublic = isChecked
            updatePrivacyTextColors(isChecked)
        }
        
        // 텍스트 클릭 시에도 토글 상태 변경
        privacyPrivateText.setOnClickListener {
            privacyToggle.setChecked(false)
            isPublic = false
            updatePrivacyTextColors(false)
        }
        
        privacyPublicText.setOnClickListener {
            privacyToggle.setChecked(true)
            isPublic = true
            updatePrivacyTextColors(true)
        }
        
        // 편집 모드에 따라 UI 조정
        if (isEditMode) {
            confirmButton.text = "수정하기"  // 버튼 텍스트 변경
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
            intent.putExtra("PLAN_ID", planId)
            Log.d(TAG, "Sending Plan ID: $planId to AddPlaceActivity")
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
        
        // 계획 ID 검증
        Log.d(TAG, "Plan ID: $planId")
        if (planId <= 0) {
            Toast.makeText(this, "유효한 일정 ID가 없습니다. 일정을 다시 생성해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun saveAndReturn() {
        if (planId > 0) {
            if (isEditMode) {
                // 기존 장소 수정 API 호출
                updatePlace()
            } else {
                // 새 장소 생성 API 호출
                createPlace()
            }
        } else {
            // 로컬 데이터만 반환 (이전 동작 유지)
            returnLocalData()
        }
    }
    
    private fun createPlace() {
        // 로딩 다이얼로그 표시
        val loadingDialog = android.app.AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // 날짜 형식 변환
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        calendar.add(Calendar.DAY_OF_MONTH, selectedDay)
        
        val visitDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val visitDate = visitDateFormat.format(calendar.time)
        
        // 비용 파싱 (예외 처리)
        val costStr = estimatedCostInput.text.toString()
        val cost = if (costStr.isEmpty()) 0 else try {
            costStr.toInt()
        } catch (e: Exception) {
            0
        }
        
        // 사용자가 입력한 장소명 가져오기 (이것이 실제 name 필드에 들어가야 함)
        val placeName = placeNameInput.text.toString().trim()
        
        // 로그 추가: 실제 전송 데이터 표시
        Log.d(TAG, "Creating place with data:")
        Log.d(TAG, "Plan ID: $planId")
        Log.d(TAG, "Place Name (사용자 입력): $placeName")
        Log.d(TAG, "Place Address: $placeAddress")
        Log.d(TAG, "Visit Date: $visitDate")
        Log.d(TAG, "Is Public: $isPublic")
        Log.d(TAG, "Cost: $cost")
        Log.d(TAG, "Coordinates: $placeLat, $placeLng")
        
        // 요청 모델 생성
        val placeRequest = PlaceCreateRequest(
            place = placeAddress,  // address 필드에는 주소를 보냄
            isPublic = isPublic,
            visitDate = visitDate,
            placeCost = cost,
            memo = memoInput.text.toString(),
            lat = placeLat.toString(),
            lon = placeLng.toString(),
            name = placeName,  // name 필드에는 사용자가 입력한 장소명을 보냄
            order = 1, // 서버에서 자동 할당하도록 1로 설정
            googleId = placeId
        )
        
        // API 요청 로그
        Log.d(TAG, "API Request: $placeRequest")
        
        // API 호출
        RetrofitClient.apiService.createPlace(planId, placeRequest)
            .enqueue(object: Callback<PlaceCreateResponse> {
                override fun onResponse(call: Call<PlaceCreateResponse>, response: Response<PlaceCreateResponse>) {
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val placeData = response.body()?.data
                        Log.d(TAG, "Place added successfully with response: ${response.body()}")
                        Log.d(TAG, "Added place - Name: ${placeData?.name}, Address: ${placeData?.address}")
                        
                        // 반환된 이름이 요청한 이름과 다른지 확인
                        if (placeData?.name != placeRequest.name) {
                            Log.w(TAG, "⚠️ Warning: Sent name '${placeRequest.name}' but received '${placeData?.name}'")
                        }
                        
                        Toast.makeText(this@PlaceInfoActivity, "장소가 추가되었습니다", Toast.LENGTH_SHORT).show()
                        
                        // 결과 전달 및 액티비티 종료
                        val resultIntent = Intent()
                        resultIntent.putExtra("PLACE_ADDED", true)
                        resultIntent.putExtra("SELECTED_DAY", selectedDay)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        // 에러 처리
                        val errorMsg = response.errorBody()?.string() ?: "장소 추가 중 오류가 발생했습니다"
                        Toast.makeText(this@PlaceInfoActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "API Error: $errorMsg")
                        Log.e(TAG, "Response code: ${response.code()}")
                        Log.e(TAG, "Request sent: name='${placeRequest.name}', place='${placeRequest.place}'")
                    }
                }
                
                override fun onFailure(call: Call<PlaceCreateResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@PlaceInfoActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Network Error: ${t.message}")
                }
            })
    }
    
    // 이전 방식 (로컬 데이터 반환)
    private fun returnLocalData() {
        val intent = Intent()
        intent.putExtra("PLACE_ID", placeId)
        intent.putExtra("PLACE_NAME", placeNameInput.text.toString())
        intent.putExtra("PLACE_ADDRESS", placeAddress)
        intent.putExtra("PLACE_LAT", placeLat)
        intent.putExtra("PLACE_LNG", placeLng)
        intent.putExtra("ESTIMATED_COST", estimatedCostInput.text.toString())
        intent.putExtra("MEMO", memoInput.text.toString())
        intent.putExtra("SELECTED_DAY", selectedDay)
        intent.putExtra("IS_PUBLIC", isPublic)
        
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
            placeId = data.getStringExtra("placeId") ?: ""
            
            // 주소에서 '대한민국' 제거
            placeAddress = removeCountryPrefix(placeAddress)
            
            Log.d(TAG, "Received place data - name: '$placeName', address: '$placeAddress'")
            
            // UI 업데이트 - 이제 장소명이 올바르게 설정되었는지 확인
            if (placeName.isNotEmpty()) {
                placeNameInput.setText(placeName)  // 장소명 필드에 실제 장소명 설정
                Log.d(TAG, "Setting place name input to: '$placeName'")
            } else {
                // 장소명이 비어있다면 주소를 기반으로 임시 이름 설정
                placeNameInput.setText(placeAddress.split(",").firstOrNull() ?: "")
                Log.d(TAG, "Place name was empty, using address part instead")
            }
            
            placeAddressText.text = placeAddress  // 주소 필드에 주소 설정
            
            // 장소 이미지 로드
            loadPlaceImage()
        }
    }
    
    // 주소에서 '대한민국' 접두어 제거
    private fun removeCountryPrefix(address: String): String {
        return address.replace("대한민국", "")
            .replace("^\\s+".toRegex(), "") // 앞쪽 공백 제거
            .trim()
    }
    
    // Google Places API를 사용하여 장소 이미지 로드
    private fun loadPlaceImage() {
        if (placeId.isNotEmpty()) {
            try {
                Log.d(TAG, "Loading image for place ID: $placeId")
                
                // Places Photo API를 사용하여 사진 요청
                val placeFields = listOf(Place.Field.PHOTO_METADATAS)
                val request = FetchPlaceRequest.builder(placeId, placeFields).build()
                
                placesClient.fetchPlace(request).addOnSuccessListener { response ->
                    val place = response.place
                    val metadatas = place.photoMetadatas
                    
                    if (metadatas == null || metadatas.isEmpty()) {
                        Log.d(TAG, "No photos found for this place")
                        return@addOnSuccessListener
                    }
                    
                    // 첫 번째 사진 가져오기
                    val photoMetadata = metadatas[0]
                    
                    // 사진 가져오기 요청 생성
                    val photoRequest = FetchPhotoRequest.builder(photoMetadata)
                        .setMaxWidth(800) // 이미지 최대 폭 설정
                        .setMaxHeight(480) // 이미지 최대 높이 설정
                        .build()
                    
                    // 사진 가져오기
                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener { fetchPhotoResponse ->
                        val bitmap = fetchPhotoResponse.bitmap
                        // 안내 메시지 텍스트 숨기기
                        findViewById<LinearLayout>(R.id.infoTextContainer).visibility = View.GONE
                        
                        // 이미지 표시
                        val infoImage = findViewById<ImageView>(R.id.infoImage)
                        val imageCardView = findViewById<androidx.cardview.widget.CardView>(R.id.imageCardView)
                        imageCardView.visibility = View.VISIBLE
                        infoImage.setImageBitmap(bitmap)
                        
                        // 컨테이너 스타일 변경
                        val infoContainer = findViewById<LinearLayout>(R.id.infoContainer)
                        infoContainer.background = null
                        
                        Log.d(TAG, "Image loaded successfully")
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to fetch photo", exception)
                        Toast.makeText(this, "이미지를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to fetch place", exception)
                    Toast.makeText(this, "장소 정보를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading place image", e)
                Toast.makeText(this, "이미지를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 공개/비공개 텍스트 색상 업데이트
    private fun updatePrivacyTextColors(isPublic: Boolean) {
        if (isPublic) {
            privacyPublicText.setTextColor(ContextCompat.getColor(this, R.color.blue))
            privacyPrivateText.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
        } else {
            privacyPublicText.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
            privacyPrivateText.setTextColor(ContextCompat.getColor(this, R.color.blue))
        }
    }
    
    // 기존 장소 데이터 로드 (편집 모드)
    private fun loadExistingPlaceData() {
        try {
            existingPlaceId = intent.getIntExtra("PLACE_ID", 0)
            val placeName = intent.getStringExtra("PLACE_NAME") ?: ""
            val placeAddress = intent.getStringExtra("PLACE_ADDRESS") ?: ""
            val googlePlaceId = intent.getStringExtra("GOOGLE_PLACE_ID") ?: ""
            val estimatedCost = intent.getStringExtra("ESTIMATED_COST") ?: "0"
            val memo = intent.getStringExtra("MEMO") ?: ""
            isPublic = intent.getBooleanExtra("IS_PUBLIC", true)
            
            // UI에 데이터 표시
            placeNameInput.setText(placeName)
            placeAddressText.text = placeAddress
            estimatedCostInput.setText(estimatedCost)
            memoInput.setText(memo)
            
            // 공개 여부 설정
            privacyToggle.setChecked(isPublic)
            updatePrivacyTextColors(isPublic)
            
            // Google Place ID가 있으면 저장하고 이미지 로드
            this.placeId = googlePlaceId
            this.placeName = placeName
            this.placeAddress = placeAddress
            
            if (googlePlaceId.isNotEmpty()) {
                loadPlaceImage()
            }
            
            Log.d(TAG, "Loaded existing place: ID=$existingPlaceId, name=$placeName, googleId=$googlePlaceId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading existing place data", e)
            Toast.makeText(this, "장소 정보를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 장소 수정 API 호출
    private fun updatePlace() {
        // 로딩 다이얼로그 표시
        val loadingDialog = android.app.AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // 날짜 형식 변환
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        calendar.add(Calendar.DAY_OF_MONTH, selectedDay)
        
        val visitDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val visitDate = visitDateFormat.format(calendar.time)
        
        // 비용 파싱 (예외 처리)
        val costStr = estimatedCostInput.text.toString()
        val cost = if (costStr.isEmpty()) 0 else try {
            costStr.toInt()
        } catch (e: Exception) {
            0
        }
        
        // 사용자가 입력한 장소명 가져오기
        val placeName = placeNameInput.text.toString().trim()
        
        // 요청 모델 생성 (createPlace와 동일한 모델 사용)
        val placeRequest = PlaceCreateRequest(
            place = placeAddress,  // address 필드에는 주소를 보냄
            isPublic = isPublic,
            visitDate = visitDate,
            placeCost = cost,
            memo = memoInput.text.toString(),
            lat = placeLat.toString(),
            lon = placeLng.toString(),
            name = placeName,  // name 필드에는 사용자가 입력한 장소명을 보냄
            order = 1, // 서버에서 자동 할당하도록 1로 설정
            googleId = placeId
        )
        
        // API 요청 로그
        Log.d(TAG, "Updating place with ID: $existingPlaceId")
        Log.d(TAG, "API Request: $placeRequest")
        
        // API 호출 (업데이트용 API 엔드포인트 사용)
        RetrofitClient.apiService.updatePlace(planId, existingPlaceId, placeRequest)
            .enqueue(object: Callback<com.example.travelonna.api.BasicResponse> {
                override fun onResponse(call: Call<com.example.travelonna.api.BasicResponse>, response: Response<com.example.travelonna.api.BasicResponse>) {
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        Log.d(TAG, "Place updated successfully: ${response.body()}")
                        Toast.makeText(this@PlaceInfoActivity, "장소가 수정되었습니다", Toast.LENGTH_SHORT).show()
                        
                        // 결과 전달 및 액티비티 종료
                        val resultIntent = Intent()
                        resultIntent.putExtra("PLACE_UPDATED", true)
                        resultIntent.putExtra("SELECTED_DAY", selectedDay)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        // 에러 처리
                        val errorMsg = response.errorBody()?.string() ?: "장소 수정 중 오류가 발생했습니다"
                        Toast.makeText(this@PlaceInfoActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "API Error: $errorMsg")
                        Log.e(TAG, "Response code: ${response.code()}")
                    }
                }
                
                override fun onFailure(call: Call<com.example.travelonna.api.BasicResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@PlaceInfoActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Network Error: ${t.message}")
                }
            })
    }
} 