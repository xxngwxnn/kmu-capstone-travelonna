package com.example.travelonna.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelonna.R
import com.example.travelonna.api.Station

class StationAdapter(
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
        
        holder.stationName.text = station.name
        holder.stationType.text = station.type
        holder.stationLocation.text = station.location
        
        holder.itemView.setOnClickListener {
            onItemClick(station)
        }
    }

    override fun getItemCount() = stations.size

    fun updateData(newStations: List<Station>) {
        stations = newStations
        notifyDataSetChanged()
    }
} 