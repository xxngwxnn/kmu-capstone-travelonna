package com.example.travelonna

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.widget.HorizontalScrollView
import android.content.Intent
import android.widget.Toast
import com.example.travelonna.ui.schedule.AddPlaceActivity
import com.example.travelonna.ui.schedule.PlaceInfoActivity
import androidx.recyclerview.widget.ItemTouchHelper

class ScheduleDetailActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var addNewPlaceButton: TextView
    private lateinit var confirmButton: TextView
    
    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance()
    private var dayCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_detail)
        
        // 뷰 초기화
        viewPager = findViewById(R.id.viewPager)
        addNewPlaceButton = findViewById(R.id.addNewPlaceButton)
        confirmButton = findViewById(R.id.confirmButton)
        
        // 인텐트에서 날짜 데이터 가져오기
        val startDate = intent.getLongExtra("START_DATE", System.currentTimeMillis())
        val endDate = intent.getLongExtra("END_DATE", System.currentTimeMillis())
        val scheduleName = intent.getStringExtra("SCHEDULE_NAME") ?: "일정"
        
        startDateCalendar.timeInMillis = startDate
        endDateCalendar.timeInMillis = endDate
        
        // 일정 이름과 날짜 표시
        val titleText = findViewById<TextView>(R.id.titleText)
        val dateRangeText = findViewById<TextView>(R.id.dateRangeText)
        
        titleText.text = scheduleName
        
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        val startDateStr = dateFormat.format(startDateCalendar.time)
        val endDateStr = dateFormat.format(endDateCalendar.time)
        dateRangeText.text = "$startDateStr ~ $endDateStr"
        
        // 뒤로가기 버튼 설정
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
        
        // 총 일수 계산
        val diffInMillis = endDateCalendar.timeInMillis - startDateCalendar.timeInMillis
        dayCount = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt() + 1
        
        // ViewPager 설정
        viewPager.adapter = DayPagerAdapter(this, dayCount, startDateCalendar)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabSelection(position)
            }
        })
        
        // 커스텀 탭 설정
        setupCustomTabs()
        
        // 장소 추가 버튼 리스너
        addNewPlaceButton.setOnClickListener {
            try {
                val intent = Intent(this@ScheduleDetailActivity, PlaceInfoActivity::class.java)
                intent.putExtra("SELECTED_DAY", viewPager.currentItem)
                intent.putExtra("START_DATE", startDateCalendar.timeInMillis)
                intent.putExtra("END_DATE", endDateCalendar.timeInMillis)
                intent.putExtra("SCHEDULE_NAME", scheduleName)
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                Toast.makeText(this, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
        
        // 완료 버튼 리스너
        confirmButton.setOnClickListener {
            finish() // 현재 화면 종료하고 이전 화면으로 돌아가기
        }
    }
    
    // 결과 처리를 위한 onActivityResult 추가
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // 선택된 장소 정보 처리
            val placeName = data?.getStringExtra("placeName")
            val placeAddress = data?.getStringExtra("placeAddress")
            val placeLat = data?.getDoubleExtra("placeLat", 0.0)
            val placeLng = data?.getDoubleExtra("placeLng", 0.0)
            
            // TODO: 선택된 장소 정보를 화면에 추가하는 로직 구현
            Toast.makeText(this, "선택된 장소: $placeName", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 드래그 앤 드롭 헬퍼 설정
    private fun setupItemTouchHelper(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // 드래그 방향
            0  // 스와이프 사용하지 않음
        ) {
            private var dragFrom = -1
            private var dragTo = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val adapter = recyclerView.adapter as PlaceAdapter
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                if (dragFrom == -1) {
                    dragFrom = fromPosition
                }
                dragTo = toPosition

                adapter.moveItem(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 스와이프 기능 사용하지 않음
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true  // 롱 프레스로 드래그 활성화
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                
                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    // 드래그가 끝났을 때 전체 리스트 갱신
                    recyclerView.adapter?.notifyDataSetChanged()
                }
                
                // 드래그 위치 초기화
                dragFrom = -1
                dragTo = -1
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
    
    // 장소 목록 어댑터
    inner class PlaceAdapter(private val places: MutableList<PlaceItem>) : 
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

        fun moveItem(fromPosition: Int, toPosition: Int) {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    places.swap(i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    places.swap(i, i - 1)
                }
            }
            notifyItemMoved(fromPosition, toPosition)
        }

        private fun MutableList<PlaceItem>.swap(index1: Int, index2: Int) {
            val tmp = this[index1]
            this[index1] = this[index2]
            this[index2] = tmp
        }
        
        override fun getItemCount() = places.size
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
            val adapter = PlaceAdapter(generateDummyData().toMutableList())
            holder.recyclerView.adapter = adapter
            setupItemTouchHelper(holder.recyclerView)
        }
        
        override fun getItemCount(): Int = dayCount
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
    
    // 커스텀 탭 설정
    private fun setupCustomTabs() {
        val tabContainer = findViewById<LinearLayout>(R.id.tabContainer)
        tabContainer.removeAllViews()
        
        for (i in 0 until dayCount) {
            val tabView = LayoutInflater.from(this).inflate(R.layout.tab_day_item, tabContainer, false)
            val dayNumber = tabView.findViewById<TextView>(R.id.dayNumber)
            val dayDate = tabView.findViewById<TextView>(R.id.dayDate)
            val selectionIndicator = tabView.findViewById<View>(R.id.selectionIndicator)
            
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDateCalendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, i)
            
            // 요일과 일자 형식으로 변경
            val dayOfWeekFormat = SimpleDateFormat("E", Locale.KOREA) // 요일
            val dayOfMonthFormat = SimpleDateFormat("dd", Locale.KOREA) // 일
            val dayOfWeek = dayOfWeekFormat.format(calendar.time)
            val dayOfMonth = dayOfMonthFormat.format(calendar.time)
            
            // Day N 형식
            val dayNum = i + 1
            val dayNumStr = String.format("%02d", dayNum) // 01, 02, ... 형식으로
            dayNumber.text = "DAY $dayNumStr"
            
            // 요일.일 형식
            dayDate.text = "$dayOfWeek.$dayOfMonth"
            
            // 첫 번째 탭을 선택된 상태로 표시
            if (i == 0) {
                dayNumber.setTextColor(ContextCompat.getColor(this, R.color.black))
                selectionIndicator.visibility = View.VISIBLE
            } else {
                selectionIndicator.visibility = View.INVISIBLE
            }
            
            tabView.tag = i
            tabView.setOnClickListener { v ->
                // 모든 탭 초기화
                for (j in 0 until tabContainer.childCount) {
                    val tab = tabContainer.getChildAt(j)
                    tab.findViewById<TextView>(R.id.dayNumber).setTextColor(ContextCompat.getColor(this, R.color.black))
                    tab.findViewById<View>(R.id.selectionIndicator).visibility = View.INVISIBLE
                }
                
                // 선택된 탭 하이라이트
                v.findViewById<TextView>(R.id.dayNumber).setTextColor(ContextCompat.getColor(this, R.color.black))
                v.findViewById<View>(R.id.selectionIndicator).visibility = View.VISIBLE
                
                // ViewPager 페이지 변경
                viewPager.currentItem = v.tag as Int
            }
            
            tabContainer.addView(tabView)
        }
    }
    
    // 페이지 변경 시 탭 선택 상태 업데이트
    private fun updateTabSelection(position: Int) {
        val tabContainer = findViewById<LinearLayout>(R.id.tabContainer)
        
        // 모든 탭 초기화
        for (i in 0 until tabContainer.childCount) {
            val tab = tabContainer.getChildAt(i)
            tab.findViewById<TextView>(R.id.dayNumber).setTextColor(ContextCompat.getColor(this, R.color.black))
            tab.findViewById<View>(R.id.selectionIndicator).visibility = View.INVISIBLE
        }
        
        // 선택된 탭 하이라이트
        val selectedTab = tabContainer.getChildAt(position)
        selectedTab.findViewById<TextView>(R.id.dayNumber).setTextColor(ContextCompat.getColor(this, R.color.black))
        selectedTab.findViewById<View>(R.id.selectionIndicator).visibility = View.VISIBLE
        
        // 선택된 탭이 보이도록 스크롤
        val scrollView = findViewById<HorizontalScrollView>(R.id.tabScrollView)
        scrollView.smoothScrollTo(selectedTab.left - 50, 0)
    }
}