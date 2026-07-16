package com.example.safehome.ui.monitor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.R
import com.example.safehome.data.remote.HistoryRecordDto
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentHistoryAdapter : RecyclerView.Adapter<RecentHistoryAdapter.ViewHolder>() {

    private var historyList = listOf<HistoryRecordDto>()
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submitList(list: List<HistoryRecordDto>) {
        historyList = list.take(10)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    override fun getItemCount(): Int = historyList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTime: TextView = itemView.findViewById(R.id.txtHistoryTime)
        private val txtTemp: TextView = itemView.findViewById(R.id.txtHistoryTemp)
        private val txtHumid: TextView = itemView.findViewById(R.id.txtHistoryHumid)
        private val txtMQ2: TextView = itemView.findViewById(R.id.txtHistoryMQ2)
        private val txtMQ135: TextView = itemView.findViewById(R.id.txtHistoryMQ135)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtHistoryStatus)
        private val cardStatus: MaterialCardView = itemView.findViewById(R.id.cardHistoryStatus)

        fun bind(record: HistoryRecordDto) {
            try {
                val date = Date(java.time.Instant.parse(record.createdAt).toEpochMilli())
                txtTime.text = dateFormat.format(date)
            } catch (e: Exception) {
                txtTime.text = "--:--"
            }

            txtTemp.text = "${record.sensor.temperature}°C"
            txtHumid.text = "${record.sensor.humidity}%"
            
            val mq2Ppm = ((record.sensor.mq2Raw.toDouble() / 4095.0) * 1000).toInt()
            val mq135Ppm = ((record.sensor.mq135Raw.toDouble() / 4095.0) * 1000).toInt()
            
            txtMQ2.text = mq2Ppm.toString()
            txtMQ135.text = mq135Ppm.toString()

            val systemStatus = record.status.system
            txtStatus.text = systemStatus

            when (systemStatus) {
                "SAFE" -> {
                    cardStatus.setCardBackgroundColor(Color.parseColor("#D1FAE5"))
                    txtStatus.setTextColor(Color.parseColor("#065F46"))
                }
                "WARNING" -> {
                    cardStatus.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
                    txtStatus.setTextColor(Color.parseColor("#92400E"))
                }
                "DANGER" -> {
                    cardStatus.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                    txtStatus.setTextColor(Color.parseColor("#991B1B"))
                }
                else -> {
                    cardStatus.setCardBackgroundColor(Color.parseColor("#F1F5F9"))
                    txtStatus.setTextColor(Color.parseColor("#64748B"))
                }
            }
        }
    }
}
