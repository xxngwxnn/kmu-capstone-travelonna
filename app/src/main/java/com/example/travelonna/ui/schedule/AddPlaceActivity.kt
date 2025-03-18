package com.example.travelonna.ui.schedule

import android.content.Intent
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

class AddPlaceActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityAddPlaceBinding
    private lateinit var map: GoogleMap
    private var selectedPlace: PlaceInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMap()
        setupSearchView()
        setupAddButton()
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
        binding.searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchPlace(v.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun setupAddButton() {
        binding.addButton.setOnClickListener {
            selectedPlace?.let { place ->
                val intent = Intent().apply {
                    putExtra("placeName", place.name)
                    putExtra("placeAddress", place.address)
                    putExtra("placeLat", place.latLng.latitude)
                    putExtra("placeLng", place.latLng.longitude)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // 초기 위치를 대한민국으로 설정
        val korea = LatLng(36.0, 128.0)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(korea, 7f))

        map.setOnMapClickListener { latLng ->
            getAddressFromLocation(latLng)
        }
    }

    private fun searchPlace(query: String) {
        val geocoder = Geocoder(this)
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
        val geocoder = Geocoder(this)
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
        selectedPlace = PlaceInfo(name, address, latLng)
        
        // 마커 업데이트
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title(name))

        // UI 업데이트
        binding.placeInfoCard.visibility = View.VISIBLE
        binding.placeNameText.text = name
        binding.placeAddressText.text = address
    }

    data class PlaceInfo(
        val name: String,
        val address: String,
        val latLng: LatLng
    )
} 