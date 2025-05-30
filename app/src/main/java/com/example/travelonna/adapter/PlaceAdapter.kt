package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.model.Place

class PlaceAdapter(private var places: List<Place>) : 
    RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.placeImageView)
        val nameTextView: TextView = itemView.findViewById(R.id.placeNameTextView)
        val addressTextView: TextView = itemView.findViewById(R.id.placeAddressTextView)
        val ratingTextView: TextView = itemView.findViewById(R.id.placeRatingTextView)
        val distanceTextView: TextView = itemView.findViewById(R.id.placeDistanceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_search, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.imageView.setImageResource(place.imageResource)
        holder.nameTextView.text = place.name
        
        // 주소가 있는 경우 표시
        holder.addressTextView.text = place.address ?: ""
        holder.addressTextView.visibility = if (place.address.isNullOrEmpty()) View.GONE else View.VISIBLE
        
        // 평점이 있는 경우 표시
        holder.ratingTextView.text = place.rating?.toString() ?: ""
        holder.ratingTextView.visibility = if (place.rating == null) View.GONE else View.VISIBLE
        
        // 거리가 있는 경우 표시
        holder.distanceTextView.text = place.distance
        holder.distanceTextView.visibility = if (place.distance.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = places.size

    fun updateData(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }
} 