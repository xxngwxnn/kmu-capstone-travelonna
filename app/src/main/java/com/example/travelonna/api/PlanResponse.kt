package com.example.travelonna.api

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class PlanListResponse(
    val success: Boolean,
    val message: String,
    val data: List<PlanData>
)

data class PlanData(
    val planId: Long,
    val userId: Long,
    val title: String,
    val startDate: String,
    val endDate: String,
    val location: String,
    val transportInfo: String,
    val groupId: Long,
    val isPublic: Boolean,
    val totalCost: Int,
    val memo: String,
    val createdAt: String,
    val updatedAt: String
) {
    // 디데이 계산 메서드
    fun calculateDday(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val start = LocalDate.parse(startDate, formatter)
        
        val daysUntil = ChronoUnit.DAYS.between(today, start)
        
        return when {
            daysUntil > 0 -> "D-$daysUntil"
            daysUntil == 0L -> "D-Day"
            else -> "D+${-daysUntil}"
        }
    }
}

data class PlanResponse(
    val success: Boolean,
    val message: String,
    val data: PlanData
)

data class PlanUpdateRequest(
    val title: String,
    val description: String?,
    val startDate: String,
    val endDate: String,
    val isPublic: Boolean = true
) 