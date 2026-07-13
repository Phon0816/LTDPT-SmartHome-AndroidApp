package com.example.safehome.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safehome.data.remote.HistoryRecordDto
import com.example.safehome.data.remote.PaginationDto
import com.example.safehome.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceHistoryUiState(
    val isLoading: Boolean = false,
    val history: List<HistoryRecordDto> = emptyList(),
    val pagination: PaginationDto? = null,
    val currentPage: Int = 1,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && history.isEmpty() && errorMessage == null
}

class DeviceHistoryViewModel(
    private val deviceRepository: DeviceRepository,
    private val deviceId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceHistoryUiState(isLoading = true))
    val uiState: StateFlow<DeviceHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = deviceRepository.getDeviceHistory(deviceId, page, 100)
                if (response.isSuccessful) {
                    val historyResponse = response.body()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        history = historyResponse?.data.orEmpty(),
                        pagination = historyResponse?.pagination,
                        currentPage = page,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        history = emptyList(),
                        errorMessage = "Không thể tải lịch sử (${response.code()})"
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    history = emptyList(),
                    errorMessage = "Lỗi kết nối. Vui lòng thử lại"
                )
            }
        }
    }

    fun loadNextPage() {
        val pagination = _uiState.value.pagination
        val currentPage = _uiState.value.currentPage
        if (pagination != null && currentPage < pagination.totalPages) {
            loadHistory(currentPage + 1)
        }
    }

    fun loadPreviousPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage > 1) {
            loadHistory(currentPage - 1)
        }
    }
}
