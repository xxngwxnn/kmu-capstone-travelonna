package com.example.travelonna.model

data class Post(
    val id: Long,
    val imageResource: Int,
    val userName: String,
    var isFollowing: Boolean,
    val description: String,
    val date: String,
    var isLiked: Boolean = false,
    var likeCount: Int = 0,
    var commentCount: Int = 0
) 