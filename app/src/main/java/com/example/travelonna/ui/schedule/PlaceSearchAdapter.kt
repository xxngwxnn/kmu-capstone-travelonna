package com.example.travelonna.ui.schedule

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R

class PlaceSearchAdapter(
    private val onPlaceClick: (PlaceInfo) -> Unit,
    private val onRouteClick: (PlaceInfo) -> Unit,
    private val onWebsiteClick: ((PlaceInfo) -> Unit)?
) : RecyclerView.Adapter<PlaceSearchAdapter.PlaceViewHolder>() {

    private var places: List<PlaceInfo> = emptyList()

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.placeName)
        val placeAddress: TextView = itemView.findViewById(R.id.placeAddress)
        val directionButton: ImageButton = itemView.findViewById(R.id.directionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_search_result, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.placeName.text = place.name
        holder.placeAddress.text = place.address

        // 전체 아이템 클릭 시 장소 상세 정보 표시
        holder.itemView.setOnClickListener {
            onPlaceClick(place)
        }

        // 길 찾기 버튼 클릭 시
        holder.directionButton.setOnClickListener {
            onRouteClick(place)
        }
    }

    override fun getItemCount() = places.size

    fun updatePlaces(newPlaces: List<PlaceInfo>) {
        places = newPlaces
        notifyDataSetChanged()
    }
} 