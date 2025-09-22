package com.example.travelonna

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.travelonna.api.ProfileCreateRequest
import com.example.travelonna.api.ProfileResponse
import com.example.travelonna.api.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import com.example.travelonna.util.CustomToast

class ProfileCreateActivity : AppCompatActivity() {
    
    private lateinit var emailEditText: EditText
    private lateinit var nicknameEditText: EditText
    private lateinit var introductionEditText: EditText
    private lateinit var profileAddIcon: ImageView
    
    private var selectedImageUri: Uri? = null
    private val TAG = "ProfileCreateActivity"
    
    // 편집 모드 관련 변수들
    private var isEditMode = false
    private var profileId: Int = 0
    private var currentProfileImageUrl: String? = null
    
    // 권한 요청 코드
    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1002
    }
    
    // Image picker result launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // 선택한 이미지를 프로필 이미지로 표시
            try {
                // 이미지를 선택한 경우, 원형에 꽉 차게 표시하기 위한 설정
                profileAddIcon.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                profileAddIcon.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                profileAddIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                profileAddIcon.setPadding(0, 0, 0, 0)
                profileAddIcon.setImageURI(it)
                
                // 이미지가 로드되었음을 로그로 확인
                Log.d(TAG, "이미지가 선택되었습니다: $it")
            } catch (e: Exception) {
                Log.e(TAG, "이미지 로드 중 오류 발생", e)
                CustomToast.error(this, "이미지를 불러올 수 없습니다.")
                // 오류 발생 시 기본 아이콘으로 복원
                resetProfileIconToDefault()
            }
        }
    }
    
    // 기본 아이콘 설정으로 되돌리는 함수
    private fun resetProfileIconToDefault() {
        profileAddIcon.layoutParams.width = dpToPx(48)
        profileAddIcon.layoutParams.height = dpToPx(48)
        profileAddIcon.scaleType = ImageView.ScaleType.FIT_CENTER
        profileAddIcon.setPadding(0, 0, 0, 0)
        profileAddIcon.setImageResource(R.drawable.ic_profile_add)
        profileAddIcon.requestLayout()
    }
    
    // dp를 픽셀로 변환하는 유틸리티 함수
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
    
    // 갤러리 권한 확인 및 이미지 선택
    private fun checkStoragePermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 이미 허용됨
                pickImage.launch("image/*")
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                // 권한 설명이 필요한 경우
                showStoragePermissionRationale()
            }
            else -> {
                // 권한 요청
                requestStoragePermission()
            }
        }
    }
    
    // 갤러리 권한 설명 다이얼로그
    private fun showStoragePermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("갤러리 접근 권한 필요")
            .setMessage("프로필 사진을 설정하기 위해 갤러리 접근 권한이 필요합니다.")
            .setPositiveButton("권한 허용") { _, _ ->
                requestStoragePermission()
            }
            .setNegativeButton("취소") { _, _ ->
                CustomToast.warning(this, "갤러리 접근 권한이 필요합니다.")
            }
            .show()
    }
    
    // 갤러리 권한 요청
    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }
    
    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 허용됨
                    CustomToast.success(this, "갤러리 접근 권한이 허용되었습니다.")
                    pickImage.launch("image/*")
                } else {
                    // 권한 거부됨
                    CustomToast.error(this, "갤러리 접근 권한이 거부되었습니다. 프로필 사진을 설정할 수 없습니다.")
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_create)
        
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        nicknameEditText = findViewById(R.id.nicknameEditText)
        introductionEditText = findViewById(R.id.introductionEditText)
        profileAddIcon = findViewById(R.id.profileAddIcon)
        
        // 기본 아이콘 설정은 XML에서 이미 되어 있음
        
        // Find the CardView parent (using simple findViewById instead of parent)
        val profileImageContainer = findViewById<CardView>(R.id.profileCardView)
        
        // Get user email from Google Sign In
        val account = GoogleSignIn.getLastSignedInAccount(this)
        account?.let {
            // Auto-fill the email field
            emailEditText.setText(it.email)
        }
        
        // Set up profile image picker
        profileImageContainer.setOnClickListener {
            checkStoragePermissionAndPickImage()
        }
        
        // Set up back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // 편집 모드 확인 및 데이터 설정
        checkEditModeAndSetupData()
        
        // Set up confirm button
        findViewById<ImageButton>(R.id.confirmButton).setOnClickListener {
            if (isEditMode) {
                updateProfile()
            } else {
                createProfile()
            }
        }
    }
    
    // 편집 모드 확인 및 데이터 설정
    private fun checkEditModeAndSetupData() {
        isEditMode = intent.getBooleanExtra("isEditMode", false)
        
        if (isEditMode) {
            // 편집 모드일 때 전달받은 데이터로 UI 설정
            val nickname = intent.getStringExtra("nickname") ?: ""
            val introduction = intent.getStringExtra("introduction") ?: ""
            val profileImageUrl = intent.getStringExtra("profileImageUrl") ?: ""
            profileId = intent.getIntExtra("profileId", 0)
            
            // 현재 프로필 이미지 URL 저장
            currentProfileImageUrl = if (profileImageUrl.isNotEmpty() && !profileImageUrl.contains("example.com")) {
                profileImageUrl
            } else {
                null
            }
            
            // UI에 기존 데이터 설정
            nicknameEditText.setText(nickname)
            introductionEditText.setText(introduction)
            
            // 프로필 이미지 로드 (URL이 있는 경우)
            currentProfileImageUrl?.let { imageUrl ->
                loadProfileImage(imageUrl)
            }
            
            Log.d(TAG, "편집 모드로 시작: profileId=$profileId, nickname=$nickname, imageUrl=$currentProfileImageUrl")
        }
    }
    
    // 기존 프로필 이미지 로드
    private fun loadProfileImage(imageUrl: String) {
        try {
            // 이미지를 원형에 꽉 차게 표시하기 위한 설정
            profileAddIcon.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            profileAddIcon.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            profileAddIcon.scaleType = ImageView.ScaleType.CENTER_CROP
            profileAddIcon.setPadding(0, 0, 0, 0)
            
            // Glide를 사용하여 이미지 로드
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile_add)
                .error(R.drawable.ic_profile_add)
                .centerCrop()
                .into(profileAddIcon)
                
            Log.d(TAG, "프로필 이미지 로드: $imageUrl")
        } catch (e: Exception) {
            Log.e(TAG, "프로필 이미지 로드 중 오류 발생", e)
            resetProfileIconToDefault()
        }
    }
    
    // 프로필 업데이트 (편집 모드)
    private fun updateProfile() {
        val nickname = nicknameEditText.text.toString().trim()
        val introduction = introductionEditText.text.toString().trim()
        
        // Validate input
        if (nickname.isEmpty()) {
            CustomToast.warning(this, "닉네임을 입력해주세요")
            return
        }
        
        Log.d(TAG, "프로필 업데이트 시작: profileId=$profileId")
        Log.d(TAG, "새 이미지 선택됨: ${selectedImageUri != null}")
        Log.d(TAG, "기존 이미지 URL: $currentProfileImageUrl")
        
        // Show loading
        CustomToast.info(this, "프로필을 수정 중입니다...")
        
        // 이미지가 새로 선택되었는지 확인
        if (selectedImageUri != null) {
            // 새 이미지와 함께 프로필 업데이트
            updateProfileWithNewImage(profileId, nickname, introduction)
        } else {
            // 이미지 변경 없이 프로필 업데이트 (기존 이미지 유지)
            updateProfileWithExistingImage(profileId, nickname, introduction)
        }
    }
    
    // 새 이미지와 함께 프로필 업데이트
    private fun updateProfileWithNewImage(profileId: Int, nickname: String, introduction: String) {
        try {
            // 1. URI에서 실제 파일 가져오기
            val imageFile = uriToFile(selectedImageUri!!)
            
            // 2. RequestBody 객체 생성
            val nicknamePart = nickname.toRequestBody("text/plain".toMediaTypeOrNull())
            val introductionPart = (if (introduction.isEmpty()) "여행을 좋아하는 직장인입니다." else introduction)
                .toRequestBody("text/plain".toMediaTypeOrNull())
            
            // 3. 이미지 파일 MIME 타입 확인 및 설정
            val mimeType = getMimeType(imageFile)
            if (!isValidImageMimeType(mimeType)) {
                Toast.makeText(this, "지원하지 않는 이미지 형식입니다.", Toast.LENGTH_LONG).show()
                updateProfileWithExistingImage(profileId, nickname, introduction)
                return
            }
            
            // 4. 이미지 파일 MultipartBody.Part로 변환
            val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData(
                "profileImage", 
                "profile_image.${getExtension(mimeType)}", 
                requestFile
            )
            
            Log.d(TAG, "프로필 업데이트 (이미지 포함) 요청 준비 완료 - profileId: $profileId")
            
            // 5. PUT API 호출 - 프로필 수정용 API 사용
            RetrofitClient.apiService.updateUserProfileWithImage(
                profileId, nicknamePart, introductionPart, imagePart
            ).enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    handleUpdateResponse(response)
                }
                
                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    handleUpdateFailure(call, t)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "프로필 업데이트 중 오류 발생", e)
            Toast.makeText(this, "이미지 처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            updateProfileWithExistingImage(profileId, nickname, introduction)
        }
    }
    
    // 기존 이미지를 유지하면서 프로필 업데이트
    private fun updateProfileWithExistingImage(profileId: Int, nickname: String, introduction: String) {
        // Multipart 형식으로 데이터 준비 (기존 이미지 유지)
        val nicknamePart = nickname.toRequestBody("text/plain".toMediaTypeOrNull())
        val introductionPart = (if (introduction.isEmpty()) "여행을 좋아하는 직장인입니다." else introduction)
            .toRequestBody("text/plain".toMediaTypeOrNull())
        
        Log.d(TAG, "프로필 업데이트 (기존 이미지 유지) 요청 - profileId: $profileId")
        Log.d(TAG, "기존 이미지 URL: $currentProfileImageUrl")
        Log.d(TAG, "업데이트 데이터: nickname=$nickname, introduction=$introduction")
        
        // PUT API 호출 - Multipart 형식으로 프로필 수정 (이미지는 null로 전달하여 기존 이미지 유지)
        RetrofitClient.apiService.updateUserProfileWithImage(
            profileId, nicknamePart, introductionPart, null
        ).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                handleUpdateResponse(response)
            }
            
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                handleUpdateFailure(call, t)
            }
        })
    }
    
    // 프로필 업데이트 응답 처리
    private fun handleUpdateResponse(response: Response<ProfileResponse>) {
        val responseCode = response.code()
        val responseBody = response.body()
        val errorBody = response.errorBody()?.string()
        
        Log.d(TAG, "프로필 업데이트 응답 코드: $responseCode")
        
        if (response.isSuccessful && response.body() != null) {
            CustomToast.success(this@ProfileCreateActivity, "프로필이 수정되었습니다")
            
            // 프로필 페이지로 돌아가기
            finish()
        } else {
            Log.e(TAG, "프로필 업데이트 실패: ${response.code()}, ${response.message()}")
            if (errorBody != null) {
                Log.e(TAG, "에러 본문: $errorBody")
            }
            CustomToast.error(this@ProfileCreateActivity, "프로필 수정에 실패했습니다")
        }
    }
    
    // 프로필 업데이트 실패 처리
    private fun handleUpdateFailure(call: Call<ProfileResponse>, t: Throwable) {
        Log.e(TAG, "프로필 업데이트 네트워크 오류", t)
        Toast.makeText(this@ProfileCreateActivity, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun createProfile() {
        val nickname = nicknameEditText.text.toString().trim()
        val introduction = introductionEditText.text.toString().trim()
        
        // Validate input
        if (nickname.isEmpty()) {
            Toast.makeText(this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 로그인 시 저장된 실제 userId 가져오기
        val userId = RetrofitClient.getUserId()
        if (userId <= 0) {
            Log.e(TAG, "Invalid userId: $userId, using default value 1")
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show()
            // 오류 상황에서도 계속 진행 (기본값 사용)
        }
        
        // 상세 요청 로깅
        Log.d(TAG, "프로필 생성 요청 URL: ${RetrofitClient.BASE_URL}api/v1/profiles")
        Log.d(TAG, "사용된 userId: $userId")
        Log.d(TAG, "선택된 이미지 URI: $selectedImageUri")
        
        // Show loading
        Toast.makeText(this, "프로필을 생성 중입니다...", Toast.LENGTH_SHORT).show()
        
        // 이미지가 선택되었는지 확인
        if (selectedImageUri != null) {
            // Multipart 요청으로 이미지 업로드
            uploadProfileWithImage(userId, nickname, introduction)
        } else {
            // 이미지 없이 기본 JSON 요청으로 프로필 생성
            uploadProfileWithoutImage(userId, nickname, introduction)
        }
    }
    
    // 이미지와 함께 프로필 업로드
    private fun uploadProfileWithImage(userId: Int, nickname: String, introduction: String) {
        try {
            // 1. URI에서 실제 파일 가져오기
            val imageFile = uriToFile(selectedImageUri!!)
            
            // 이미지 크기 확인 및 로그 출력
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
            val imageWidth = bitmap.width
            val imageHeight = bitmap.height
            val fileSize = imageFile.length() / 1024 // KB 단위
            Log.d(TAG, "업로드 이미지 정보: ${imageWidth}x${imageHeight} 픽셀, 파일 크기: ${fileSize}KB, 파일명: ${imageFile.name}")
            
            // 2. RequestBody 객체 생성
            val userIdPart = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val nicknamePart = nickname.toRequestBody("text/plain".toMediaTypeOrNull())
            val introductionPart = (if (introduction.isEmpty()) "여행을 좋아하는 직장인입니다." else introduction)
                .toRequestBody("text/plain".toMediaTypeOrNull())
            
            // 3. 이미지 파일 MIME 타입 확인 및 설정
            val mimeType = getMimeType(imageFile)
            if (!isValidImageMimeType(mimeType)) {
                Toast.makeText(this, "지원하지 않는 이미지 형식입니다. 지원 형식: JPEG, PNG, GIF, BMP, WEBP", Toast.LENGTH_LONG).show()
                uploadProfileWithoutImage(userId, nickname, introduction)
                return
            }
            
            // 4. 이미지 파일 MultipartBody.Part로 변환 (구체적인 MIME 타입 지정)
            val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData(
                "profileImage", 
                "profile_image.${getExtension(mimeType)}", 
                requestFile
            )
            
            Log.d(TAG, "Multipart 요청 준비 완료: userId=$userId, nickname=$nickname, imageFile=${imageFile.name}, mimeType=$mimeType")
            
            // 5. API 호출
            RetrofitClient.apiService.createUserProfileWithImage(
                userIdPart, nicknamePart, introductionPart, imagePart
            ).enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    handleProfileResponse(response)
                }
                
                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    handleProfileFailure(call, t)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "이미지 업로드 중 오류 발생", e)
            Toast.makeText(this, "이미지 처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            
            // 오류 발생 시 이미지 없이 업로드 시도
            uploadProfileWithoutImage(userId, nickname, introduction)
        }
    }
    
    // MIME 타입을 확인하는 함수
    private fun getMimeType(file: File): String {
        val filename = file.name
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            else -> "image/jpeg" // 기본값
        }
    }
    
    // 지원하는 이미지 형식인지 확인
    private fun isValidImageMimeType(mimeType: String): Boolean {
        return mimeType in listOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp"
        )
    }
    
    // MIME 타입에서 확장자 추출
    private fun getExtension(mimeType: String): String {
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/bmp" -> "bmp"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }
    
    // 이미지 없이 프로필 업로드
    private fun uploadProfileWithoutImage(userId: Int, nickname: String, introduction: String) {
        // 기본 이미지 파일 가져오기
        try {
            val inputStream = resources.openRawResource(R.drawable.profile_basic)
            val file = File(cacheDir, "profile_basic.png")
            
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            
            // RequestBody 객체 생성
            val userIdPart = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val nicknamePart = nickname.toRequestBody("text/plain".toMediaTypeOrNull())
            val introductionPart = (if (introduction.isEmpty()) "여행을 좋아하는 직장인입니다." else introduction)
                .toRequestBody("text/plain".toMediaTypeOrNull())
            
            // 기본 이미지 파일 MultipartBody.Part로 변환
            val requestFile = file.asRequestBody("image/png".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData(
                "profileImage",
                "profile_basic.png",
                requestFile
            )
            
            Log.d(TAG, "기본 이미지로 프로필 생성 시도")
            
            // Multipart API 호출
            RetrofitClient.apiService.createUserProfileWithImage(
                userIdPart, nicknamePart, introductionPart, imagePart
            ).enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    handleProfileResponse(response)
                }
                
                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    handleProfileFailure(call, t)
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "기본 이미지 처리 중 오류 발생", e)
            
            // 기본 이미지 처리 실패 시 이미지 없이 JSON 요청으로 진행
            val request = ProfileCreateRequest(
                userId = userId,
                nickname = nickname,
                profileImageUrl = "", // 빈 문자열로 설정
                introduction = if (introduction.isEmpty()) "여행을 좋아하는 직장인입니다." else introduction
            )
            
            RetrofitClient.apiService.createUserProfile(request).enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    handleProfileResponse(response)
                }
                
                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    handleProfileFailure(call, t)
                }
            })
        }
    }
    
    // 프로필 생성 응답 처리
    private fun handleProfileResponse(response: Response<ProfileResponse>) {
        // 응답 상세 로깅
        val responseCode = response.code()
        val responseBody = response.body()
        val errorBody = response.errorBody()?.string()
        
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        Log.d(TAG, "프로필 생성 응답 코드: $responseCode")
        Log.d(TAG, "프로필 생성 응답 본문: ${gson.toJson(responseBody)}")
        if (errorBody != null) {
            Log.e(TAG, "프로필 생성 에러 본문: $errorBody")
        }
        
        if (response.isSuccessful && response.body() != null) {
            // Profile created successfully
            Toast.makeText(this@ProfileCreateActivity, "프로필이 생성되었습니다", Toast.LENGTH_SHORT).show()
            
            // Navigate to HomeActivity (main screen)
            val intent = Intent(this@ProfileCreateActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Error creating profile
            Log.e(TAG, "Error creating profile: ${response.code()}, ${response.message()}")
            Toast.makeText(this@ProfileCreateActivity, "프로필 생성에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 프로필 생성 실패 처리
    private fun handleProfileFailure(call: Call<ProfileResponse>, t: Throwable) {
        Log.e(TAG, "Network error when creating profile", t)
        Log.e(TAG, "요청 URL: ${call.request().url}")
        Toast.makeText(this@ProfileCreateActivity, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
    }
    
    // URI를 File로 변환하는 유틸리티 함수
    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "profile_image_${System.currentTimeMillis()}.jpg")
        
        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        
        inputStream?.close()
        return file
    }
} 