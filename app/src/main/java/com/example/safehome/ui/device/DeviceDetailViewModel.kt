package com.example.safehome.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safehome.data.remote.DeviceDto
import com.example.safehome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceDetailUiState(
    val isLoading: Boolean = false,
    val device: DeviceDto? = null,
    val isControlling: Boolean = false,
    val errorMessage: String? = null,
    val controlError: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && device == null && errorMessage == null
}

class DeviceDetailViewModel(
    private val deviceRepository: DeviceRepository,
    private val deviceId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceDetailUiState(isLoading = true))
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    init {
        loadDeviceDetail()
    }

    fun loadDeviceDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = deviceRepository.getDeviceDetail(deviceId)
                if (response.isSuccessful) {
                    val device = response.body()?.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        device = device,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        device = null,
                        errorMessage = "Không thể tải dữ liệu thiết bị (${response.code()})"
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    device = null,
                    errorMessage = "Lỗi kết nối. Vui lòng thử lại"
                )
            }
        }
    }

    fun controlDevice(field: String, value: Boolean, previousValue: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isControlling = true, controlError = null)
            
            // Optimistic update
            val currentDevice = _uiState.value.device
            if (currentDevice != null) {
                val updatedControl = when (field) {
                    "led1" -> currentDevice.control?.copy(led1 = value)
                    "led2" -> currentDevice.control?.copy(led2 = value)
                    "led3" -> currentDevice.control?.copy(led3 = value)
                    "led4" -> currentDevice.control?.copy(led4 = value)
                    "led5" -> currentDevice.control?.copy(led5 = value)
                    "buzzerMuted" -> currentDevice.control?.copy(buzzerMuted = value)
                    else -> currentDevice.control
                }
                _uiState.value = _uiState.value.copy(
                    device = currentDevice.copy(control = updatedControl)
                )
            }

            try {
                val response = deviceRepository.controlDevice(deviceId, mapOf(field to value))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isControlling = false)
                } else {
                    // Revert on failure
                    val revertedDevice = _uiState.value.device
                    if (revertedDevice != null) {
                        val revertedControl = when (field) {
                            "led1" -> revertedDevice.control?.copy(led1 = previousValue)
                            "led2" -> revertedDevice.control?.copy(led2 = previousValue)
                            "led3" -> revertedDevice.control?.copy(led3 = previousValue)
                            "led4" -> revertedDevice.control?.copy(led4 = previousValue)
                            "led5" -> revertedDevice.control?.copy(led5 = previousValue)
                            "buzzerMuted" -> revertedDevice.control?.copy(buzzerMuted = previousValue)
                            else -> revertedDevice.control
                        }
                        _uiState.value = _uiState.value.copy(
                            device = revertedDevice.copy(control = revertedControl),
                            isControlling = false,
                            controlError = "Điều khiển thất bại (${response.code()})"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isControlling = false,
                            controlError = "Điều khiển thất bại (${response.code()})"
                        )
                    }
                }
            } catch (_: Exception) {
                // Revert on error
                val revertedDevice = _uiState.value.device
                if (revertedDevice != null) {
                    val revertedControl = when (field) {
                        "led1" -> revertedDevice.control?.copy(led1 = previousValue)
                        "led2" -> revertedDevice.control?.copy(led2 = previousValue)
                        "led3" -> revertedDevice.control?.copy(led3 = previousValue)
                        "led4" -> revertedDevice.control?.copy(led4 = previousValue)
                        "led5" -> revertedDevice.control?.copy(led5 = previousValue)
                        "buzzerMuted" -> revertedDevice.control?.copy(buzzerMuted = previousValue)
                        else -> revertedDevice.control
                    }
                    _uiState.value = _uiState.value.copy(
                        device = revertedDevice.copy(control = revertedControl),
                        isControlling = false,
                        controlError = "Lỗi kết nối. Vui lòng thử lại"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isControlling = false,
                        controlError = "Lỗi kết nối. Vui lòng thử lại"
                    )
                }
            }
        }
    }

    fun consumeControlError() {
        _uiState.value = _uiState.value.copy(controlError = null)
    }
}
