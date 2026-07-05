package com.example.safehome.data.repository

import com.example.safehome.data.remote.DeviceApi
import com.example.safehome.data.remote.DeviceDto
import com.example.safehome.data.remote.DeviceHistoryResponse
import retrofit2.Response

class DeviceRepository(
    private val deviceApi: DeviceApi
) {
    suspend fun getDevices(): Response<List<DeviceDto>> {
        return deviceApi.getDevices()
    }

    suspend fun getDeviceHistory(id: Int): Response<DeviceHistoryResponse> {
        return deviceApi.getDeviceHistory(id)
    }
}
