package com.example.safehome.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsPreferences(private val context: Context) {

    suspend fun isNotificationEnabled(): Boolean = context.safeHomeDataStore.data
        .map { preferences -> preferences[NOTIFICATION_ENABLED_KEY] ?: true }
        .first()

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.safeHomeDataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED_KEY] = enabled
        }
    }

    private companion object {
        val NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("notification_enabled")
    }
}
