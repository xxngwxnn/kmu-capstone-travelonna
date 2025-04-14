package com.example.travelonna.util

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.adapter.TransportOptionAdapter
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.TransportationData
import com.example.travelonna.api.TransportationRequest
import com.example.travelonna.api.TransportationResponse
import com.example.travelonna.data.LocationData
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
    
    // 지역 선택 대화상자를 표시하는 메서드
    private fun showRegionSelectionDialog(isMainRegion: Boolean, mainRegion: String = "", callback: (String) -> Unit) {
        val items = if (isMainRegion) {
            LocationData.regions.toTypedArray()
        } else {
            (LocationData.subRegions[mainRegion] ?: listOf()).toTypedArray()
        }
        
        if (items.isEmpty()) {
            callback("")
            return
        }
        
        val title = if (isMainRegion) "시/도 선택" else "시/군/구 선택"
        
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items) { dialog, which ->
                val selectedItem = items[which]
                callback(selectedItem)
                dialog.dismiss()
            }
            .show()
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
        val sourceSubRegionLayout = dialog.findViewById<LinearLayout>(R.id.sourceSubRegionLayout)
        val sourceSubRegionText = dialog.findViewById<TextView>(R.id.sourceSubRegionText)
        val destinationText = dialog.findViewById<TextView>(R.id.destinationText)
        val transportTypeText = dialog.findViewById<TextView>(R.id.transportTypeText)
        val departureDateText = dialog.findViewById<TextView>(R.id.departureDateText)
        val searchButton = dialog.findViewById<Button>(R.id.searchButton)
        
        // 초기값 설정
        sourceRegionText?.text = "시/도 선택"
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
        
        // 시/도 선택 클릭 리스너
        sourceRegionText?.setOnClickListener {
            showRegionSelectionDialog(true) { selectedRegion ->
                sourceRegionText.text = selectedRegion
                sourceLocation = selectedRegion
                
                // 하위 지역 여부에 따라 UI 설정
                if (LocationData.provincesWithSubregions.contains(selectedRegion)) {
                    sourceSubRegionLayout?.visibility = View.VISIBLE
                    sourceSubRegionText?.text = "시/군/구 선택"
                } else {
                    sourceSubRegionLayout?.visibility = View.GONE
                }
            }
        }
        
        // 시/군/구 선택 클릭 리스너
        sourceSubRegionText?.setOnClickListener {
            val mainRegion = sourceRegionText?.text?.toString() ?: ""
            if (mainRegion.isNotEmpty() && mainRegion != "시/도 선택") {
                showRegionSelectionDialog(false, mainRegion) { selectedSubRegion ->
                    if (selectedSubRegion.isNotEmpty()) {
                        sourceSubRegionText.text = selectedSubRegion
                        sourceLocation = "$mainRegion $selectedSubRegion"
                    }
                }
            } else {
                Toast.makeText(context, "먼저 시/도를 선택해주세요", Toast.LENGTH_SHORT).show()
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
    
    // 교통수단 검색 API 호출
    private fun searchTransportation() {
        val loadingDialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        
        loadingDialog.show()
        
        val request = TransportationRequest(
            source = sourceLocation,
            destination = destinationLocation,
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
        
        // 닫기 버튼 설정
        closeButton?.setOnClickListener {
            dialog.dismiss()
        }
    }
} 