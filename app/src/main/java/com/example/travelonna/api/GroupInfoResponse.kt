package com.example.travelonna.api

import com.google.gson.annotations.SerializedName

data class GroupInfoResponse(
    val id: Long,
    val url: String,
    val isGroup: Boolean,
    val createdDate: String,
    val hostId: Long
) 