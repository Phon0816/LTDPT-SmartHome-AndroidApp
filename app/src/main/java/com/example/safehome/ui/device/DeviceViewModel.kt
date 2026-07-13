package com.example.safehome.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safehome.data.remote.DeviceDto
import com.example.safehome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceUiState(
    val isLoading: Boolean = false,
    val devices: List<DeviceDto> = emptyList(),
    val isClaiming: Boolean = false,
    val claimSuccess: Boolean = false,
    val claimError: String? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && devices.isEmpty() && errorMessage == null
}

class DeviceViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceUiState(isLoading = true))
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = deviceRepository.getDevices()
                if (response.isSuccessful) {
                    val devices = response.body()?.devices.orEmpty()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        devices = devices,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        devices = emptyList(),
                        errorMessage = "Không thể tải danh sách thiết bị (${response.code()})"
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    devices = emptyList(),
                    errorMessage = "Lỗi kết nối. Vui lòng thử lại"
                )
            }
        }
    }

    fun claimDevice(deviceCode: String, deviceSecret: String, deviceName: String) {
        val code = deviceCode.trim()
        val secret = deviceSecret.trim()
        val name = deviceName.trim()

        if (code.isEmpty() || secret.isEmpty() || name.isEmpty()) {
            _uiState.value = _uiState.value.copy(claimError = "Vui lòng nhập đầy đủ thông tin thiết bị")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClaiming = true, claimError = null, claimSuccess = false)
            try {
                val response = deviceRepository.claimDevice(code, secret, name)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isClaiming = false,
                        claimSuccess = true,
                        claimError = null
                    )
                    loadDevices()
                } else {
                    val claimMessage = when (response.code()) {
                        400, 401 -> "Mã thiết bị hoặc mật khẩu không đúng"
                        404 -> "Không tìm thấy thiết bị"
                        409 -> "Thiết bị đã được liên kết"
                        else -> "Liên kết thiết bị thất bại (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(
                        isClaiming = false,
                        claimError = claimMessage
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClaiming = false,
                    claimError = "Lỗi kết nối. Vui lòng thử lại"
                )
            }
        }
    }

    fun consumeClaimSuccess() {
        _uiState.value = _uiState.value.copy(claimSuccess = false)
    }

    fun consumeClaimError() {
        _uiState.value = _uiState.value.copy(claimError = null)
    }
}
