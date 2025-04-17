package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R

// 추천 장소 데이터 모델
data class PlaceRecommendItem(
    val imageResId: Int,
    val name: String
)

// 추천 장소 어댑터
class PlaceRecommendAdapter(private val placeList: List<PlaceRecommendItem>) : 
    RecyclerView.Adapter<PlaceRecommendAdapter.PlaceViewHolder>() {
    
    class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val placeImage: ImageView = view.findViewById(R.id.placeImage)
        val placeName: TextView = view.findViewById(R.id.placeName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place_recommend, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = placeList[position]
        holder.placeImage.setImageResource(place.imageResId)
        holder.placeName.text = place.name
    }

    override fun getItemCount() = placeList.size
} 