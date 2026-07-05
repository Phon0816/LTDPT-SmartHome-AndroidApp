package com.example.safehome.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safehome.data.remote.NotificationDto
import com.example.safehome.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val errorMessage: String? = null,
    val actionMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && notifications.isEmpty() && errorMessage == null

    val hasUnread: Boolean
        get() = notifications.any { !it.isRead }
}

class NotificationViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = _uiState.value.notifications.isEmpty(),
                isRefreshing = _uiState.value.notifications.isNotEmpty(),
                errorMessage = null,
                actionMessage = null
            )

            val result = notificationRepository.getNotificationsResult()
            val notifications = result.notifications.map { it.toNotificationItem() }

            _uiState.value = NotificationUiState(
                isLoading = false,
                isRefreshing = false,
                notifications = notifications,
                errorMessage = if (!result.success && notifications.isEmpty()) {
                    "Không thể tải thông báo"
                } else {
                    null
                }
            )
        }
    }

    fun markAsRead(item: NotificationItem) {
        val notificationId = item.id ?: return
        if (item.isRead) return

        viewModelScope.launch {
            val success = notificationRepository.markAsRead(notificationId)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    notifications = _uiState.value.notifications.map { current ->
                        if (current.id == notificationId) current.copy(isRead = true) else current
                    },
                    actionMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    actionMessage = "Không thể đánh dấu thông báo đã đọc"
                )
            }
        }
    }

    fun markAllAsRead() {
        if (!_uiState.value.hasUnread) return

        viewModelScope.launch {
            val success = notificationRepository.markAllAsRead()
            if (success) {
                _uiState.value = _uiState.value.copy(
                    notifications = _uiState.value.notifications.map { it.copy(isRead = true) },
                    actionMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    actionMessage = "Không thể đánh dấu tất cả đã đọc"
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    private fun NotificationDto.toNotificationItem(): NotificationItem {
        return NotificationItem(
            id = id,
            title = title?.takeIf { it.isNotBlank() } ?: "Thông báo SafeHome",
            body = body.orEmpty(),
            type = NotificationType.from(type),
            isRead = isRead ?: false,
            createdAt = createdAt.orEmpty()
        )
    }
}
