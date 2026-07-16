package com.example.safehome.ui.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.R
import com.example.safehome.data.remote.HistoryRecordDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FlameTimelineAdapter : RecyclerView.Adapter<FlameTimelineAdapter.ViewHolder>() {

    private var flameEvents = listOf<FlameEvent>()
    private val dateFormat = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())

    data class FlameEvent(
        val timestamp: String,
        val detected: Boolean,
        val stateChange: Boolean // true if this is a state change event
    )

    fun submitEvents(events: List<FlameEvent>) {
        flameEvents = events
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flame_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(flameEvents[position])
    }

    override fun getItemCount(): Int = flameEvents.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTime: TextView = itemView.findViewById(R.id.txtFlameTime)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtFlameStatus)
        private val viewDot: View = itemView.findViewById(R.id.viewFlameDot)

        fun bind(event: FlameEvent) {
            try {
                val date = Date(java.time.Instant.parse(event.timestamp).toEpochMilli())
                txtTime.text = dateFormat.format(date)
            } catch (e: Exception) {
                txtTime.text = "--:--"
            }

            if (event.detected) {
                txtStatus.text = "Phát hiện lửa 🔥"
                txtStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                viewDot.setBackgroundResource(R.drawable.bg_badge_dot_red)
            } else {
                txtStatus.text = "Tắt lửa ✓"
                txtStatus.setTextColor(android.graphics.Color.parseColor("#22C55E"))
                viewDot.setBackgroundResource(R.drawable.bg_badge_dot_green)
            }
        }
    }
}
