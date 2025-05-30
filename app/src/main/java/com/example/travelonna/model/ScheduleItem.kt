package com.example.travelonna.model

data class ScheduleItem(
    val id: Long,
    val number: Int,
    val placeName: String,
    val isLocked: Boolean = false,
    val day: Int = 1
) 