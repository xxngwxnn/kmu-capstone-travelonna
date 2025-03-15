package com.example.travelonna

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScheduleDetailActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var addPlaceButton: TextView
    
    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance()
    private var dayCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_detail)
        
        // 뷰 초기화
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        addPlaceButton = findViewById(R.id.addPlaceButton)
        
        // 인텐트에서 날짜 데이터 가져오기
        val startDate = intent.getLongExtra("START_DATE", System.currentTimeMillis())
        val endDate = intent.getLongExtra("END_DATE", System.currentTimeMillis())
        
        startDateCalendar.timeInMillis = startDate
        endDateCalendar.timeInMillis = endDate
        
        // 총 일수 계산
        val diffInMillis = endDateCalendar.timeInMillis - startDateCalendar.timeInMillis
        dayCount = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt() + 1
        
        // ViewPager 설정
        viewPager.adapter = DayPagerAdapter(this, dayCount, startDateCalendar)
        
        // TabLayout과 ViewPager 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDateCalendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, position)
            
            val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
            
            tab.text = "Day ${position + 1}\n${dayFormat.format(calendar.time)}"
            tab.setCustomView(R.layout.tab_day_item)
            val customView = tab.customView
            customView?.findViewById<TextView>(R.id.dayNumber)?.text = "Day ${position + 1}"
            customView?.findViewById<TextView>(R.id.dayDate)?.text = "${dayFormat.format(calendar.time)} ${monthFormat.format(calendar.time)} ${calendar.get(Calendar.YEAR)}"
        }.attach()
        
        // 장소 추가 버튼 리스너
        addPlaceButton.setOnClickListener {
            // 장소 추가 기능 구현 (향후)
        }
    }
    
    // Day별 페이지 어댑터
    inner class DayPagerAdapter(private val activity: AppCompatActivity, 
                               private val dayCount: Int, 
                               private val startDate: Calendar) : 
        RecyclerView.Adapter<DayPagerAdapter.DayPageHolder>() {
        
        inner class DayPageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val recyclerView: RecyclerView = itemView.findViewById(R.id.dayRecyclerView)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayPageHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.day_page_layout, parent, false)
            return DayPageHolder(view)
        }
        
        override fun onBindViewHolder(holder: DayPageHolder, position: Int) {
            // 날짜별 데이터 설정
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDate.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, position)
            
            // 더미 데이터로 RecyclerView 설정
            holder.recyclerView.layoutManager = LinearLayoutManager(activity)
            holder.recyclerView.adapter = PlaceAdapter(generateDummyData())
        }
        
        override fun getItemCount(): Int = dayCount
    }
    
    // 장소 목록 어댑터
    inner class PlaceAdapter(private val places: List<PlaceItem>) : 
        RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {
        
        inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val numberCircle: TextView = itemView.findViewById(R.id.numberCircle)
            val placeImage: ImageView = itemView.findViewById(R.id.placeImage)
            val placeName: TextView = itemView.findViewById(R.id.placeName)
            val placeAddress: TextView = itemView.findViewById(R.id.placeAddress)
            val editButton: TextView = itemView.findViewById(R.id.editButton)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_place, parent, false)
            return PlaceViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
            val place = places[position]
            holder.numberCircle.text = (position + 1).toString()
            holder.placeImage.setImageResource(place.imageResId)
            holder.placeName.text = place.name
            holder.placeAddress.text = place.address
            
            holder.editButton.setOnClickListener {
                // 편집 기능 구현 (향후)
            }
        }
        
        override fun getItemCount(): Int = places.size
    }
    
    // 장소 아이템 데이터 클래스
    data class PlaceItem(val name: String, val address: String, val imageResId: Int)
    
    // 더미 데이터 생성
    private fun generateDummyData(): List<PlaceItem> {
        return listOf(
            PlaceItem("동대구역", "대구광역시 동구 동대구로 550 (신암동)", R.drawable.dummy_place_1),
            PlaceItem("향기마싯당", "대구광역시 동구 카페로드 696-8", R.drawable.dummy_place_2),
            PlaceItem("이월드", "대구광역시 달서구 두류공원로 200", R.drawable.dummy_place_3),
            PlaceItem("팔공힐링", "대구광역시 북구 칠곡중앙대로 597", R.drawable.dummy_place_4),
            PlaceItem("스파크랜드", "대구광역시 달서구 두류공원로 200", R.drawable.dummy_place_3)
        )
    }
} 