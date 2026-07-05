package com.example.safehome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.safehome.R
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.AuthRepository
import com.example.safehome.ui.auth.AuthViewModel
import com.example.safehome.ui.auth.AuthViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safehome.data.SensorItem
import com.example.safehome.ui.HomeAdapter

class HomeActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private var txtStatus: TextView? = null
    private var txtName: TextView? = null
    private var txtEmail: TextView? = null
    private var txtRole: TextView? = null
    private var btnLogout: MaterialButton? = null
    private var openedLogin = false
    private var lastErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        authViewModel = createAuthViewModel()

        txtStatus = findViewByName("txtStatus")
        txtName = findViewByName("txtName")
        txtEmail = findViewByName("txtEmail")
        txtRole = findViewByName("txtRole")
        btnLogout = findViewByName("btnLogout")

        btnLogout?.setOnClickListener {
            authViewModel.logout()
        }

        // Xử lý ẩn hiện chấm thông báo xanh khi click (tìm động bằng tên ID tránh crash khi chưa kéo thả)
        val notificationBadge = findViewByName<View>("notificationBadge")
        val bellContainer = findViewByName<View>("bellContainer")
        bellContainer?.setOnClickListener {
            notificationBadge?.visibility = View.GONE
            Toast.makeText(this, "Đã đọc tất cả thông báo", Toast.LENGTH_SHORT).show()
        }

        // Gán dữ liệu cho 5 ô cảm biến tĩnh (tìm động bằng ID tránh crash khi người dùng đang kéo thả thiết kế)
        val bindStaticSensor = { prefix: String, value: String, status: String, statusColor: String ->
            val txtValue = findViewByName<TextView>("txt${prefix}Value")
            val txtStatus = findViewByName<TextView>("txt${prefix}Status")
            val viewDot = findViewByName<View>("view${prefix}Dot")
            
            txtValue?.text = value
            txtStatus?.text = status
            try {
                txtStatus?.setTextColor(android.graphics.Color.parseColor(statusColor))
                viewDot?.background?.setTint(android.graphics.Color.parseColor(statusColor))
            } catch (e: Exception) {}
        }

        bindStaticSensor("Temp", "N/A", "Chưa kết nối", "#94A3B8")
        bindStaticSensor("Humid", "N/A", "Chưa kết nối", "#94A3B8")
        bindStaticSensor("Gas", "N/A", "Chưa kết nối", "#94A3B8")
        bindStaticSensor("Aqi", "N/A", "Chưa kết nối", "#94A3B8")
        bindStaticSensor("Fire", "N/A", "Chưa kết nối", "#94A3B8")

        lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                btnLogout?.isEnabled = !state.isLoading
                
                if (state.isLoading) {
                    txtStatus?.text = "Đang kiểm tra phiên đăng nhập..."
                    setUserContentVisible(false)
                    return@collect
                }
                
                state.errorMessage?.let { message ->
                    txtStatus?.text = message
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
                    txtStatus?.text = "Đăng nhập thành công"
                    txtName?.text = "Nhà của ${user.fullName} ∨"
                    txtEmail?.text = user.email
                    txtRole?.text = user.role
                    
                    // Cập nhật câu chào động nếu có TextView txtGreeting trong layout
                    val txtGreeting = findViewByName<TextView>("txtGreeting")
                    txtGreeting?.text = "Chào ${user.fullName.split(" ").lastOrNull() ?: ""} 👋"
                    
                    setUserContentVisible(true)
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
        txtName?.visibility = visibility
        txtEmail?.visibility = visibility
        txtRole?.visibility = visibility
    }

    private fun openLogin() {
        if (openedLogin) return
        openedLogin = true

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun <T : View> findViewByName(name: String): T? {
        val id = resources.getIdentifier(name, "id", packageName)
        return if (id != 0) findViewById(id) else null
    }
}
