package com.example.safehome.firebase

import android.util.Log
import com.example.safehome.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SafeHomeFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        serviceScope.launch {
            FcmTokenManager(applicationContext).registerTokenIfLoggedIn(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title = message.notification?.title
            ?: data["title"]
            ?: "Thông báo SafeHome"
        val body = message.notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: ""

        Log.d(TAG, "FCM message received: $data")

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            notificationId = data["notificationId"],
            type = data["type"],
            deviceId = data["deviceId"],
            screen = data["screen"]
        )
    }

    companion object {
        private const val TAG = "SafeHomeFCM"
    }
}
