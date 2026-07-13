package com.example.safehome.ui.device

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.R
import com.example.safehome.data.remote.DeviceDto
import com.example.safehome.databinding.ItemDeviceBinding
import java.time.Duration
import java.time.Instant
import java.util.Locale

class DeviceAdapter(
    private val onItemClick: (DeviceDto) -> Unit
) : ListAdapter<DeviceDto, DeviceAdapter.DeviceViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding, parent.context, onItemClick)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: ItemDeviceBinding,
        private val context: Context,
        private val onItemClick: (DeviceDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: DeviceDto) {
            binding.txtDeviceName.text = device.name ?: "Thiết bị chưa đặt tên"
            binding.txtDeviceCode.text = "Code: ${device.deviceCode ?: "N/A"}"

            val temperature = device.sensor?.temperature
            binding.txtTemperature.text = if (temperature != null) {
                String.format(Locale.getDefault(), "Nhiệt độ: %.1f°C", temperature)
            } else {
                "Nhiệt độ: N/A"
            }

            val humidity = device.sensor?.humidity
            binding.txtHumidity.text = if (humidity != null) {
                String.format(Locale.getDefault(), "Độ ẩm: %.0f%%", humidity)
            } else {
                "Độ ẩm: N/A"
            }

            bindRiskBadge(device.status?.system)
            bindOnlineStatus(device.device?.lastSeen ?: device.device?.updatedAt)

            binding.root.setOnClickListener {
                onItemClick(device)
            }
        }

        private fun bindRiskBadge(status: String?) {
            when (status?.uppercase()) {
                "WARNING" -> {
                    binding.txtRiskBadge.text = "WARNING"
                    binding.txtRiskBadge.setTextColor(context.getColor(R.color.safehome_warning_text))
                    binding.txtRiskBadge.setBackgroundResource(R.drawable.bg_device_badge_warning)
                }

                "DANGER" -> {
                    binding.txtRiskBadge.text = "DANGER"
                    binding.txtRiskBadge.setTextColor(context.getColor(R.color.safehome_danger_text))
                    binding.txtRiskBadge.setBackgroundResource(R.drawable.bg_device_badge_danger)
                }

                else -> {
                    binding.txtRiskBadge.text = "SAFE"
                    binding.txtRiskBadge.setTextColor(context.getColor(R.color.safehome_green_700))
                    binding.txtRiskBadge.setBackgroundResource(R.drawable.bg_device_badge_safe)
                }
            }
        }

        private fun bindOnlineStatus(lastSeen: String?) {
            val isOffline = isOffline(lastSeen)
            if (isOffline) {
                binding.viewOnlineDot.setBackgroundResource(R.drawable.bg_badge_dot_grey)
                binding.txtOnlineStatus.text = "Offline"
                binding.txtOnlineStatus.setTextColor(context.getColor(R.color.safehome_text_muted))
            } else {
                binding.viewOnlineDot.setBackgroundResource(R.drawable.bg_badge_dot_green)
                binding.txtOnlineStatus.text = "Online"
                binding.txtOnlineStatus.setTextColor(context.getColor(R.color.safehome_green_700))
            }
        }

        private fun isOffline(lastSeen: String?): Boolean {
            if (lastSeen.isNullOrBlank()) return true
            return try {
                val now = Instant.now()
                val seenAt = Instant.parse(lastSeen)
                Duration.between(seenAt, now).seconds > 30
            } catch (_: Exception) {
                true
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DeviceDto>() {
            override fun areItemsTheSame(oldItem: DeviceDto, newItem: DeviceDto): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DeviceDto, newItem: DeviceDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}
