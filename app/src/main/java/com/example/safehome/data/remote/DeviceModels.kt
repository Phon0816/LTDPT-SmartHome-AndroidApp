package com.example.safehome.data.remote

import com.google.gson.annotations.SerializedName

data class DeviceDto(
    val id: Int,
    val deviceCode: String?,
    val name: String?,
    val sensor: SensorDataDto?,
    val status: StatusDataDto?,
    val control: ControlDataDto?,
    val device: DeviceMetadataDto?
)

data class DeviceListResponse(
    val devices: List<DeviceDto>
)

data class ClaimDeviceRequest(
    val deviceCode: String,
    val deviceSecret: String,
    val deviceName: String
)

data class ClaimDeviceResponse(
    val message: String?,
    val data: DeviceDto?
)

data class DeviceHistoryResponse(
    val pagination: PaginationDto,
    val data: List<HistoryRecordDto>
)

data class PaginationDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

data class HistoryRecordDto(
    val id: Int,
    val sensor: SensorDataDto,
    val status: StatusDataDto,
    val control: ControlDataDto,
    val uptimeS: Long,
    val createdAt: String
)

data class SensorDataDto(
    val temperature: Double,
    val humidity: Double,
    @SerializedName(value = "mq2_raw", alternate = ["mq2Raw"])
    val mq2Raw: Int,
    @SerializedName(value = "mq135_raw", alternate = ["mq135Raw"])
    val mq135Raw: Int,
    @SerializedName(value = "flame_detected", alternate = ["flameDetected"])
    val flameDetected: Boolean
)

data class StatusDataDto(
    val temperature: String,
    val humidity: String,
    val mq2: String,
    val mq135: String,
    val flame: String,
    val system: String
)

data class ControlDataDto(
    @SerializedName(value = "buzzer_active", alternate = ["buzzerActive"])
    val buzzerActive: Boolean,
    @SerializedName(value = "buzzer_muted", alternate = ["buzzerMuted"])
    val buzzerMuted: Boolean,
    val led1: Boolean,
    val led2: Boolean,
    val led3: Boolean,
    val led4: Boolean,
    val led5: Boolean
)

data class DeviceMetadataDto(
    @SerializedName(value = "uptime_s", alternate = ["uptimeS"])
    val uptimeS: Long?,
    @SerializedName(value = "last_seen", alternate = ["lastSeen"])
    val lastSeen: String?,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String?,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"])
    val updatedAt: String?
)
