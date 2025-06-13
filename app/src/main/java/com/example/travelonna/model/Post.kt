package com.example.travelonna.model

data class Post(
    val id: Long,
    val imageResource: Int,
    val imageUrl: String? = null,
    val hasImage: Boolean = true,
    val userName: String,
    val userId: Int = 0,
    var isFollowing: Boolean,
    val description: String,
    val date: String,
    var isLiked: Boolean = false,
    var likeCount: Int = 0,
    var commentCount: Int = 0,
    val planId: Int = 0
) 