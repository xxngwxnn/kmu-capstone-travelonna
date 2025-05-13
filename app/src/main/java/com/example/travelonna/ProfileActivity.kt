package com.example.travelonna

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var backButton: ImageButton
    private lateinit var notificationButton: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var postsCount: TextView
    private lateinit var distanceCount: TextView
    private lateinit var placesCount: TextView
    private lateinit var bioText: TextView
    private lateinit var airplaneProgress: ProgressBar
    private lateinit var profileEditButton: Button
    private lateinit var postsTab: TextView
    private lateinit var mapTab: TextView
    private lateinit var divider: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize views
        initViews()
        // Setup listeners
        setupListeners()
        // Load dummy profile data
        loadProfileData()
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.back_button)
        notificationButton = findViewById(R.id.notification_button)
        profileImage = findViewById(R.id.profile_image)
        postsCount = findViewById(R.id.posts_count)
        distanceCount = findViewById(R.id.distance_count)
        placesCount = findViewById(R.id.places_count)
        bioText = findViewById(R.id.bio_text)
        airplaneProgress = findViewById(R.id.airplane_progress)
        profileEditButton = findViewById(R.id.profile_edit_button)
        postsTab = findViewById(R.id.posts_tab)
        mapTab = findViewById(R.id.map_tab)
        divider = findViewById(R.id.divider)
    }
    
    private fun setupListeners() {
        // Back button - finish activity
        backButton.setOnClickListener {
            finish()
        }
        
        // Notification button
        notificationButton.setOnClickListener {
            // TODO: Implement notification screen navigation
        }
        
        // Profile edit button
        profileEditButton.setOnClickListener {
            // TODO: Navigate to profile edit screen
        }
        
        // Tab switching
        postsTab.setOnClickListener {
            switchToPostsTab()
        }
        
        mapTab.setOnClickListener {
            switchToMapTab()
        }
    }
    
    private fun loadProfileData() {
        // Load dummy profile stats
        postsCount.text = "25"
        distanceCount.text = "220M"
        placesCount.text = "77"
        
        // Set dummy profile image
        profileImage.setImageResource(R.drawable.ic_launcher_background)
        
        // Set dummy bio
        bioText.text = "여행을 하기 위해 살아가는 사나이"
        
        // Set progress bar value (75%)
        airplaneProgress.progress = 75
    }
    
    private fun switchToPostsTab() {
        postsTab.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        postsTab.setTypeface(null, android.graphics.Typeface.BOLD)
        
        mapTab.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        mapTab.setTypeface(null, android.graphics.Typeface.NORMAL)
        
        // TODO: Show posts content, hide map content
    }
    
    private fun switchToMapTab() {
        mapTab.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        mapTab.setTypeface(null, android.graphics.Typeface.BOLD)
        
        postsTab.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        postsTab.setTypeface(null, android.graphics.Typeface.NORMAL)
        
        // TODO: Show map content, hide posts content
    }
} 