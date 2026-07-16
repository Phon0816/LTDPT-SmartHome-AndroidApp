package com.example.safehome.data.repository

import com.example.safehome.data.local.SettingsPreferences
import com.example.safehome.data.remote.UserDto
import com.example.safehome.firebase.FcmTokenManager

class SettingsRepository(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsPreferences: SettingsPreferences,
    private val fcmTokenManager: FcmTokenManager
) {
    suspend fun getCurrentUser(): UserDto? = authRepository.getCurrentUser()

    suspend fun isNotificationEnabled(): Boolean = settingsPreferences.isNotificationEnabled()

    suspend fun enableNotifications(syncFcm: Boolean): Boolean {
        settingsPreferences.setNotificationEnabled(true)
        if (!syncFcm) return true
        fcmTokenManager.syncTokenIfLoggedIn()
        return true
    }

    suspend fun disableNotifications(): Boolean {
        settingsPreferences.setNotificationEnabled(false)
        return fcmTokenManager.unregisterCurrentToken()
    }

    suspend fun getDeviceCount(): Int {
        return try {
            val response = deviceRepository.getDevices()
            if (response.isSuccessful) {
                response.body()?.devices?.size ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getUnreadCount(): Int {
        return notificationRepository.getUnreadCount()
    }

    suspend fun updateProfile(fullName: String?, avatarFile: java.io.File?): AuthActionResult {
        return authRepository.updateProfile(fullName, avatarFile)
    }

    suspend fun changePassword(oldPass: String, newPass: String): AuthActionResult {
        return authRepository.changePassword(oldPass, newPass)
    }

    suspend fun deleteAccount(): AuthActionResult {
        try {
            fcmTokenManager.unregisterCurrentToken()
        } catch (_: Exception) {}
        return authRepository.deleteAccount()
    }

    suspend fun logout() {
        try {
            fcmTokenManager.unregisterCurrentToken()
        } finally {
            authRepository.logout()
        }
    }
}
