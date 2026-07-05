package com.example.safehome.data.repository

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
                NotificationListResult(
                    success = false,
                    notifications = emptyList()
                )
            }
        } catch (e: Exception) {
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
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun markAsRead(id: Long): Boolean {
        return try {
            notificationApi.markAsRead(id).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAllAsRead(): Boolean {
        return try {
            notificationApi.markAllAsRead().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerFcmToken(token: String, deviceName: String? = null): Boolean {
        return try {
            fcmApi.register(
                FcmRegisterRequest(
                    token = token,
                    deviceName = deviceName
                )
            ).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unregisterFcmToken(token: String): Boolean {
        return try {
            fcmApi.unregister(FcmUnregisterRequest(token = token)).isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
