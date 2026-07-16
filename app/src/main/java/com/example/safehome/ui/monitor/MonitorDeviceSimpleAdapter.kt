package com.example.safehome.ui.monitor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.data.remote.DeviceDto
import com.example.safehome.R

class MonitorDeviceSimpleAdapter(
    private val onDeviceClick: (DeviceDto) -> Unit
) : RecyclerView.Adapter<MonitorDeviceSimpleAdapter.DeviceViewHolder>() {

    private var devices = listOf<DeviceDto>()

    fun submitDevices(newDevices: List<DeviceDto>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitor_device_simple, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val txtDeviceName = itemView.findViewById<android.widget.TextView>(R.id.txtDeviceName)
        private val txtDeviceCode = itemView.findViewById<android.widget.TextView>(R.id.txtDeviceCode)
        private val cardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(device: DeviceDto) {
            txtDeviceName.text = device.name ?: "Thiết bị không tên"
            txtDeviceCode.text = "Code: ${device.deviceCode ?: "N/A"}"
            
            cardView.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }
}
