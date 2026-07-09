package com.example.safehome.data.remote

data class NotificationDto(
    val id: Long? = null,
    val userId: Long? = null,
    val deviceId: Long? = null,
    val title: String? = null,
    val body: String? = null,
    val type: String? = null,
    val screen: String? = null,
    val data: NotificationPayloadDto? = null,
    val isRead: Boolean? = null,
    val readAt: String? = null,
    val createdAt: String? = null,
    val device: NotificationDeviceDto? = null
)

data class NotificationPayloadDto(
    val type: String? = null,
    val screen: String? = null,
    val deviceId: String? = null,
    val deviceCode: String? = null,
    val reasons: List<NotificationReasonDto>? = null
)

data class NotificationReasonDto(
    val key: String? = null,
    val title: String? = null,
    val body: String? = null
)

data class NotificationDeviceDto(
    val id: Long? = null,
    val deviceCode: String? = null,
    val name: String? = null
)

data class NotificationsResponse(
    val data: List<NotificationDto>? = null
)

data class UnreadCountResponse(
    val data: UnreadCountData? = null
)

data class UnreadCountData(
    val unreadCount: Int? = null
)

data class MarkReadResponse(
    val message: String? = null,
    val data: MarkReadData? = null
)

data class MarkReadData(
    val id: Long? = null,
    val isRead: Boolean? = null,
    val readAt: String? = null,
    val createdAt: String? = null
)

data class MarkAllReadResponse(
    val message: String? = null,
    val data: MarkAllReadData? = null
)

data class MarkAllReadData(
    val updatedCount: Int? = null
)

data class FcmRegisterRequest(
    val token: String,
    val platform: String = "android",
    val deviceName: String? = null
)

data class FcmUnregisterRequest(
    val token: String
)

data class FcmRegisterResponse(
    val message: String? = null,
    val data: FcmTokenData? = null
)

data class FcmUnregisterResponse(
    val message: String? = null,
    val data: FcmUnregisterData? = null
)

data class FcmTokenData(
    val id: Long? = null,
    val token: String? = null,
    val platform: String? = null,
    val deviceName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class FcmUnregisterData(
    val removed: Int? = null
)
