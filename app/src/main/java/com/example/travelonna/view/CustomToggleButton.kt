package com.example.travelonna.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.travelonna.R

class CustomToggleButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val track: View
    private val thumb: View
    private var isChecked = false
    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null
    private var pendingCheck: Boolean? = null
    
    // 색상 정의
    private val primaryColor = Color.parseColor("#5E7BF9")
    private val uncheckedTrackColor = Color.parseColor("#F2F2F2")
    private val uncheckedBorderColor = Color.parseColor("#DDDDDD")
    
    init {
        LayoutInflater.from(context).inflate(R.layout.custom_toggle_button, this, true)
        track = findViewById(R.id.toggleTrack)
        thumb = findViewById(R.id.toggleThumb)
        
        // 레이아웃이 준비되면 보류 중인 체크 상태를 적용
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                pendingCheck?.let {
                    isChecked = it
                    updateThumbPositionImmediate(it)
                    pendingCheck = null
                }
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        
        setOnClickListener {
            toggle()
        }
    }
    
    fun toggle() {
        isChecked = !isChecked
        updateThumbPosition(isChecked)
        onCheckedChangeListener?.invoke(isChecked)
    }
    
    private fun updateThumbPosition(checked: Boolean) {
        if (width == 0) {
            // 아직 레이아웃이 준비되지 않았으면 보류
            pendingCheck = checked
            return
        }
        
        val translateX = if (checked) (track.width - thumb.width) else 0
        thumb.animate()
            .translationX(translateX.toFloat())
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        updateBackgroundColors(checked)
    }
    
    // 애니메이션 없이 즉시 위치 변경
    private fun updateThumbPositionImmediate(checked: Boolean) {
        val translateX = if (checked) (track.width - thumb.width) else 0
        thumb.translationX = translateX.toFloat()
        updateBackgroundColors(checked)
    }
    
    // 배경색 직접 업데이트
    private fun updateBackgroundColors(checked: Boolean) {
        if (checked) {
            // 켜진 상태 - 파란색 배경
            val trackDrawable = GradientDrawable().apply {
                setColor(primaryColor)
                cornerRadius = 15f * resources.displayMetrics.density
            }
            track.background = trackDrawable
            
            // 흰색 동그라미 + 파란색 테두리
            val thumbDrawable = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 14f * resources.displayMetrics.density
                setStroke(1, primaryColor)
            }
            thumb.background = thumbDrawable
        } else {
            // 꺼진 상태 - 회색 배경
            val trackDrawable = GradientDrawable().apply {
                setColor(uncheckedTrackColor)
                cornerRadius = 15f * resources.displayMetrics.density
                setStroke(1, uncheckedBorderColor)
            }
            track.background = trackDrawable
            
            // 흰색 동그라미 + 회색 테두리
            val thumbDrawable = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 14f * resources.displayMetrics.density
                setStroke(1, uncheckedBorderColor)
            }
            thumb.background = thumbDrawable
        }
    }
    
    fun setChecked(checked: Boolean) {
        if (isChecked != checked) {
            isChecked = checked
            updateThumbPosition(checked)
        }
    }
    
    fun isChecked() = isChecked
    
    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        onCheckedChangeListener = listener
    }
} 