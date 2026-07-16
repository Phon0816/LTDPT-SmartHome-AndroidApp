package com.example.safehome.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.safehome.LoginActivity
import com.example.safehome.HomeActivity
import com.example.safehome.R
import com.example.safehome.data.local.SettingsPreferences
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.AuthRepository
import com.example.safehome.data.repository.DeviceRepository
import com.example.safehome.data.repository.NotificationRepository
import com.example.safehome.data.repository.SettingsRepository
import com.example.safehome.firebase.FcmTokenManager
import com.example.safehome.ui.notification.NotificationActivity
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

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            processAndUploadAvatar(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        viewModel = createViewModel()
        notificationSwitch = findViewById(R.id.switchNotifications)

        findViewById<TextView>(R.id.txtAppVersion).text = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnOpenNotificationSettings).setOnClickListener {
            openSystemNotificationSettings()
        }
        findViewById<View>(R.id.btnLogout).setOnClickListener { viewModel.logout() }

        // Avatar select
        findViewById<View>(R.id.layoutAvatarContainer).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Edit Profile Name
        findViewById<View>(R.id.btnEditName).setOnClickListener {
            val currentName = viewModel.uiState.value.user?.fullName ?: ""
            showEditNameDialog(currentName)
        }

        // Manage Devices redirects to Devices Tab in HomeActivity (Index 2)
        findViewById<View>(R.id.layoutManageDevices).setOnClickListener {
            navigateToHome(2)
        }

        // Alert log shortcut redirects to NotificationActivity
        findViewById<View>(R.id.layoutAlertShortcut).setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Change Password Dialog
        findViewById<View>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }

        // Delete Account Dialog
        findViewById<View>(R.id.btnDeleteAccount).setOnClickListener {
            showDeleteAccountDialog()
        }

        // Terms and Privacy dialogues
        findViewById<View>(R.id.btnPrivacyPolicy).setOnClickListener { showPrivacyPolicyDialog() }
        findViewById<View>(R.id.btnTerms).setOnClickListener { showTermsDialog() }

        notificationSwitch.setOnCheckedChangeListener { _, enabled ->
            if (applyingState) return@setOnCheckedChangeListener
            if (enabled && !hasNotificationPermission()) {
                viewModel.setNotificationsEnabled(true, canSyncFcm = false)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setNotificationsEnabled(enabled, canSyncFcm = enabled)
            }
        }

        // Bind Footer bottom navigation tabs
        findViewById<View>(R.id.layoutTabHome).setOnClickListener { navigateToHome(0) }
        findViewById<View>(R.id.layoutTabMonitor).setOnClickListener { navigateToHome(1) }
        findViewById<View>(R.id.layoutTabDevices).setOnClickListener { navigateToHome(2) }
        findViewById<View>(R.id.layoutTabAlerts).setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
            finish()
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state -> render(state) }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.txtNotificationStatus).text = if (hasNotificationPermission()) {
            "Quyền thông báo đã được cấp"
        } else {
            "Quyền thông báo đang tắt trong hệ thống"
        }
        viewModel.loadSettings()
    }

    private fun createViewModel(): SettingsViewModel {
        val appContext = applicationContext
        val tokenManager = TokenManager(appContext)
        val authApi = RetrofitClient.createAuthApi(tokenManager)
        val authRepository = AuthRepository(authApi, tokenManager)
        
        val deviceApi = RetrofitClient.createDeviceApi(tokenManager)
        val deviceRepository = DeviceRepository(deviceApi)
        
        val notificationApi = RetrofitClient.createNotificationApi(tokenManager)
        val fcmApi = RetrofitClient.createFcmApi(tokenManager)
        val notificationRepository = NotificationRepository(notificationApi, fcmApi)
        
        val repository = SettingsRepository(
            authRepository = authRepository,
            deviceRepository = deviceRepository,
            notificationRepository = notificationRepository,
            settingsPreferences = SettingsPreferences(appContext),
            fcmTokenManager = FcmTokenManager(appContext)
        )
        return ViewModelProvider(this, SettingsViewModelFactory(repository))[SettingsViewModel::class.java]
    }

    private fun render(state: SettingsUiState) {
        findViewById<View>(R.id.progressLoading).visibility = if (state.isLoading || state.isUpdatingProfile || state.isDeletingAccount) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.txtProfileName).text = state.user?.fullName ?: "Tài khoản SafeHome"
        findViewById<TextView>(R.id.txtProfileEmail).text = state.user?.email ?: "Không có email"
        findViewById<TextView>(R.id.txtRole).text = state.user?.role ?: "USER"

        // Member since formatted from createdAt
        val memberSinceText = if (!state.user?.createdAt.isNullOrBlank()) {
            try {
                val zonedDateTime = java.time.ZonedDateTime.parse(state.user.createdAt)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                "Thành viên từ: " + zonedDateTime.format(formatter)
            } catch (e: Exception) {
                "Thành viên từ: " + state.user.createdAt.take(10)
            }
        } else {
            "Thành viên từ: --/--/----"
        }
        findViewById<TextView>(R.id.txtMemberSince).text = memberSinceText

        // Device and Notification alerts count
        findViewById<TextView>(R.id.txtDeviceCount).text = "${state.deviceCount} thiết bị đã liên kết"
        findViewById<TextView>(R.id.txtUnreadNotificationCount).text = if (state.unreadNotificationsCount > 0) {
            "Có ${state.unreadNotificationsCount} cảnh báo chưa đọc"
        } else {
            "Đã đọc hết cảnh báo"
        }

        // Avatar image load or initials fallback
        val txtAvatar = findViewById<TextView>(R.id.txtAvatar)
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)

        if (!state.user?.avatar.isNullOrBlank()) {
            loadAvatarImage(state.user.avatar)
        } else {
            showInitialsPlaceholder(state.user?.fullName)
        }

        applyingState = true
        notificationSwitch.isChecked = state.notificationEnabled
        applyingState = false
        notificationSwitch.isEnabled = !state.isUpdatingNotifications && !state.isLoggingOut
        findViewById<View>(R.id.btnLogout).isEnabled = !state.isLoggingOut

        state.errorMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }

        state.profileUpdateSuccessMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        state.passwordChangeSuccessMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }

        if (state.logoutCompleted || state.accountDeleteCompleted) openLogin()
    }

    private fun showInitialsPlaceholder(fullName: String?) {
        val txtAvatar = findViewById<TextView>(R.id.txtAvatar)
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)
        txtAvatar.visibility = View.VISIBLE
        imgAvatar.setImageResource(R.drawable.bg_soft_blue_circle)

        txtAvatar.text = fullName?.trim()?.split(Regex("\\s+"))?.take(2)
            ?.joinToString("") { it.first().uppercase() } ?: "SH"
    }

    private fun loadAvatarImage(avatarPath: String) {
        val fullUrl = "https://smarthome-backend-lvin.onrender.com" + avatarPath
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL(fullUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    if (bitmap != null) {
                        findViewById<TextView>(R.id.txtAvatar).visibility = View.GONE
                        findViewById<ImageView>(R.id.imgAvatar).setImageBitmap(bitmap)
                    } else {
                        showInitialsPlaceholder(viewModel.uiState.value.user?.fullName)
                    }
                }
            } catch (e: Exception) {
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    showInitialsPlaceholder(viewModel.uiState.value.user?.fullName)
                }
            }
        }
    }

    private fun processAndUploadAvatar(uri: android.net.Uri) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    showToastOnMain("Không thể đọc tệp hình ảnh")
                    return@launch
                }

                // Compress image to 500x500 to keep it light
                val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(
                    originalBitmap, 500, 500, true
                )

                // Save to a temporary cache file
                val tempFile = java.io.File(cacheDir, "temp_avatar.jpg")
                val outputStream = java.io.FileOutputStream(tempFile)
                resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
                outputStream.close()

                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    viewModel.updateProfile(fullName = null, avatarFile = tempFile)
                }
            } catch (e: Exception) {
                showToastOnMain("Lỗi xử lý ảnh: ${e.message}")
            }
        }
    }

    private fun showToastOnMain(message: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showEditNameDialog(currentName: String) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Đổi họ tên")

        val input = android.widget.EditText(this)
        input.setText(currentName)
        input.setSelection(currentName.length)
        builder.setView(input)

        builder.setPositiveButton("Lưu") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                viewModel.updateProfile(fullName = newName, avatarFile = null)
            } else {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Hủy") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showChangePasswordDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Đổi mật khẩu")

        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(48, 24, 48, 24)

        val edtOldPass = android.widget.EditText(this)
        edtOldPass.hint = "Mật khẩu hiện tại"
        edtOldPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        container.addView(edtOldPass)

        val edtNewPass = android.widget.EditText(this)
        edtNewPass.hint = "Mật khẩu mới"
        edtNewPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = 24
        edtNewPass.layoutParams = params
        container.addView(edtNewPass)

        builder.setView(container)

        builder.setPositiveButton("Cập nhật") { dialog, _ ->
            val oldPass = edtOldPass.text.toString().trim()
            val newPass = edtNewPass.text.toString().trim()
            if (oldPass.isNotEmpty() && newPass.isNotEmpty()) {
                viewModel.changePassword(oldPass, newPass)
            } else {
                Toast.makeText(this, "Không được để trống các trường", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Hủy") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showDeleteAccountDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Xóa tài khoản")
            .setMessage("Hành động này không thể hoàn tác. Toàn bộ thiết bị liên kết, dữ liệu và thông báo của bạn sẽ bị xóa vĩnh viễn khỏi hệ thống. Bạn có chắc chắn muốn xóa tài khoản?")
            .setPositiveButton("Xóa vĩnh viễn") { dialog, _ ->
                viewModel.deleteAccount()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showPrivacyPolicyDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Chính sách bảo mật")
            .setMessage("SafeHome cam kết bảo vệ dữ liệu cá nhân của bạn. Mọi thông tin cảm biến, thông báo và thông tin tài khoản được lưu trữ an toàn trên máy chủ đám mây và chỉ hiển thị cho tài khoản của bạn. Chúng tôi không chia sẻ thông tin với bất kỳ bên thứ ba nào.")
            .setPositiveButton("Đồng ý", null)
            .show()
    }

    private fun showTermsDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Điều khoản dịch vụ")
            .setMessage("Bằng việc sử dụng SafeHome, bạn đồng ý với các điều khoản kết nối phần cứng an toàn. Bạn chịu trách nhiệm duy trì kết nối mạng ổn định cho các thiết bị giám sát khẩn cấp. Ứng dụng cung cấp các cảnh báo mang tính chất tham khảo, không thay thế hoàn toàn các hệ thống cảnh báo cháy nổ chuyên nghiệp.")
            .setPositiveButton("Đồng ý", null)
            .show()
    }

    private fun hasNotificationPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openSystemNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        })
    }

    private fun navigateToHome(tabIndex: Int) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.putExtra("SELECT_TAB", tabIndex)
        startActivity(intent)
        finish()
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
