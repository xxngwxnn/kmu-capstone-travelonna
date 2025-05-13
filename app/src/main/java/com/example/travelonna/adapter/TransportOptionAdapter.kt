package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.api.TransportOption
import java.text.NumberFormat
import java.util.Calendar
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
        
        // 교통수단 유형 한글로 변환
        val typeKorean = when (option.type.lowercase()) {
            "car" -> "자가용"
            "train" -> "기차"
            "bus" -> "버스"
            "airplane" -> "비행기"
            else -> option.type
        }
        holder.optionTypeText.text = typeKorean
        
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
        
        // 경로 정보 - 요일 제한 확인 및 처리
        val routeInfo = option.routeInfo
        
        // 가격이 0원인데 요일 제한이 있는 경우 (ex: [금토일], [매일] 등)
        if (option.price == 0 && (routeInfo.contains("[금토일]") || routeInfo.contains("[매일]"))) {
            // 현재 요일 확인
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            
            // 주말인지 여부 확인 (금=6, 토=7, 일=1)
            val isWeekend = (currentDay == Calendar.FRIDAY || 
                             currentDay == Calendar.SATURDAY || 
                             currentDay == Calendar.SUNDAY)
            
            // 주말이 아닌데 [금토일] 표시가 있는 경우 아이템 숨기기
            if (routeInfo.contains("[금토일]") && !isWeekend) {
                // 아이템 전체를 숨기는 대신, 해당 요일에 운행하지 않음을 표시
                holder.itemView.alpha = 0.5f  // 흐리게 표시
                holder.routeInfoText.text = "$routeInfo (현재 운행일이 아님)"
            } else {
                holder.itemView.alpha = 1.0f
                holder.routeInfoText.text = routeInfo
            }
        } else {
            holder.routeInfoText.text = routeInfo
        }
    }

    override fun getItemCount(): Int = options.size
} 