package com.example.travelonna.api

data class SearchResponse(
    val success: Boolean,
    val message: String,
    val data: SearchData
)

data class SearchData(
    val users: List<SearchUser>,
    val logs: List<SearchLog>,
    val places: List<SearchPlace>
)

data class SearchUser(
    val userId: Int,
    val nickname: String,
    val introduction: String
)

data class SearchLog(
    val logId: Int,
    val userName: String,
    val comment: String,
    val createdAt: String,
    val likeCount: Int
)

data class SearchPlace(
    val placeId: Int,
    val name: String,
    val address: String
) 