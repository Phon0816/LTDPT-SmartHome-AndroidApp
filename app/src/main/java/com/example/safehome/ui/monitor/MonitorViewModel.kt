package com.example.safehome.ui.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safehome.data.remote.DeviceDto
import com.example.safehome.data.remote.HistoryRecordDto
import com.example.safehome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class GasTab {
    MQ2, MQ135
}

enum class TimeFilter {
    HOURS_24, DAYS_7, DAYS_30
}

data class MonitorUiState(
    val isLoading: Boolean = false,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val history: List<HistoryRecordDto> = emptyList(),
    val selectedGasTab: GasTab = GasTab.MQ2,
    val selectedTimeFilter: TimeFilter = TimeFilter.DAYS_7,
    val errorMessage: String? = null
) {
    val hasNoDevice: Boolean
        get() = deviceId == null
    
    val isEmpty: Boolean
        get() = !isLoading && history.isEmpty() && errorMessage == null
}

class MonitorViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private var fullHistory: List<HistoryRecordDto> = emptyList()

    fun selectDevice(deviceId: String, deviceName: String?) {
        _uiState.value = _uiState.value.copy(
            deviceId = deviceId,
            deviceName = deviceName
        )
        loadDeviceHistory(deviceId)
    }

    fun selectGasTab(tab: GasTab) {
        _uiState.value = _uiState.value.copy(selectedGasTab = tab)
    }

    fun selectTimeFilter(filter: TimeFilter) {
        _uiState.value = _uiState.value.copy(selectedTimeFilter = filter)
        applyTimeFilter()
    }

    private fun loadDeviceHistory(deviceId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val deviceIdInt = deviceId.toIntOrNull() ?: return@launch
                val response = deviceRepository.getDeviceHistory(deviceIdInt, 1, 500)
                
                if (response.isSuccessful) {
                    fullHistory = response.body()?.data.orEmpty()
                    applyTimeFilter()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Không thể tải dữ liệu lịch sử (Code: ${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Lỗi kết nối: ${e.message}"
                )
            }
        }
    }

    private fun applyTimeFilter() {
        val now = Instant.now()
        val cutoffTime = when (_uiState.value.selectedTimeFilter) {
            TimeFilter.HOURS_24 -> now.minus(24, ChronoUnit.HOURS)
            TimeFilter.DAYS_7 -> now.minus(7, ChronoUnit.DAYS)
            TimeFilter.DAYS_30 -> now.minus(30, ChronoUnit.DAYS)
        }

        val filteredHistory = fullHistory.filter { record ->
            try {
                val recordTime = Instant.parse(record.createdAt)
                recordTime.isAfter(cutoffTime)
            } catch (e: Exception) {
                true
            }
        }

        _uiState.value = _uiState.value.copy(history = filteredHistory)
    }

    fun refresh() {
        val deviceId = _uiState.value.deviceId
        if (deviceId != null) {
            loadDeviceHistory(deviceId)
        }
    }

    fun clearDeviceSelection() {
        _uiState.value = _uiState.value.copy(
            deviceId = null,
            deviceName = null,
            history = emptyList()
        )
    }
}
