package com.example.safehome.ui.notification

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.R

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val notificationList = findViewById<RecyclerView>(R.id.recyclerNotifications)
        notificationList.layoutManager = LinearLayoutManager(this)
        notificationList.adapter = NotificationAdapter(createMockNotifications())
    }

    private fun createMockNotifications(): List<NotificationItem> {
        return listOf(
            NotificationItem(
                id = 1L,
                title = "Hệ thống hoạt động ổn định",
                message = "SafeHome đang giám sát bình thường. Không có cảnh báo mới từ các thiết bị.",
                type = NotificationType.INFO,
                isRead = true,
                createdAt = "Hôm nay, 08:15"
            ),
            NotificationItem(
                id = 2L,
                title = "Nhiệt độ phòng khách tăng",
                message = "Cảm biến ghi nhận nhiệt độ cao hơn ngưỡng thường ngày. Hãy kiểm tra khu vực phòng khách.",
                type = NotificationType.WARNING,
                isRead = false,
                createdAt = "Hôm nay, 09:40"
            ),
            NotificationItem(
                id = 3L,
                title = "Cảnh báo khí gas",
                message = "Nồng độ khí gas vượt ngưỡng an toàn tại khu vực bếp. Vui lòng kiểm tra ngay.",
                type = NotificationType.DANGER,
                isRead = false,
                createdAt = "Hôm nay, 10:05"
            )
        )
    }
}
