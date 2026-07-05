package com.example.safehome.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DeviceApi {

    @GET("api/user/devices")
    suspend fun getDevices(): Response<DeviceListResponse>

    @GET("api/user/devices/{id}/history")
    suspend fun getDeviceHistory(
        @Path("id") id: Int
    ): Response<DeviceHistoryResponse>

    @POST("api/user/claim")
    suspend fun claimDevice(
        @Body request: ClaimDeviceRequest
    ): Response<ClaimDeviceResponse>
}
