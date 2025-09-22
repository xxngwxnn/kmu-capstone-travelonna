package com.example.travelonna

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.widget.Toast
import android.widget.ToggleButton
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.travelonna.model.TravelPlace
import com.example.travelonna.api.RetrofitClient
import com.example.travelonna.api.PlanDetailResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TravelDetailActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var typeTextView: TextView
    private lateinit var backButton: ImageView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    // 날짜 관련 변수
    private var startDateCalendar: Calendar = Calendar.getInstance()
    private var endDateCalendar: Calendar = Calendar.getInstance()
    private var dayCount: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel_detail)

        // 하단 네비게이션 바 설정 (기본적으로 아무것도 선택되지 않음)
        setupBottomNavBar(-1)

        // UI 요소 초기화
        titleTextView = findViewById(R.id.detailTitleTextView)
        dateTextView = findViewById(R.id.detailDateTextView)
        typeTextView = findViewById(R.id.detailTypeTextView)
        backButton = findViewById(R.id.backButton)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Intent에서 planId 가져오기
        val planId = intent.getIntExtra("PLAN_ID", -1)
        if (planId == -1) {
            Toast.makeText(this, "여행 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // API를 통해 여행 상세 정보 가져오기
        fetchTravelDetail(planId)

        // 뒤로가기 버튼 설정
        backButton.setOnClickListener {
            finish()
        }
    }

    // 하단 네비게이션 바 클릭 및 선택 상태 관리 함수 추가
    private fun setupBottomNavBar(selectedId: Int) {
        val navHome = findViewById<ImageButton>(R.id.navHome)
        val navMap = findViewById<ImageButton>(R.id.navMap)
        val navPlan = findViewById<ImageButton>(R.id.navPlan)
        val navSearch = findViewById<ImageButton>(R.id.navSearch)
        val navProfile = findViewById<ImageButton>(R.id.navProfile)

        val navButtons = listOf(navHome, navMap, navPlan, navSearch, navProfile)
        navButtons.forEach { it.isSelected = false }
        if (selectedId != -1) {
            findViewById<ImageButton>(selectedId).isSelected = true
        }

        navHome.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, HomeActivity::class.java))
        }
        navMap.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, MyMapActivity::class.java))
        }
        navPlan.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, PlanActivity::class.java))
        }
        navSearch.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, SearchActivity::class.java))
        }
        navProfile.setOnClickListener {
            if (!it.isSelected) startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun fetchTravelDetail(planId: Int) {
        RetrofitClient.apiService.getPlanDetail(planId).enqueue(object : Callback<PlanDetailResponse> {
            override fun onResponse(call: Call<PlanDetailResponse>, response: Response<PlanDetailResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val planDetail = response.body()?.data
                    if (planDetail != null) {
                        // 기본 정보 표시
                        titleTextView.text = planDetail.title
                        dateTextView.text = "${planDetail.startDate} - ${planDetail.endDate}"
                        typeTextView.text = planDetail.location

                        // 날짜 설정
                        setupDates("${planDetail.startDate} - ${planDetail.endDate}")

                        // 장소 목록 변환
                        Log.d("TravelDetail", "places size: ${planDetail.places.size}")
                        planDetail.places.forEach { Log.d("TravelDetail", "place: ${it.name}, ${it.address}, ${it.visitDate}, ${it.day}") }
                        val places = planDetail.places.map { place ->
                            TravelPlace(
                                id = place.id,
                                name = place.name,
                                address = place.address,
                                visitDate = place.visitDate,
                                dayVisit = place.day
                            )
                        }

                        // ViewPager 및 탭 설정
                        setupViewPagerAndTabs(places)
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> {
                            val errorBody = response.errorBody()?.string()
                            if (errorBody?.contains("권한이 없습니다") == true) {
                                "이 여행 일정에 대한 접근 권한이 없습니다."
                            } else {
                                "해당 여행 일정을 찾을 수 없습니다."
                            }
                        }
                        401 -> "로그인이 필요합니다."
                        403 -> "접근 권한이 없습니다."
                        else -> "여행 정보를 불러오는데 실패했습니다. (${response.code()})"
                    }
                    Toast.makeText(this@TravelDetailActivity, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e("TravelDetail", "API Error: ${response.code()} - ${response.errorBody()?.string()}")
                    finish()
                }
            }

            override fun onFailure(call: Call<PlanDetailResponse>, t: Throwable) {
                Log.e("TravelDetail", "Network error: ${t.message}", t)
                Toast.makeText(this@TravelDetailActivity, "네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요.", Toast.LENGTH_LONG).show()
                finish()
            }
        })
    }

    private fun setupDates(dateString: String) {
        val parts = dateString.split(" - ")
        if (parts.size == 2) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            try {
                val startDate = dateFormat.parse(parts[0])
                val endDate = dateFormat.parse(parts[1])

                if (startDate != null && endDate != null) {
                    startDateCalendar.time = startDate
                    endDateCalendar.time = endDate

                    // 날짜 차이 계산 (일 수)
                    val diffInMillis = endDate.time - startDate.time
                    dayCount = (TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1).toInt()
                }
            } catch (e: Exception) {
                dayCount = 1
                Toast.makeText(this, "날짜 형식을 파싱하는 데 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        } else {
            dayCount = 1
        }
    }

    private fun setupViewPagerAndTabs(places: List<TravelPlace>) {
        // 일자별로 장소를 그룹화
        val dayPlaces = mutableMapOf<Int, MutableList<TravelPlace>>()

        // 각 일자별로 빈 목록 초기화
        for (day in 1..dayCount) {
            dayPlaces[day] = mutableListOf()
        }

        // 각 장소를 해당 일자에 할당
        for (place in places) {
            val day = place.dayVisit
            if (day in 1..dayCount) {
                dayPlaces[day]?.add(place)
            } else {
                // 잘못된 일자인 경우 첫째 날에 추가
                dayPlaces[1]?.add(place)
            }
        }

        // 그룹핑 결과 로그 출력
        for ((day, list) in dayPlaces) {
            Log.d("TravelDetail", "day $day: ${list.size} places")
        }

        // ViewPager 어댑터 설정
        val pagerAdapter = DayPagerAdapter(dayPlaces, dayCount)
        viewPager.adapter = pagerAdapter

        // TabLayout과 ViewPager 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val dayNumber = position + 1
            val dayCalendar = Calendar.getInstance()
            dayCalendar.time = startDateCalendar.time
            dayCalendar.add(Calendar.DAY_OF_MONTH, position)

            val dayOfWeek = SimpleDateFormat("E", Locale.KOREA).format(dayCalendar.time)
            val dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(dayCalendar.time)

            tab.text = "Day $dayNumber"
            tab.setCustomView(R.layout.custom_tab_layout)
            val customView = tab.customView
            customView?.let {
                val tabDayText = it.findViewById<TextView>(R.id.tabDayNumber)
                val tabDateText = it.findViewById<TextView>(R.id.tabDateInfo)

                tabDayText.text = "Day $dayNumber"
                tabDateText.text = "$dayOfWeek, $dayOfMonth"
            }
        }.attach()
    }
}

