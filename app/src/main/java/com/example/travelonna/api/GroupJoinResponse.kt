package com.example.travelonna.api

import com.google.gson.annotations.SerializedName

data class GroupJoinResponse(
    val success: Boolean,
    val message: String,
    val data: GroupJoinData?
)

data class GroupJoinData(
    val planId: Int,
    val groupId: Int,
    val url: String
) 