package com.example.safehome.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.safehome.data.repository.DeviceRepository

class DeviceDetailViewModelFactory(
    private val repository: DeviceRepository,
    private val deviceId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeviceDetailViewModel(repository, deviceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
