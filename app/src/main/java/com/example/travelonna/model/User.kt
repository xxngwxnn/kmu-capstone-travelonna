package com.example.travelonna.model

data class User(
    val id: String,
    val username: String,
    val profileImageUrl: String? = null,
    var isFollowing: Boolean = false
) 