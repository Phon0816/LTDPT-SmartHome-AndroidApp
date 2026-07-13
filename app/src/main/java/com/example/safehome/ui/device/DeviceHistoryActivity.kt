package com.example.safehome.ui.device

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safehome.databinding.ActivityDeviceHistoryBinding

class DeviceHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deviceId = intent.getIntExtra("deviceId", -1)
        
        // TODO: Implement history functionality later
        binding.txtTitle.text = "Lịch sử thiết bị #$deviceId"
    }
}
