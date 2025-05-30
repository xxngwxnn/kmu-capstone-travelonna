package com.example.travelonna.model

data class Place(
    val id: Long,
    val name: String,
    val imageResource: Int,
    val address: String? = null,
    val rating: Float? = null,
    val distance: String? = null
) 