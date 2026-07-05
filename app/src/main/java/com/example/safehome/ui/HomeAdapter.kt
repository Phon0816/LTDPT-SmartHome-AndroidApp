package com.example.safehome.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.R
import com.example.safehome.data.SensorItem
import com.google.android.material.card.MaterialCardView

class HomeAdapter(private val sensors: List<SensorItem>) :
    RecyclerView.Adapter<HomeAdapter.SensorViewHolder>() {

    class SensorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardIconBg: MaterialCardView = view.findViewById(R.id.cardIconBg)
        val imgSensorIcon: ImageView = view.findViewById(R.id.imgSensorIcon)
        val txtSensorName: TextView = view.findViewById(R.id.txtSensorName)
        val txtSensorValue: TextView = view.findViewById(R.id.txtSensorValue)
        val viewStatusDot: View = view.findViewById(R.id.viewStatusDot)
        val txtSensorStatus: TextView = view.findViewById(R.id.txtSensorStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor, parent, false)
        return SensorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val sensor = sensors[position]
        holder.txtSensorName.text = sensor.name
        holder.txtSensorValue.text = sensor.value
        holder.txtSensorStatus.text = sensor.status
        
        holder.imgSensorIcon.setImageResource(sensor.iconResId)
        
        try {
            holder.cardIconBg.setCardBackgroundColor(Color.parseColor(sensor.iconBgColor))
            holder.imgSensorIcon.setColorFilter(Color.parseColor(sensor.iconTintColor))
            holder.txtSensorStatus.setTextColor(Color.parseColor(sensor.statusColor))
            
            // Đặt màu chấm tròn nhỏ trạng thái
            val bgDot = holder.viewStatusDot.background
            bgDot?.setTint(Color.parseColor(sensor.statusColor))
        } catch (e: Exception) {
            // Tránh crash nếu parse màu lỗi
        }
    }

    override fun getItemCount(): Int = sensors.size
}
