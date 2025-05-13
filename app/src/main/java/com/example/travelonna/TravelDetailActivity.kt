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

class TravelDetailActivity : AppCompatActivity() {

    private lateinit var recyclerViewPlaces: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var typeTextView: TextView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel_detail)

        // UI 요소 초기화
        titleTextView = findViewById(R.id.detailTitleTextView)
        dateTextView = findViewById(R.id.detailDateTextView)
        typeTextView = findViewById(R.id.detailTypeTextView)
        backButton = findViewById(R.id.backButton)
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces)

        // Intent에서 데이터 가져오기
        val title = intent.getStringExtra("TRAVEL_TITLE") ?: ""
        val date = intent.getStringExtra("TRAVEL_DATE") ?: ""
        val type = intent.getStringExtra("TRAVEL_TYPE") ?: ""
        val placesCount = intent.getIntExtra("TRAVEL_PLACES_COUNT", 0)

        // 기본 정보 표시
        titleTextView.text = title
        dateTextView.text = date
        typeTextView.text = type

        // 장소 목록 가져오기
        val places = mutableListOf<TravelPlace>()
        for (i in 0 until placesCount) {
            val name = intent.getStringExtra("PLACE_NAME_$i") ?: ""
            val address = intent.getStringExtra("PLACE_ADDRESS_$i") ?: ""
            val time = intent.getStringExtra("PLACE_TIME_$i") ?: ""
            places.add(TravelPlace(name, address, time))
        }

        // RecyclerView 설정
        recyclerViewPlaces.layoutManager = LinearLayoutManager(this)
        recyclerViewPlaces.adapter = PlaceAdapter(places)

        // 뒤로가기 버튼 설정
        backButton.setOnClickListener {
            finish()
        }
    }
}

// 장소 어댑터
class PlaceAdapter(private val places: List<TravelPlace>) : 
    RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val placeImage: ImageView = view.findViewById(R.id.placeImage)
        val nameText: TextView = view.findViewById(R.id.placeName)
        val addressText: TextView = view.findViewById(R.id.placeAddress)
        val timeText: TextView = view.findViewById(R.id.placeTime)
        val privacyToggle: ToggleButton = view.findViewById(R.id.privacyToggle)
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
        holder.timeText.text = place.visitTime
        
        // 공개/비공개 토글 설정
        holder.privacyToggle.setOnCheckedChangeListener { _, isChecked ->
            // 토글 상태에 따라 공개/비공개 설정
            val status = if (isChecked) "공개" else "비공개"
            Toast.makeText(holder.itemView.context, 
                "${place.name} 장소가 ${status}로 설정되었습니다", 
                Toast.LENGTH_SHORT).show()
        }
        
        // 아이템 클릭 이벤트
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PlaceMemoryActivity::class.java).apply {
                putExtra("PLACE_NAME", place.name)
                putExtra("PLACE_ADDRESS", place.address)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = places.size
} 