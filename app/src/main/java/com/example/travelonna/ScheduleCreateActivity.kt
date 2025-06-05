package com.example.travelonna

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import com.example.travelonna.data.LocationData
import com.example.travelonna.view.CustomToggleButton
import com.example.travelonna.api.PlanCreateRequest
import com.example.travelonna.api.PlanCreateResponse
import com.example.travelonna.api.PlanCreateData
import com.example.travelonna.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.util.Log
import android.content.Context
import android.widget.RadioGroup
import com.example.travelonna.util.TransportationSearchManager
import android.widget.ImageButton
import com.example.travelonna.api.GroupUrlRequest
import com.example.travelonna.api.GroupUrlResponse

class ScheduleCreateActivity : AppCompatActivity() {

    private lateinit var typeToggle: CustomToggleButton
    private lateinit var personalText: TextView
    private lateinit var groupText: TextView
    private lateinit var scheduleNameInput: EditText
    private lateinit var dateRangeButton: TextView
    private lateinit var locationSelectButton: TextView
    private lateinit var memoInput: EditText
    private lateinit var createScheduleButton: Button
    private lateinit var transportCar: TextView
    private lateinit var transportBus: TextView
    private lateinit var transportTrain: TextView
    private lateinit var transportEtc: TextView

    private var selectedTransport = "" // 기본 선택 없음
    private var isEditMode = false // 편집 모드 여부
    private var planId = 0 // 수정할 일정의 ID

    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance()

