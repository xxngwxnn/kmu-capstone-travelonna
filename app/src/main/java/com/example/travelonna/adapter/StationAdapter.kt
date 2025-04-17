package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.api.Station

open class StationAdapter(
    private var stations: List<Station> = emptyList(),
    private val onItemClick: (Station) -> Unit
) : RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    class StationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stationName: TextView = view.findViewById(R.id.stationName)
        val stationType: TextView = view.findViewById(R.id.stationType)
        val stationLocation: TextView = view.findViewById(R.id.stationLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]
        
        // 역 이름 바인딩 (오버라이드 가능)
        bindStationName(holder.stationName, station)
        
        // 나머지 정보 바인딩
        holder.stationType.text = station.type
        holder.stationLocation.text = station.location
        
        holder.itemView.setOnClickListener {
            onItemClick(station)
        }
    }
    
    // 역 이름 바인딩 메소드 (서브클래스에서 오버라이드 가능)
    open fun bindStationName(stationView: TextView, station: Station) {
        stationView.text = station.name
    }

    override fun getItemCount() = stations.size

    fun updateData(newStations: List<Station>) {
        stations = newStations
        notifyDataSetChanged()
    }
} 