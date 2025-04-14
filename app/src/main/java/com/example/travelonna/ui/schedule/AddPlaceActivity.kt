package com.example.travelonna.ui.schedule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.location.Geocoder
import com.example.travelonna.R
import com.example.travelonna.databinding.ActivityAddPlaceBinding
import com.google.android.gms.maps.model.MapStyleOptions
import java.util.*
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import android.util.Log

class AddPlaceActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val TAG = "AddPlaceActivity"
    }

    private lateinit var binding: ActivityAddPlaceBinding
    private lateinit var map: GoogleMap
    private var selectedPlace: PlaceInfo? = null
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var placesClient: PlacesClient
    private lateinit var adapter: PlaceSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 바인딩 초기화 및 콘텐츠 뷰 설정
        binding = ActivityAddPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Places API 초기화
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        // 디버그 로그 추가
        val planId = intent.getIntExtra("PLAN_ID", 0)
        Log.d("AddPlaceActivity", "Received Plan ID: $planId")
        
        placesClient = Places.createClient(this)
        
        // 뷰 초기화 및 설정
        setupViews()
        
        // 초기 UI 상태 설정
        updateNoticeVisibility(true)
    }

    private fun setupViews() {
        setupToolbar()
        setupMap()
        setupSearchView()
        setupAddButton()
        setupRecyclerView()
        
        // 버튼 리스너 설정
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 장소 추가 버튼 상태 업데이트
        binding.addButton.isEnabled = false
        
        // 추가: planId 가져오기
        Log.d(TAG, "In setupViews - Plan ID: ${intent.getIntExtra("PLAN_ID", 0)}")
    }

    private fun setupListeners() {
        // 뒤로가기 버튼
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 장소 추가 버튼 상태 업데이트
        binding.addButton.isEnabled = false
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupSearchView() {
        searchView = findViewById(R.id.searchView)
        Log.d(TAG, "SearchView setup started")
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(TAG, "Search submitted: $query")
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d(TAG, "Search text changed: $newText")
                if (!newText.isNullOrEmpty() && newText.length >= 2) {
                    performSearch(newText)
                }
                return true
            }
        })
    }

    private fun setupAddButton() {
        binding.addButton.setOnClickListener {
            selectedPlace?.let { place ->
                Log.d(TAG, "Selected Place - Name: ${place.name}, Address: ${place.address}")
                
                val intent = Intent().apply {
                    putExtra("placeName", place.name)  // 장소명 필드에 실제 장소명 전달
                    putExtra("placeAddress", place.address)  // 주소 필드에 주소 전달
                    putExtra("placeLat", place.latitude)
                    putExtra("placeLng", place.longitude)
                    putExtra("placeId", place.placeId)
                }
                
                Log.d(TAG, "Sending data back: placeName=${place.name}, placeAddress=${place.address}")
                
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        adapter = PlaceSearchAdapter(
            onPlaceClick = { place ->
                fetchPlaceDetails(place.placeId)
            },
            onRouteClick = { place ->
                // 경로 안내 전에 장소 상세 정보를 먼저 가져옵니다
                val placeFields = listOf(Place.Field.LAT_LNG)
                val request = FetchPlaceRequest.builder(place.placeId, placeFields).build()
                
                placesClient.fetchPlace(request)
                    .addOnSuccessListener { response ->
                        val latLng = response.place.latLng
                        if (latLng != null) {
                            // 구글 맵으로 바로 경로 안내
                            val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${latLng.latitude},${latLng.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            startActivity(mapIntent)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "경로 안내를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Failed to fetch place details for navigation", exception)
                    }
            },
            onWebsiteClick = null
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun performSearch(query: String) {
        Log.d(TAG, "Performing search for query: $query")
        
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("KR")  // 한국 내 장소로 제한
            .build()
        
        Log.d(TAG, "Search request built, calling Places API")

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                Log.d(TAG, "Search successful, predictions count: ${response.autocompletePredictions.size}")
                
                val places = response.autocompletePredictions.map { prediction ->
                    Log.d(TAG, "Processing prediction: primary=${prediction.getPrimaryText(null)}, secondary=${prediction.getSecondaryText(null)}")
                    
                    PlaceInfo(
                        placeId = prediction.placeId,
                        name = prediction.getPrimaryText(null).toString(),
                        address = prediction.getSecondaryText(null).toString(),
                        rating = null,
                        latitude = 0.0,
                        longitude = 0.0,
                        websiteUri = null,
                        phoneNumber = null
                    )
                }
                Log.d(TAG, "Mapped ${places.size} places, updating adapter")
                adapter.updatePlaces(places)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Search failed", exception)
                Toast.makeText(this, "검색에 실패했습니다: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // 맵 언어를 한글로 설정
        map.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                this,
                R.raw.map_style_korean
            )
        )

        // 초기 위치를 대한민국으로 설정
        val korea = LatLng(36.0, 128.0)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(korea, 7f))

        map.setOnMapClickListener { latLng ->
            getAddressFromLocation(latLng)
        }
    }

    private fun searchPlace(query: String) {
        val geocoder = Geocoder(this, Locale.KOREA)
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            addresses?.firstOrNull()?.let { address ->
                val latLng = LatLng(address.latitude, address.longitude)
                val placeName = address.featureName ?: query
                val placeAddress = address.getAddressLine(0) ?: ""

                updateSelectedPlace(placeName, placeAddress, latLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "장소 검색에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAddressFromLocation(latLng: LatLng) {
        val geocoder = Geocoder(this, Locale.KOREA)
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                val placeName = address.featureName ?: "선택한 위치"
                val placeAddress = address.getAddressLine(0) ?: ""
                
                updateSelectedPlace(placeName, placeAddress, latLng)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "주소를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectedPlace(name: String, address: String, latLng: LatLng) {
        selectedPlace = PlaceInfo(
            placeId = "", // 직접 선택한 장소는 placeId가 없음
            name = name,
            address = address,
            rating = null,  // 지도에서 직접 선택한 장소는 rating 정보가 없음
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            websiteUri = null,
            phoneNumber = null
        )
        
        // 마커 업데이트
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title(name))

        // UI 업데이트
        binding.placeInfoCard.visibility = View.VISIBLE
        binding.placeNameText.text = name
        binding.placeAddressText.text = address
    }

    private fun fetchPlaceDetails(placeId: String) {
        Log.d(TAG, "Fetching details for place ID: $placeId")
        
        // Place API 필드 지정
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.RATING,
            Place.Field.WEBSITE_URI,
            Place.Field.PHONE_NUMBER
        )
        
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                Log.d(TAG, "Place details fetched successfully: ${place.name}")
                Log.d(TAG, "Place details - Name: ${place.name}, Address: ${place.address}")
                
                // 지도에 마커 추가 및 카메라 이동
                place.latLng?.let { latLng ->
                    // 지도 컨테이너를 보이게 설정
                    binding.searchContainer.visibility = View.GONE
                    binding.mapContainer.visibility = View.VISIBLE
                    
                    map.clear()
                    map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(place.name)
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    
                    // 선택된 장소 정보 저장
                    val placeName = place.name ?: "" // 정확한 장소명 저장
                    val placeAddress = place.address ?: "" // 주소 저장
                    
                    Log.d(TAG, "Storing selected place with Name: $placeName, Address: $placeAddress")
                    
                    selectedPlace = PlaceInfo(
                        placeId = place.id ?: "",
                        name = placeName,
                        address = placeAddress,
                        rating = place.rating,
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        websiteUri = place.websiteUri,
                        phoneNumber = place.phoneNumber
                    )
                    
                    // 장소 정보 카드에 정보 채우기
                    binding.placeInfoCard.visibility = View.VISIBLE
                    binding.placeNameText.text = placeName
                    binding.placeAddressText.text = placeAddress
                    
                    // 장소 선택 후 UI 업데이트
                    updateNoticeVisibility(false)
                    binding.addButton.isEnabled = true
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching place details", exception)
                Toast.makeText(this, "장소 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateNoticeVisibility(showNotice: Boolean) {
        if (showNotice) {
            binding.placeInfoCard.visibility = View.GONE
            binding.addButton.isEnabled = false
        } else {
            binding.placeInfoCard.visibility = View.VISIBLE
            binding.addButton.isEnabled = true
        }
    }
} 