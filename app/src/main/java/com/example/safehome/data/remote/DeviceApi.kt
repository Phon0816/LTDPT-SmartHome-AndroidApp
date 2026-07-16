package com.example.safehome.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DeviceApi {

    @GET("api/user/devices")
    suspend fun getDevices(): Response<DeviceListResponse>

    @GET("api/user/devices/{id}")
    suspend fun getDeviceDetail(
        @Path("id") id: Int
    ): Response<DeviceDetailResponse>

    @GET("api/user/devices/{id}/history")
    suspend fun getDeviceHistory(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Response<DeviceHistoryResponse>

    @POST("api/user/claim")
    suspend fun claimDevice(
        @Body request: ClaimDeviceRequest
    ): Response<ClaimDeviceResponse>

    @POST("api/user/devices/{id}/control")
    suspend fun controlDevice(
        @Path("id") id: Int,
        @Body request: Map<String, Boolean>
    ): Response<okhttp3.ResponseBody>
}
