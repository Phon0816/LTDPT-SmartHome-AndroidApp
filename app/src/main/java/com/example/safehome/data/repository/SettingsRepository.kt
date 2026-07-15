package com.example.safehome.data.repository

import com.example.safehome.data.local.SettingsPreferences
import com.example.safehome.data.remote.UserDto
import com.example.safehome.firebase.FcmTokenManager

class SettingsRepository(
    private val authRepository: AuthRepository,
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

    suspend fun logout() {
        try {
            fcmTokenManager.unregisterCurrentToken()
        } finally {
            authRepository.logout()
        }
    }
}
