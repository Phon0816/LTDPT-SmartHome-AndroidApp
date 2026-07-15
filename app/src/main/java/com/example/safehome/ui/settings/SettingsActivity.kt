package com.example.safehome.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.safehome.LoginActivity
import com.example.safehome.R
import com.example.safehome.data.local.SettingsPreferences
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.AuthRepository
import com.example.safehome.data.repository.SettingsRepository
import com.example.safehome.firebase.FcmTokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var viewModel: SettingsViewModel
    private lateinit var notificationSwitch: MaterialSwitch
    private var applyingState = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.syncNotificationsAfterPermissionGranted()
        } else {
            Toast.makeText(this, "Bạn có thể bật quyền thông báo trong cài đặt ứng dụng", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        viewModel = createViewModel()
        notificationSwitch = findViewById(R.id.switchNotifications)
        findViewById<TextView>(R.id.txtAppVersion).text = packageManager
            .getPackageInfo(packageName, 0)
            .versionName
            ?: ""

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnOpenNotificationSettings).setOnClickListener {
            openSystemNotificationSettings()
        }
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { viewModel.logout() }

        notificationSwitch.setOnCheckedChangeListener { _, enabled ->
            if (applyingState) return@setOnCheckedChangeListener
            if (enabled && !hasNotificationPermission()) {
                viewModel.setNotificationsEnabled(true, canSyncFcm = false)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setNotificationsEnabled(enabled, canSyncFcm = enabled)
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state -> render(state) }
        }
    }

    private fun createViewModel(): SettingsViewModel {
        val appContext = applicationContext
        val tokenManager = TokenManager(appContext)
        val authRepository = AuthRepository(RetrofitClient.createAuthApi(tokenManager), tokenManager)
        val repository = SettingsRepository(
            authRepository = authRepository,
            settingsPreferences = SettingsPreferences(appContext),
            fcmTokenManager = FcmTokenManager(appContext)
        )
        return ViewModelProvider(this, SettingsViewModelFactory(repository))[SettingsViewModel::class.java]
    }

    private fun render(state: SettingsUiState) {
        findViewById<android.view.View>(R.id.progressLoading).visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<TextView>(R.id.txtProfileName).text = state.user?.fullName ?: "Tài khoản SafeHome"
        findViewById<TextView>(R.id.txtProfileEmail).text = state.user?.email ?: "Không có email"
        findViewById<TextView>(R.id.txtAvatar).text = state.user?.fullName
            ?.trim()?.split(Regex("\\s+"))?.take(2)?.joinToString("") { it.first().uppercase() } ?: "SH"

        applyingState = true
        notificationSwitch.isChecked = state.notificationEnabled
        applyingState = false
        notificationSwitch.isEnabled = !state.isUpdatingNotifications && !state.isLoggingOut
        findViewById<MaterialButton>(R.id.btnLogout).isEnabled = !state.isLoggingOut
        findViewById<TextView>(R.id.txtNotificationStatus).text = if (hasNotificationPermission()) {
            "Quyền thông báo đã được cấp"
        } else {
            "Quyền thông báo đang tắt trong hệ thống"
        }

        state.errorMessage?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        if (state.logoutCompleted) openLogin()
    }

    private fun hasNotificationPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openSystemNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        })
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
