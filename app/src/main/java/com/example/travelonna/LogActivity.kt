package com.example.travelonna

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.example.travelonna.api.PlanData
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.model.TravelLog
import com.example.travelonna.model.TravelPlace
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LogActivity : AppCompatActivity() {

    private lateinit var dropdownSpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var groupCheckbox: ImageView
    private lateinit var personalCheckbox: ImageView
    private lateinit var groupFilterLayout: LinearLayout
    private lateinit var personalFilterLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var logAdapter: LogAdapter
    private lateinit var backButton: ImageView
    
    private var isGroupSelected = true
    private var isPersonalSelected = true
    private var allTravelLogs = listOf<TravelLog>()
    
    // 로그 태그 상수 추가
    private val TAG = "LogActivity"
    
    // 날짜 포맷
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        // 초기화
        dropdownSpinner = findViewById(R.id.dropdownSpinner)
        yearSpinner = findViewById(R.id.yearSpinner)
        groupCheckbox = findViewById(R.id.groupCheckbox)
        personalCheckbox = findViewById(R.id.personalCheckbox)
        groupFilterLayout = findViewById(R.id.groupFilterLayout)
        personalFilterLayout = findViewById(R.id.personalFilterLayout)
        recyclerView = findViewById(R.id.recyclerView)
        backButton = findViewById(R.id.backButton)

        // 뒤로가기 버튼 클릭 이벤트
        backButton.setOnClickListener {
            finish()
        }

        // 드롭다운에 표시할 데이터 설정
        val options = arrayOf("최근 순", "오래된 순", "연도별")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dropdownSpinner.adapter = adapter

        // 연도별 스피너 설정
        // 현재 연도부터 몇 년 전까지 표시
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear downTo currentYear - 5).map { it.toString() }.toTypedArray()
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter

        // 드롭다운 선택 이벤트 처리
        dropdownSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // "연도별" 옵션이 선택되었을 때(position이 2일 때) 연도 스피너 표시
                yearSpinner.visibility = if (position == 2) View.VISIBLE else View.GONE
                
                // 정렬 방식에 따라 데이터 필터링
                when (position) {
                    0 -> sortByRecent() // 최근 순
                    1 -> sortByOldest() // 오래된 순
                    2 -> filterByYear(yearSpinner.selectedItem.toString()) // 연도별
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택되지 않았을 때
            }
        }

        // 연도 스피너 선택 이벤트 처리
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedYear = years[position]
                filterByYear(selectedYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택되지 않았을 때
            }
        }

        // 그룹 필터 클릭 리스너 설정
        groupFilterLayout.setOnClickListener {
            // 현재 그룹만 선택된 상태에서 그룹을 해제하려 할 때
            if (isGroupSelected && !isPersonalSelected) {
                // 최소 하나는 선택되어 있어야 함
                Toast.makeText(this, "최소 하나의 필터는 선택되어야 합니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            isGroupSelected = !isGroupSelected
            updateGroupCheckboxState()
            applyFilters()
        }
        
        // 개인 필터 클릭 리스너 설정
        personalFilterLayout.setOnClickListener {
            // 현재 개인만 선택된 상태에서 개인을 해제하려 할 때
            if (!isGroupSelected && isPersonalSelected) {
                // 최소 하나는 선택되어 있어야 함
                Toast.makeText(this, "최소 하나의 필터는 선택되어야 합니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            isPersonalSelected = !isPersonalSelected
            updatePersonalCheckboxState()
            applyFilters()
        }

        // 초기 체크박스 상태 설정 (둘 다 선택)
        updateGroupCheckboxState()
        updatePersonalCheckboxState()
        
        // RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // 초기 빈 어댑터 설정
        logAdapter = LogAdapter(emptyList())
        recyclerView.adapter = logAdapter
        
        // API에서 데이터 가져오기
        fetchTravelPlans()
    }
    
    // API에서 여행 일정 데이터 가져오기
    private fun fetchTravelPlans() {
        Log.d("LogActivity", "Fetching travel plans from API: ${RetrofitClient.BASE_URL}api/v1/plans")
        
        RetrofitClient.apiService.getPlans("Bearer YOUR_TOKEN").enqueue(object : Callback<com.example.travelonna.api.PlanListResponse> {
            override fun onResponse(
                call: Call<com.example.travelonna.api.PlanListResponse>,
                response: Response<com.example.travelonna.api.PlanListResponse>
            ) {
                if (response.isSuccessful) {
                    val planResponse = response.body()
                    Log.d("LogActivity", "API response successful: ${planResponse?.message}")
                    
                    if (planResponse != null && planResponse.success) {
                        // API 응답을 TravelLog 객체로 변환
                        Log.d("LogActivity", "Received ${planResponse.data?.size ?: 0} plans from API")
                        Log.d("LogActivity", "Raw plan data: ${planResponse.data}")
                        
                        allTravelLogs = convertPlansToTravelLogs(planResponse.data)
                        Log.d("LogActivity", "Converted to ${allTravelLogs.size} travel logs")
                        
                        // 리스트 업데이트
                        updateAdapterWithLogs(allTravelLogs)
                    } else {
                        Log.e("LogActivity", "API call not successful: ${planResponse?.message}")
                        Toast.makeText(this@LogActivity, "데이터를 가져오는데 실패했습니다: ${planResponse?.message}", Toast.LENGTH_SHORT).show()
                        // 실패 시 샘플 데이터 사용
                        allTravelLogs = getSampleTravelLogs()
                        updateAdapterWithLogs(allTravelLogs)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("LogActivity", "Server response error: ${response.code()}, Body: $errorBody")
                    Toast.makeText(this@LogActivity, "서버 응답 오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                    // 실패 시 샘플 데이터 사용
                    allTravelLogs = getSampleTravelLogs()
                    updateAdapterWithLogs(allTravelLogs)
                }
            }

            override fun onFailure(call: Call<com.example.travelonna.api.PlanListResponse>, t: Throwable) {
                Log.e("LogActivity", "API call failed", t)
                Toast.makeText(this@LogActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                // 실패 시 샘플 데이터 사용
                allTravelLogs = getSampleTravelLogs()
                updateAdapterWithLogs(allTravelLogs)
            }
        })
    }
    
    // Plan 객체를 TravelLog 객체로 변환
    private fun convertPlansToTravelLogs(plans: List<PlanData>?): List<TravelLog> {
        if (plans.isNullOrEmpty()) {
            Log.d(TAG, "Plans list is null or empty")
            return emptyList()
        }
        
        val currentDate = Calendar.getInstance().time
        
        // Intent로부터 기록작성 모드인지 확인
        val isFromWriteMemory = intent.getBooleanExtra("from_write_memory", false)
        
        Log.d(TAG, "현재 날짜: ${displayDateFormat.format(currentDate)}")
        Log.d(TAG, "기록작성 모드: $isFromWriteMemory")
        Log.d(TAG, "전체 일정 수: ${plans.size}")
        
        return plans
            .filter { plan ->
                try {
                    // 시작일과 종료일 파싱
                    val startDate = apiDateFormat.parse(plan.startDate)
                    val endDate = apiDateFormat.parse(plan.endDate)
                    
                    Log.d(TAG, "일정 확인: ${plan.title} (${plan.startDate} ~ ${plan.endDate})")
                    Log.d(TAG, "  - 시작일 파싱: ${startDate?.let { displayDateFormat.format(it) }}")
                    Log.d(TAG, "  - 종료일 파싱: ${endDate?.let { displayDateFormat.format(it) }}")
                    
                    val result = if (isFromWriteMemory) {
                        // 기록작성 모드: 완료된 여행만 포함 (종료일이 현재 이전인 경우)
                        val isCompleted = startDate != null && endDate != null && endDate.before(currentDate)
                        Log.d(TAG, "  - 완료 여부: $isCompleted")
                        isCompleted
                    } else {
                        // 일반 모드: 진행 중이거나 완료된 여행만 포함
                        val isStartedOrCompleted = startDate != null && endDate != null && 
                            (startDate.before(currentDate) || startDate == currentDate)
                        Log.d(TAG, "  - 시작됨/완료됨 여부: $isStartedOrCompleted")
                        isStartedOrCompleted
                    }
                    
                    Log.d(TAG, "  - 필터 결과: $result")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "날짜 파싱 오류: ${e.message}")
                    false
                }
            }
            .map { plan ->
                // 날짜 형식 변환 (yyyy-MM-dd -> yyyy.MM.dd)
                val startDate = try {
                    val date = apiDateFormat.parse(plan.startDate)
                    displayDateFormat.format(date!!)
                } catch (e: Exception) {
                    plan.startDate.replace("-", ".")
                }
                
                val endDate = try {
                    val date = apiDateFormat.parse(plan.endDate)
                    displayDateFormat.format(date!!)
                } catch (e: Exception) {
                    plan.endDate.replace("-", ".")
                }
                
                // 그룹 여부 결정
                val type = if (plan.groupId != null) "그룹" else "개인"
                
                // 진행 상태 확인
                val status = try {
                    val parsedEndDate = apiDateFormat.parse(plan.endDate)
                    if (parsedEndDate.before(currentDate)) "완료" else "진행중"
                } catch (e: Exception) {
                    "완료"
                }
                
                TravelLog(
                    title = plan.title,
                    date = "$startDate - $endDate",
                    type = type,
                    places = listOf(), // 장소 데이터는 아직 API에 없음
                    planId = plan.planId.toInt(), // planId 추가
                    status = status // 상태 추가
                )
            }
    }
    
    // 어댑터 업데이트
    private fun updateAdapterWithLogs(logs: List<TravelLog>) {
        val isFromWriteMemory = intent.getBooleanExtra("from_write_memory", false)
        
        Log.d(TAG, "어댑터 업데이트: ${logs.size}개 항목")
        
        if (logs.isEmpty()) {
            // 빈 목록에 대한 안내 메시지
            val message = if (isFromWriteMemory) {
                "기록을 작성할 수 있는 완료된 여행이 없습니다"
            } else {
                "진행 중이거나 완료된 여행이 없습니다"
            }
            Log.d(TAG, "빈 목록 - 메시지: $message")
            
            // API 호출이 완료된 후에만 Toast 표시 (샘플 데이터 사용 시에는 Toast 표시 안함)
            // fetchTravelPlans 중에 호출되는 경우는 Toast 표시하지 않음
        } else {
            Log.d(TAG, "여행 목록:")
            logs.forEachIndexed { index, log ->
                Log.d(TAG, "  $index: ${log.title} (${log.date}) - ${log.status}")
            }
        }
        
        // 기존 어댑터가 있으면 데이터만 업데이트, 없으면 새로 생성
        if (::logAdapter.isInitialized) {
            // 어댑터의 데이터 업데이트
            logAdapter.updateLogs(logs)
        } else {
            // 새 어댑터 생성
            logAdapter = LogAdapter(logs)
            recyclerView.adapter = logAdapter
        }
    }

    // 최근 날짜순으로 정렬
    private fun sortByRecent() {
        val sortedLogs = allTravelLogs.sortedByDescending { getStartDateForSorting(it.date) }
        updateAdapterWithLogs(sortedLogs)
    }

    // 오래된 날짜순으로 정렬
    private fun sortByOldest() {
        val sortedLogs = allTravelLogs.sortedBy { getStartDateForSorting(it.date) }
        updateAdapterWithLogs(sortedLogs)
    }

    // 연도별 필터링
    private fun filterByYear(year: String) {
        val filteredLogs = allTravelLogs.filter { 
            it.date.contains(year) 
        }
        updateAdapterWithLogs(filteredLogs)
    }

    // 유형별 필터링 (그룹/개인)
    private fun filterByType(type: String) {
        val filteredLogs = allTravelLogs.filter { it.type == type }
        updateAdapterWithLogs(filteredLogs)
    }
    
    // 날짜 문자열에서 시작 날짜 추출 (정렬용)
    private fun getStartDateForSorting(dateString: String): Long {
        val startDateStr = dateString.split(" - ")[0]
        return try {
            displayDateFormat.parse(startDateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    // 그룹 체크박스 상태 업데이트
    private fun updateGroupCheckboxState() {
        groupCheckbox.setImageResource(
            if (isGroupSelected) R.drawable.group_checkbox_on else R.drawable.checkbox_off_icon
        )
    }
    
    // 개인 체크박스 상태 업데이트
    private fun updatePersonalCheckboxState() {
        personalCheckbox.setImageResource(
            if (isPersonalSelected) R.drawable.personal_checkbox_on else R.drawable.checkbox_off_icon
        )
    }
    
    // 필터 적용
    private fun applyFilters() {
        val filtered = when {
            isGroupSelected && !isPersonalSelected -> allTravelLogs.filter { it.type == "그룹" }
            !isGroupSelected && isPersonalSelected -> allTravelLogs.filter { it.type == "개인" }
            isGroupSelected && isPersonalSelected -> allTravelLogs
            else -> allTravelLogs
        }
        
        // 현재 선택된 정렬 방식에 따라 필터 적용 후 정렬
        when (dropdownSpinner.selectedItemPosition) {
            0 -> updateAdapterWithLogs(filtered.sortedByDescending { getStartDateForSorting(it.date) })
            1 -> updateAdapterWithLogs(filtered.sortedBy { getStartDateForSorting(it.date) })
            2 -> {
                val year = yearSpinner.selectedItem.toString()
                updateAdapterWithLogs(filtered.filter { log -> log.date.contains(year) })
            }
        }
    }
    
    // 샘플 데이터 (API 호출 실패 시 사용)
    private fun getSampleTravelLogs(): List<TravelLog> {
        // 과거 날짜로 샘플 데이터 생성 (현재 기준 -1개월, -2개월, -3개월, -4개월)
        val cal = Calendar.getInstance()
        
        // 첫 번째 여행: 1개월 전
        cal.add(Calendar.MONTH, -1)
        val date1End = displayDateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -3) // 3일간의 여행
        val date1Start = displayDateFormat.format(cal.time)
        
        // 두 번째 여행: 2개월 전
        cal.add(Calendar.DAY_OF_MONTH, 3) // 첫 번째 여행 종료일로 되돌리기
        cal.add(Calendar.MONTH, -1) // 추가로 1개월 전 (총 2개월 전)
        val date2End = displayDateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -2) // 2일간의 여행
        val date2Start = displayDateFormat.format(cal.time)
        
        // 세 번째 여행: 3개월 전
        cal.add(Calendar.DAY_OF_MONTH, 2) // 두 번째 여행 종료일로 되돌리기
        cal.add(Calendar.MONTH, -1) // 추가로 1개월 전 (총 3개월 전)
        val date3End = displayDateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -3) // 3일간의 여행
        val date3Start = displayDateFormat.format(cal.time)
        
        // 네 번째 여행: 4개월 전
        cal.add(Calendar.DAY_OF_MONTH, 3) // 세 번째 여행 종료일로 되돌리기
        cal.add(Calendar.MONTH, -1) // 추가로 1개월 전 (총 4개월 전)
        val date4End = displayDateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -2) // 2일간의 여행
        val date4Start = displayDateFormat.format(cal.time)
        
        return listOf(
            TravelLog(
                "제주도 여행", 
                "$date1Start - $date1End", 
                "그룹",
                listOf(
                    TravelPlace(id = 1, name = "성산일출봉", address = "제주특별자치도 서귀포시 성산읍", visitDate = "09:00 - 11:00"),
                    TravelPlace(id = 2, name = "우도", address = "제주특별자치도 제주시 우도면", visitDate = "12:00 - 15:00"),
                    TravelPlace(id = 3, name = "만장굴", address = "제주특별자치도 제주시 구좌읍", visitDate = "16:00 - 18:00")
                ),
                planId = 1
            ),
            TravelLog(
                "부산 여행", 
                "$date2Start - $date2End", 
                "개인",
                listOf(
                    TravelPlace(id = 4, name = "해운대", address = "부산광역시 해운대구", visitDate = "10:00 - 13:00"),
                    TravelPlace(id = 5, name = "감천문화마을", address = "부산광역시 사하구", visitDate = "14:00 - 16:00")
                ),
                planId = 2
            ),
            TravelLog(
                "강원도 여행", 
                "$date3Start - $date3End", 
                "그룹",
                listOf(
                    TravelPlace(id = 6, name = "양양 서핑", address = "강원도 양양군", visitDate = "09:00 - 12:00"),
                    TravelPlace(id = 7, name = "속초 해변", address = "강원도 속초시", visitDate = "13:00 - 15:00"),
                    TravelPlace(id = 8, name = "설악산", address = "강원도 속초시", visitDate = "16:00 - 18:00")
                ),
                planId = 3
            ),
            TravelLog(
                "서울 여행", 
                "$date4Start - $date4End", 
                "개인",
                listOf(
                    TravelPlace(id = 9, name = "경복궁", address = "서울특별시 종로구", visitDate = "09:00 - 12:00"),
                    TravelPlace(id = 10, name = "남산타워", address = "서울특별시 용산구", visitDate = "14:00 - 16:00")
                ),
                planId = 4
            )
        )
    }
}