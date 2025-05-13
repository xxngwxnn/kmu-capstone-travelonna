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
import com.example.travelonna.api.Plan
import com.example.travelonna.api.RetrofitClient
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
        
        // API에서 데이터 가져오기
        fetchTravelPlans()
    }
    
    // API에서 여행 일정 데이터 가져오기
    private fun fetchTravelPlans() {
        Log.d("LogActivity", "Fetching travel plans from API: ${RetrofitClient.BASE_URL}api/v1/plans")
        
        RetrofitClient.planApiService.getPlans().enqueue(object : Callback<com.example.travelonna.api.PlanResponse> {
            override fun onResponse(
                call: Call<com.example.travelonna.api.PlanResponse>,
                response: Response<com.example.travelonna.api.PlanResponse>
            ) {
                if (response.isSuccessful) {
                    val planResponse = response.body()
                    Log.d("LogActivity", "API response successful: ${planResponse?.message}")
                    
                    if (planResponse != null && planResponse.success) {
                        // API 응답을 TravelLog 객체로 변환
                        allTravelLogs = convertPlansToTravelLogs(planResponse.data)
                        Log.d("LogActivity", "Received ${planResponse.data.size} plans from API")
                        
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

            override fun onFailure(call: Call<com.example.travelonna.api.PlanResponse>, t: Throwable) {
                Log.e("LogActivity", "API call failed", t)
                Toast.makeText(this@LogActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                // 실패 시 샘플 데이터 사용
                allTravelLogs = getSampleTravelLogs()
                updateAdapterWithLogs(allTravelLogs)
            }
        })
    }
    
    // Plan 객체를 TravelLog 객체로 변환
    private fun convertPlansToTravelLogs(plans: List<Plan>): List<TravelLog> {
        return plans.map { plan ->
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
            
            TravelLog(
                title = plan.title,
                date = "$startDate - $endDate",
                type = type,
                places = listOf() // 장소 데이터는 아직 API에 없음
            )
        }
    }
    
    // 어댑터 업데이트
    private fun updateAdapterWithLogs(logs: List<TravelLog>) {
        logAdapter = LogAdapter(logs)
        recyclerView.adapter = logAdapter
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
                updateAdapterWithLogs(filtered.filter { it.date.contains(year) })
            }
        }
    }
    
    // 샘플 데이터 (API 호출 실패 시 사용)
    private fun getSampleTravelLogs(): List<TravelLog> {
        return listOf(
            TravelLog(
                "제주도 여행", 
                "2024.03.15 - 2024.03.18", 
                "그룹",
                listOf(
                    TravelPlace("성산일출봉", "제주특별자치도 서귀포시 성산읍", "09:00 - 11:00"),
                    TravelPlace("우도", "제주특별자치도 제주시 우도면", "12:00 - 15:00"),
                    TravelPlace("만장굴", "제주특별자치도 제주시 구좌읍", "16:00 - 18:00")
                )
            ),
            TravelLog(
                "부산 여행", 
                "2024.02.20 - 2024.02.22", 
                "개인",
                listOf(
                    TravelPlace("해운대", "부산광역시 해운대구", "10:00 - 13:00"),
                    TravelPlace("감천문화마을", "부산광역시 사하구", "14:00 - 16:00")
                )
            ),
            TravelLog(
                "강원도 여행", 
                "2023.01.10 - 2023.01.12", 
                "그룹",
                listOf(
                    TravelPlace("양양 서핑", "강원도 양양군", "09:00 - 12:00"),
                    TravelPlace("속초 해변", "강원도 속초시", "13:00 - 15:00"),
                    TravelPlace("설악산", "강원도 속초시", "16:00 - 18:00")
                )
            ),
            TravelLog(
                "서울 여행", 
                "2022.08.10 - 2022.08.12", 
                "개인",
                listOf(
                    TravelPlace("경복궁", "서울특별시 종로구", "09:00 - 12:00"),
                    TravelPlace("남산타워", "서울특별시 용산구", "14:00 - 16:00")
                )
            )
        )
    }
}