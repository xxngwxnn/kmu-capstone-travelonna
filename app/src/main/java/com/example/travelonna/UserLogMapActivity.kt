package com.example.travelonna

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.example.travelonna.model.TravelLog
import com.example.travelonna.model.TravelPlace
import com.example.travelonna.api.UserLogsResponse
import com.example.travelonna.api.UserLogItem
import com.example.travelonna.api.PlanPlacesResponse
import com.example.travelonna.api.PlanPlace
import com.example.travelonna.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class UserLogMapActivity : AppCompatActivity(), OnMapReadyCallback {
    
    companion object {
        private const val TAG = "UserLogMapActivity"
        private const val DEFAULT_ZOOM = 12f
    }
    
    private lateinit var googleMap: GoogleMap
    private var job: Job? = null
    private var userLogs: List<UserLogItem> = emptyList()
    private var allPlaces: List<PlaceMarkerData> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_log_map)
        
        // 지도 초기화
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // 뒤로가기 버튼 설정
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.let { toolbar ->
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "내 여행 기록 지도"
        }
        
        // 사용자 로그 및 장소 데이터 로드
        loadUserLogsAndPlaces()
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // 지도 설정
        setupMap()
        
        // 장소가 이미 로드되었다면 마커 추가
        if (allPlaces.isNotEmpty()) {
            addRealPlaceMarkers()
        }
    }
    
    private fun loadUserLogsAndPlaces() {
        val userId = RetrofitClient.getUserId()
        
        if (userId == 0) {
            Log.w(TAG, "User ID not found, using dummy data")
            showErrorMessage("사용자 정보를 찾을 수 없습니다.")
            return
        }
        
        Log.d(TAG, "Loading user logs and places for userId: $userId")
        
        RetrofitClient.apiService.getUserLogs(userId).enqueue(object : Callback<UserLogsResponse> {
            override fun onResponse(call: Call<UserLogsResponse>, response: Response<UserLogsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    
                    if (apiResponse.success) {
                        Log.d(TAG, "User logs loaded: ${apiResponse.data.size} logs")
                        userLogs = apiResponse.data
                        
                        // 각 로그의 장소 정보 로드
                        loadAllPlacesFromLogs()
                    } else {
                        Log.w(TAG, "API returned success=false: ${apiResponse.message}")
                        showErrorMessage("로그 데이터를 불러올 수 없습니다: ${apiResponse.message}")
                    }
                } else {
                    Log.e(TAG, "Failed to load user logs: ${response.code()}")
                    when (response.code()) {
                        404 -> showErrorMessage("사용자를 찾을 수 없습니다.")
                        else -> showErrorMessage("로그 데이터를 불러올 수 없습니다.")
                    }
                }
            }
            
            override fun onFailure(call: Call<UserLogsResponse>, t: Throwable) {
                Log.e(TAG, "Network error while loading user logs", t)
                showErrorMessage("네트워크 오류가 발생했습니다.")
            }
        })
    }
    
    private fun loadAllPlacesFromLogs() {
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val allPlaceData = mutableListOf<PlaceMarkerData>()
                
                // 각 로그의 planId에 대해 병렬로 장소 정보 로드
                val deferredPlaces = userLogs.map { log ->
                    async {
                        loadPlacesForPlan(log)
                    }
                }
                
                // 모든 비동기 작업 완료 대기
                val results = deferredPlaces.awaitAll()
                results.forEach { places ->
                    allPlaceData.addAll(places)
                }
                
                withContext(Dispatchers.Main) {
                    allPlaces = allPlaceData
                    
                    if (allPlaces.isNotEmpty()) {
                        Log.d(TAG, "Total places loaded: ${allPlaces.size}")
                        // 지도가 준비되었다면 마커 추가
                        if (::googleMap.isInitialized) {
                            addRealPlaceMarkers()
                        }
                    } else {
                        Log.d(TAG, "No places with location data found")
                        Toast.makeText(this@UserLogMapActivity, 
                            "위치 정보가 있는 여행 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                        useDummyData()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading places", e)
                withContext(Dispatchers.Main) {
                    showErrorMessage("장소 정보 로드 중 오류가 발생했습니다.")
                }
            }
        }
    }
    
    private suspend fun loadPlacesForPlan(log: UserLogItem): List<PlaceMarkerData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getPlanPlaces(log.plan.planId).execute()
                
                if (response.isSuccessful && response.body() != null) {
                    val placesResponse = response.body()!!
                    
                    if (placesResponse.success) {
                        // 위치 정보가 있는 장소들만 필터링
                        placesResponse.data.filter { place ->
                            place.lat.isNotEmpty() && place.lon.isNotEmpty()
                        }.map { place ->
                            PlaceMarkerData(
                                name = place.name,
                                address = place.address,
                                latitude = place.lat.toDoubleOrNull() ?: 0.0,
                                longitude = place.lon.toDoubleOrNull() ?: 0.0,
                                memo = place.memo ?: "",
                                logComment = log.comment,
                                planTitle = log.plan.title,
                                visitDate = place.visitDate,
                                cost = place.cost,
                                day = place.day
                            )
                        }
                    } else {
                        Log.w(TAG, "Failed to load places for plan ${log.plan.planId}: ${placesResponse.message}")
                        emptyList()
                    }
                } else {
                    Log.e(TAG, "HTTP error loading places for plan ${log.plan.planId}: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading places for plan ${log.plan.planId}", e)
                emptyList()
            }
        }
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        // 에러 시 더미 데이터 사용
        useDummyData()
    }
    
    private fun setupMap() {
        try {
            // 지도 타입 설정
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            
            // 줌 컨트롤 활성화
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            
            // 한국 중심으로 초기 카메라 설정
            val korea = LatLng(37.5665, 126.9780) // 서울 중심
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(korea, 7f))
            
        } catch (e: Exception) {
            Log.e(TAG, "Map setup error", e)
        }
    }
    
    private fun addRealPlaceMarkers() {
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                // 실제 장소 데이터를 마커용 데이터로 변환
                val markerPlaces = allPlaces.map { place ->
                    val description = buildString {
                        append("🗓️ ${place.planTitle}")
                        if (place.logComment.isNotEmpty()) {
                            append("\n💭 ${place.logComment}")
                        }
                        if (place.memo.isNotEmpty()) {
                            append("\n📝 ${place.memo}")
                        }
                        if (place.cost > 0) {
                            append("\n💰 비용: ${String.format("%,d", place.cost)}원")
                        }
                        append("\n📍 ${place.day}일차")
                    }
                    
                    MarkerPlace(
                        name = place.name,
                        description = description,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        placeNames = listOf(place.address),
                        createdAt = place.visitDate
                    )
                }
                
                withContext(Dispatchers.Main) {
                    if (markerPlaces.isNotEmpty()) {
                        addMarkersToMap(markerPlaces)
                        adjustCameraToShowAllMarkers(markerPlaces)
                    } else {
                        Toast.makeText(this@UserLogMapActivity, 
                            "위치 정보가 있는 여행 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                        // 에러 시 더미 데이터 사용
                        useDummyData()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error adding markers", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserLogMapActivity, 
                        "마커 추가 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun addMarkersToMap(places: List<MarkerPlace>) {
        places.forEachIndexed { index, place ->
            val position = LatLng(place.latitude, place.longitude)
            
            // 더 상세한 마커 정보 구성
            val snippetText = buildString {
                append(place.description)
                if (place.placeNames.isNotEmpty()) {
                    append("\n📍 주소: ${place.placeNames.joinToString(", ")}")
                }
                append("\n📅 방문일: ${place.createdAt.split("T").firstOrNull() ?: place.createdAt}")
            }
            
            // 마커 옵션 설정
            val markerOptions = MarkerOptions()
                .position(position)
                .title("🏷️ ${place.name}")
                .snippet(snippetText)
                .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(index)))
            
            // 마커 추가
            googleMap.addMarker(markerOptions)
        }
        
        // 정보창 클릭 리스너 설정
        googleMap.setOnInfoWindowClickListener { marker ->
            Toast.makeText(this, 
                "${marker.title?.removePrefix("🏷️ ")}의 자세한 정보를 확인하시겠습니까?", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun adjustCameraToShowAllMarkers(places: List<MarkerPlace>) {
        if (places.size == 1) {
            // 장소가 하나만 있으면 해당 위치로 줌
            val place = places.first()
            val position = LatLng(place.latitude, place.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, DEFAULT_ZOOM))
        } else if (places.size > 1) {
            // 여러 장소가 있으면 모든 마커가 보이도록 카메라 조정
            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            places.forEach { place ->
                builder.include(LatLng(place.latitude, place.longitude))
            }
            val bounds = builder.build()
            val padding = 100 // 패딩 (픽셀)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }
    
    private fun getMarkerColor(index: Int): Float {
        // 인덱스에 따라 다른 색상의 마커 반환
        return when (index % 6) {
            0 -> BitmapDescriptorFactory.HUE_RED
            1 -> BitmapDescriptorFactory.HUE_BLUE
            2 -> BitmapDescriptorFactory.HUE_GREEN
            3 -> BitmapDescriptorFactory.HUE_ORANGE
            4 -> BitmapDescriptorFactory.HUE_VIOLET
            5 -> BitmapDescriptorFactory.HUE_CYAN
            else -> BitmapDescriptorFactory.HUE_RED
        }
    }
    
    private fun useDummyData() {
        // 더미 데이터로 마커 표시
        val dummyPlaces = listOf(
            MarkerPlace(
                name = "서울 여행",
                description = "경복궁, 북촌한옥마을, 명동 탐방",
                latitude = 37.5665,
                longitude = 126.9780,
                placeNames = listOf("경복궁", "북촌한옥마을", "명동"),
                createdAt = "2024-01-15"
            ),
            MarkerPlace(
                name = "부산 여행", 
                description = "해운대, 감천문화마을 방문",
                latitude = 35.1796,
                longitude = 129.0756,
                placeNames = listOf("해운대해수욕장", "감천문화마을"),
                createdAt = "2024-02-20"
            )
        )
        
        addMarkersToMap(dummyPlaces)
        adjustCameraToShowAllMarkers(dummyPlaces)
    }
    
    // 실제 장소 정보를 위한 데이터 클래스
    data class PlaceMarkerData(
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val memo: String,
        val logComment: String,
        val planTitle: String,
        val visitDate: String,
        val cost: Int,
        val day: Int
    )
    
    // 마커 표시용 데이터 클래스 (기존 호환성 유지)
    data class MarkerPlace(
        val name: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val placeNames: List<String>,
        val createdAt: String
    )
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
} 