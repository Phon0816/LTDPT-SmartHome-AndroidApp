package com.example.safehome.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface DeviceApi {

    @GET("api/user/devices")
    suspend fun getDevices(): Response<List<DeviceDto>>

    @GET("api/user/devices/{id}/history")
    suspend fun getDeviceHistory(
        @Path("id") id: Int
    ): Response<DeviceHistoryResponse>
}
