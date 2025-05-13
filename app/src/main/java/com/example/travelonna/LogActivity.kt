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

class LogActivity : AppCompatActivity() {

    private lateinit var dropdownSpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var checkboxOption1: CheckBox
    private lateinit var checkboxOption2: CheckBox
    private lateinit var recyclerView: RecyclerView
    private lateinit var logAdapter: LogAdapter
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        // 초기화
        dropdownSpinner = findViewById(R.id.dropdownSpinner)
        yearSpinner = findViewById(R.id.yearSpinner)
        checkboxOption1 = findViewById(R.id.checkboxOption1)
        checkboxOption2 = findViewById(R.id.checkboxOption2)
        recyclerView = findViewById(R.id.recyclerView)
        backButton = findViewById(R.id.backButton)

        // 뒤로가기 버튼 클릭 이벤트
        backButton.setOnClickListener {
            finish()
        }

        // 드롭다운에 표시할 데이터 설정
        val options = arrayOf("최근 순", "오래된 순", "연도별")
        val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dropdownSpinner.adapter = adapter

        // 연도별 스피너 설정
        // 현재 연도부터 몇 년 전까지 표시
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val years = (currentYear downTo currentYear - 5).map { it.toString() }.toTypedArray()
        val yearAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, years)
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

        // 체크박스 클릭 리스너 설정
        checkboxOption1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d("LogActivity", "그룹 선택됨")
                filterByType("그룹")
            } else {
                updateTravelList() // 체크박스 해제 시 모든 데이터 표시
            }
        }

        checkboxOption2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d("LogActivity", "개인 선택됨")
                filterByType("개인")
            } else {
                updateTravelList() // 체크박스 해제 시 모든 데이터 표시
            }
        }

        // RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // 샘플 데이터 생성
        updateTravelList()
    }

    // 전체 여행 목록 업데이트
    private fun updateTravelList() {
        val travelLogs = listOf(
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

        logAdapter = LogAdapter(travelLogs)
        recyclerView.adapter = logAdapter
    }

    // 최근 날짜순으로 정렬
    private fun sortByRecent() {
        val allLogs = getAllTravelLogs()
        val sortedLogs = allLogs.sortedByDescending { getStartDate(it.date) }
        logAdapter = LogAdapter(sortedLogs)
        recyclerView.adapter = logAdapter
    }

    // 오래된 날짜순으로 정렬
    private fun sortByOldest() {
        val allLogs = getAllTravelLogs()
        val sortedLogs = allLogs.sortedBy { getStartDate(it.date) }
        logAdapter = LogAdapter(sortedLogs)
        recyclerView.adapter = logAdapter
    }

    // 연도별 필터링
    private fun filterByYear(year: String) {
        val allLogs = getAllTravelLogs()
        val filteredLogs = allLogs.filter { 
            it.date.contains(year) 
        }
        logAdapter = LogAdapter(filteredLogs)
        recyclerView.adapter = logAdapter
    }

    // 유형별 필터링 (그룹/개인)
    private fun filterByType(type: String) {
        val allLogs = getAllTravelLogs()
        val filteredLogs = allLogs.filter { it.type == type }
        logAdapter = LogAdapter(filteredLogs)
        recyclerView.adapter = logAdapter
    }

    // 모든 여행 데이터 가져오기 (실제로는 DB나 API에서 가져올 것)
    private fun getAllTravelLogs(): List<TravelLog> {
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

    // 날짜 문자열에서 시작 날짜 추출
    private fun getStartDate(dateString: String): String {
        return dateString.split(" - ")[0]
    }
}