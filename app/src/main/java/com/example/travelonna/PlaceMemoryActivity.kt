package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PlaceMemoryActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var placeNameTextView: TextView
    private lateinit var placeAddressTextView: TextView
    private lateinit var memoryEditText: EditText
    private lateinit var counterTextView: TextView
    private lateinit var uploadButton: Button
    
    private val MAX_SYMBOLS = 350

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_memory)

        // UI 요소 초기화
        backButton = findViewById(R.id.backButton)
        placeNameTextView = findViewById(R.id.placeNameTextView)
        placeAddressTextView = findViewById(R.id.placeAddressTextView)
        memoryEditText = findViewById(R.id.memoryEditText)
        counterTextView = findViewById(R.id.counterText)
        uploadButton = findViewById(R.id.uploadButton)

        // Intent에서 데이터 가져오기
        val placeName = intent.getStringExtra("PLACE_NAME") ?: "동대구역"
        val placeAddress = intent.getStringExtra("PLACE_ADDRESS") ?: "대구광역시 동구 동대구로 550 (신암동 294)"

        // 장소 정보 표시
        placeNameTextView.text = placeName
        placeAddressTextView.text = placeAddress

        // 텍스트 입력 감지 및 카운터 업데이트
        memoryEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                counterTextView.text = "$currentLength/$MAX_SYMBOLS symbols"
                
                // 최대 글자수 제한
                if (currentLength > MAX_SYMBOLS) {
                    memoryEditText.setText(s?.subSequence(0, MAX_SYMBOLS))
                    memoryEditText.setSelection(MAX_SYMBOLS)
                }
            }
        })

        // 뒤로가기 버튼 설정
        backButton.setOnClickListener {
            finish()
        }

        // 업로드 버튼 설정
        uploadButton.setOnClickListener {
            val memoryText = memoryEditText.text.toString()
            if (memoryText.isNotEmpty()) {
                // 업로드 로직 구현 (나중에 서버와 연동)
                Toast.makeText(this, "여행 기록이 저장되었습니다", Toast.LENGTH_SHORT).show()
                
                // 업로드 완료 화면으로 이동
                val intent = Intent(this, UploadCompleteActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "여행 기록을 작성해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 