package com.example.safehome.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    }

    suspend fun saveAccessToken(accessToken: String) {
        context.safeHomeDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
        }
    }

    suspend fun getAccessToken(): String? {
        return context.safeHomeDataStore.data
            .map { prefs -> prefs[ACCESS_TOKEN_KEY] }
            .first()
    }

    suspend fun clearAccessToken() {
        context.safeHomeDataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
        }
    }
}
