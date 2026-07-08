package com.example.safehome

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.AuthRepository
import com.example.safehome.firebase.FcmTokenManager
import com.example.safehome.ui.auth.AuthViewModel
import com.example.safehome.ui.auth.AuthViewModelFactory
import com.example.safehome.ui.notification.NotificationActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var fcmTokenManager: FcmTokenManager
    private lateinit var txtStatus: TextView
    private lateinit var txtName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtRole: TextView
    private lateinit var btnNotifications: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private var openedLogin = false
    private var lastErrorMessage: String? = null
    private var requestedFcmSync = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Bạn có thể bật thông báo trong cài đặt ứng dụng",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        authViewModel = createAuthViewModel()
        fcmTokenManager = FcmTokenManager(applicationContext)

        txtStatus = findViewById(R.id.txtStatus)
        txtName = findViewById(R.id.txtName)
        txtEmail = findViewById(R.id.txtEmail)
        txtRole = findViewById(R.id.txtRole)
        btnNotifications = findViewById(R.id.btnNotifications)
        btnLogout = findViewById(R.id.btnLogout)

        btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        btnLogout.setOnClickListener {
            authViewModel.logout()
        }

        lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                btnLogout.isEnabled = !state.isLoading
                btnNotifications.isEnabled = !state.isLoading

                if (state.isLoading) {
                    txtStatus.text = "Đang kiểm tra phiên đăng nhập..."
                    setUserContentVisible(false)
                    return@collect
                }

                state.errorMessage?.let { message ->
                    txtStatus.text = message
                    setUserContentVisible(false)

                    if (message != lastErrorMessage) {
                        Toast.makeText(this@HomeActivity, message, Toast.LENGTH_LONG).show()
                        lastErrorMessage = message
                    }
                    return@collect
                }

                if (!state.isLoggedIn) {
                    openLogin()
                    return@collect
                }

                val user = state.currentUser
                if (user != null) {
                    txtStatus.text = "Đăng nhập thành công"
                    txtName.text = user.fullName
                    txtEmail.text = user.email
                    txtRole.text = user.role
                    setUserContentVisible(true)
                    syncFcmTokenAfterLogin()
                }
            }
        }
    }

    private fun createAuthViewModel(): AuthViewModel {
        val tokenManager = TokenManager(applicationContext)
        val authApi = RetrofitClient.createAuthApi(tokenManager)
        val authRepository = AuthRepository(authApi, tokenManager)
        val factory = AuthViewModelFactory(authRepository, tokenManager)
        return ViewModelProvider(this, factory)[AuthViewModel::class.java]
    }

    private fun setUserContentVisible(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        txtName.visibility = visibility
        txtEmail.visibility = visibility
        txtRole.visibility = visibility
    }

    private fun syncFcmTokenAfterLogin() {
        if (requestedFcmSync) return
        requestedFcmSync = true

        requestNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            fcmTokenManager.syncTokenIfLoggedIn()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openLogin() {
        if (openedLogin) return
        openedLogin = true

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
