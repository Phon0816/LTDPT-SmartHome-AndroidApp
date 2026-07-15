package com.example.safehome.firebase

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.local.SettingsPreferences
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine

class FcmTokenManager(
    private val context: Context
) {

    private val tokenManager = TokenManager(context.applicationContext)
    private val settingsPreferences = SettingsPreferences(context.applicationContext)
    private val notificationRepository: NotificationRepository by lazy {
        val notificationApi = RetrofitClient.createNotificationApi(tokenManager)
        val fcmApi = RetrofitClient.createFcmApi(tokenManager)
        NotificationRepository(notificationApi, fcmApi)
    }

    suspend fun syncTokenIfLoggedIn() {
        if (!settingsPreferences.isNotificationEnabled()) {
            Log.d(TAG, "Notifications are disabled, skip FCM sync")
            return
        }
        val token = getFirebaseTokenOrNull()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "FCM token is empty, skip register")
            return
        }

        registerTokenIfLoggedIn(token)
    }

    suspend fun registerTokenIfLoggedIn(token: String) {
        if (!settingsPreferences.isNotificationEnabled()) {
            Log.d(TAG, "Notifications are disabled, skip FCM register")
            return
        }
        val accessToken = tokenManager.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            Log.d(TAG, "FCM token available but user is not logged in yet")
            return
        }

        val success = notificationRepository.registerFcmToken(
            token = token,
            deviceName = buildDeviceName()
        )

        if (success) {
            Log.d(TAG, "Registered FCM token successfully")
        } else {
            Log.w(TAG, "Register FCM token failed")
        }
    }

    suspend fun unregisterToken(token: String): Boolean {
        return try {
            notificationRepository.unregisterFcmToken(token)
        } catch (e: Exception) {
            Log.w(TAG, "Unregister FCM token failed", e)
            false
        }
    }

    suspend fun unregisterCurrentToken(): Boolean {
        val token = getFirebaseTokenOrNull()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "FCM token is empty, skip unregister")
            return false
        }
        return unregisterToken(token)
    }

    private suspend fun getFirebaseTokenOrNull(): String? {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        Log.d(TAG, "FCM token: $token")
                        continuation.resume(token)
                    }
                    .addOnFailureListener { error ->
                        Log.w(TAG, "Get FCM token failed", error)
                        continuation.resume(null)
                    }
            }
        }
    }

    private fun buildDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty()
            .replaceFirstChar { it.titlecase() }
        val model = Build.MODEL.orEmpty()
        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { "Android device" }
    }

    companion object {
        private const val TAG = "FcmTokenManager"
    }
}
