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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleCreateActivity : AppCompatActivity() {

    private lateinit var typeToggle: CustomToggleButton
    private lateinit var personalText: TextView
    private lateinit var groupText: TextView
    private lateinit var scheduleNameInput: EditText
    private lateinit var dateRangeButton: TextView
    private lateinit var locationSelectButton: TextView
    private lateinit var memoInput: EditText
    private lateinit var createScheduleButton: Button

    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance()

    // 카드뷰 변수 추가
    private lateinit var typeCard: CardView
    private lateinit var nameCard: CardView
    private lateinit var dateCard: CardView
    private lateinit var locationCard: CardView
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

        // 카드뷰 초기화
        typeCard = findViewById(R.id.typeCard)
        nameCard = findViewById(R.id.nameCard)
        dateCard = findViewById(R.id.dateCard)
        locationCard = findViewById(R.id.locationCard)
        memoCard = findViewById(R.id.memoCard)
        
        // 초기 UI 설정 - 처음에는 일정 유형과 일정 이름 모두 바로 표시
        setupInitialUI()

        // 토글 버튼 리스너 수정
        typeToggle.setOnCheckedChangeListener { isChecked ->
            updateToggleTextColors(isChecked)
        }
        
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
                updateDateRangeLabel()
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

    private fun updateDateRangeLabel() {
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        val startDateStr = dateFormat.format(startDateCalendar.time)
        val endDateStr = dateFormat.format(endDateCalendar.time)
        
        // 시작일~종료일 형식으로 표시
        dateRangeButton.text = "$startDateStr~$endDateStr"
        
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

        return true
    }

    private fun createSchedule() {
        // 일정 생성 로직 구현
        // API 호출 또는 로컬 DB 저장 등의 작업 수행
        Toast.makeText(this, "일정이 생성되었습니다.", Toast.LENGTH_SHORT).show()
        
        // 일정 상세 화면으로 이동
        val intent = Intent(this, ScheduleDetailActivity::class.java).apply {
            putExtra("START_DATE", startDateCalendar.timeInMillis)
            putExtra("END_DATE", endDateCalendar.timeInMillis)
            putExtra("SCHEDULE_NAME", scheduleNameInput.text.toString())
            putExtra("LOCATION", locationSelectButton.text.toString())
            putExtra("MEMO", memoInput.text.toString())
            putExtra("IS_GROUP", typeToggle.isChecked())
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
            
            // 위치 선택 후 메모 카드와 생성 버튼 함께 표시
            showNextStep(memoCard)
            showNextStep(createScheduleButton)
        }
        
        // 다이얼로그 표시
        dialog.show()
    }

    // 초기 UI 설정 메서드
    private fun setupInitialUI() {
        // 처음에는 일정 유형과 일정 이름 모두 바로 표시
        typeCard.visibility = View.VISIBLE
        nameCard.visibility = View.VISIBLE  // 지연 없이 바로 표시
        dateCard.visibility = View.GONE
        locationCard.visibility = View.GONE
        memoCard.visibility = View.GONE
        createScheduleButton.visibility = View.GONE
    }
    
    // 다음 단계 표시 메서드
    private fun showNextStep(cardView: View) {
        cardView.visibility = View.VISIBLE
    }
} 