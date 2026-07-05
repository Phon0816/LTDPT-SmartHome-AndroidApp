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
    val hasNoDevices: Boolean = false
)

class HomeViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadDevices() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "📡 Calling getDevices...")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = deviceRepository.getDevices()
                Log.d("HomeViewModel", "✅ getDevices response: ${response.code()} body=${response.body()}")
                if (response.isSuccessful) {
                    val deviceList = response.body()?.data ?: emptyList()
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
        Log.d("HomeViewModel", "🔄 Starting poll for deviceId=$deviceId")
        historyPollJob = viewModelScope.launch {
            while (true) {
                try {
                    Log.d("HomeViewModel", "📡 Calling getDeviceHistory(id=$deviceId)...")
                    val response = deviceRepository.getDeviceHistory(deviceId)
                    Log.d("HomeViewModel", "✅ history response: ${response.code()} data=${response.body()?.data?.size} items")
                    if (response.isSuccessful) {
                        val history = response.body()
                        val latest = history?.data?.firstOrNull()
                        Log.d("HomeViewModel", "🎯 Latest reading: temp=${latest?.sensor?.temperature} humid=${latest?.sensor?.humidity}")
                        _uiState.value = _uiState.value.copy(
                            latestReading = latest,
                            errorMessage = null
                        )
                    } else {
                        Log.e("HomeViewModel", "❌ history error: ${response.code()} ${response.errorBody()?.string()}")
                        if (response.code() != 404) {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Không thể tải lịch sử thiết bị (${response.code()})"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "💥 history exception: ${e.message}")
                }
                kotlinx.coroutines.delay(3000) // Polling tự động mỗi 3 giây
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
                        400 -> "Mã thiết bị hoặc mật khẩu không đúng"
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
}
