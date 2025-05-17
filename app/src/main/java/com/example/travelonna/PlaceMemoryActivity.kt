package com.example.travelonna

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.example.travelonna.api.BasicResponse
import com.example.travelonna.api.PlaceCreateRequest
import com.example.travelonna.api.PlaceDetail
import com.example.travelonna.api.PlanDetailResponse
import com.example.travelonna.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class PlaceMemoryActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var placeNameTextView: TextView
    private lateinit var placeAddressTextView: TextView
    private lateinit var memoryEditText: EditText
    private lateinit var counterTextView: TextView
    private lateinit var uploadButton: Button
    private lateinit var imageContainer: RelativeLayout
    private lateinit var lockIconView: ImageView
    private lateinit var placesClient: PlacesClient
    
    private var selectedImageUri: Uri? = null
    private val MAX_SYMBOLS = 350
    private var isPublic = true  // 기본값은 공개 상태
    private var placeId = 0      // 장소 ID
    private var planId = 0       // 계획 ID
    private var googleId = ""    // Google Place ID 저장
    private var visitDate = ""   // 방문 날짜
    private var cost = 0         // 비용
    private var lat = ""         // 위도
    private var lon = ""         // 경도
    private var order = 1        // 순서
    private var memo = ""        // 메모
    private val TAG = "PlaceMemoryActivity"
    
    // 이미지 선택을 위한 ActivityResultLauncher 선언
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // 이미지 컨테이너의 배경을 선택한 이미지로 변경
            try {
                // 이미지 컨테이너의 배경을 투명하게 설정 (선택적)
                imageContainer.setBackgroundResource(android.R.color.transparent)
                
                // 이미지 컨테이너 내부의 안내 텍스트들을 숨김
                val childViews = imageContainer.getChildAt(0) as? android.view.ViewGroup
                childViews?.visibility = android.view.View.GONE
                
                // 배경 이미지 설정
                imageContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                imageContainer.background = android.graphics.drawable.BitmapDrawable(
                    resources, 
                    android.provider.MediaStore.Images.Media.getBitmap(contentResolver, it)
                )
                
                Toast.makeText(this, "이미지가 선택되었습니다", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_memory)
        
        // Google Places API 초기화
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        // UI 요소 초기화
        backButton = findViewById(R.id.backButton)
        placeNameTextView = findViewById(R.id.placeNameTextView)
        placeAddressTextView = findViewById(R.id.placeAddressTextView)
        memoryEditText = findViewById(R.id.memoryEditText)
        counterTextView = findViewById(R.id.counterText)
        uploadButton = findViewById(R.id.uploadButton)
        imageContainer = findViewById(R.id.imageContainer)
        lockIconView = findViewById(R.id.lockIconView)

        // Intent에서 데이터 가져오기
        val placeName = intent.getStringExtra("PLACE_NAME") ?: "동대구역"
        val placeAddress = intent.getStringExtra("PLACE_ADDRESS") ?: "대구광역시 동구 동대구로 550 (신암동 294)"
        
        // 추가 데이터 가져오기
        placeId = intent.getIntExtra("PLACE_ID", 0)
        planId = intent.getIntExtra("PLAN_ID", 0)
        isPublic = intent.getBooleanExtra("IS_PUBLIC", true)
        googleId = intent.getStringExtra("GOOGLE_ID") ?: ""
        visitDate = intent.getStringExtra("VISIT_DATE") ?: ""
        cost = intent.getIntExtra("COST", 0)
        memo = intent.getStringExtra("MEMO") ?: ""
        lat = intent.getStringExtra("LAT") ?: ""
        lon = intent.getStringExtra("LON") ?: ""
        order = intent.getIntExtra("ORDER", 1)
        
        Log.d(TAG, "받은 데이터 - placeId: $placeId, planId: $planId, googleId: $googleId, isPublic: $isPublic")

        // 장소 정보 표시
        placeNameTextView.text = placeName
        placeAddressTextView.text = placeAddress
        
        // 메모가 있으면 설정
        if (memo.isNotEmpty()) {
            memoryEditText.setText(memo)
        }
        
        // 자물쇠 아이콘 초기 상태 설정
        updateLockIcon()
        
        // 서버에서 장소 상세 정보 가져오기 (placeId와 planId가 있는 경우)
        if (placeId > 0 && planId > 0) {
            fetchPlaceDetail()
        }
        
        // Google 이미지 로드 (googleId가 있는 경우)
        if (googleId.isNotEmpty()) {
            loadPlaceImage(googleId)
        }
        
        // 자물쇠 아이콘 클릭 리스너 설정
        lockIconView.setOnClickListener {
            togglePublicStatus()
        }

        // 이미지 영역 클릭 리스너 설정
        imageContainer.setOnClickListener {
            pickImage.launch("image/*")
        }

        // 텍스트 입력 감지 및 카운터 업데이트
        memoryEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                counterTextView.text = "$currentLength/$MAX_SYMBOLS symbols"
                
                // 최대 글자수 제한
                if (currentLength > MAX_SYMBOLS) {
                    memoryEditText.setText(s?.subSequence(0, MAX_SYMBOLS))
                    memoryEditText.setSelection(MAX_SYMBOLS)
                }
            }
        })

        // 뒤로가기 버튼 설정
        backButton.setOnClickListener {
            finish()
        }

        // 업로드 버튼 설정
        uploadButton.setOnClickListener {
            val memoryText = memoryEditText.text.toString()
            if (memoryText.isEmpty()) {
                Toast.makeText(this, "여행 기록을 작성해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedImageUri == null) {
                Toast.makeText(this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 이미지와 텍스트가 모두 있는 경우 업로드 진행
            Toast.makeText(this, "여행 기록이 저장되었습니다", Toast.LENGTH_SHORT).show()
            
            // 업로드 완료 화면으로 이동
            val intent = Intent(this, UploadCompleteActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    // 장소 이미지 로드 메서드
    private fun loadPlaceImage(googlePlaceId: String) {
        if (googlePlaceId.isEmpty()) return
        
        Log.d(TAG, "Loading image for Google Place ID: $googlePlaceId")
        
        try {
            // Places Photo API 사용하여 이미지 로드
            val placeFields = listOf(Place.Field.PHOTO_METADATAS)
            val request = FetchPlaceRequest.newInstance(googlePlaceId, placeFields)
            
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
                        // 이미지 컨테이너의 배경을 투명하게 설정
                        imageContainer.setBackgroundResource(android.R.color.transparent)
                        
                        // 이미지 컨테이너 내부의 안내 텍스트들을 숨김
                        val childViews = imageContainer.getChildAt(0) as? android.view.ViewGroup
                        childViews?.visibility = android.view.View.GONE
                        
                        // 비트맵 설정
                        val bitmap = fetchPhotoResponse.bitmap
                        imageContainer.background = android.graphics.drawable.BitmapDrawable(resources, bitmap)
                        
                        Log.d(TAG, "Image loaded successfully for place ID: $googlePlaceId")
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to fetch photo: ${exception.message}")
                    }
                } else {
                    Log.d(TAG, "No photos found for place ID: $googlePlaceId")
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch place details: ${exception.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading place image", e)
        }
    }
    
    // 서버에서 장소 상세 정보 가져오기
    private fun fetchPlaceDetail() {
        Log.d(TAG, "Fetching place detail for planId: $planId, placeId: $placeId")
        
        // 일정 정보 가져오기 API 호출
        RetrofitClient.apiService.getPlanDetail(planId).enqueue(object : Callback<PlanDetailResponse> {
            override fun onResponse(call: Call<PlanDetailResponse>, response: Response<PlanDetailResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val planDetail = response.body()?.data
                    val places = planDetail?.places
                    
                    // 현재 장소 정보 찾기
                    val placeDetail = places?.find { it.id == placeId }
                    
                    placeDetail?.let { place ->
                        // 장소 정보 저장
                        googleId = place.googleId
                        visitDate = place.visitDate
                        cost = place.cost
                        lat = place.lat
                        lon = place.lon
                        order = place.order
                        isPublic = place.isPublic
                        
                        Log.d(TAG, "Place detail fetched: name=${place.name}, googleId=${place.googleId}, isPublic=${place.isPublic}")
                        
                        // UI 업데이트
                        updateLockIcon()
                        
                        // 메모가 있는 경우 텍스트 설정
                        if (place.memo.isNotEmpty()) {
                            memoryEditText.setText(place.memo)
                        }
                        
                        // Google 이미지 로드 (id가 있는 경우)
                        if (place.googleId.isNotEmpty()) {
                            loadPlaceImage(place.googleId)
                        }
                    } ?: Log.w(TAG, "Place not found in plan detail")
                } else {
                    Log.e(TAG, "Failed to fetch plan detail: ${response.code()}")
                }
            }
            
            override fun onFailure(call: Call<PlanDetailResponse>, t: Throwable) {
                Log.e(TAG, "Network error when fetching plan detail", t)
            }
        })
    }
    
    private fun updateLockIcon() {
        // 공개/비공개 상태에 따라 아이콘 변경
        lockIconView.setImageResource(
            if (!isPublic) R.drawable.ic_circle_lock else R.drawable.ic_circle_open
        )
    }
    
    private fun togglePublicStatus() {
        // placeId와 planId가 0이면 API 호출 없이 로컬만 변경
        if (placeId <= 0 || planId <= 0) {
            isPublic = !isPublic
            updateLockIcon()
            Toast.makeText(this, 
                "장소가 ${if (!isPublic) "비공개" else "공개"}로 설정되었습니다", 
                Toast.LENGTH_SHORT).show()
            return
        }
        
        // API 호출을 위한 요청 객체 생성
        val newIsPublic = !isPublic
        val request = PlaceCreateRequest(
            place = placeAddressTextView.text.toString(),
            isPublic = newIsPublic,
            visitDate = visitDate,  // 기존 방문 날짜 유지
            placeCost = cost,       // 기존 비용 유지
            memo = memoryEditText.text.toString(),
            lat = lat,              // 기존 위도 유지
            lon = lon,              // 기존 경도 유지
            name = placeNameTextView.text.toString(),
            order = order,          // 기존 순서 유지
            googleId = googleId     // 중요: Google Place ID 유지
        )
        
        Log.d(TAG, "Toggling public status - Request: place=${request.place}, isPublic=${request.isPublic}, googleId=${request.googleId}")
        
        // API 호출
        RetrofitClient.apiService.updatePlace(
            planId = planId,
            placeId = placeId,
            request = request
        ).enqueue(object : Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                if (response.isSuccessful) {
                    // 상태 업데이트
                    isPublic = newIsPublic
                    
                    // UI 업데이트
                    updateLockIcon()
                    
                    Toast.makeText(
                        this@PlaceMemoryActivity,
                        "${placeNameTextView.text} 장소가 ${if (!newIsPublic) "비공개" else "공개"}로 설정되었습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    Log.d(TAG, "장소 ID: $placeId 공개 상태가 $newIsPublic 로 업데이트되었습니다.")
                } else {
                    // 오류 처리
                    Log.e(TAG, "API 오류: ${response.code()}, ${response.message()}")
                    Toast.makeText(
                        this@PlaceMemoryActivity,
                        "설정 변경에 실패했습니다: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                // 네트워크 오류 처리
                Log.e(TAG, "네트워크 오류", t)
                Toast.makeText(
                    this@PlaceMemoryActivity,
                    "네트워크 오류: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Places API 클라이언트 명시적 종료 (메모리 누수 방지)
        try {
            val field = Places::class.java.getDeclaredField("zza")
            field.isAccessible = true
            val instance = field.get(null)
            
            val shutdownMethod = instance.javaClass.getDeclaredMethod("shutdown")
            shutdownMethod.isAccessible = true
            shutdownMethod.invoke(instance)
            
            Log.d(TAG, "Successfully shut down Places API client")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to shut down Places API client", e)
        }
    }
} 