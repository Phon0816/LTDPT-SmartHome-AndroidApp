package com.example.safehome.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationApi {

    @GET("api/notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    @PATCH("api/notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: Long
    ): Response<MarkReadResponse>

    @PATCH("api/notifications/read-all")
    suspend fun markAllAsRead(): Response<MarkAllReadResponse>
}
