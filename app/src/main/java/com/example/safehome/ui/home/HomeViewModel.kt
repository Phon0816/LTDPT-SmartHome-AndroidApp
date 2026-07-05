package com.example.safehome.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val errorMessage: String? = null
)

class HomeViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = deviceRepository.getDevices()
                if (response.isSuccessful) {
                    val deviceList = response.body() ?: emptyList()
                    val active = deviceList.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        devices = deviceList,
                        activeDevice = active
                    )
                    if (active != null) {
                        loadDeviceHistory(active.id)
                    } else {
                        // Fallback to default ID 10 matching user example if list is empty
                        loadDeviceHistory(10)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Không thể tải danh sách thiết bị (${response.code()})"
                    )
                    // Fallback to default ID 10 history on error
                    loadDeviceHistory(10)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Lỗi kết nối thiết bị: ${e.message}"
                )
                // Fallback to default ID 10 history on exception
                loadDeviceHistory(10)
            }
        }
    }

    fun loadDeviceHistory(deviceId: Int) {
        viewModelScope.launch {
            try {
                val response = deviceRepository.getDeviceHistory(deviceId)
                if (response.isSuccessful) {
                    val history = response.body()
                    val latest = history?.data?.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        latestReading = latest,
                        errorMessage = null
                    )
                } else {
                    if (response.code() != 404) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Không thể tải lịch sử thiết bị (${response.code()})"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Lỗi tải lịch sử thiết bị: ${e.message}"
                )
            }
        }
    }
}
