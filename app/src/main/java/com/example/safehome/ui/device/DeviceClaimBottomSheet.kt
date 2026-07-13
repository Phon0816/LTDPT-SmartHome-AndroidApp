package com.example.safehome.ui.device

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.DeviceRepository
import com.example.safehome.databinding.BottomSheetClaimDeviceBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class DeviceClaimBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetClaimDeviceBinding? = null
    private val binding get() = _binding!!

    private val deviceViewModel: DeviceViewModel by lazy {
        val tokenManager = TokenManager(requireContext().applicationContext)
        val deviceApi = RetrofitClient.createDeviceApi(tokenManager)
        val repository = DeviceRepository(deviceApi)
        val factory = DeviceViewModelFactory(repository)
        ViewModelProvider(requireActivity(), factory)[DeviceViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetClaimDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancelClaim.setOnClickListener { dismiss() }
        binding.btnClaimDevice.setOnClickListener {
            validateAndSubmit()
        }

        clearErrorWhenTyping()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                deviceViewModel.uiState.collect { state ->
                    binding.btnClaimDevice.isEnabled = !state.isClaiming
                    binding.btnClaimDevice.text = if (state.isClaiming) {
                        "Đang liên kết..."
                    } else {
                        "Liên kết thiết bị"
                    }

                    if (!state.claimError.isNullOrBlank()) {
                        binding.txtClaimError.visibility = View.VISIBLE
                        binding.txtClaimError.text = state.claimError
                    } else {
                        binding.txtClaimError.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun clearErrorWhenTyping() {
        binding.etDeviceCode.doAfterTextChanged {
            binding.tilDeviceCode.error = null
            deviceViewModel.consumeClaimError()
        }
        binding.etDeviceSecret.doAfterTextChanged {
            binding.tilDeviceSecret.error = null
            deviceViewModel.consumeClaimError()
        }
        binding.etDeviceName.doAfterTextChanged {
            binding.tilDeviceName.error = null
            deviceViewModel.consumeClaimError()
        }
    }

    private fun validateAndSubmit() {
        val code = binding.etDeviceCode.text?.toString().orEmpty().trim()
        val secret = binding.etDeviceSecret.text?.toString().orEmpty().trim()
        val name = binding.etDeviceName.text?.toString().orEmpty().trim()

        binding.tilDeviceCode.error = null
        binding.tilDeviceSecret.error = null
        binding.tilDeviceName.error = null

        var hasError = false
        if (code.isBlank()) {
            hasError = true
            binding.tilDeviceCode.error = "Vui lòng nhập Device Code"
        }
        if (secret.isBlank()) {
            hasError = true
            binding.tilDeviceSecret.error = "Vui lòng nhập Device Secret"
        }
        if (name.isBlank()) {
            hasError = true
            binding.tilDeviceName.error = "Vui lòng nhập Device Name"
        }
        if (hasError) return

        deviceViewModel.claimDevice(code, secret, name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DeviceClaimBottomSheet"
    }
}
