package com.example.safehome.data.repository

import com.example.safehome.data.remote.ClaimDeviceRequest
import com.example.safehome.data.remote.ClaimDeviceResponse
import com.example.safehome.data.remote.DeviceApi
import com.example.safehome.data.remote.DeviceDetailResponse
import com.example.safehome.data.remote.DeviceHistoryResponse
import com.example.safehome.data.remote.DeviceListResponse
import retrofit2.Response

class DeviceRepository(
    private val deviceApi: DeviceApi
) {
    suspend fun getDevices(): Response<DeviceListResponse> {
        return deviceApi.getDevices()
    }

    suspend fun getDeviceDetail(id: Int): Response<DeviceDetailResponse> {
        return deviceApi.getDeviceDetail(id)
    }

    suspend fun getDeviceHistory(id: Int, page: Int = 1, limit: Int = 100): Response<DeviceHistoryResponse> {
        return deviceApi.getDeviceHistory(id, page, limit)
    }

    suspend fun claimDevice(code: String, secret: String, name: String): Response<ClaimDeviceResponse> {
        return deviceApi.claimDevice(ClaimDeviceRequest(code, secret, name))
    }

    suspend fun controlDevice(id: Int, payload: Map<String, Boolean>): Response<okhttp3.ResponseBody> {
        return deviceApi.controlDevice(id, payload)
    }
}
