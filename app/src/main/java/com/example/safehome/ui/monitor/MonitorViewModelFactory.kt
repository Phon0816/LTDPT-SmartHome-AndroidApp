package com.example.safehome.ui.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.safehome.data.repository.DeviceRepository

class MonitorViewModelFactory(
    private val deviceRepository: DeviceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonitorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonitorViewModel(deviceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
