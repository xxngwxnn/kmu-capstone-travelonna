package com.example.travelonna

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager

// RecyclerView 어댑터
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class PlanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan)

        val planList = listOf(
            PlanItem("서울 나들이", "2025.09.09 ~ 2025.09.11", "그룹", "D-7", true),
            PlanItem("사나이들의 여행", "2025.09.09 ~ 2025.09.11", "그룹", "D-14", true),
            PlanItem("나홀로 부산 여행", "2025.09.09 ~ 2025.09.11", "개인", "D-21", false),
            PlanItem("힐링 경주 여행", "2025.09.09 ~ 2025.09.11", "그룹", "D-28", true)
        )

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PlanAdapter(planList)
    }
}

// 데이터 모델
data class PlanItem(
    val title: String,
    val dateRange: String,
    val type: String,
    val dDay: String,
    val isPublic: Boolean
)

class PlanAdapter(private val planList: List<PlanItem>) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {
    class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.planTitle)
        val dateRange: TextView = view.findViewById(R.id.planDateRange)
        val type: TextView = view.findViewById(R.id.planType)
        val dDay: TextView = view.findViewById(R.id.planDDay)
        val isPublic: TextView = view.findViewById(R.id.planPrivacy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = planList[position]
        holder.title.text = plan.title
        holder.dateRange.text = plan.dateRange
        holder.type.text = plan.type
        holder.dDay.text = plan.dDay
        holder.isPublic.text = if (plan.isPublic) "공개" else "비공개"
    }

    override fun getItemCount() = planList.size
}
