package com.example.safehome.ui.monitor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.R
import com.example.safehome.data.remote.DeviceDto
import com.example.safehome.data.remote.HistoryRecordDto
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorDeviceAdapter : RecyclerView.Adapter<MonitorDeviceAdapter.ViewHolder>() {

    private var deviceDataList = listOf<DeviceMonitorData>()
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    data class DeviceMonitorData(
        val device: DeviceDto,
        val history: List<HistoryRecordDto>
    )

    fun submitList(data: List<DeviceMonitorData>) {
        deviceDataList = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitor_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(deviceDataList[position])
    }

    override fun getItemCount(): Int = deviceDataList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDeviceName: TextView = itemView.findViewById(R.id.txtDeviceName)
        private val txtDeviceTemp: TextView = itemView.findViewById(R.id.txtDeviceTemp)
        private val txtDeviceHumid: TextView = itemView.findViewById(R.id.txtDeviceHumid)
        private val txtDeviceStatus: TextView = itemView.findViewById(R.id.txtDeviceStatus)
        private val cardDeviceStatus: MaterialCardView = itemView.findViewById(R.id.cardDeviceStatus)
        
        // Charts
        private val chartTemp: LineChart = itemView.findViewById(R.id.chartTemp)
        private val chartHumid: LineChart = itemView.findViewById(R.id.chartHumid)
        private val chartMQ2: LineChart = itemView.findViewById(R.id.chartMQ2)
        private val chartMQ135: LineChart = itemView.findViewById(R.id.chartMQ135)
        
        // Temperature stats
        private val txtTempCurrent: TextView = itemView.findViewById(R.id.txtTempCurrent)
        private val txtTempRange: TextView = itemView.findViewById(R.id.txtTempRange)
        
        // Humidity stats
        private val txtHumidCurrent: TextView = itemView.findViewById(R.id.txtHumidCurrent)
        private val txtHumidRange: TextView = itemView.findViewById(R.id.txtHumidRange)
        
        // MQ2 stats
        private val txtMQ2Current: TextView = itemView.findViewById(R.id.txtMQ2Current)
        private val txtMQ2Range: TextView = itemView.findViewById(R.id.txtMQ2Range)
        
        // MQ135 stats
        private val txtMQ135Current: TextView = itemView.findViewById(R.id.txtMQ135Current)
        private val txtMQ135Range: TextView = itemView.findViewById(R.id.txtMQ135Range)
        
        // Footer stats
        private val txtDeviceTime: TextView = itemView.findViewById(R.id.txtDeviceTime)
        private val txtFlameStatus: TextView = itemView.findViewById(R.id.txtFlameStatus)
        private val txtBuzzerStatus: TextView = itemView.findViewById(R.id.txtBuzzerStatus)

        fun bind(data: DeviceMonitorData) {
            val device = data.device
            val history = data.history

            txtDeviceName.text = device.name ?: "Thiết bị ${device.id}"

            if (history.isNotEmpty()) {
                val latest = history.first()
                txtDeviceTemp.text = "${latest.sensor.temperature}°C"
                txtDeviceHumid.text = "${latest.sensor.humidity}%"

                val systemStatus = latest.status.system
                txtDeviceStatus.text = systemStatus

                when (systemStatus) {
                    "SAFE" -> {
                        cardDeviceStatus.setCardBackgroundColor(Color.parseColor("#D1FAE5"))
                        txtDeviceStatus.setTextColor(Color.parseColor("#065F46"))
                    }
                    "WARNING" -> {
                        cardDeviceStatus.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
                        txtDeviceStatus.setTextColor(Color.parseColor("#92400E"))
                    }
                    "DANGER" -> {
                        cardDeviceStatus.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                        txtDeviceStatus.setTextColor(Color.parseColor("#991B1B"))
                    }
                    else -> {
                        cardDeviceStatus.setCardBackgroundColor(Color.parseColor("#F1F5F9"))
                        txtDeviceStatus.setTextColor(Color.parseColor("#64748B"))
                    }
                }

                // Temperature chart
                val tempEntries = history.mapIndexed { index, record ->
                    com.github.mikephil.charting.data.Entry(
                        ChartHelper.formatTimestamp(record.createdAt).toFloat(),
                        record.sensor.temperature.toFloat()
                    )
                }
                ChartHelper.updateMiniChart(chartTemp, tempEntries, Color.parseColor("#DC2626"))

                val tempValues = history.map { it.sensor.temperature }
                val (tempMin, tempMax) = ChartHelper.getMinMax(tempValues)
                txtTempCurrent.text = "${String.format("%.1f", latest.sensor.temperature)}°C"
                txtTempRange.text = "${String.format("%.1f", tempMin)}-${String.format("%.1f", tempMax)}"

                // Humidity chart
                val humidEntries = history.mapIndexed { index, record ->
                    com.github.mikephil.charting.data.Entry(
                        ChartHelper.formatTimestamp(record.createdAt).toFloat(),
                        record.sensor.humidity.toFloat()
                    )
                }
                ChartHelper.updateMiniChart(chartHumid, humidEntries, Color.parseColor("#2563EB"))

                val humidValues = history.map { it.sensor.humidity }
                val (humidMin, humidMax) = ChartHelper.getMinMax(humidValues)
                txtHumidCurrent.text = "${String.format("%.1f", latest.sensor.humidity)}%"
                txtHumidRange.text = "${String.format("%.1f", humidMin)}-${String.format("%.1f", humidMax)}"

                // MQ2 chart
                val mq2Entries = history.mapIndexed { index, record ->
                    com.github.mikephil.charting.data.Entry(
                        ChartHelper.formatTimestamp(record.createdAt).toFloat(),
                        ((record.sensor.mq2Raw.toDouble() / 4095.0) * 1000).toFloat()
                    )
                }
                ChartHelper.updateMiniChart(chartMQ2, mq2Entries, Color.parseColor("#D97706"))

                val mq2Values = history.map { (it.sensor.mq2Raw.toDouble() / 4095.0) * 1000 }
                val (mq2Min, mq2Max) = ChartHelper.getMinMax(mq2Values)
                txtMQ2Current.text = String.format("%.0f", mq2Values.first())
                txtMQ2Range.text = "${String.format("%.0f", mq2Min)}-${String.format("%.0f", mq2Max)}"

                // MQ135 chart
                val mq135Entries = history.mapIndexed { index, record ->
                    com.github.mikephil.charting.data.Entry(
                        ChartHelper.formatTimestamp(record.createdAt).toFloat(),
                        ((record.sensor.mq135Raw.toDouble() / 4095.0) * 1000).toFloat()
                    )
                }
                ChartHelper.updateMiniChart(chartMQ135, mq135Entries, Color.parseColor("#059669"))

                val mq135Values = history.map { (it.sensor.mq135Raw.toDouble() / 4095.0) * 1000 }
                val (mq135Min, mq135Max) = ChartHelper.getMinMax(mq135Values)
                txtMQ135Current.text = String.format("%.0f", mq135Values.first())
                txtMQ135Range.text = "${String.format("%.0f", mq135Min)}-${String.format("%.0f", mq135Max)}"

                // Footer stats
                try {
                    val date = Date(java.time.Instant.parse(latest.createdAt).toEpochMilli())
                    txtDeviceTime.text = dateFormat.format(date)
                } catch (e: Exception) {
                    txtDeviceTime.text = "--:--"
                }

                txtFlameStatus.text = if (latest.sensor.flameDetected) "Có" else "Không"
                txtFlameStatus.setTextColor(if (latest.sensor.flameDetected) Color.parseColor("#EF4444") else Color.parseColor("#22C55E"))

                txtBuzzerStatus.text = if (latest.control.buzzerActive) "Bật" else "Tắt"
                txtBuzzerStatus.setTextColor(if (latest.control.buzzerActive) Color.parseColor("#F59E0B") else Color.parseColor("#64748B"))
            } else {
                txtDeviceTemp.text = "--°C"
                txtDeviceHumid.text = "--%"
                txtDeviceStatus.text = "N/A"
                cardDeviceStatus.setCardBackgroundColor(Color.parseColor("#F1F5F9"))
                txtDeviceStatus.setTextColor(Color.parseColor("#64748B"))
                
                chartTemp.clear()
                chartHumid.clear()
                chartMQ2.clear()
                chartMQ135.clear()
                
                txtTempCurrent.text = "--°C"
                txtTempRange.text = "--/--"
                txtHumidCurrent.text = "--%"
                txtHumidRange.text = "--/--"
                txtMQ2Current.text = "--"
                txtMQ2Range.text = "--/--"
                txtMQ135Current.text = "--"
                txtMQ135Range.text = "--/--"
                txtDeviceTime.text = "--:--"
                txtFlameStatus.text = "--"
                txtBuzzerStatus.text = "--"
            }
        }
    }
}
