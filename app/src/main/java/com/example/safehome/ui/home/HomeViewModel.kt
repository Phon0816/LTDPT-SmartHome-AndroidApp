package com.example.safehome.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.safehome.data.remote.DeviceDto
import com.example.safehome.data.remote.HistoryRecordDto
import com.example.safehome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val devices: List<DeviceDto> = emptyList(),
    val activeDevice: DeviceDto? = null,
    val latestReading: HistoryRecordDto? = null,
    val errorMessage: String? = null,
    // Claim device state
    val isClaiming: Boolean = false,
    val claimSuccess: Boolean = false,
    val claimError: String? = null,
    val hasNoDevices: Boolean = false,
    val lastPollTime: Long = 0L
)

class HomeViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Cờ tạm dừng polling khi đang gửi lệnh điều khiển đèn
    @Volatile
    private var isControlling = false

    fun loadDevices() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "📡 Calling getDevices...")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = deviceRepository.getDevices()
                Log.d("HomeViewModel", "✅ getDevices response: ${response.code()} body=${response.body()}")
                if (response.isSuccessful) {
                    val deviceList = response.body()?.devices ?: emptyList()
                    val active = deviceList.firstOrNull()
                    Log.d("HomeViewModel", "📱 Active device: $active")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        devices = deviceList,
                        activeDevice = active
                    )
                    if (active != null) {
                        loadDeviceHistory(active.id)
                    } else {
                        Log.w("HomeViewModel", "⚠️ Không có thiết bị nào được trả về từ server")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasNoDevices = true
                        )
                    }
                } else {
                    Log.e("HomeViewModel", "❌ getDevices error: ${response.code()} ${response.errorBody()?.string()}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Không thể tải danh sách thiết bị (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "💥 getDevices exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Lỗi kết nối: ${e.message}"
                )
            }
        }
    }

    private var historyPollJob: kotlinx.coroutines.Job? = null

    fun loadDeviceHistory(deviceId: Int) {
        historyPollJob?.cancel()
        Log.d("HomeViewModel", "🔄 Starting device poll (via getDevices) for deviceId=$deviceId")
        historyPollJob = viewModelScope.launch {
            while (true) {
                if (!isControlling) { // Bỏ qua nếu đang gửi lệnh điều khiển
                    try {
                        Log.d("HomeViewModel", "📡 Polling getDevices for fresh sensor data...")
                        val response = deviceRepository.getDevices()
                        if (response.isSuccessful) {
                            val deviceList = response.body()?.devices ?: emptyList()
                            val updatedDevice = deviceList.firstOrNull { it.id == deviceId }
                            if (updatedDevice != null) {
                                Log.d("HomeViewModel", "🎯 Poll: temp=${updatedDevice.sensor?.temperature} humid=${updatedDevice.sensor?.humidity}")
                                _uiState.value = _uiState.value.copy(
                                    devices = deviceList,
                                    activeDevice = updatedDevice,
                                    lastPollTime = System.currentTimeMillis(),
                                    errorMessage = null
                                )
                            }
                        } else {
                            Log.e("HomeViewModel", "❌ poll error: ${response.code()}")
                            _uiState.value = _uiState.value.copy(lastPollTime = System.currentTimeMillis())
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "💥 poll exception: ${e.message}")
                        _uiState.value = _uiState.value.copy(lastPollTime = System.currentTimeMillis())
                    }
                }
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        historyPollJob?.cancel()
    }

    fun claimDevice(deviceCode: String, deviceSecret: String, deviceName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClaiming = true, claimError = null, claimSuccess = false)
            try {
                val response = deviceRepository.claimDevice(deviceCode.trim(), deviceSecret.trim(), deviceName.trim())
                if (response.isSuccessful) {
                    Log.d("HomeViewModel", "🎉 Claim success: ${response.body()?.message}")
                    _uiState.value = _uiState.value.copy(
                        isClaiming = false,
                        claimSuccess = true,
                        claimError = null,
                        hasNoDevices = false
                    )
                    // Tự động tải lại danh sách thiết bị sau khi claim thành công
                    loadDevices()
                } else {
                    val errBody = response.errorBody()?.string()
                    Log.e("HomeViewModel", "❌ Claim error: ${response.code()} $errBody")
                    val msg = when (response.code()) {
                        400, 401 -> "Mã thiết bị hoặc mật khẩu không đúng"
                        409 -> "Thiết bị đã được liên kết với tài khoản khác"
                        404 -> "Không tìm thấy thiết bị với mã này"
                        else -> "Thất bại (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(isClaiming = false, claimError = msg)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "💥 Claim exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isClaiming = false,
                    claimError = "Lỗi kết nối: ${e.message}"
                )
            }
        }
    }

    fun resetClaimState() {
        _uiState.value = _uiState.value.copy(claimSuccess = false, claimError = null)
    }

    fun controlDeviceLed(deviceId: Int, ledKey: String, state: Boolean) {
        val oldDevices = _uiState.value.devices

        // 1. UPDATE STATE IMMEDIATELY (True Optimistic Update)
        val updatedDevices = _uiState.value.devices.map { device ->
            if (device.id == deviceId) {
                val currentControl = device.control
                val updatedControl = when (ledKey) {
                    "led1" -> currentControl?.copy(led1 = state)
                    "led2" -> currentControl?.copy(led2 = state)
                    "led3" -> currentControl?.copy(led3 = state)
                    "led4" -> currentControl?.copy(led4 = state)
                    "led5" -> currentControl?.copy(led5 = state)
                    else -> currentControl
                }
                device.copy(control = updatedControl)
            } else {
                device
            }
        }
        val updatedActive = updatedDevices.firstOrNull { it.id == _uiState.value.activeDevice?.id }
        _uiState.value = _uiState.value.copy(
            devices = updatedDevices,
            activeDevice = updatedActive ?: _uiState.value.activeDevice
        )

        // 2. Gửi lệnh xuống ESP32 qua Backend
        viewModelScope.launch {
            isControlling = true // Dừng polling, tránh ghi đè UI
            try {
                Log.d("HomeViewModel", "💡 Toggling LED: deviceId=$deviceId key=$ledKey state=$state")
                val response = deviceRepository.controlDevice(deviceId, mapOf(ledKey to state))
                if (response.isSuccessful) {
                    Log.d("HomeViewModel", "✅ Control LED success: ${response.code()}")
                } else {
                    Log.e("HomeViewModel", "❌ Control LED error: ${response.code()}, reverting state")
                    revertDeviceState(oldDevices)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "💥 Control LED exception: ${e.message}, reverting state")
                revertDeviceState(oldDevices)
            } finally {
                kotlinx.coroutines.delay(4000) // Chờ ESP32 cập nhật xong rồi mới poll lại
                isControlling = false
            }
        }
    }

    private fun revertDeviceState(oldDevices: List<DeviceDto>) {
        val updatedActive = oldDevices.firstOrNull { it.id == _uiState.value.activeDevice?.id }
        _uiState.value = _uiState.value.copy(
            devices = oldDevices,
            activeDevice = updatedActive ?: _uiState.value.activeDevice
        )
    }
}
