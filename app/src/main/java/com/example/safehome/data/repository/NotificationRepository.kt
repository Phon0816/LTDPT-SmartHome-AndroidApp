package com.example.safehome.data.repository

import android.util.Log
import com.example.safehome.data.remote.FcmApi
import com.example.safehome.data.remote.FcmRegisterRequest
import com.example.safehome.data.remote.FcmUnregisterRequest
import com.example.safehome.data.remote.NotificationApi
import com.example.safehome.data.remote.NotificationDto

data class NotificationListResult(
    val success: Boolean,
    val notifications: List<NotificationDto>
)

class NotificationRepository(
    private val notificationApi: NotificationApi,
    private val fcmApi: FcmApi
) {

    companion object {
        private const val TAG = "NotificationRepo"
    }

    suspend fun getNotifications(): List<NotificationDto> {
        return getNotificationsResult().notifications
    }

    suspend fun getNotificationsResult(): NotificationListResult {
        return try {
            val response = notificationApi.getNotifications()
            if (response.isSuccessful) {
                NotificationListResult(
                    success = true,
                    notifications = response.body()?.data.orEmpty()
                )
            } else {
                Log.e(TAG, "❌ getNotifications error: HTTP ${response.code()} - ${response.message()}")
                NotificationListResult(
                    success = false,
                    notifications = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ getNotifications exception: ${e.message}", e)
            NotificationListResult(
                success = false,
                notifications = emptyList()
            )
        }
    }

    suspend fun getUnreadCount(): Int {
        return try {
            val response = notificationApi.getUnreadCount()
            if (response.isSuccessful) {
                response.body()?.data?.unreadCount ?: 0
            } else {
                Log.e(TAG, "❌ getUnreadCount error: HTTP ${response.code()} - ${response.message()}")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ getUnreadCount exception: ${e.message}", e)
            0
        }
    }

    suspend fun markAsRead(id: Long): Boolean {
        return try {
            val response = notificationApi.markAsRead(id)
            if (response.isSuccessful) {
                true
            } else {
                Log.e(TAG, "❌ markAsRead ($id) error: HTTP ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ markAsRead exception for id $id: ${e.message}", e)
            false
        }
    }

    suspend fun markAllAsRead(): Boolean {
        return try {
            val response = notificationApi.markAllAsRead()
            if (response.isSuccessful) {
                true
            } else {
                Log.e(TAG, "❌ markAllAsRead error: HTTP ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ markAllAsRead exception: ${e.message}", e)
            false
        }
    }

    suspend fun registerFcmToken(token: String, deviceName: String? = null): Boolean {
        return try {
            val response = fcmApi.register(
                FcmRegisterRequest(
                    token = token,
                    deviceName = deviceName
                )
            )
            if (response.isSuccessful) {
                true
            } else {
                Log.e(TAG, "❌ registerFcmToken error: HTTP ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ registerFcmToken exception: ${e.message}", e)
            false
        }
    }

    suspend fun unregisterFcmToken(token: String): Boolean {
        return try {
            val response = fcmApi.unregister(FcmUnregisterRequest(token = token))
            if (response.isSuccessful) {
                true
            } else {
                Log.e(TAG, "❌ unregisterFcmToken error: HTTP ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ unregisterFcmToken exception: ${e.message}", e)
            false
        }
    }
}