// 일자별 페이저 어댑터
class DayPagerAdapter(
    private val dayPlaces: Map<Int, List<TravelPlace>>,
    private val dayCount: Int
) : androidx.recyclerview.widget.RecyclerView.Adapter<DayPagerAdapter.DayPageViewHolder>() {

    class DayPageViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewDayPlaces)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayPageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_page, parent, false)
        return DayPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayPageViewHolder, position: Int) {
        val day = position + 1
        val places = dayPlaces[day] ?: emptyList()

        holder.recyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        // planId를 넘겨줌
        val activity = holder.itemView.context as? TravelDetailActivity
        val planId = activity?.intent?.getIntExtra("PLAN_ID", -1) ?: -1
        holder.recyclerView.adapter = PlaceAdapter(places, planId)
    }

    override fun getItemCount(): Int = dayCount
}

// 장소 어댑터
class PlaceAdapter(private val places: List<TravelPlace>, private val planId: Int) :
    RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val placeImage: ImageView = view.findViewById(R.id.placeImage)
        val nameText: TextView = view.findViewById(R.id.placeName)
        val addressText: TextView = view.findViewById(R.id.placeAddress)
        val lockIconView: ImageView = view.findViewById(R.id.lockIconView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        holder.nameText.text = place.name
        holder.addressText.text = place.address

        // 이미지 설정 (여기서는 더미 이미지 사용)
        holder.placeImage.setImageResource(R.drawable.ic_place_holder)

        // 잠금 아이콘 설정 (공개/비공개 상태에 따라)
        holder.lockIconView.setImageResource(R.drawable.ic_circle_open)

        // 아이템 클릭 이벤트 추가 - 기록 작성 화면으로 이동
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PlaceMemoryActivity::class.java)
            intent.putExtra("PLACE_NAME", place.name)
            intent.putExtra("PLACE_ADDRESS", place.address)
            intent.putExtra("PLACE_ID", place.id)
            intent.putExtra("PLAN_ID", planId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = places.size
} 