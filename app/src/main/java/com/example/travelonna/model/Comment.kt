package com.example.travelonna.model

data class Comment(
    val id: Long,
    val postId: Long,
    val userName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userProfileImage: String? = null
) 