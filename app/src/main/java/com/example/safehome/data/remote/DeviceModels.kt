package com.example.safehome.data.remote

data class DeviceDto(
    val id: Int,
    val name: String?,
    val serialNumber: String?,
    val status: String?,
    val createdAt: String?
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
    val mq2Raw: Int,
    val mq135Raw: Int,
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
    val buzzerActive: Boolean,
    val buzzerMuted: Boolean,
    val led1: Boolean,
    val led2: Boolean,
    val led3: Boolean,
    val led4: Boolean,
    val led5: Boolean
)
