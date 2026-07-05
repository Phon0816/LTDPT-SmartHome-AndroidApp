package com.example.safehome.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.safehome.R
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.DeviceRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ClaimDeviceBottomSheet : BottomSheetDialogFragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_claim_device, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dùng chung ViewModel với HomeActivity (scope = activity)
        val tokenManager = TokenManager(requireContext())
        val deviceApi = RetrofitClient.createDeviceApi(tokenManager)
        val deviceRepository = DeviceRepository(deviceApi)
        val factory = HomeViewModelFactory(deviceRepository)
        homeViewModel = ViewModelProvider(requireActivity(), factory)[HomeViewModel::class.java]

        val etCode = view.findViewById<TextInputEditText>(R.id.etDeviceCode)
        val etSecret = view.findViewById<TextInputEditText>(R.id.etDeviceSecret)
        val etName = view.findViewById<TextInputEditText>(R.id.etDeviceName)
        val tilCode = view.findViewById<TextInputLayout>(R.id.tilDeviceCode)
        val tilSecret = view.findViewById<TextInputLayout>(R.id.tilDeviceSecret)
        val tilName = view.findViewById<TextInputLayout>(R.id.tilDeviceName)
        val txtError = view.findViewById<TextView>(R.id.txtClaimError)
        val btnClaim = view.findViewById<MaterialButton>(R.id.btnClaimDevice)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelClaim)

        btnCancel.setOnClickListener { dismiss() }

        btnClaim.setOnClickListener {
            val code = etCode.text?.toString()?.trim() ?: ""
            val secret = etSecret.text?.toString()?.trim() ?: ""
            val name = etName.text?.toString()?.trim() ?: ""

            // Validate
            tilCode.error = null
            tilSecret.error = null
            tilName.error = null
            var hasError = false

            if (code.isEmpty()) {
                tilCode.error = "Vui lòng nhập mã thiết bị"
                hasError = true
            }
            if (secret.isEmpty()) {
                tilSecret.error = "Vui lòng nhập mật khẩu"
                hasError = true
            }
            if (name.isEmpty()) {
                tilName.error = "Vui lòng đặt tên cho thiết bị"
                hasError = true
            }
            if (hasError) return@setOnClickListener

            homeViewModel.claimDevice(code, secret, name)
        }

        // Theo dõi trạng thái claim
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.uiState.collect { state ->
                // Loading state
                btnClaim.isEnabled = !state.isClaiming
                btnClaim.text = if (state.isClaiming) "Đang kết nối..." else "Kết nối thiết bị"

                // Error state
                if (state.claimError != null) {
                    txtError.visibility = View.VISIBLE
                    txtError.text = "⚠️  ${state.claimError}"
                    homeViewModel.resetClaimState()
                } else {
                    txtError.visibility = View.GONE
                }

                // Success → đóng bottom sheet
                if (state.claimSuccess) {
                    homeViewModel.resetClaimState()
                    dismiss()
                }
            }
        }
    }

    companion object {
        const val TAG = "ClaimDeviceBottomSheet"
    }
}
