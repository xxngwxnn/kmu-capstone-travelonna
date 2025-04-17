package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class ScheduleCompleteActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_schedule_complete)
        
        val confirmButton = findViewById<ImageButton>(R.id.confirmButton)
        
        confirmButton.setOnClickListener {
            // 일정 목록 화면으로 이동 (모든 이전 액티비티 종료)
            val intent = Intent(this, PlanActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
} 