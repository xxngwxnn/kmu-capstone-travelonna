package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.travelonna.api.ProfileCreateRequest
import com.example.travelonna.api.ProfileResponse
import com.example.travelonna.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileCreateActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_create)
        
        // This is a placeholder activity for now
        // Later you can implement the actual profile creation form
        
        // Get the back button if exists
        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            finish()
        }
        
        // Get the confirm button if exists
        findViewById<Button>(R.id.confirmButton)?.setOnClickListener {
            // Todo: Implement profile creation logic
            // This is just a placeholder that navigates to PlanActivity
            
            val intent = Intent(this, PlanActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
} 