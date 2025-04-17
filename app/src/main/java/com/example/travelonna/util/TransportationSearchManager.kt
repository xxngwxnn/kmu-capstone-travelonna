package com.example.travelonna.util

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.adapter.TransportOptionAdapter
import com.example.travelonna.adapter.StationAdapter
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.TransportationData
import com.example.travelonna.api.TransportationRequest
import com.example.travelonna.api.TransportationResponse
import com.example.travelonna.api.StationSearchResponse
import com.example.travelonna.api.Station
import com.example.travelonna.data.LocationData
import com.example.travelonna.data.StationData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class TransportationSearchManager(private val context: Context) {
    
    private var sourceLocation: String = ""
    private var destinationLocation: String = ""
    private var departureDate: String = ""
    private var selectedTransportType: String = "car"
    
    // 출발지가 역(station)인지 여부를 나타내는 플래그
    private var isSourceStation: Boolean = false
    private var selectedSourceStation: Station? = null
    
    // 지역 선택 대화상자를 표시하는 메서드
    private fun showRegionSelectionDialog(isMainRegion: Boolean, mainRegion: String = "", callback: (String) -> Unit) {
        val items = if (isMainRegion) {
            StationData.getAllRegions().toTypedArray()  // StationData에서 모든 지역 목록 가져오기
        } else {
            (LocationData.subRegions[mainRegion] ?: listOf()).toTypedArray()
        }
        
        if (items.isEmpty()) {
            callback("")
            return
        }
        
        val title = if (isMainRegion) "지역 선택" else "시/군/구 선택"
        
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items) { dialog, which ->
                val selectedItem = items[which]
                callback(selectedItem)
                dialog.dismiss()
                
                // 지역을 선택한 경우 자동으로 역 목록 표시 기능은 제거
                // 이제 callback 내부에서 필요한 경우에만 호출하도록 변경
            }
            .show()
    }
    
    // 지역 내 역 목록을 보여줄지 확인하는 다이얼로그
    private fun showRegionStationsConfirmDialog(region: String) {
        AlertDialog.Builder(context)
            .setTitle("역 목록")
            .setMessage("${region}에 있는 역 목록을 확인하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                fetchStationsByRegion(region)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    // 지역 내 역 목록 가져오기
    private fun fetchStationsByRegion(region: String) {
        val loadingDialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
            
        loadingDialog.show()
        
        // StationData에서 지역별 역 정보 가져오기
        val stations = StationData.getStationsByRegion(region)
        
        loadingDialog.dismiss()
        
        if (stations.isNotEmpty()) {
            showRegionStationsDialog(region, stations)
        } else {
            Toast.makeText(
                context,
                "${region}에 등록된 역이 없습니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // 지역 내 역 목록 다이얼로그 표시
    private fun showRegionStationsDialog(region: String, stations: List<Station>) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("")  // 타이틀을 비워둠
            .setCancelable(true)
            .create()
            
        // 커스텀 뷰 생성 및 설정
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_region_stations, null)
        dialog.setView(dialogView)
        
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.stationsRecyclerView)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)
        
        // 다이얼로그 제목 설정 (한 번만 표시)
        dialogTitle?.text = "${region} 지역 역 목록"
        
        // RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // 커스텀 어댑터 생성 (역 이름 형식을 변경)
        val adapter = object : StationAdapter(stations, { station ->
            // 역 선택 시 처리
            sourceLocation = station.name
            isSourceStation = true
            selectedSourceStation = station
            
            // 다이얼로그 닫기
            dialog.dismiss()
            
            // 선택한 역 정보 업데이트
            updateSelectedSourceStation(station)
            
            // 사용자에게 출발지 설정 알림
            Toast.makeText(context, "${station.name}이(가) 출발지로 설정되었습니다.", Toast.LENGTH_SHORT).show()
        }) {
            // StationAdapter 클래스의 bindStationName 메소드 오버라이드
            override fun bindStationName(stationView: TextView, station: Station) {
                stationView.text = station.name  // 역 이름만 표시
            }
        }
        
        recyclerView.adapter = adapter
        
        // 닫기 버튼 설정
        closeButton.setOnClickListener { 
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    // 선택한 출발지 역 정보 업데이트
    private fun updateSelectedSourceStation(station: Station) {
        // 교통편 검색 다이얼로그의 뷰 참조
        val currentDialog = currentSearchDialog ?: return
        
        val sourceRegionText = currentDialog.findViewById<TextView>(R.id.sourceRegionText)
        val sourceSubRegionLayout = currentDialog.findViewById<LinearLayout>(R.id.sourceSubRegionLayout)
        val selectedSourceInfo = currentDialog.findViewById<TextView>(R.id.selectedSourceInfo)
        
        // 지역 정보 추출 (location에서 첫 번째 단어만 추출)
        val region = station.location.split(" ").firstOrNull() ?: ""
        
        // 하위 지역 선택 UI는 숨김
        sourceSubRegionLayout?.visibility = View.GONE
        
        // 상단 박스에는 지역명 표시
        sourceRegionText?.text = region
        
        // 하단 박스에는 역 이름 표시
        selectedSourceInfo?.visibility = View.VISIBLE
        selectedSourceInfo?.text = station.name
    }
    
    // 현재 열려있는 검색 다이얼로그 참조
    private var currentSearchDialog: AlertDialog? = null
    
    // 역 검색 다이얼로그를 표시하는 메서드
    private fun showStationSearchDialog(callback: (Station) -> Unit) {
        val dialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_station_search)
            .setCancelable(true)
            .create()
        
        dialog.show()
        
        // 뷰 참조
        val searchKeywordInput = dialog.findViewById<EditText>(R.id.searchKeywordInput)
        val searchButton = dialog.findViewById<ImageButton>(R.id.searchButton)
        val searchResultsTitle = dialog.findViewById<TextView>(R.id.searchResultsTitle)
        val stationResultsRecyclerView = dialog.findViewById<RecyclerView>(R.id.stationResultsRecyclerView)
        val searchProgressBar = dialog.findViewById<ProgressBar>(R.id.searchProgressBar)
        val noResultsText = dialog.findViewById<TextView>(R.id.noResultsText)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        
        // RecyclerView 설정
        stationResultsRecyclerView?.layoutManager = LinearLayoutManager(context)
        val stationAdapter = StationAdapter(emptyList()) { station ->
            callback(station)
            dialog.dismiss()
        }
        stationResultsRecyclerView?.adapter = stationAdapter
        
        // 검색 버튼 클릭 리스너
        searchButton?.setOnClickListener {
            val keyword = searchKeywordInput?.text?.toString()?.trim() ?: ""
            if (keyword.isNotEmpty()) {
                searchStations(keyword, stationAdapter, searchResultsTitle, searchProgressBar, noResultsText)
            } else {
                Toast.makeText(context, "검색어를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 취소 버튼 클릭 리스너
        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }
    }
    
    // 역 검색 API 호출
    private fun searchStations(
        keyword: String,
        adapter: StationAdapter,
        resultsTitle: TextView?,
        progressBar: ProgressBar?,
        noResultsText: TextView?
    ) {
        progressBar?.visibility = View.VISIBLE
        noResultsText?.visibility = View.GONE
        
        // API 호출 대신 로컬 데이터 사용
        val stations = StationData.searchStations(keyword)
        
        progressBar?.visibility = View.GONE
        
        if (stations.isNotEmpty()) {
            resultsTitle?.visibility = View.VISIBLE
            noResultsText?.visibility = View.GONE
            adapter.updateData(stations)
        } else {
            resultsTitle?.visibility = View.GONE
            noResultsText?.visibility = View.VISIBLE
        }
    }
    
    // 검색 다이얼로그
    fun showTransportationSearchDialog(
        destinationName: String, 
        startDate: Long,
        transportType: String = "bus" // 기본값은 버스
    ) {
        val dialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_transportation_search)
            .setCancelable(true)
            .create()
        
        // 현재 다이얼로그 참조 저장
        currentSearchDialog = dialog
        
        dialog.show()
        
        // 초기 데이터 설정
        this.destinationLocation = destinationName
        this.selectedTransportType = transportType
        
        // 출발 날짜 설정 (여행 시작일)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startDate
        }
        this.departureDate = dateFormat.format(calendar.time)
        
        // 뷰 참조
        val sourceRegionText = dialog.findViewById<TextView>(R.id.sourceRegionText)
        val sourceSearchButton = dialog.findViewById<ImageButton>(R.id.sourceSearchButton)
        val sourceSubRegionLayout = dialog.findViewById<LinearLayout>(R.id.sourceSubRegionLayout)
        val sourceSubRegionText = dialog.findViewById<TextView>(R.id.sourceSubRegionText)
        val selectedSourceInfo = dialog.findViewById<TextView>(R.id.selectedSourceInfo)
        val destinationText = dialog.findViewById<TextView>(R.id.destinationText)
        val destinationSearchButton = dialog.findViewById<ImageButton>(R.id.destinationSearchButton)
        val transportTypeText = dialog.findViewById<TextView>(R.id.transportTypeText)
        val departureDateText = dialog.findViewById<TextView>(R.id.departureDateText)
        val searchButton = dialog.findViewById<Button>(R.id.searchButton)
        
        // 초기값 설정
        sourceRegionText?.text = "지역 선택"
        destinationText?.text = destinationLocation
        departureDateText?.text = departureDate
        
        // 교통수단 유형 설정
        val transportTypeKorean = when (transportType) {
            "car" -> "자가용"
            "bus" -> "버스"
            "train" -> "기차"
            "airplane" -> "비행기"
            else -> "버스"
        }
        transportTypeText?.text = transportTypeKorean
        
        // 초기에 도착지 지역에 해당하는 역 목록 표시 준비
        // StationData에서 지역별 역 정보 가져오기 시도
        val destinationRegions = StationData.getAllRegions()
        
        // 도착지와 정확히 일치하거나 포함하는 지역 찾기
        val matchingRegion = destinationRegions.find { region ->
            region == destinationLocation || destinationLocation.contains(region)
        }
        
        // 일치하는 지역이 있으면 자동으로 역 목록 다이얼로그 표시
        if (matchingRegion != null) {
            // 메인 스레드에서 약간의 지연 후 실행 (UI가 완전히 렌더링 된 후)
            dialog.findViewById<View>(R.id.dialogTitle)?.post {
                // 도착지 텍스트에 매칭된 지역 표시
                destinationText?.text = matchingRegion
                this.destinationLocation = matchingRegion
                
                // 역 목록 다이얼로그 표시
                fetchDestinationStationsByRegion(matchingRegion)
            }
        }
        
        // 출발지 검색 버튼 클릭 리스너
        sourceSearchButton?.setOnClickListener {
            showRegionSelectionDialog(true) { selectedRegion ->
                sourceRegionText.text = selectedRegion
                sourceLocation = selectedRegion
                
                // 지역 선택시 역 목록 표시
                if (selectedRegion.isNotEmpty()) {
                    fetchStationsByRegion(selectedRegion)
                }
            }
        }
        
        // 시/도 선택 클릭 리스너
        sourceRegionText?.setOnClickListener {
            // 역 선택을 해제
            isSourceStation = false
            selectedSourceStation = null
            selectedSourceInfo?.visibility = View.GONE
            sourceRegionText.visibility = View.VISIBLE
            
            showRegionSelectionDialog(true) { selectedRegion ->
                sourceRegionText.text = selectedRegion
                sourceLocation = selectedRegion
                
                // 하위 지역 여부에 따라 UI 설정
                if (LocationData.provincesWithSubregions.contains(selectedRegion)) {
                    sourceSubRegionLayout?.visibility = View.VISIBLE
                    sourceSubRegionText?.text = "시/군/구 선택"
                } else {
                    sourceSubRegionLayout?.visibility = View.GONE
                    
                    // 하위 지역이 없는 경우에만 역 목록 표시
                    fetchStationsByRegion(selectedRegion)
                }
            }
        }
        
        // 도착지 검색 버튼 클릭 리스너
        destinationSearchButton?.setOnClickListener {
            // 도착지 지역 선택 다이얼로그 표시
            showRegionSelectionDialog(true) { selectedRegion ->
                destinationText?.text = selectedRegion
                destinationLocation = selectedRegion
                
                // 도착지 정보 초기화
                dialog.findViewById<TextView>(R.id.selectedDestinationInfo)?.also { infoView ->
                    infoView.visibility = View.GONE
                }
                
                // 지역 선택시 역 목록 표시
                if (selectedRegion.isNotEmpty()) {
                    fetchDestinationStationsByRegion(selectedRegion)
                }
            }
        }
        
        // 도착지 텍스트 클릭 리스너 (출발지와 동일하게 작동)
        destinationText?.setOnClickListener {
            // 도착지 정보 초기화
            dialog.findViewById<TextView>(R.id.selectedDestinationInfo)?.also { infoView ->
                infoView.visibility = View.GONE
            }
            
            // 도착지 지역 선택 다이얼로그 표시
            showRegionSelectionDialog(true) { selectedRegion ->
                destinationText?.text = selectedRegion
                destinationLocation = selectedRegion
                
                // 지역 선택시 역 목록 표시
                if (selectedRegion.isNotEmpty()) {
                    fetchDestinationStationsByRegion(selectedRegion)
                }
            }
        }
        
        // 시/군/구 선택 클릭 리스너
        sourceSubRegionText?.setOnClickListener {
            val mainRegion = sourceRegionText?.text?.toString() ?: ""
            if (mainRegion.isNotEmpty() && mainRegion != "지역 선택") {
                showRegionSelectionDialog(false, mainRegion) { selectedSubRegion ->
                    if (selectedSubRegion.isNotEmpty()) {
                        sourceSubRegionText.text = selectedSubRegion
                        sourceLocation = "$mainRegion $selectedSubRegion"
                    }
                }
            } else {
                Toast.makeText(context, "먼저 지역을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 검색 버튼 클릭 리스너
        searchButton?.setOnClickListener {
            if (validateInput()) {
                dialog.dismiss()
                searchTransportation()
            } else {
                Toast.makeText(context, "출발지를 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 입력값 유효성 검사
    private fun validateInput(): Boolean {
        return sourceLocation.isNotEmpty() && 
               sourceLocation != "시/도 선택" &&
               destinationLocation.isNotEmpty() && 
               departureDate.isNotEmpty()
    }
    
    // 역 이름에서 '역' 단어를 제거하는 함수
    private fun removeStationSuffix(stationName: String): String {
        // "역"으로 끝나는 경우 '역'을 제거
        if (stationName.endsWith("역")) {
            return stationName.substring(0, stationName.length - 1)
        }
        // "터미널"로 끝나는 경우 그대로 유지 (이미 지역명이 포함됨)
        return stationName
    }
    
    // 교통수단 검색 API 호출
    private fun searchTransportation() {
        val loadingDialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        
        loadingDialog.show()
        
        // 출발지와 도착지에서 '역' 단어 제거
        val processedSource = removeStationSuffix(sourceLocation)
        val processedDestination = removeStationSuffix(destinationLocation)
        
        val request = TransportationRequest(
            source = processedSource,
            destination = processedDestination,
            departureDate = departureDate,
            transportType = selectedTransportType
        )
        
        Log.d("TransportSearch", "Request: $request")
        
        RetrofitClient.apiService.searchTransportation(request)
            .enqueue(object : Callback<TransportationResponse> {
                override fun onResponse(
                    call: Call<TransportationResponse>,
                    response: Response<TransportationResponse>
                ) {
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data
                        Log.d("TransportSearch", "Response: ${response.body()}")
                        
                        if (data != null) {
                            showTransportationResults(data)
                        } else {
                            Toast.makeText(context, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            context, 
                            "교통수단 검색에 실패했습니다: ${response.errorBody()?.string()}", 
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("TransportSearch", "Error: ${response.errorBody()?.string()}")
                    }
                }
                
                override fun onFailure(call: Call<TransportationResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        context, 
                        "네트워크 오류: ${t.message}", 
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("TransportSearch", "Network error", t)
                }
            })
    }
    
    // 검색 결과 표시
    private fun showTransportationResults(data: TransportationData) {
        val dialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_transportation_results)
            .setCancelable(true)
            .create()
        
        dialog.show()
        
        // 결과 화면 뷰 참조
        val sourceDestinationText = dialog.findViewById<TextView>(R.id.sourceDestinationText)
        val transportTypeText = dialog.findViewById<TextView>(R.id.transportTypeText)
        val departureDateResultText = dialog.findViewById<TextView>(R.id.departureDateResultText)
        val transportOptionsRecyclerView = dialog.findViewById<RecyclerView>(R.id.transportOptionsRecyclerView)
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)
        val closeDialogButton = dialog.findViewById<ImageButton>(R.id.closeDialogButton)
        
        // 데이터 설정
        sourceDestinationText?.text = "${data.source} → ${data.destination}"
        
        // 교통수단 표시 (한글로 변환)
        val transportTypeKorean = when (data.transportType) {
            "car" -> "자가용"
            "train" -> "기차"
            "bus" -> "버스"
            "airplane" -> "비행기"
            else -> data.transportType
        }
        transportTypeText?.text = transportTypeKorean
        
        departureDateResultText?.text = "출발일: ${data.departureDate}"
        
        // RecyclerView 설정
        transportOptionsRecyclerView?.layoutManager = LinearLayoutManager(context)
        transportOptionsRecyclerView?.adapter = TransportOptionAdapter(data.options)
        
        // 하단 닫기 버튼 설정
        closeButton?.setOnClickListener {
            dialog.dismiss()
        }
        
        // X 아이콘 닫기 버튼 설정
        closeDialogButton?.setOnClickListener {
            dialog.dismiss()
        }
    }
    
    // 도착지 지역 내 역 목록 가져오기
    private fun fetchDestinationStationsByRegion(region: String) {
        val loadingDialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
            
        loadingDialog.show()
        
        // StationData에서 지역별 역 정보 가져오기
        val stations = StationData.getStationsByRegion(region)
        
        loadingDialog.dismiss()
        
        if (stations.isNotEmpty()) {
            showDestinationStationsList(region, stations)
        } else {
            Toast.makeText(
                context,
                "${region}에 등록된 역이 없습니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // 도착지 역 목록 다이얼로그 표시
    private fun showDestinationStationsList(destination: String, stations: List<Station>) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("")  // 타이틀을 비워둠
            .setCancelable(true)
            .create()
            
        // 커스텀 뷰 생성 및 설정
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_region_stations, null)
        dialog.setView(dialogView)
        
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.stationsRecyclerView)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)
        
        // 다이얼로그 제목 설정 (한 번만 표시)
        dialogTitle?.text = "${destination} 지역 역 목록"
        
        // RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // 커스텀 어댑터 생성
        val adapter = object : StationAdapter(stations, { station ->
            // 역 선택 시 처리 (도착지로 설정)
            destinationLocation = station.name
            
            // 현재 다이얼로그의 뷰 참조
            val dialogRef = currentSearchDialog
            if (dialogRef != null) {
                // 도착지 영역의 뷰 참조
                val destinationRegionText = dialogRef.findViewById<TextView>(R.id.destinationText)
                val selectedDestinationInfo = dialogRef.findViewById<TextView>(R.id.selectedDestinationInfo)
                
                // 지역 정보 추출
                val region = station.location.split(" ").firstOrNull() ?: ""
                
                // 상단 박스에는 지역명 표시
                destinationRegionText?.text = region
                
                // 하단 박스에는 역 이름 표시
                if (selectedDestinationInfo != null) {
                    selectedDestinationInfo.visibility = View.VISIBLE
                    selectedDestinationInfo.text = station.name
                }
            }
            
            // 다이얼로그 닫기
            dialog.dismiss()
            
            // 사용자에게 도착지 설정 알림
            Toast.makeText(context, "${station.name}이(가) 도착지로 설정되었습니다.", Toast.LENGTH_SHORT).show()
        }) {
            // StationAdapter 클래스의 bindStationName 메소드 오버라이드
            override fun bindStationName(stationView: TextView, station: Station) {
                stationView.text = station.name  // 역 이름만 표시
            }
        }
        
        recyclerView.adapter = adapter
        
        // 닫기 버튼 설정
        closeButton.setOnClickListener { 
            dialog.dismiss()
        }
        
        dialog.show()
    }
} 