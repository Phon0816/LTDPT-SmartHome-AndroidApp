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
    val logoutCompleted: Boolean = false,
    val deviceCount: Int = 0,
    val unreadNotificationsCount: Int = 0,
    val isUpdatingProfile: Boolean = false,
    val profileUpdateSuccessMessage: String? = null,
    val passwordChangeSuccessMessage: String? = null,
    val isDeletingAccount: Boolean = false,
    val accountDeleteCompleted: Boolean = false
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, profileUpdateSuccessMessage = null, passwordChangeSuccessMessage = null)
            val user = repository.getCurrentUser()
            val enabled = repository.isNotificationEnabled()
            val deviceCount = repository.getDeviceCount()
            val unreadCount = repository.getUnreadCount()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                user = user,
                notificationEnabled = enabled,
                deviceCount = deviceCount,
                unreadNotificationsCount = unreadCount,
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

    fun updateProfile(fullName: String?, avatarFile: java.io.File?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingProfile = true, errorMessage = null, profileUpdateSuccessMessage = null)
            val result = repository.updateProfile(fullName, avatarFile)
            if (result.success) {
                val user = repository.getCurrentUser()
                _uiState.value = _uiState.value.copy(
                    isUpdatingProfile = false,
                    user = user,
                    profileUpdateSuccessMessage = result.message
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isUpdatingProfile = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, passwordChangeSuccessMessage = null)
            val result = repository.changePassword(oldPass, newPass)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    passwordChangeSuccessMessage = result.message
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAccount = true, errorMessage = null)
            val result = repository.deleteAccount()
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    accountDeleteCompleted = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true, errorMessage = null)
            repository.logout()
            _uiState.value = _uiState.value.copy(isLoggingOut = false, logoutCompleted = true)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            profileUpdateSuccessMessage = null,
            passwordChangeSuccessMessage = null
        )
    }
}
