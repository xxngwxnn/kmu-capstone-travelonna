package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // Get the image view and start button
        val startDisplayImage = findViewById<ImageView>(R.id.startDisplayImage)
        val startButton = findViewById<ImageButton>(R.id.startButton)
        
        // Hide both initially
        startDisplayImage.alpha = 0f
        startButton.visibility = View.INVISIBLE

        // Create fade-in animation for background image
        startDisplayImage.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()

        // Show the button after 1.5 seconds with fade-in animation
        Handler(Looper.getMainLooper()).postDelayed({
            // Create fade-in animation for button
            val fadeIn = AlphaAnimation(0.0f, 1.0f)
            fadeIn.duration = 500 // 0.5 seconds duration
            fadeIn.fillAfter = true
            
            // Make button visible and start animation
            startButton.visibility = View.VISIBLE
            startButton.startAnimation(fadeIn)
        }, 1500) // 1.5 seconds delay

        // Set click listener for the start button
        startButton.setOnClickListener {
            // Navigate to MainActivity (navigator screen) instead of LoginActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close this activity so user can't go back to splash screen
        }
    }
} 