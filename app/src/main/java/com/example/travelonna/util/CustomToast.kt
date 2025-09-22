package com.example.travelonna.util

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.FrameLayout
import android.os.Handler
import android.os.Looper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.example.travelonna.R

object CustomToast {
    
    enum class Type {
        SUCCESS, ERROR, WARNING, INFO
    }
    
    fun show(context: Context, message: String, type: Type = Type.INFO, duration: Int = Toast.LENGTH_SHORT) {
        // Activity인 경우 커스텀 오버레이 사용, 아니면 기본 Toast 사용
        if (context is Activity) {
            showCustomOverlay(context, message, type, duration)
        } else {
            showFallbackToast(context, message, type, duration)
        }
    }
    
    private fun showCustomOverlay(activity: Activity, message: String, type: Type, duration: Int) {
        try {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            val inflater = LayoutInflater.from(activity)
            val customView = inflater.inflate(R.layout.custom_snackbar, null)
            
            val icon = customView.findViewById<ImageView>(R.id.snackbar_icon)
            val messageText = customView.findViewById<TextView>(R.id.snackbar_message)
            
            messageText.text = message
            
            // 타입에 따라 배경과 아이콘 설정
            when (type) {
                Type.SUCCESS -> {
                    customView.setBackgroundResource(R.drawable.custom_toast_success)
                    icon.setImageResource(R.drawable.ic_check_circle)
                    icon.visibility = View.VISIBLE
                }
                Type.ERROR -> {
                    customView.setBackgroundResource(R.drawable.custom_toast_error)
                    icon.setImageResource(R.drawable.ic_error_circle)
                    icon.visibility = View.VISIBLE
                }
                Type.WARNING -> {
                    customView.setBackgroundResource(R.drawable.custom_toast_warning)
                    icon.setImageResource(R.drawable.ic_warning_circle)
                    icon.visibility = View.VISIBLE
                }
                Type.INFO -> {
                    customView.setBackgroundResource(R.drawable.custom_snackbar_background)
                    icon.visibility = View.GONE
                }
            }
            
            // 레이아웃 파라미터 설정
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                setMargins(32, 0, 32, 100) // 하단에서 100dp 위에 표시
            }
            
            customView.layoutParams = layoutParams
            
            // 뷰 추가
            rootView.addView(customView)
            
            // 애니메이션 효과
            customView.alpha = 0f
            customView.translationY = 100f
            customView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()
            
            // 지정된 시간 후 제거
            val delayMillis = if (duration == Toast.LENGTH_LONG) 3500L else 2000L
            Handler(Looper.getMainLooper()).postDelayed({
                customView.animate()
                    .alpha(0f)
                    .translationY(-100f)
                    .setDuration(300)
                    .withEndAction {
                        try {
                            rootView.removeView(customView)
                        } catch (e: Exception) {
                            // 뷰가 이미 제거된 경우 무시
                        }
                    }
                    .start()
            }, delayMillis)
            
        } catch (e: Exception) {
            // 오류 발생 시 기본 Toast로 폴백
            showFallbackToast(activity, message, type, duration)
        }
    }
    
    private fun showFallbackToast(context: Context, message: String, type: Type, duration: Int) {
        val toast = Toast.makeText(context, message, duration)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)
        toast.show()
    }
    
    // Snackbar를 사용하는 메서드 (CoordinatorLayout이 있는 경우)
    fun showSnackbar(view: View, message: String, type: Type = Type.INFO, duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        
        // 타입에 따라 색상 설정
        val backgroundColor = when (type) {
            Type.SUCCESS -> 0xE04CAF50.toInt()
            Type.ERROR -> 0xE0F44336.toInt()
            Type.WARNING -> 0xE0FF9800.toInt()
            Type.INFO -> 0xE0333333.toInt()
        }
        
        snackbar.view.setBackgroundColor(backgroundColor)
        snackbar.show()
    }
    
    // 편의 메서드들
    fun success(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        show(context, message, Type.SUCCESS, duration)
    }
    
    fun error(context: Context, message: String, duration: Int = Toast.LENGTH_LONG) {
        show(context, message, Type.ERROR, duration)
    }
    
    fun warning(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        show(context, message, Type.WARNING, duration)
    }
    
    fun info(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        show(context, message, Type.INFO, duration)
    }
} 