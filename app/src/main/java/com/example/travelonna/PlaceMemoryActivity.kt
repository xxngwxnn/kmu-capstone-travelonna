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
import com.example.travelonna.api.PlaceDetailResponse
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import okhttp3.ResponseBody
import com.google.gson.GsonBuilder

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
    private var travelLogIsPublic = true // 여행 기록(travel log)의 공개 여부, 기본값은 공개
    private val TAG = "PlaceMemoryActivity"
    
    // 기존 기록 관련 변수들
    private var existingLogId: Int? = null
    private var isEditMode = false
    private var existingComment = ""
    private var existingImageUrls: List<String> = emptyList()
    
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
        
        // 토큰과 사용자 ID 확인
        val userId = RetrofitClient.getUserId()
        Log.d(TAG, "현재 사용자 ID: $userId, 계획 ID: $planId")
        
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
        
        // 기존 기록 조회 (planId가 있는 경우)
        if (planId > 0) {
            checkExistingTravelLog()
        }
        
        // 자물쇠 아이콘 클릭 리스너 설정
        lockIconView.setOnClickListener {
            toggleTravelLogPublicStatus()
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
            
            // 수정 모드가 아닌 경우에만 이미지 필수 체크
            if (!isEditMode && selectedImageUri == null) {
                Toast.makeText(this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 로딩 다이얼로그 표시
            val loadingDialog = android.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_loading)
                .setCancelable(false)
                .create()
            
            loadingDialog.show()
            
            // 수정 모드인지 확인하여 적절한 API 호출
            if (isEditMode && existingLogId != null) {
                Log.d(TAG, "여행 기록 수정 시작 - logId: $existingLogId, planId: $planId")
                updateTravelLog(memoryText, selectedImageUri, loadingDialog)
            } else {
                Log.d(TAG, "여행 기록 업로드 시작 - planId: $planId, 텍스트 길이: ${memoryText.length}")
                uploadTravelLog(memoryText, selectedImageUri, loadingDialog)
            }
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
        Log.d(TAG, "Fetching place detail for placeId: $placeId")
        
        // 장소 상세 정보 API 호출
        RetrofitClient.apiService.getPlaceDetail(placeId).enqueue(object : Callback<PlaceDetailResponse> {
            override fun onResponse(
                call: Call<PlaceDetailResponse>,
                response: Response<PlaceDetailResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val placeDetail = response.body()?.data
                    
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
                    } ?: Log.w(TAG, "Place detail is null")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "장소 정보를 가져오는데 실패했습니다"
                    Log.e(TAG, "API Error: $errorMsg")
                    Toast.makeText(this@PlaceMemoryActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<PlaceDetailResponse>, t: Throwable) {
                Log.e(TAG, "Network error when fetching place detail", t)
                Toast.makeText(this@PlaceMemoryActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun updateLockIcon() {
        // 여행 기록의 공개/비공개 상태에 따라 아이콘 변경
        lockIconView.setImageResource(
            if (!travelLogIsPublic) R.drawable.ic_circle_lock else R.drawable.ic_circle_open
        )
    }
    
    // 여행 기록(travel log)의 공개/비공개 상태 토글 함수
    private fun toggleTravelLogPublicStatus() {
        travelLogIsPublic = !travelLogIsPublic
        updateLockIcon()
        Toast.makeText(this, 
            "여행 기록이 ${if (!travelLogIsPublic) "비공개" else "공개"}로 설정되었습니다", 
            Toast.LENGTH_SHORT).show()
    }

    // 장소의 공개/비공개 상태 변경 함수는 유지하되 이름 변경
    private fun togglePlacePublicStatus() {
        // placeId와 planId가 0이면 API 호출 없이 로컬만 변경
        if (placeId <= 0 || planId <= 0) {
            isPublic = !isPublic
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
    
    // 여행 기록 업로드 메서드
    private fun uploadTravelLog(comment: String, imageUri: Uri?, loadingDialog: android.app.AlertDialog) {
        try {
            // 이미지 URL 처리 (이미지가 있으면 새 URL, 없으면 기본 URL)
            val imageUrl = if (imageUri != null) {
                "https://example.com/placeholder_image.jpg"
            } else {
                "https://example.com/default_image.jpg"
            }
            
            // 바로 여행 로그 생성 API 호출
            createTravelLog(comment, imageUrl, loadingDialog)
        } catch (e: Exception) {
            loadingDialog.dismiss()
            Log.e(TAG, "업로드 처리 중 오류 발생", e)
            Toast.makeText(this, "처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 여행 로그 생성 API 호출
    private fun createTravelLog(comment: String, imageUrl: String, loadingDialog: android.app.AlertDialog) {
        // API 문서 형식에 맞게 요청 객체 생성
        val requestBody = HashMap<String, Any>()
        requestBody["planId"] = planId
        requestBody["placeId"] = placeId
        requestBody["comment"] = comment
        requestBody["isPublic"] = travelLogIsPublic
        
        // 이미지 URL 리스트 추가
        val imageUrls = ArrayList<String>()
        imageUrls.add(imageUrl)
        requestBody["imageUrls"] = imageUrls
        
        // 인증 정보 확인
        val userId = RetrofitClient.getUserId()
        Log.d(TAG, "사용자 ID: $userId, 요청 계획 ID: $planId")
        
        // API 요청 로깅
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonRequest = gson.toJson(requestBody)
        Log.d(TAG, "여행 기록 생성 요청 URL: ${RetrofitClient.BASE_URL}api/v1/logs")
        Log.d(TAG, "여행 기록 생성 요청 본문:\n$jsonRequest")
        
        // API 호출
        RetrofitClient.apiService.createTravelLog(requestBody).enqueue(object : Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                loadingDialog.dismiss()
                
                // 응답 상세 로깅
                val responseCode = response.code()
                val responseBody = response.body()
                val errorBody = response.errorBody()?.string()
                
                Log.d(TAG, "여행 기록 생성 응답 코드: $responseCode")
                if (responseBody != null) {
                    Log.d(TAG, "여행 기록 생성 응답 본문: ${gson.toJson(responseBody)}")
                }
                if (errorBody != null) {
                    Log.e(TAG, "여행 기록 생성 에러 본문: $errorBody")
                    
                    // JSON 에러 메시지 파싱 시도
                    try {
                        val errorJson = JSONObject(errorBody)
                        val message = errorJson.optString("message", "알 수 없는 오류")
                        Log.e(TAG, "파싱된 에러 메시지: $message")
                        
                        // 소유권 관련 오류인 경우 더 자세한 정보 제공
                        if (message.contains("owner") || message.contains("not found")) {
                            Toast.makeText(
                                this@PlaceMemoryActivity,
                                "해당 여행 일정($planId)에 대한 권한이 없습니다. 사용자 ID: $userId",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@PlaceMemoryActivity,
                                "오류: $message",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON 파싱 오류", e)
                    }
                }
                
                if (response.isSuccessful) {
                    Log.d(TAG, "여행 로그 생성 성공")
                    Toast.makeText(this@PlaceMemoryActivity, "여행 기록이 저장되었습니다", Toast.LENGTH_SHORT).show()
                
                // 업로드 완료 화면으로 이동
                    val intent = Intent(this@PlaceMemoryActivity, UploadCompleteActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                    Log.e(TAG, "여행 로그 생성 실패: ${response.code()}, ${response.message()}")
                    Toast.makeText(
                        this@PlaceMemoryActivity, 
                        "여행 기록 저장에 실패했습니다: ${response.message()}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                loadingDialog.dismiss()
                Log.e(TAG, "여행 로그 생성 네트워크 오류", t)
                Log.e(TAG, "요청 URL: ${call.request().url}")
                Toast.makeText(
                    this@PlaceMemoryActivity, 
                    "네트워크 오류: 여행 기록 저장에 실패했습니다", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // 기존 여행 기록 조회
    private fun checkExistingTravelLog() {
        Log.d(TAG, "기존 여행 기록 조회 시작 - placeId: $placeId")
        
        RetrofitClient.apiService.getTravelLogsByPlace(placeId).enqueue(object : Callback<TravelLogResponse> {
            override fun onResponse(call: Call<TravelLogResponse>, response: Response<TravelLogResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val logs = response.body()?.data
                    if (!logs.isNullOrEmpty()) {
                        // 첫 번째 기록을 기존 기록으로 설정
                        val existingLog = logs.first()
                        existingLogId = existingLog.logId
                        existingComment = existingLog.comment
                        existingImageUrls = existingLog.imageUrls
                        travelLogIsPublic = existingLog.isPublic
                        isEditMode = true
                        
                        Log.d(TAG, "기존 기록 발견 - logId: ${existingLog.logId}, comment: ${existingLog.comment}")
                        
                        // UI 업데이트
                        memoryEditText.setText(existingComment)
                        updateLockIcon()
                        uploadButton.text = "수정"
                        
                        Toast.makeText(this@PlaceMemoryActivity, "기존 기록을 불러왔습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "기존 기록이 없습니다")
                        isEditMode = false
                        uploadButton.text = "업로드"
                    }
                } else {
                    Log.e(TAG, "기존 기록 조회 실패: ${response.code()}")
                    isEditMode = false
                    uploadButton.text = "업로드"
                }
            }
            
            override fun onFailure(call: Call<TravelLogResponse>, t: Throwable) {
                Log.e(TAG, "기존 기록 조회 중 네트워크 오류", t)
                isEditMode = false
                uploadButton.text = "업로드"
            }
        })
    }

    // 여행 기록 수정 메서드
    private fun updateTravelLog(comment: String, imageUri: Uri?, loadingDialog: android.app.AlertDialog) {
        try {
            // 기존 이미지 URL 사용 또는 새 이미지 URL
            val imageUrl = if (imageUri != null) {
                "https://example.com/new_image.jpg" // 새 이미지가 있으면 새 URL
            } else {
                existingImageUrls.firstOrNull() ?: "https://example.com/placeholder_image.jpg" // 기존 이미지 사용
            }
            
            // 여행 로그 수정 API 호출
            updateTravelLogApi(comment, imageUrl, loadingDialog)
        } catch (e: Exception) {
            loadingDialog.dismiss()
            Log.e(TAG, "수정 처리 중 오류 발생", e)
            Toast.makeText(this, "처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 여행 로그 수정 API 호출
    private fun updateTravelLogApi(comment: String, imageUrl: String, loadingDialog: android.app.AlertDialog) {
        // API 문서 형식에 맞게 요청 객체 생성
        val requestBody = HashMap<String, Any>()
        requestBody["planId"] = planId
        requestBody["placeId"] = placeId
        requestBody["comment"] = comment
        requestBody["isPublic"] = travelLogIsPublic
        
        // 이미지 URL 리스트 추가
        val imageUrls = ArrayList<String>()
        imageUrls.add(imageUrl)
        requestBody["imageUrls"] = imageUrls
        
        // 인증 정보 확인
        val userId = RetrofitClient.getUserId()
        Log.d(TAG, "사용자 ID: $userId, 수정할 로그 ID: $existingLogId")
        
        // API 요청 로깅
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonRequest = gson.toJson(requestBody)
        Log.d(TAG, "여행 기록 수정 요청 URL: ${RetrofitClient.BASE_URL}api/v1/logs/$existingLogId")
        Log.d(TAG, "여행 기록 수정 요청 본문:\n$jsonRequest")
        
        // API 호출
        RetrofitClient.apiService.updateTravelLog(existingLogId!!, requestBody).enqueue(object : Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                loadingDialog.dismiss()
                
                // 응답 상세 로깅
                val responseCode = response.code()
                val responseBody = response.body()
                val errorBody = response.errorBody()?.string()
                
                Log.d(TAG, "여행 기록 수정 응답 코드: $responseCode")
                if (responseBody != null) {
                    Log.d(TAG, "여행 기록 수정 응답 본문: ${gson.toJson(responseBody)}")
                }
                if (errorBody != null) {
                    Log.e(TAG, "여행 기록 수정 에러 본문: $errorBody")
                    
                    // JSON 에러 메시지 파싱 시도
                    try {
                        val errorJson = JSONObject(errorBody)
                        val message = errorJson.optString("message", "알 수 없는 오류")
                        Log.e(TAG, "파싱된 에러 메시지: $message")
                        
                        Toast.makeText(
                            this@PlaceMemoryActivity,
                            "오류: $message",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON 파싱 오류", e)
                    }
                }
                
                if (response.isSuccessful) {
                    Log.d(TAG, "여행 로그 수정 성공")
                    Toast.makeText(this@PlaceMemoryActivity, "여행 기록이 수정되었습니다", Toast.LENGTH_SHORT).show()
                
                    // 업로드 완료 화면으로 이동
                    val intent = Intent(this@PlaceMemoryActivity, UploadCompleteActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e(TAG, "여행 로그 수정 실패: ${response.code()}, ${response.message()}")
                    Toast.makeText(
                        this@PlaceMemoryActivity, 
                        "여행 기록 수정에 실패했습니다: ${response.message()}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                loadingDialog.dismiss()
                Log.e(TAG, "여행 로그 수정 네트워크 오류", t)
                Log.e(TAG, "요청 URL: ${call.request().url}")
                Toast.makeText(
                    this@PlaceMemoryActivity, 
                    "네트워크 오류: 여행 기록 수정에 실패했습니다", 
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