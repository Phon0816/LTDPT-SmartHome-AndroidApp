package com.example.safehome.ui.device

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.safehome.databinding.ActivityDeviceDetailBinding
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.DeviceRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceDetailBinding
    private lateinit var viewModel: DeviceDetailViewModel
    private var deviceId: Int = -1
    private var lastControlError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceId = intent.getIntExtra("deviceId", -1)
        if (deviceId == -1) {
            finish()
            return
        }

        viewModel = createViewModel()
        setupViews()
        observeUiState()

        viewModel.loadDeviceDetail()
    }

    private fun createViewModel(): DeviceDetailViewModel {
        val tokenManager = TokenManager(applicationContext)
        val deviceApi = RetrofitClient.createDeviceApi(tokenManager)
        val repository = DeviceRepository(deviceApi)
        val factory = DeviceDetailViewModelFactory(repository, deviceId)
        return ViewModelProvider(this, factory)[DeviceDetailViewModel::class.java]
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadDeviceDetail()
        }

        binding.cardHistory.setOnClickListener {
            val intent = Intent(this, DeviceHistoryActivity::class.java)
            intent.putExtra("deviceId", deviceId)
            startActivity(intent)
        }

        // Setup switches
        setupSwitchListeners()
    }

    private fun setupSwitchListeners() {
        binding.switchLed1.setOnCheckedChangeListener { _, isChecked ->
            val previousValue = viewModel.uiState.value.device?.control?.led1 ?: false
            viewModel.controlDevice("led1", isChecked, previousValue)
        }

        binding.switchLed2.setOnCheckedChangeListener { _, isChecked ->
            val previousValue = viewModel.uiState.value.device?.control?.led2 ?: false
            viewModel.controlDevice("led2", isChecked, previousValue)
        }

        binding.switchLed3.setOnCheckedChangeListener { _, isChecked ->
            val previousValue = viewModel.uiState.value.device?.control?.led3 ?: false
            viewModel.controlDevice("led3", isChecked, previousValue)
        }

        binding.switchLed4.setOnCheckedChangeListener { _, isChecked ->
            val previousValue = viewModel.uiState.value.device?.control?.led4 ?: false
            viewModel.controlDevice("led4", isChecked, previousValue)
        }

        binding.switchLed5.setOnCheckedChangeListener { _, isChecked ->
            val previousValue = viewModel.uiState.value.device?.control?.led5 ?: false
            viewModel.controlDevice("led5", isChecked, previousValue)
        }

        binding.switchBuzzerMuted.setOnCheckedChangeListener { _, isChecked ->
            val previousValue = viewModel.uiState.value.device?.control?.buzzerMuted ?: false
            viewModel.controlDevice("buzzerMuted", isChecked, previousValue)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateLoadingState(state.isLoading)
                updateErrorState(state.errorMessage)
                updateContentState(state.device)
                updateControlError(state.controlError)
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun updateErrorState(errorMessage: String?) {
        binding.layoutError.visibility = if (errorMessage != null) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (errorMessage != null) View.GONE else View.VISIBLE
    }

    private fun updateContentState(device: com.example.safehome.data.remote.DeviceDto?) {
        if (device == null) return

        // Header
        binding.txtDeviceName.text = device.name ?: "Thiết bị"
        binding.txtDeviceCode.text = device.deviceCode ?: "N/A"
        
        // Online status based on last seen
        val isOnline = isDeviceOnline(device.device?.lastSeen)
        binding.txtDeviceStatus.text = if (isOnline) "● Online" else "● Offline"
        binding.txtDeviceStatus.setTextColor(
            if (isOnline) getColor(android.R.color.holo_green_dark) 
            else getColor(android.R.color.darker_gray)
        )

        // Safety Card
        updateSafetyCard(device)

        // Sensor Data
        updateSensorData(device.sensor)

        // Status Chips
        updateStatusChips(device.status)

        // Control Switches
        updateControlSwitches(device.control)

        // Device Info
        updateDeviceInfo(device)
    }

    private fun updateSafetyCard(device: com.example.safehome.data.remote.DeviceDto) {
        val systemStatus = device.status?.system ?: "UNKNOWN"
        val isSafe = systemStatus == "SAFE"
        val isWarning = systemStatus == "WARNING"
        val isDanger = systemStatus == "DANGER"

        binding.txtSafetyBadge.text = when {
            isSafe -> "✓ SAFE"
            isWarning -> "⚠ WARNING"
            isDanger -> "✕ DANGER"
            else -> "? UNKNOWN"
        }

        binding.txtSafetyBadge.setTextColor(
            when {
                isSafe -> getColor(android.R.color.holo_green_dark)
                isWarning -> getColor(android.R.color.holo_orange_dark)
                isDanger -> getColor(android.R.color.holo_red_dark)
                else -> getColor(android.R.color.darker_gray)
            }
        )

        val badgeColor = when {
            isSafe -> "#D1FAE5"
            isWarning -> "#FEF3C7"
            isDanger -> "#FEE2E2"
            else -> "#F1F5F9"
        }
        binding.txtSafetyBadge.parent?.let { parent ->
            if (parent is com.google.android.material.card.MaterialCardView) {
                parent.setCardBackgroundColor(android.graphics.Color.parseColor(badgeColor))
            }
        }

        binding.txtSafetyMessage.text = if (isSafe) "Thiết bị đang hoạt động" else "Cảnh báo an toàn"
        
        // Calculate safety score based on status
        val score = when (systemStatus) {
            "SAFE" -> 98
            "WARNING" -> 65
            "DANGER" -> 25
            else -> 50
        }
        binding.txtSafetyScore.text = "$score%"
        binding.txtSafetyScore.setTextColor(
            when {
                isSafe -> getColor(android.R.color.holo_green_dark)
                isWarning -> getColor(android.R.color.holo_orange_dark)
                isDanger -> getColor(android.R.color.holo_red_dark)
                else -> getColor(android.R.color.darker_gray)
            }
        )

        // Last update time
        val lastSeen = device.device?.lastSeen
        binding.txtLastUpdate.text = if (lastSeen != null) {
            "Cập nhật ${formatTimeAgo(lastSeen)}"
        } else {
            "Chưa cập nhật"
        }
    }

    private fun updateSensorData(sensor: com.example.safehome.data.remote.SensorDataDto?) {
        if (sensor == null) {
            binding.txtTemperature.text = "N/A"
            binding.txtHumidity.text = "N/A"
            binding.txtMq2.text = "N/A"
            binding.txtMq135.text = "N/A"
            binding.txtFlame.text = "N/A"
            return
        }

        binding.txtTemperature.text = "${sensor.temperature}°C"
        binding.txtHumidity.text = "${sensor.humidity.toInt()}%"
        binding.txtMq2.text = "${sensor.mq2Raw} ppm"
        binding.txtMq135.text = "${sensor.mq135Raw} ppm"
        binding.txtFlame.text = if (sensor.flameDetected) "Có" else "Không"
        binding.txtFlame.setTextColor(
            if (sensor.flameDetected) getColor(android.R.color.holo_red_dark)
            else getColor(android.R.color.black)
        )
    }

    private fun updateStatusChips(status: com.example.safehome.data.remote.StatusDataDto?) {
        if (status == null) return

        updateChip(binding.chipTempStatus, "Temperature", status.temperature)
        updateChip(binding.chipHumidStatus, "Humidity", status.humidity)
        updateChip(binding.chipMq2Status, "MQ2", status.mq2)
        updateChip(binding.chipMq135Status, "MQ135", status.mq135)
        updateChip(binding.chipFlameStatus, "Flame", status.flame)
        updateChip(binding.chipSystemStatus, "System", status.system)
    }

    private fun updateChip(chip: com.google.android.material.chip.Chip, label: String, status: String) {
        chip.text = "$label: $status"
        
        val (textColor, bgColor) = when (status) {
            "SAFE" -> Pair("#065F46", "#D1FAE5")
            "WARNING" -> Pair("#92400E", "#FEF3C7")
            "DANGER" -> Pair("#991B1B", "#FEE2E2")
            else -> Pair("#475569", "#F1F5F9")
        }
        
        chip.setTextColor(android.graphics.Color.parseColor(textColor))
        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(bgColor))
    }

    private fun updateControlSwitches(control: com.example.safehome.data.remote.ControlDataDto?) {
        if (control == null) return

        // Disable listeners temporarily to avoid triggering controlDevice
        binding.switchLed1.setOnCheckedChangeListener(null)
        binding.switchLed2.setOnCheckedChangeListener(null)
        binding.switchLed3.setOnCheckedChangeListener(null)
        binding.switchLed4.setOnCheckedChangeListener(null)
        binding.switchLed5.setOnCheckedChangeListener(null)
        binding.switchBuzzerMuted.setOnCheckedChangeListener(null)

        binding.switchLed1.isChecked = control.led1
        binding.switchLed2.isChecked = control.led2
        binding.switchLed3.isChecked = control.led3
        binding.switchLed4.isChecked = control.led4
        binding.switchLed5.isChecked = control.led5
        binding.switchBuzzerMuted.isChecked = control.buzzerMuted

        // Re-enable listeners
        setupSwitchListeners()
    }

    private fun updateDeviceInfo(device: com.example.safehome.data.remote.DeviceDto) {
        binding.txtInfoDeviceCode.text = device.deviceCode ?: "N/A"
        
        val uptime = device.device?.uptimeS
        binding.txtInfoUptime.text = if (uptime != null) formatUptime(uptime) else "N/A"
        
        val lastSeen = device.device?.lastSeen
        binding.txtInfoLastSeen.text = if (lastSeen != null) formatTimeAgo(lastSeen) else "N/A"
        
        val createdAt = device.device?.createdAt
        binding.txtInfoCreatedAt.text = if (createdAt != null) formatDate(createdAt) else "N/A"
        
        val updatedAt = device.device?.updatedAt
        binding.txtInfoUpdatedAt.text = if (updatedAt != null) formatDate(updatedAt) else "N/A"
    }

    private fun updateControlError(error: String?) {
        if (!error.isNullOrBlank() && error != lastControlError) {
            Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
            lastControlError = error
            viewModel.consumeControlError()
        }
    }

    private fun isDeviceOnline(lastSeen: String?): Boolean {
        if (lastSeen == null) return false
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = formatter.parse(lastSeen)
            val now = Date()
            val diff = now.time - (date?.time ?: 0)
            diff < TimeUnit.MINUTES.toMillis(5) // Consider online if seen within 5 minutes
        } catch (e: Exception) {
            false
        }
    }

    private fun formatTimeAgo(isoString: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = formatter.parse(isoString)
            val now = Date()
            val diff = now.time - (date?.time ?: 0)
            
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            
            when {
                seconds < 60 -> "$seconds giây trước"
                minutes < 60 -> "$minutes phút trước"
                hours < 24 -> "$hours giờ trước"
                else -> "$days ngày trước"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun formatUptime(seconds: Long): String {
        val hours = TimeUnit.SECONDS.toHours(seconds)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        return when {
            hours > 24 -> "${TimeUnit.HOURS.toDays(hours)} ngày"
            hours > 0 -> "$hours giờ $minutes phút"
            else -> "$minutes phút"
        }
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
}
