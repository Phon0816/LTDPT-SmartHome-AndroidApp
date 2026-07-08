package com.example.safehome.ui.notification

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safehome.R
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.NotificationRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var btnMarkAllRead: MaterialButton
    private lateinit var progressLoading: ProgressBar
    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var txtErrorMessage: TextView
    private lateinit var txtUnreadCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        notificationViewModel = createNotificationViewModel()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
        progressLoading = findViewById(R.id.progressNotificationLoading)
        recyclerNotifications = findViewById(R.id.recyclerNotifications)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        layoutErrorState = findViewById(R.id.layoutErrorState)
        txtErrorMessage = findViewById(R.id.txtErrorMessage)
        txtUnreadCount = findViewById(R.id.txtUnreadCount)

        notificationAdapter = NotificationAdapter { item ->
            notificationViewModel.markAsRead(item)
        }

        recyclerNotifications.layoutManager = LinearLayoutManager(this)
        recyclerNotifications.layoutAnimation = AnimationUtils.loadLayoutAnimation(
            this,
            R.anim.layout_animation_notifications
        )
        recyclerNotifications.adapter = notificationAdapter

        btnMarkAllRead.setOnClickListener {
            notificationViewModel.markAllAsRead()
        }

        findViewById<MaterialButton>(R.id.btnRetry).setOnClickListener {
            notificationViewModel.loadNotifications()
        }

        lifecycleScope.launch {
            notificationViewModel.uiState.collect { state ->
                renderState(state)
            }
        }
    }

    private fun createNotificationViewModel(): NotificationViewModel {
        val tokenManager = TokenManager(applicationContext)
        val notificationApi = RetrofitClient.createNotificationApi(tokenManager)
        val fcmApi = RetrofitClient.createFcmApi(tokenManager)
        val notificationRepository = NotificationRepository(notificationApi, fcmApi)
        val factory = NotificationViewModelFactory(notificationRepository)
        return ViewModelProvider(this, factory)[NotificationViewModel::class.java]
    }

    private fun renderState(state: NotificationUiState) {
        progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        recyclerNotifications.visibility =
            if (!state.isLoading && state.notifications.isNotEmpty()) View.VISIBLE else View.GONE
        layoutEmptyState.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        layoutErrorState.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
        txtErrorMessage.text = state.errorMessage ?: "Không thể tải thông báo"
        txtUnreadCount.text = if (state.unreadCount > 0) {
            "${state.unreadCount} chưa đọc"
        } else {
            "Đã đọc hết"
        }

        btnMarkAllRead.isEnabled = state.hasUnread && !state.isLoading && !state.isRefreshing
        btnMarkAllRead.alpha = if (btnMarkAllRead.isEnabled) 1f else 0.45f

        notificationAdapter.submitNotifications(state.notifications)
        recyclerNotifications.scheduleLayoutAnimation()

        state.actionMessage?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            notificationViewModel.clearActionMessage()
        }
    }
}
