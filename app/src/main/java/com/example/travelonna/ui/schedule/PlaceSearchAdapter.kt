package com.example.travelonna.ui.schedule

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
    private val onWebsiteClick: ((PlaceInfo) -> Unit)? = null  // nullable and optional
) : RecyclerView.Adapter<PlaceSearchAdapter.PlaceViewHolder>() {

    private var places = listOf<PlaceInfo>()

    fun updatePlaces(newPlaces: List<PlaceInfo>) {
        places = newPlaces
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_search, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.bind(place)
    }

    override fun getItemCount() = places.size

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlaceName: TextView = itemView.findViewById(R.id.tvPlaceName)
        private val tvPlaceAddress: TextView = itemView.findViewById(R.id.tvPlaceAddress)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        private val btnRoute: ImageButton = itemView.findViewById(R.id.btnRoute)

        fun bind(place: PlaceInfo) {
            tvPlaceName.text = place.name
            tvPlaceAddress.text = place.address
            tvRating.text = if (place.rating != null) "★ ${place.rating}" else ""

            itemView.setOnClickListener { onPlaceClick(place) }
            btnRoute.setOnClickListener { onRouteClick(place) }
        }
    }
} 