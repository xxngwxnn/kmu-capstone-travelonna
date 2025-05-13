package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.api.PlanData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PlanAdapter(private var plans: List<PlanData>) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {
    
    private var onItemClickListener: ((PlanData) -> Unit)? = null
    private var onItemDeleteListener: ((PlanData, Int) -> Unit)? = null
    
    class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val scheduleTitle: TextView = view.findViewById(R.id.scheduleTitle)
        val scheduleDDay: TextView = view.findViewById(R.id.scheduleDDay)
        val travelTypeText: TextView = view.findViewById(R.id.travelTypeText)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return PlanViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]
        holder.scheduleTitle.text = plan.title
        
        // 여행 타입 설정 (개인/그룹)
        val isGroup = plan.groupId > 0
        holder.travelTypeText.text = if (isGroup) "그룹" else "개인"
        holder.travelTypeText.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context, 
                if (isGroup) R.color.blue_primary else R.color.orange
            )
        )
        
        // D-day 계산
        val dday = calculateDday(plan.startDate)
        holder.scheduleDDay.text = dday
        
        // D-day에 따라 배경색 변경
        val daysLeft = extractDaysFromDday(dday)
        if (daysLeft in 0..7) {
            holder.scheduleDDay.setBackgroundResource(R.drawable.bg_d_day)
        } else {
            holder.scheduleDDay.setBackgroundResource(R.drawable.bg_d_day_gray)
        }
        
        // 아이템 클릭 이벤트
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(plan)
        }
    }
    
    override fun getItemCount() = plans.size
    
    fun updateData(newPlans: List<PlanData>) {
        plans = newPlans
        notifyDataSetChanged()
    }
    
    fun setOnItemClickListener(listener: (PlanData) -> Unit) {
        onItemClickListener = listener
    }
    
    fun setOnItemDeleteListener(listener: (PlanData, Int) -> Unit) {
        onItemDeleteListener = listener
    }
    
    // 아이템 삭제 메서드
    fun removeItem(position: Int) {
        if (position >= 0 && position < plans.size) {
            val plansList = plans.toMutableList()
            val removedPlan = plansList[position]
            plansList.removeAt(position)
            plans = plansList
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, plans.size - position)
        }
    }
    
    // 삭제한 스케줄 서버에 전달
    fun deleteSchedule(position: Int) {
        if (position >= 0 && position < plans.size) {
            val plan = plans[position]
            onItemDeleteListener?.invoke(plan, position)
        }
    }
    
    // D-day 계산 함수
    private fun calculateDday(startDateStr: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val startDate = LocalDate.parse(startDateStr, formatter)
        
        val daysUntil = ChronoUnit.DAYS.between(today, startDate)
        
        return when {
            daysUntil > 0 -> "D-$daysUntil"
            daysUntil == 0L -> "D-Day"
            else -> "D+${-daysUntil}"
        }
    }
    
    // D-day 문자열에서 숫자만 추출
    private fun extractDaysFromDday(dday: String): Int {
        return when {
            dday == "D-Day" -> 0
            dday.startsWith("D-") -> dday.substring(2).toIntOrNull() ?: 999
            dday.startsWith("D+") -> -1 // 이미 지난 날짜
            else -> 999
        }
    }
} 