package com.example.safehome.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safehome.data.remote.UserDto
import com.example.safehome.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val user: UserDto? = null,
    val notificationEnabled: Boolean = true,
    val isUpdatingNotifications: Boolean = false,
    val isLoggingOut: Boolean = false,
    val errorMessage: String? = null,
    val logoutCompleted: Boolean = false
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val user = repository.getCurrentUser()
            val enabled = repository.isNotificationEnabled()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                user = user,
                notificationEnabled = enabled,
                errorMessage = if (user == null) "Không thể tải thông tin tài khoản" else null
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean, canSyncFcm: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingNotifications = true, errorMessage = null)
            val unregisterSucceeded = if (enabled) {
                repository.enableNotifications(canSyncFcm)
            } else {
                repository.disableNotifications()
            }
            _uiState.value = _uiState.value.copy(
                notificationEnabled = enabled,
                isUpdatingNotifications = false,
                errorMessage = if (!enabled && !unregisterSucceeded) "Đã lưu cài đặt, nhưng chưa thể hủy đăng ký thiết bị" else null
            )
        }
    }

    fun syncNotificationsAfterPermissionGranted() {
        if (_uiState.value.notificationEnabled) {
            setNotificationsEnabled(true, canSyncFcm = true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true, errorMessage = null)
            repository.logout()
            _uiState.value = _uiState.value.copy(isLoggingOut = false, logoutCompleted = true)
        }
    }
}
