package com.example.safehome.ui.device

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.data.remote.HistoryRecordDto
import com.example.safehome.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeviceHistoryAdapter : ListAdapter<HistoryRecordDto, DeviceHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: HistoryRecordDto) {
            binding.txtTimestamp.text = formatDate(record.createdAt)
            
            // System status
            binding.txtSystemStatus.text = record.status?.system ?: "UNKNOWN"
            val systemStatus = record.status?.system ?: "UNKNOWN"
            val (textColor, bgColor) = when (systemStatus) {
                "SAFE" -> Pair("#065F46", "#D1FAE5")
                "WARNING" -> Pair("#92400E", "#FEF3C7")
                "DANGER" -> Pair("#991B1B", "#FEE2E2")
                else -> Pair("#475569", "#F1F5F9")
            }
            binding.txtSystemStatus.setTextColor(android.graphics.Color.parseColor(textColor))
            binding.txtSystemStatus.setBackgroundColor(android.graphics.Color.parseColor(bgColor))
            
            // Sensor data
            binding.txtTemperature.text = "${record.sensor.temperature}°C"
            binding.txtHumidity.text = "${record.sensor.humidity.toInt()}%"
            binding.txtMq2.text = record.sensor.mq2Raw.toString()
            binding.txtMq135.text = record.sensor.mq135Raw.toString()
            binding.txtFlame.text = if (record.sensor.flameDetected) "Có" else "Không"
            binding.txtFlame.setTextColor(
                if (record.sensor.flameDetected) 
                    android.graphics.Color.parseColor("#E11D48")
                else 
                    android.graphics.Color.parseColor("#1E293B")
            )
            binding.txtUptime.text = formatUptime(record.uptimeS)
        }

        private fun formatDate(isoString: String): String {
            return try {
                val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val outputFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormatter.parse(isoString)
                outputFormatter.format(date ?: Date())
            } catch (e: Exception) {
                "N/A"
            }
        }

        private fun formatUptime(seconds: Long): String {
            val hours = TimeUnit.SECONDS.toHours(seconds)
            val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
            return when {
                hours > 24 -> "${TimeUnit.HOURS.toDays(hours)} ngày"
                hours > 0 -> "${hours}h${minutes}p"
                else -> "${minutes}p"
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryRecordDto>() {
        override fun areItemsTheSame(oldItem: HistoryRecordDto, newItem: HistoryRecordDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryRecordDto, newItem: HistoryRecordDto): Boolean {
            return oldItem == newItem
        }
    }
}
