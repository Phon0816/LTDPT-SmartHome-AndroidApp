package com.example.safehome.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.safehome.data.repository.DeviceRepository

class HomeViewModelFactory(
    private val deviceRepository: DeviceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(deviceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
