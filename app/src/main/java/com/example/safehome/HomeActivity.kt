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
import com.example.safehome.ui.home.HomeViewModel
import com.example.safehome.ui.home.HomeViewModelFactory
import com.example.safehome.data.repository.DeviceRepository

class HomeActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var homeViewModel: HomeViewModel
    private var txtStatus: TextView? = null
    private var txtName: TextView? = null
    private var txtEmail: TextView? = null
    private var txtRole: TextView? = null
    private var btnLogout: MaterialButton? = null
    private var openedLogin = false
    private var lastErrorMessage: String? = null
    private var hasLoadedDevices = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        authViewModel = createAuthViewModel()
        homeViewModel = createHomeViewModel()

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

                    if (!hasLoadedDevices) {
                        hasLoadedDevices = true
                        homeViewModel.loadDevices()
                    }
                }
            }
        }

        // Theo dõi dữ liệu cảm biến từ HomeViewModel để cập nhật giao diện
        lifecycleScope.launch {
            homeViewModel.uiState.collect { state ->
                val latest = state.latestReading
                if (latest != null) {
                    // Cập nhật Nhiệt độ
                    val temp = latest.sensor.temperature
                    val tempStatus = latest.status.temperature
                    val tempColor = if (tempStatus == "SAFE") "#22C55E" else "#EF4444"
                    val tempStatusText = if (tempStatus == "SAFE") "Bình thường" else "Nóng"
                    bindStaticSensor("Temp", "$temp°C", tempStatusText, tempColor)

                    // Cập nhật Độ ẩm
                    val humid = latest.sensor.humidity
                    val humidStatus = latest.status.humidity
                    val humidColor = if (humidStatus == "SAFE") "#22C55E" else "#EF4444"
                    val humidStatusText = if (humidStatus == "SAFE") "Bình thường" else "Kém"
                    bindStaticSensor("Humid", "$humid%", humidStatusText, humidColor)

                    // Cập nhật Khí gas (MQ-2 quy đổi ADC sang PPM)
                    val rawMq2 = latest.sensor.mq2Raw
                    val mq2Ppm = if (rawMq2 > 400) {
                        ((rawMq2 - 400).toDouble() / (4095 - 400) * 1000).toInt().coerceAtLeast(0)
                    } else 0
                    val mq2Status = latest.status.mq2
                    val mq2Color = if (mq2Status == "SAFE") "#22C55E" else "#EF4444"
                    val mq2StatusText = if (mq2Status == "SAFE") "Bình thường" else "Rò rỉ khói/gas!"
                    bindStaticSensor("Gas", "$mq2Ppm ppm", mq2StatusText, mq2Color)

                    // Cập nhật Chất lượng không khí (MQ-135 quy đổi ADC sang AQI)
                    val rawMq135 = latest.sensor.mq135Raw
                    val mq135Aqi = if (rawMq135 > 400) {
                        ((rawMq135 - 400).toDouble() / (4095 - 400) * 500).toInt().coerceAtLeast(0)
                    } else 0
                    val mq135Status = latest.status.mq135
                    val mq135Color = if (mq135Status == "SAFE") "#22C55E" else "#EF4444"
                    val mq135StatusText = if (mq135Status == "SAFE") "Tốt" else "Ô nhiễm!"
                    bindStaticSensor("Aqi", "$mq135Aqi AQI", mq135StatusText, mq135Color)

                    // Cập nhật Phát hiện lửa
                    val flame = latest.sensor.flameDetected
                    val flameColor = if (flame) "#EF4444" else "#22C55E"
                    val flameStatusText = if (flame) "Nguy hiểm!" else "Bình thường"
                    bindStaticSensor("Fire", if (flame) "Có lửa!" else "Không", flameStatusText, flameColor)
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

    private fun createHomeViewModel(): HomeViewModel {
        val tokenManager = TokenManager(applicationContext)
        val deviceApi = RetrofitClient.createDeviceApi(tokenManager)
        val deviceRepository = DeviceRepository(deviceApi)
        val factory = HomeViewModelFactory(deviceRepository)
        return ViewModelProvider(this, factory)[HomeViewModel::class.java]
    }
}
