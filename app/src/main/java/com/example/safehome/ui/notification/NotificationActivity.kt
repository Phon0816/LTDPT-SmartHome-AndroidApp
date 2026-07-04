package com.example.safehome.ui.notification

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.safehome.R

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val listNotifications = findViewById<ListView>(R.id.listNotifications)

        btnBack.setOnClickListener {
            finish()
        }

        listNotifications.adapter = NotificationAdapter(this, createMockNotifications())
    }

    private fun createMockNotifications(): List<NotificationItem> {
        return listOf(
            NotificationItem(
                id = 1L,
                title = "He thong hoat dong on dinh",
                message = "SafeHome dang giam sat binh thuong. Khong co canh bao moi tu cac thiet bi.",
                type = NotificationType.INFO,
                isRead = true,
                createdAt = "Hom nay, 08:15"
            ),
            NotificationItem(
                id = 2L,
                title = "Nhiet do phong khach tang",
                message = "Cam bien ghi nhan nhiet do cao hon nguong thuong ngay. Hay kiem tra khu vuc phong khach.",
                type = NotificationType.WARNING,
                isRead = false,
                createdAt = "Hom nay, 09:40"
            ),
            NotificationItem(
                id = 3L,
                title = "Canh bao khi gas",
                message = "Nong do khi gas vuot nguong an toan tai khu vuc bep. Vui long kiem tra ngay.",
                type = NotificationType.DANGER,
                isRead = false,
                createdAt = "Hom nay, 10:05"
            )
        )
    }
}
