package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.api.TransportOption
import java.text.NumberFormat
import java.util.Locale

class TransportOptionAdapter(
    private val options: List<TransportOption>
) : RecyclerView.Adapter<TransportOptionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val optionTypeText: TextView = itemView.findViewById(R.id.optionTypeText)
        val priceText: TextView = itemView.findViewById(R.id.priceText)
        val departureTimeText: TextView = itemView.findViewById(R.id.departureTimeText)
        val arrivalTimeText: TextView = itemView.findViewById(R.id.arrivalTimeText)
        val totalTimeText: TextView = itemView.findViewById(R.id.totalTimeText)
        val routeInfoText: TextView = itemView.findViewById(R.id.routeInfoText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transport_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        
        holder.optionTypeText.text = option.type
        
        // 가격 표시 (통화 형식)
        val priceFormat = NumberFormat.getCurrencyInstance(Locale.KOREA)
        holder.priceText.text = priceFormat.format(option.price)
        
        // 출발 시간 및 도착 시간
        holder.departureTimeText.text = option.departureTime
        holder.arrivalTimeText.text = option.arrivalTime
        
        // 총 소요 시간
        val hours = option.totalTime / 60
        val minutes = option.totalTime % 60
        
        val timeText = if (hours > 0) {
            "${hours}시간"
        } else {
            ""
        } + if (minutes > 0) {
            " ${minutes}분"
        } else {
            ""
        }
        
        holder.totalTimeText.text = timeText.trim()
        
        // 경로 정보
        holder.routeInfoText.text = option.routeInfo
    }

    override fun getItemCount(): Int = options.size
} 