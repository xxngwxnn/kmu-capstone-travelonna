package com.example.travelonna

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.CameraUpdateFactory
import android.widget.ImageView
import android.widget.Toast
import com.example.travelonna.model.RegionPolygon
import com.example.travelonna.util.CoordinateConverter
import com.google.gson.JsonParser
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

class MyMapActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var map: GoogleMap
    private val regions = mutableListOf<RegionPolygon>()
    private val polygons = mutableMapOf<String, Polygon>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_map)
        
        // 뒤로가기 버튼 설정
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // 지도 프래그먼트 초기화
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // 행정구역 데이터 로드
        loadRegionData()
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // 커스텀 스타일 적용
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
            if (!success) {
                Log.e("MyMapActivity", "스타일 파싱 실패")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MyMapActivity", "스타일 파일을 찾을 수 없습니다.", e)
        }
        
        // 초기 위치를 대한민국 중심으로 설정
        val koreaCenter = LatLng(36.0, 128.0)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(koreaCenter, 7f))
        
        // 지도 UI 설정
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
        }
        
        // 폴리곤 표시
        displayRegions()
        
        // 폴리곤 클릭 리스너
        map.setOnPolygonClickListener { polygon ->
            val regionName = polygon.tag as String
            val region = regions.find { it.name == regionName }
            region?.let {
                // 방문 여부 토글
                it.isVisited = !it.isVisited
                if (it.isVisited) it.visitCount++
                
                // 폴리곤 색상 업데이트
                updatePolygonColor(polygon, it.isVisited)
                
                Toast.makeText(this, 
                    "${it.name} - 방문: ${if (it.isVisited) "O" else "X"} (${it.visitCount}회)", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadRegionData() {
        try {
            // sig.json 파일 읽기
            val jsonString = assets.open("sig.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val features = jsonObject.getJSONArray("features")
            
            Log.d("MyMapActivity", "Total features found: ${features.length()}")
            
            // 각 행정구역 데이터 처리
            for (i in 0 until features.length()) {
                try {
                    val feature = features.getJSONObject(i)
                    val properties = feature.getJSONObject("properties")
                    val geometry = feature.getJSONObject("geometry")
                    
                    // 행정구역 정보 로깅
                    val sigCd = properties.optString("SIG_CD", "")
                    val name = properties.optString("SIG_KOR_NM", "")
                    Log.d("MyMapActivity", "Processing region[$i]: code=$sigCd, name=$name")
                    
                    // geometry 타입 확인
                    val geoType = geometry.optString("type", "")
                    Log.d("MyMapActivity", "Geometry type for $name: $geoType")
                    
                    // 좌표 배열 가져오기
                    val coordinates = geometry.getJSONArray("coordinates")
                    Log.d("MyMapActivity", "$name coordinates array length: ${coordinates.length()}")
                    
                    when (geoType) {
                        "Polygon" -> {
                            // 단일 폴리곤 처리
                            val latLngList = processPolygonCoordinates(coordinates.getJSONArray(0), name)
                            if (latLngList.isNotEmpty()) {
                                regions.add(RegionPolygon(name, latLngList))
                            }
                        }
                        "MultiPolygon" -> {
                            // 멀티 폴리곤의 각 부분을 개별 폴리곤으로 처리
                            for (j in 0 until coordinates.length()) {
                                val partCoords = coordinates.getJSONArray(j).getJSONArray(0)
                                val latLngList = processPolygonCoordinates(partCoords, "$name-part$j")
                                if (latLngList.isNotEmpty()) {
                                    regions.add(RegionPolygon(name, latLngList))
                                }
                            }
                        }
                        else -> {
                            Log.e("MyMapActivity", "Unsupported geometry type: $geoType for region $name")
                            continue
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e("MyMapActivity", "Error processing feature $i", e)
                }
            }
            
            Log.d("MyMapActivity", "Successfully loaded ${regions.size} regions")
            
        } catch (e: Exception) {
            Log.e("MyMapActivity", "행정구역 데이터 로드 실패", e)
            e.printStackTrace()
            Toast.makeText(this, "행정구역 데이터를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun processPolygonCoordinates(coordinates: JSONArray, regionName: String): List<LatLng> {
        val latLngList = mutableListOf<LatLng>()
        try {
            for (i in 0 until coordinates.length()) {
                val coordArray = coordinates.getJSONArray(i)
                val x = coordArray.getDouble(0)
                val y = coordArray.getDouble(1)
                
                try {
                    val latLng = CoordinateConverter.convertToLatLng(x, y)
                    latLngList.add(latLng)
                    
                    // 첫 번째와 마지막 좌표만 로깅
                    if (i == 0 || i == coordinates.length() - 1) {
                        Log.d("MyMapActivity", "$regionName coordinate $i: $latLng")
                    }
                } catch (e: Exception) {
                    Log.e("MyMapActivity", "Error converting coordinates ($x, $y) for $regionName", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MyMapActivity", "Error processing coordinates for $regionName", e)
        }
        return latLngList
    }
    
    private fun displayRegions() {
        regions.forEach { region ->
            val polygon = map.addPolygon(PolygonOptions()
                .clickable(true)
                .addAll(region.coordinates)
                .strokeColor(Color.BLACK)
                .strokeWidth(1f)  // 선 두께를 줄임
                .fillColor(Color.argb(50, 200, 200, 200)))
            
            polygon.tag = region.name
            polygons[region.name] = polygon
        }
    }
    
    private fun updatePolygonColor(polygon: Polygon, isVisited: Boolean) {
        if (isVisited) {
            polygon.fillColor = Color.argb(80, 76, 175, 80) // 초록색
        } else {
            polygon.fillColor = Color.argb(50, 200, 200, 200) // 회색
        }
    }
} 