package com.example.safehome.ui.device

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safehome.databinding.ActivityDeviceBinding
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.DeviceRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DeviceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceBinding
    private lateinit var deviceViewModel: DeviceViewModel
    private lateinit var deviceAdapter: DeviceAdapter
    private var lastErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceViewModel = createDeviceViewModel()
        setupRecyclerView()
        setupActions()
        observeUiState()

        deviceViewModel.loadDevices()
    }

    private fun createDeviceViewModel(): DeviceViewModel {
        val tokenManager = TokenManager(applicationContext)
        val deviceApi = RetrofitClient.createDeviceApi(tokenManager)
        val repository = DeviceRepository(deviceApi)
        val factory = DeviceViewModelFactory(repository)
        return ViewModelProvider(this, factory)[DeviceViewModel::class.java]
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device ->
            val intent = Intent(this, DeviceDetailActivity::class.java)
            intent.putExtra("deviceId", device.id)
            startActivity(intent)
        }

        binding.recyclerDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerDevices.adapter = deviceAdapter
    }

    private fun setupActions() {
        binding.fabAddDevice.setOnClickListener {
            showClaimBottomSheet()
        }
        binding.layoutDeviceEmpty.btnLinkDevice.setOnClickListener {
            showClaimBottomSheet()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            deviceViewModel.uiState.collect { state ->
                val hasDevices = state.devices.isNotEmpty()
                val showEmpty = !state.isLoading && !hasDevices

                binding.recyclerDevices.visibility = if (hasDevices) View.VISIBLE else View.GONE
                binding.layoutDeviceEmpty.root.visibility = if (showEmpty) View.VISIBLE else View.GONE

                deviceAdapter.submitList(state.devices)

                val error = state.errorMessage
                if (!error.isNullOrBlank() && error != lastErrorMessage) {
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
                    lastErrorMessage = error
                }

                if (state.claimSuccess) {
                    dismissClaimBottomSheetIfOpened()
                    Snackbar.make(binding.root, "Liên kết thiết bị thành công", Snackbar.LENGTH_SHORT)
                        .show()
                    deviceViewModel.consumeClaimSuccess()
                }
            }
        }
    }

    private fun showClaimBottomSheet() {
        val fragment = supportFragmentManager.findFragmentByTag(DeviceClaimBottomSheet.TAG)
        if (fragment == null) {
            DeviceClaimBottomSheet().show(supportFragmentManager, DeviceClaimBottomSheet.TAG)
        }
    }

    private fun dismissClaimBottomSheetIfOpened() {
        val fragment = supportFragmentManager.findFragmentByTag(DeviceClaimBottomSheet.TAG)
        if (fragment is DeviceClaimBottomSheet) {
            fragment.dismissAllowingStateLoss()
        }
    }
}