    // 카드뷰 변수 추가
    private lateinit var typeCard: CardView
    private lateinit var nameCard: CardView
    private lateinit var dateCard: CardView
    private lateinit var locationCard: CardView
    private lateinit var transportCard: CardView
    private lateinit var memoCard: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_create)

        // 뷰 초기화
        typeToggle = findViewById(R.id.typeToggle)
        personalText = findViewById(R.id.personalText)
        groupText = findViewById(R.id.groupText)
        scheduleNameInput = findViewById(R.id.scheduleNameInput)
        dateRangeButton = findViewById(R.id.dateRangeButton)
        locationSelectButton = findViewById(R.id.locationSelectButton)
        memoInput = findViewById(R.id.memoInput)
        createScheduleButton = findViewById(R.id.createScheduleButton)
        
        // 교통수단 TextView 초기화
        transportCar = findViewById(R.id.transportCar)
        transportBus = findViewById(R.id.transportBus)
        transportTrain = findViewById(R.id.transportTrain)
        transportEtc = findViewById(R.id.transportEtc)

        // 카드뷰 초기화
        typeCard = findViewById(R.id.typeCard)
        nameCard = findViewById(R.id.nameCard)
        dateCard = findViewById(R.id.dateCard)
        locationCard = findViewById(R.id.locationCard)
        transportCard = findViewById(R.id.transportCard)
        memoCard = findViewById(R.id.memoCard)
        
        // 편집 모드 확인 및 데이터 설정
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        
        // 초기 UI 설정
        setupInitialUI()
        
        // 교통수단 선택 리스너 설정
        setupTransportSelectionListeners()

        if (isEditMode) {
            // 기존 데이터 가져오기
            planId = intent.getIntExtra("PLAN_ID", 0)
            val title = intent.getStringExtra("TITLE") ?: ""
            val startDate = intent.getStringExtra("START_DATE") ?: ""
            val endDate = intent.getStringExtra("END_DATE") ?: ""
            val location = intent.getStringExtra("LOCATION") ?: ""
            val transportInfo = intent.getStringExtra("TRANSPORT_INFO") ?: ""
            val isPublic = intent.getBooleanExtra("IS_PUBLIC", true)
            val memo = intent.getStringExtra("MEMO") ?: ""
            val totalCost = intent.getIntExtra("TOTAL_COST", 0)

            // UI에 기존 데이터 설정
            scheduleNameInput.setText(title)
            locationSelectButton.text = location
            memoInput.setText(memo)
            typeToggle.setChecked(!isPublic) // 토글이 반대로 되어있으므로

            // 날짜 설정
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                startDateCalendar.time = dateFormat.parse(startDate) ?: Calendar.getInstance().time
                endDateCalendar.time = dateFormat.parse(endDate) ?: Calendar.getInstance().time
                updateDateRangeText()
            } catch (e: Exception) {
                Log.e("ScheduleCreate", "Date parsing error", e)
            }

            // 교통수단 선택 상태 복원
            selectedTransport = transportInfo
            updateTransportSelection(transportInfo)

            // 버튼 텍스트 변경
            createScheduleButton.text = "일정 수정하기"
            
            // 타이틀 변경
            findViewById<TextView>(R.id.titleText).text = "일정 수정"
        }
        
        // 토글 버튼 리스너 수정
        typeToggle.setOnCheckedChangeListener { isChecked ->
            updateToggleTextColors(isChecked)
        }
        
        // 초기 상태에서도 토글 배경을 파란색으로 설정
        updateToggleTextColors(typeToggle.isChecked())
        
        // 일정 이름 입력 감지 리스너 추가
        scheduleNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 이름이 입력되면 날짜 선택 카드 표시
                if (!s.isNullOrEmpty()) {
                    showNextStep(dateCard)
                }
            }
        })
        
        // 날짜 선택 리스너 수정
        dateRangeButton.setOnClickListener {
            showStartDatePicker()
        }
        
        // 위치 선택 리스너 수정
        locationSelectButton.setOnClickListener {
            showLocationSelector()
        }
        
        // 일정 생성 버튼 리스너
        createScheduleButton.setOnClickListener {
            if (validateInputs()) {
                createSchedule()
            }
        }
    }

    private fun showStartDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.DatePickerDialogTheme,
            { _, year, month, dayOfMonth ->
                startDateCalendar.set(Calendar.YEAR, year)
                startDateCalendar.set(Calendar.MONTH, month)
                startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // 즉시 종료일 선택기 표시
                showEndDatePicker()
            },
            startDateCalendar.get(Calendar.YEAR),
            startDateCalendar.get(Calendar.MONTH),
            startDateCalendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // DatePicker 창 설정 추가
        datePickerDialog.setCancelable(false)
        datePickerDialog.show()
    }

    private fun showEndDatePicker() {
        // 최소 날짜를 시작일로 설정
        endDateCalendar.time = startDateCalendar.time
        
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.DatePickerDialogTheme,
            { _, year, month, dayOfMonth ->
                endDateCalendar.set(Calendar.YEAR, year)
                endDateCalendar.set(Calendar.MONTH, month)
                endDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // 날짜가 모두 선택되면 UI 업데이트
                updateDateRangeText()
            },
            endDateCalendar.get(Calendar.YEAR),
            endDateCalendar.get(Calendar.MONTH),
            endDateCalendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // 시작일 이전 날짜 선택 방지
        datePickerDialog.datePicker.minDate = startDateCalendar.timeInMillis
        datePickerDialog.setCancelable(false)
        datePickerDialog.show()
    }

    private fun updateDateRangeText() {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
        val startDateStr = dateFormat.format(startDateCalendar.time)
        val endDateStr = dateFormat.format(endDateCalendar.time)
        dateRangeButton.text = "$startDateStr - $endDateStr"
        
        // 날짜 선택 완료 후 위치 선택 카드 표시
        showNextStep(locationCard)
    }

    private fun validateInputs(): Boolean {
        if (scheduleNameInput.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "일정 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (dateRangeButton.text.toString() == "날짜를 선택해주세요") {
            Toast.makeText(this, "날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (locationSelectButton.text.toString() == "위치를 선택해주세요.") {
            Toast.makeText(this, "위치를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        // 저장된 토큰 로그 출력
        val sharedPref = getSharedPreferences("auth_token_pref", Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", null)
        Log.d("ScheduleCreate", "Token: $token")

        return true
    }

    private fun createSchedule() {
        // 로딩 다이얼로그 표시
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        // 날짜 형식 변환 (YYYY-MM-DD)
        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDateStr = apiDateFormat.format(startDateCalendar.time)
        val endDateStr = apiDateFormat.format(endDateCalendar.time)

        // 요청 객체 생성
        val planRequest = PlanCreateRequest(
            title = scheduleNameInput.text.toString(),
            startDate = startDateStr,
            endDate = endDateStr,
            location = locationSelectButton.text.toString(),
            memo = memoInput.text.toString(),
            isGroupPlan = typeToggle.isChecked(),
            transportInfo = selectedTransport,
            groupId = null
        )

        if (isEditMode) {
            // 수정 API 호출
            RetrofitClient.apiService.updatePlan(planId, planRequest)
                .enqueue(object : Callback<PlanCreateResponse> {
                    override fun onResponse(call: Call<PlanCreateResponse>, response: Response<PlanCreateResponse>) {
                        loadingDialog.dismiss()
                        
                        if (response.isSuccessful) {
                            Toast.makeText(this@ScheduleCreateActivity, "일정이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "일정 수정 중 오류가 발생했습니다"
                            Toast.makeText(this@ScheduleCreateActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            Log.e("ScheduleCreate", "API Error: $errorMsg")
                        }
                    }
                    
                    override fun onFailure(call: Call<PlanCreateResponse>, t: Throwable) {
                        loadingDialog.dismiss()
                        Toast.makeText(this@ScheduleCreateActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                        Log.e("ScheduleCreate", "Network Error: ${t.message}")
                    }
                })
        } else {
            // 그룹 여행인 경우 먼저 그룹 URL 생성 API 호출
            if (typeToggle.isChecked()) {
                createGroupUrlFirst(startDateStr, endDateStr, loadingDialog)
            } else {
                // 개인 여행의 경우 바로 일정 생성
                createPlan(startDateStr, endDateStr, null, loadingDialog)
            }
        }
    }
    
    // 그룹 URL을 먼저 생성하는 메서드
    private fun createGroupUrlFirst(startDateStr: String, endDateStr: String, loadingDialog: AlertDialog) {
        val groupUrlRequest = GroupUrlRequest(isGroup = true)
        
        // 요청 본문 로그
        Log.d("ScheduleCreate", "GroupUrlRequest: $groupUrlRequest")
        
        RetrofitClient.apiService.createGroupUrl(groupUrlRequest)
            .enqueue(object : Callback<GroupUrlResponse> {
                override fun onResponse(call: Call<GroupUrlResponse>, response: Response<GroupUrlResponse>) {
                    // 응답 코드 및 본문 로그
                    Log.d("ScheduleCreate", "GroupUrl API Response Code: ${response.code()}")
                    Log.d("ScheduleCreate", "GroupUrl API Response Body: ${response.body()}")
                    
                    if (response.isSuccessful) {
                        val groupUrlResponse = response.body()
                        val groupId = groupUrlResponse?.id ?: 0
                        val groupUrl = groupUrlResponse?.url ?: ""
                        
                        Log.d("ScheduleCreate", "Group created with ID: $groupId, URL: $groupUrl")
                        
                        // 그룹 ID를 이용하여 일정 생성
                        if (groupId > 0) {
                            createPlan(startDateStr, endDateStr, groupId, loadingDialog, groupUrl)
                        } else {
                            Toast.makeText(this@ScheduleCreateActivity, "유효하지 않은 그룹 ID입니다.", Toast.LENGTH_SHORT).show()
                            loadingDialog.dismiss()
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "그룹 URL 생성 중 오류가 발생했습니다"
                        Toast.makeText(this@ScheduleCreateActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e("ScheduleCreate", "Group URL API Error: $errorMsg")
                        
                        // 그룹 생성 실패 시 그룹 ID 없이 일정 생성
                        createPlan(startDateStr, endDateStr, null, loadingDialog)
                    }
                }
                
                override fun onFailure(call: Call<GroupUrlResponse>, t: Throwable) {
                    Log.e("ScheduleCreate", "Group URL Network Error: ${t.message}")
                    Log.e("ScheduleCreate", "Error details:", t)
                    Toast.makeText(this@ScheduleCreateActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    
                    // 네트워크 오류 시에도 그룹 ID 없이 일정 생성
                    createPlan(startDateStr, endDateStr, null, loadingDialog)
                }
            })
    }
    
    // 일정 생성 메서드 (그룹 ID 포함)
    private fun createPlan(startDateStr: String, endDateStr: String, groupId: Int?, loadingDialog: AlertDialog, groupUrl: String = "") {
        // 요청 객체 생성
        val planRequest = PlanCreateRequest(
            title = scheduleNameInput.text.toString(),
            startDate = startDateStr,
            endDate = endDateStr,
            location = locationSelectButton.text.toString(),
            memo = memoInput.text.toString(),
            isGroupPlan = typeToggle.isChecked(),
            transportInfo = selectedTransport,
            groupId = groupId
        )
        
        // 요청 로그
        Log.d("ScheduleCreate", "Plan Request: $planRequest (with groupId: $groupId)")
        
        // API 호출
        RetrofitClient.apiService.createPlan(planRequest)
            .enqueue(object : Callback<PlanCreateResponse> {
                override fun onResponse(call: Call<PlanCreateResponse>, response: Response<PlanCreateResponse>) {
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful) {
                        val planCreateData = response.body()?.data
                        val planId = planCreateData?.planId ?: 0
                        
                        // 계획 ID 로그 추가
                        Log.d("ScheduleCreate", "Plan ID from API: $planId")
                        
                        if (planId > 0) {
                            if (typeToggle.isChecked() && groupUrl.isNotEmpty()) {
                                Toast.makeText(
                                    this@ScheduleCreateActivity, 
                                    "그룹 일정이 생성되었습니다. 공유 URL: $groupUrl", 
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(this@ScheduleCreateActivity, "일정이 생성되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                            
                            // 일정 상세 화면으로 이동
                            navigateToDetailActivity(planId, groupUrl)
                        } else {
                            Toast.makeText(this@ScheduleCreateActivity, "유효하지 않은 일정 ID입니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 에러 처리
                        val errorMsg = response.errorBody()?.string() ?: "일정 생성 중 오류가 발생했습니다"
                        Toast.makeText(this@ScheduleCreateActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e("ScheduleCreate", "API Error: $errorMsg")
                    }
                }
                
                override fun onFailure(call: Call<PlanCreateResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@ScheduleCreateActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ScheduleCreate", "Network Error: ${t.message}")
                    
                    // 오류 발생 시 로컬 모드로 전환 (선택사항)
                    startLocalDetailActivity()
                }
            })
    }
    
    // 일정 상세 화면으로 이동하는 메서드
    private fun navigateToDetailActivity(planId: Int, groupUrl: String = "") {
        val intent = Intent(this@ScheduleCreateActivity, ScheduleDetailActivity::class.java).apply {
            putExtra("START_DATE", startDateCalendar.timeInMillis)
            putExtra("END_DATE", endDateCalendar.timeInMillis)
            putExtra("SCHEDULE_NAME", scheduleNameInput.text.toString())
            putExtra("LOCATION", locationSelectButton.text.toString())
            putExtra("MEMO", memoInput.text.toString())
            putExtra("IS_GROUP", typeToggle.isChecked())
            putExtra("PLAN_ID", planId)
            if (groupUrl.isNotEmpty()) {
                putExtra("GROUP_URL", groupUrl)
            }
        }
        startActivity(intent)
        finish()
    }

    // 네트워크 오류 시 로컬 모드로 상세 화면 시작
    private fun startLocalDetailActivity() {
        val intent = Intent(this, ScheduleDetailActivity::class.java).apply {
            putExtra("START_DATE", startDateCalendar.timeInMillis)
            putExtra("END_DATE", endDateCalendar.timeInMillis)
            putExtra("SCHEDULE_NAME", scheduleNameInput.text.toString())
            putExtra("LOCATION", locationSelectButton.text.toString())
            putExtra("MEMO", memoInput.text.toString())
            putExtra("IS_GROUP", typeToggle.isChecked())
            // PLAN_ID를 전달하지 않아 로컬 모드로 동작
        }
        startActivity(intent)
        finish()
    }

    // 토글 상태에 따라 텍스트 색상 업데이트
    private fun updateToggleTextColors(isGroupMode: Boolean) {
        if (isGroupMode) {
            // 그룹 모드 활성화: 그룹 텍스트 파란색, 개인 텍스트 회색
            personalText.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
            groupText.setTextColor(ContextCompat.getColor(this, R.color.blue_primary))
        } else {
            // 개인 모드 활성화: 개인 텍스트 파란색, 그룹 텍스트 회색
            personalText.setTextColor(ContextCompat.getColor(this, R.color.blue_primary))
            groupText.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
        }
        
        // 항상 토글 배경을 파란색으로 설정 (CustomToggleButton 내부 메서드 호출)
        (typeToggle as? CustomToggleButton)?.let { toggleButton ->
            // toggle 내부의 View들을 직접 조작
            val track = toggleButton.findViewById<View>(R.id.toggleTrack)
            val thumb = toggleButton.findViewById<View>(R.id.toggleThumb)
            
            // GradientDrawable로 파란색 배경 생성
            val primaryColor = ContextCompat.getColor(this, R.color.blue_primary)
            val trackDrawable = GradientDrawable().apply {
                setColor(primaryColor)
                cornerRadius = 15f * resources.displayMetrics.density
            }
            
            // 흰색 동그라미 + 파란색 테두리
            val thumbDrawable = GradientDrawable().apply {
                setColor(ContextCompat.getColor(this@ScheduleCreateActivity, android.R.color.white))
                cornerRadius = 14f * resources.displayMetrics.density
                setStroke(1, primaryColor)
            }
            
            // 배경 설정
            track.background = trackDrawable
            thumb.background = thumbDrawable
        }
    }

    private fun showLocationSelector() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_location_select, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 초기화
        val regionSpinner = dialogView.findViewById<Spinner>(R.id.regionSpinner)
        val subRegionSpinner = dialogView.findViewById<Spinner>(R.id.subRegionSpinner)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
        
        // 서브리전 관련 뷰
        val subRegionTitle = dialogView.findViewById<TextView>(R.id.subRegionTitle)
        val subRegionLayout = dialogView.findViewById<LinearLayout>(R.id.subRegionLayout)
        
        // 처음에는 하위 지역 선택 UI 숨김
        subRegionLayout.visibility = View.GONE
        
        // 시/도 어댑터 설정
        val regionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            LocationData.regions
        )
        regionSpinner.adapter = regionAdapter
        
        // 시/도 선택 시 시/군/구 리스트 업데이트
        regionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRegion = LocationData.regions[position]
                
                // 선택한 지역이 하위지역을 갖는 도인지 확인
                if (LocationData.provincesWithSubregions.contains(selectedRegion)) {
                    subRegionLayout.visibility = View.VISIBLE
                    
                    val subRegions = LocationData.subRegions[selectedRegion] ?: listOf()
                    val subRegionAdapter = ArrayAdapter(
                        this@ScheduleCreateActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        subRegions
                    )
                    subRegionSpinner.adapter = subRegionAdapter
                } else {
                    // 광역시 등은 하위 지역 선택 UI 숨김
                    subRegionLayout.visibility = View.GONE
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                subRegionLayout.visibility = View.GONE
            }
        }
        
        // 확인 버튼 클릭 시 선택한 위치 적용 및 다음 단계 표시
        confirmButton.setOnClickListener {
            val selectedRegion = regionSpinner.selectedItem.toString()
            val subRegions = LocationData.subRegions[selectedRegion] ?: listOf()
            
            if (subRegions.isNotEmpty() && subRegionSpinner.selectedItemPosition >= 0) {
                val selectedSubRegion = subRegionSpinner.selectedItem.toString()
                locationSelectButton.text = "$selectedRegion $selectedSubRegion"
            } else {
                locationSelectButton.text = selectedRegion
            }
            
            dialog.dismiss()
            
            // 위치 선택 후 교통수단 선택 카드 표시
            showNextStep(transportCard)
        }
        
        // 다이얼로그 표시
        dialog.show()
    }

    // 초기 UI 설정 메서드
    private fun setupInitialUI() {
        if (isEditMode) {
            // 편집 모드일 때는 모든 카드를 한 번에 표시
            typeCard.visibility = View.VISIBLE
            nameCard.visibility = View.VISIBLE
            dateCard.visibility = View.VISIBLE
            locationCard.visibility = View.VISIBLE
            transportCard.visibility = View.VISIBLE
            memoCard.visibility = View.VISIBLE
            createScheduleButton.visibility = View.VISIBLE
        } else {
            // 새로 생성할 때는 순차적으로 표시
            typeCard.visibility = View.VISIBLE
            nameCard.visibility = View.VISIBLE
            dateCard.visibility = View.GONE
            locationCard.visibility = View.GONE
            transportCard.visibility = View.GONE
            memoCard.visibility = View.GONE
            createScheduleButton.visibility = View.GONE
        }
    }
    
    // 다음 단계 표시 메서드
    private fun showNextStep(cardView: View) {
        cardView.visibility = View.VISIBLE
    }

    // 교통수단 선택 리스너 설정
    private fun setupTransportSelectionListeners() {
        // 초기 상태 설정 (선택 없음)
        transportCar.isSelected = false
        transportCar.background = null
        transportBus.background = null
        transportTrain.background = null
        transportEtc.background = null

        // 편집 모드가 아닐 때만 초기 색상을 회색으로 설정
        if (!isEditMode) {
            transportCar.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
            transportBus.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
            transportTrain.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
            transportEtc.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
        }
        
        val transportViews = listOf(transportCar, transportBus, transportTrain, transportEtc)
        val transportValues = listOf("car", "bus", "train", "etc")
        
        // 교통수단 검색 버튼 참조
        val searchTransportButton = findViewById<ImageButton>(R.id.searchTransportButton)
        searchTransportButton.visibility = View.GONE
        
        // 각 교통수단 TextView에 클릭 리스너 설정
        transportViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                // 모든 뷰 선택 해제 및 회색 텍스트로 설정
                transportViews.forEach { transportView -> 
                    transportView.isSelected = false
                    transportView.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
                }
                
                // 선택된 뷰만 선택 상태로 변경 및 파란색 텍스트로 설정
                view.isSelected = true
                view.setTextColor(ContextCompat.getColor(this, R.color.blue_primary))
                
                // 선택된 교통수단 값 저장
                selectedTransport = transportValues[index]
                
                // 기차 선택 시에만 검색 버튼 표시, 그 외에는 숨김
                searchTransportButton.visibility = if (selectedTransport == "train") {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                // 교통수단 선택 후 메모 카드와 생성 버튼 표시
                showNextStep(memoCard)
                showNextStep(createScheduleButton)
            }
        }
        
        // 편집 모드일 때 초기 교통수단 선택 상태 설정
        if (isEditMode && selectedTransport.isNotEmpty()) {
            val index = transportValues.indexOf(selectedTransport.lowercase())
            if (index != -1) {
                transportViews[index].setTextColor(ContextCompat.getColor(this, R.color.blue_primary))
                transportViews[index].isSelected = true
                
                // 기차가 선택된 경우 검색 버튼 표시
                if (selectedTransport == "train") {
                    searchTransportButton.visibility = View.VISIBLE
                }
            }
        }
        
        // 교통수단 검색 버튼 리스너 설정
        searchTransportButton.setOnClickListener {
            if (locationSelectButton.text.toString() != "위치를 선택해주세요." && 
                dateRangeButton.text.toString() != "날짜를 선택해주세요") {
                
                // 위치와 날짜가 모두 선택된 경우에만 교통수단 검색 다이얼로그 표시
                val destinationName = locationSelectButton.text.toString()
                val searchManager = TransportationSearchManager(this)
                // 선택된 교통수단(train)을 자동으로 전달
                searchManager.showTransportationSearchDialog(destinationName, startDateCalendar.timeInMillis, selectedTransport)
            } else {
                Toast.makeText(this, "위치와 날짜를 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 교통수단 선택 상태 업데이트 메서드
    private fun updateTransportSelection(transport: String) {
        // 모든 교통수단 버튼 초기화
        val transportViews = listOf(transportCar, transportBus, transportTrain, transportEtc)
        transportViews.forEach { view ->
            view.background = null
            view.setTextColor(ContextCompat.getColor(this, R.color.gray_text))
            view.isSelected = false
        }

        // 선택된 교통수단 강조
        val selectedView = when (transport.lowercase()) {
            "car" -> transportCar
            "bus" -> transportBus
            "train" -> transportTrain
            "etc" -> transportEtc
            else -> null
        }

        selectedView?.let {
            it.isSelected = true
            it.setTextColor(ContextCompat.getColor(this, R.color.blue_primary))
        }
    }
} 