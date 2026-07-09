package com.example.safehome

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.safehome.R
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.AuthRepository
import com.example.safehome.firebase.FcmTokenManager
import com.example.safehome.ui.auth.AuthViewModel
import com.example.safehome.ui.auth.AuthViewModelFactory
import com.example.safehome.ui.home.ClaimDeviceBottomSheet
import com.example.safehome.ui.home.HomeViewModel
import com.example.safehome.ui.home.HomeViewModelFactory
import com.example.safehome.data.repository.DeviceRepository
import com.example.safehome.ui.notification.NotificationActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var fcmTokenManager: FcmTokenManager
    private var txtStatus: TextView? = null
    private var txtName: TextView? = null
    private var txtEmail: TextView? = null
    private var txtRole: TextView? = null
    private var btnNotifications: MaterialButton? = null
    private var btnLogout: MaterialButton? = null
    private var openedLogin = false
    private var lastErrorMessage: String? = null
    private var hasLoadedDevices = false
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
        homeViewModel = createHomeViewModel()
        fcmTokenManager = FcmTokenManager(applicationContext)

        txtStatus = findViewByName("txtStatus")
        txtName = findViewByName("txtName")
        txtEmail = findViewByName("txtEmail")
        txtRole = findViewByName("txtRole")
        btnNotifications = findViewByName("btnNotifications")
        btnLogout = findViewByName("btnLogout")

        btnNotifications?.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        btnLogout?.setOnClickListener {
            btnLogout?.isEnabled = false
            lifecycleScope.launch {
                try {
                    fcmTokenManager.unregisterCurrentToken()
                } catch (e: Exception) {
                    Log.e("HomeActivity", "Error unregistering FCM token on logout", e)
                } finally {
                    authViewModel.logout()
                }
            }
        }

        // Xử lý ẩn hiện chấm thông báo xanh khi click (tìm động bằng tên ID tránh crash khi chưa kéo thả)
        val notificationBadge = findViewByName<View>("notificationBadge")
        val bellContainer = findViewByName<View>("bellContainer")
        bellContainer?.setOnClickListener {
            notificationBadge?.visibility = View.GONE
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
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
        bindStaticSensor("Air", "N/A", "Chưa kết nối", "#94A3B8")
        bindStaticSensor("Buzzer", "N/A", "Chưa kết nối", "#94A3B8")
        bindStaticSensor("Fire", "N/A", "Chưa kết nối", "#94A3B8")

        lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                Log.d("HomeActivity", "🔐 auth state: isLoading=${state.isLoading} isLoggedIn=${state.isLoggedIn} user=${state.currentUser?.email} err=${state.errorMessage}")
                btnLogout?.isEnabled = !state.isLoading
                btnNotifications?.isEnabled = !state.isLoading
                
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
                    Log.d("HomeActivity", "❌ Not logged in, opening login")
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

                    syncFcmTokenAfterLogin()

                    if (!hasLoadedDevices) {
                        hasLoadedDevices = true
                        Log.d("HomeActivity", "🚀 Triggering loadDevices() for user=${user.email}")
                        homeViewModel.loadDevices()
                    }
                }
            }
        }

        // Theo dõi dữ liệu cảm biến từ HomeViewModel để cập nhật giao diện
        lifecycleScope.launch {
            homeViewModel.uiState.collect { state ->
                // Hiện nút Thêm thiết bị khi không có device nào
                val btnAddDevice = findViewByName<MaterialButton>("btnAddDevice")
                if (state.hasNoDevices) {
                    btnAddDevice?.visibility = View.VISIBLE
                    btnAddDevice?.setOnClickListener {
                        ClaimDeviceBottomSheet()
                            .show(supportFragmentManager, ClaimDeviceBottomSheet.TAG)
                    }
                } else {
                    btnAddDevice?.visibility = View.GONE
                }

                val sensor = state.latestReading?.sensor ?: state.activeDevice?.sensor
                val status = state.latestReading?.status ?: state.activeDevice?.status
                val control = state.latestReading?.control ?: state.activeDevice?.control

                val lastUpdateTime = state.latestReading?.createdAt 
                    ?: state.activeDevice?.device?.updatedAt 
                    ?: state.activeDevice?.device?.lastSeen

                val isOffline = isTimestampExpired(lastUpdateTime)

                if (sensor != null && status != null && control != null && !isOffline) {
                    // Cập nhật Nhiệt độ
                    val temp = sensor.temperature
                    val tempStatus = status.temperature
                    val tempColor = if (tempStatus == "SAFE") "#22C55E" else "#EF4444"
                    val tempStatusText = if (tempStatus == "SAFE") "Bình thường" else "Nóng"
                    bindStaticSensor("Temp", "$temp°C", tempStatusText, tempColor)

                    // Cập nhật Độ ẩm
                    val humid = sensor.humidity
                    val humidStatus = status.humidity
                    val humidColor = if (humidStatus == "SAFE") "#22C55E" else "#EF4444"
                    val humidStatusText = if (humidStatus == "SAFE") "Bình thường" else "Kém"
                    bindStaticSensor("Humid", "$humid%", humidStatusText, humidColor)

                    // Cập nhật Khí gas (MQ-2 quy đổi ADC sang PPM tuyến tính từ 0)
                    val rawMq2 = sensor.mq2Raw
                    val mq2Ppm = ((rawMq2.toDouble() / 4095.0) * 1000).toInt()
                    val mq2Status = status.mq2
                    val mq2Color = if (mq2Status == "SAFE") "#22C55E" else "#EF4444"
                    val mq2StatusText = if (mq2Status == "SAFE") "Bình thường" else "Rò rỉ khói/gas!"
                    bindStaticSensor("Gas", "$mq2Ppm ppm", mq2StatusText, mq2Color)

                    // Cập nhật Chất lượng không khí (MQ-135 quy đổi ADC sang PPM tuyến tính từ 0)
                    val rawMq135 = sensor.mq135Raw
                    val mq135Ppm = ((rawMq135.toDouble() / 4095.0) * 1000).toInt()
                    val mq135Status = status.mq135
                    val mq135Color = if (mq135Status == "SAFE") "#22C55E" else "#EF4444"
                    val mq135StatusText = if (mq135Status == "SAFE") "Bình thường" else "Ô nhiễm!"
                    bindStaticSensor("Air", "$mq135Ppm ppm", mq135StatusText, mq135Color)

                    // Cập nhật Còi báo (Buzzer)
                    val buzzerActive = control.buzzerActive
                    val buzzerValueText = if (buzzerActive) "BẬT" else "TẮT"
                    val buzzerStatusText = if (buzzerActive) "Đang kêu!" else "Bình thường"
                    val buzzerColor = if (buzzerActive) "#EF4444" else "#22C55E"
                    bindStaticSensor("Buzzer", buzzerValueText, buzzerStatusText, buzzerColor)

                    // Cập nhật Phát hiện lửa
                    val flame = sensor.flameDetected
                    val flameColor = if (flame) "#EF4444" else "#22C55E"
                    val flameStatusText = if (flame) "Nguy hiểm!" else "Bình thường"
                    bindStaticSensor("Fire", if (flame) "Có lửa!" else "Không", flameStatusText, flameColor)
                } else {
                    // Nếu không nhận được tín hiệu mới trong 20 giây -> tự động reset về N/A
                    bindStaticSensor("Temp", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Humid", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Gas", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Air", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Buzzer", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Fire", "N/A", "Mất kết nối", "#94A3B8")
                }

                // Dynamic Device List Card Population
                val container = findViewById<android.widget.LinearLayout>(R.id.layoutDeviceListContainer)
                container?.removeAllViews()

                if (!state.hasNoDevices && state.devices.isNotEmpty()) {
                    state.devices.forEach { device ->
                        val cardView = layoutInflater.inflate(R.layout.item_device_room, container, false)
                        
                        val txtDeviceRoomName = cardView.findViewById<TextView>(R.id.txtDeviceRoomName)
                        val txtDeviceRoomCode = cardView.findViewById<TextView>(R.id.txtDeviceRoomCode)
                        val viewRoomStatusDot = cardView.findViewById<View>(R.id.viewRoomStatusDot)
                        val txtRoomStatus = cardView.findViewById<TextView>(R.id.txtRoomStatus)
                        
                        txtDeviceRoomName.text = device.name ?: "Thiết bị không tên"
                        txtDeviceRoomCode.text = "Code: ${device.deviceCode ?: "N/A"}"
                        
                        val deviceLastSeen = device.device?.updatedAt ?: device.device?.lastSeen
                        val deviceOffline = isTimestampExpired(deviceLastSeen)
                        
                        if (deviceOffline) {
                            viewRoomStatusDot.setBackgroundResource(R.drawable.bg_badge_dot_grey)
                            txtRoomStatus.text = "Ngoại tuyến"
                            txtRoomStatus.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                        } else {
                            viewRoomStatusDot.setBackgroundResource(R.drawable.bg_badge_dot_green)
                            txtRoomStatus.text = "Hoạt động"
                            txtRoomStatus.setTextColor(android.graphics.Color.parseColor("#22C55E"))
                        }
                        
                        val deviceControl = device.control
                        
                        val bindLedButton = { layoutId: Int, bgId: Int, imgId: Int, ledKey: String, isLedOn: Boolean ->
                            val layout = cardView.findViewById<View>(layoutId)
                            val bg = cardView.findViewById<View>(bgId)
                            val img = cardView.findViewById<android.widget.ImageView>(imgId)
                            
                            if (isLedOn) {
                                bg.visibility = View.VISIBLE
                                img.setColorFilter(android.graphics.Color.parseColor("#F59E0B"))
                            } else {
                                bg.visibility = View.INVISIBLE
                                img.setColorFilter(android.graphics.Color.parseColor("#94A3B8"))
                            }
                            
                            layout.setOnClickListener {
                                homeViewModel.controlDeviceLed(device.id, ledKey, !isLedOn)
                            }
                        }
                        
                        bindLedButton(R.id.layoutLed1, R.id.viewLed1Bg, R.id.imgLed1, "led1", deviceControl?.led1 ?: false)
                        bindLedButton(R.id.layoutLed2, R.id.viewLed2Bg, R.id.imgLed2, "led2", deviceControl?.led2 ?: false)
                        bindLedButton(R.id.layoutLed3, R.id.viewLed3Bg, R.id.imgLed3, "led3", deviceControl?.led3 ?: false)
                        bindLedButton(R.id.layoutLed4, R.id.viewLed4Bg, R.id.imgLed4, "led4", deviceControl?.led4 ?: false)
                        bindLedButton(R.id.layoutLed5, R.id.viewLed5Bg, R.id.imgLed5, "led5", deviceControl?.led5 ?: false)
                        
                        container.addView(cardView)
                    }
                }
            }
        }

        // --- SETUP FOOTER BOTTOM NAVIGATION ---
        val layoutTabHome = findViewById<View>(R.id.layoutTabHome)
        val layoutTabMonitor = findViewById<View>(R.id.layoutTabMonitor)
        val layoutTabDevices = findViewById<View>(R.id.layoutTabDevices)
        val layoutTabAlerts = findViewById<View>(R.id.layoutTabAlerts)
        val layoutTabSettings = findViewById<View>(R.id.layoutTabSettings)
        val btnPlaceholderAction = findViewById<View>(R.id.btnPlaceholderAction)

        val tabToSelect = intent.getIntExtra("SELECT_TAB", 0)
        selectTab(tabToSelect)

        layoutTabHome?.setOnClickListener { selectTab(0) }
        layoutTabMonitor?.setOnClickListener { selectTab(1) }
        layoutTabDevices?.setOnClickListener { selectTab(2) }
        layoutTabAlerts?.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }
        layoutTabSettings?.setOnClickListener { selectTab(4) }
        btnPlaceholderAction?.setOnClickListener { selectTab(0) }
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

    private fun isTimestampExpired(createdAt: String?): Boolean {
        if (createdAt.isNullOrBlank()) return true
        return try {
            val createdInstant = java.time.Instant.parse(createdAt)
            val now = java.time.Instant.now()
            val diffSeconds = java.time.Duration.between(createdInstant, now).seconds
            java.lang.Math.abs(diffSeconds) > 30
        } catch (e: Exception) {
            false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val tabToSelect = intent.getIntExtra("SELECT_TAB", -1)
        if (tabToSelect != -1) {
            selectTab(tabToSelect)
        }
    }

    private fun selectTab(tabIndex: Int) {
        val viewHomeActiveBg = findViewById<View>(R.id.viewHomeActiveBg) ?: return
        val viewMonitorActiveBg = findViewById<View>(R.id.viewMonitorActiveBg) ?: return
        val viewDevicesActiveBg = findViewById<View>(R.id.viewDevicesActiveBg) ?: return
        val viewAlertsActiveBg = findViewById<View>(R.id.viewAlertsActiveBg) ?: return
        val viewSettingsActiveBg = findViewById<View>(R.id.viewSettingsActiveBg) ?: return

        val imgTabHome = findViewById<android.widget.ImageView>(R.id.imgTabHome) ?: return
        val imgTabMonitor = findViewById<android.widget.ImageView>(R.id.imgTabMonitor) ?: return
        val imgTabDevices = findViewById<android.widget.ImageView>(R.id.imgTabDevices) ?: return
        val imgTabAlerts = findViewById<android.widget.ImageView>(R.id.imgTabAlerts) ?: return
        val imgTabSettings = findViewById<android.widget.ImageView>(R.id.imgTabSettings) ?: return

        val txtTabHome = findViewById<TextView>(R.id.txtTabHome) ?: return
        val txtTabMonitor = findViewById<TextView>(R.id.txtTabMonitor) ?: return
        val txtTabDevices = findViewById<TextView>(R.id.txtTabDevices) ?: return
        val txtTabAlerts = findViewById<TextView>(R.id.txtTabAlerts) ?: return
        val txtTabSettings = findViewById<TextView>(R.id.txtTabSettings) ?: return

        val homeScrollView = findViewById<View>(R.id.homeScrollView) ?: return
        val layoutPlaceholderContent = findViewById<View>(R.id.layoutPlaceholderContent) ?: return
        val imgPlaceholderIcon = findViewById<android.widget.ImageView>(R.id.imgPlaceholderIcon) ?: return
        val txtPlaceholderTitle = findViewById<TextView>(R.id.txtPlaceholderTitle) ?: return
        val txtPlaceholderDesc = findViewById<TextView>(R.id.txtPlaceholderDesc) ?: return

        val blueDeepColor = android.graphics.Color.parseColor("#2563EB")
        val textMutedColor = android.graphics.Color.parseColor("#64748B")

        // Reset
        viewHomeActiveBg.visibility = View.INVISIBLE
        viewMonitorActiveBg.visibility = View.INVISIBLE
        viewDevicesActiveBg.visibility = View.INVISIBLE
        viewAlertsActiveBg.visibility = View.INVISIBLE
        viewSettingsActiveBg.visibility = View.INVISIBLE

        imgTabHome.setColorFilter(textMutedColor)
        imgTabMonitor.setColorFilter(textMutedColor)
        imgTabDevices.setColorFilter(textMutedColor)
        imgTabAlerts.setColorFilter(textMutedColor)
        imgTabSettings.setColorFilter(textMutedColor)

        txtTabHome.setTextColor(textMutedColor)
        txtTabMonitor.setTextColor(textMutedColor)
        txtTabDevices.setTextColor(textMutedColor)
        txtTabAlerts.setTextColor(textMutedColor)
        txtTabSettings.setTextColor(textMutedColor)

        txtTabHome.setTypeface(null, android.graphics.Typeface.NORMAL)
        txtTabMonitor.setTypeface(null, android.graphics.Typeface.NORMAL)
        txtTabDevices.setTypeface(null, android.graphics.Typeface.NORMAL)
        txtTabAlerts.setTypeface(null, android.graphics.Typeface.NORMAL)
        txtTabSettings.setTypeface(null, android.graphics.Typeface.NORMAL)

        when (tabIndex) {
            0 -> {
                viewHomeActiveBg.visibility = View.VISIBLE
                imgTabHome.setColorFilter(blueDeepColor)
                txtTabHome.setTextColor(blueDeepColor)
                txtTabHome.setTypeface(null, android.graphics.Typeface.BOLD)

                homeScrollView.visibility = View.VISIBLE
                layoutPlaceholderContent.visibility = View.GONE
            }
            1 -> {
                viewMonitorActiveBg.visibility = View.VISIBLE
                imgTabMonitor.setColorFilter(blueDeepColor)
                txtTabMonitor.setTextColor(blueDeepColor)
                txtTabMonitor.setTypeface(null, android.graphics.Typeface.BOLD)

                homeScrollView.visibility = View.GONE
                layoutPlaceholderContent.visibility = View.VISIBLE

                imgPlaceholderIcon.setImageResource(R.drawable.ic_footer_monitor)
                txtPlaceholderTitle.text = "Giám Sát Hệ Thống"
                txtPlaceholderDesc.text = "Biểu đồ trực quan và thống kê dữ liệu cảm biến của bạn sẽ được hiển thị tại đây."
            }
            2 -> {
                viewDevicesActiveBg.visibility = View.VISIBLE
                imgTabDevices.setColorFilter(blueDeepColor)
                txtTabDevices.setTextColor(blueDeepColor)
                txtTabDevices.setTypeface(null, android.graphics.Typeface.BOLD)

                homeScrollView.visibility = View.GONE
                layoutPlaceholderContent.visibility = View.VISIBLE

                imgPlaceholderIcon.setImageResource(R.drawable.ic_footer_devices)
                txtPlaceholderTitle.text = "Quản Lý Thiết Bị"
                txtPlaceholderDesc.text = "Danh sách và cấu hình chi tiết các thiết bị phần cứng IoT đã liên kết trong nhà bạn."
            }
            3 -> {
                viewAlertsActiveBg.visibility = View.VISIBLE
                imgTabAlerts.setColorFilter(blueDeepColor)
                txtTabAlerts.setTextColor(blueDeepColor)
                txtTabAlerts.setTypeface(null, android.graphics.Typeface.BOLD)

                homeScrollView.visibility = View.GONE
                layoutPlaceholderContent.visibility = View.VISIBLE

                imgPlaceholderIcon.setImageResource(R.drawable.ic_footer_alerts)
                txtPlaceholderTitle.text = "Hộp Thư Cảnh Báo"
                txtPlaceholderDesc.text = "Lịch sử và nhật ký các thông báo khẩn cấp khi phát hiện rò rỉ khí gas, khói hoặc lửa."
            }
            4 -> {
                viewSettingsActiveBg.visibility = View.VISIBLE
                imgTabSettings.setColorFilter(blueDeepColor)
                txtTabSettings.setTextColor(blueDeepColor)
                txtTabSettings.setTypeface(null, android.graphics.Typeface.BOLD)

                homeScrollView.visibility = View.GONE
                layoutPlaceholderContent.visibility = View.VISIBLE

                imgPlaceholderIcon.setImageResource(R.drawable.ic_footer_settings)
                txtPlaceholderTitle.text = "Cài Đặt Hệ Thống"
                txtPlaceholderDesc.text = "Tùy chỉnh thông tin cá nhân, cấu hình ngưỡng cảnh báo khẩn cấp và thiết lập tài khoản."
            }
        }
    }
}
