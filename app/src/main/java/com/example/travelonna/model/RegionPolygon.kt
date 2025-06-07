package com.example.travelonna.model

import com.google.android.gms.maps.model.LatLng

data class RegionPolygon(
    val name: String,
    val coordinates: List<LatLng>,
    var isVisited: Boolean = false,
    var visitCount: Int = 0
) 