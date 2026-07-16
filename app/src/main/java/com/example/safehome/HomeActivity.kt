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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
import com.example.safehome.data.repository.NotificationRepository
import com.example.safehome.ui.device.DeviceAdapter
import com.example.safehome.ui.device.DeviceDetailActivity
import com.example.safehome.ui.monitor.MonitorViewModel
import com.example.safehome.ui.monitor.MonitorViewModelFactory
import com.example.safehome.ui.monitor.MonitorDeviceAdapter
import com.example.safehome.ui.monitor.MonitorDeviceSimpleAdapter
import com.example.safehome.ui.monitor.GasTab
import com.example.safehome.ui.monitor.TimeFilter
import com.example.safehome.ui.monitor.FlameTimelineAdapter
import com.example.safehome.ui.monitor.FlameTimelineAdapter.FlameEvent
import com.example.safehome.ui.monitor.RecentHistoryAdapter
import com.example.safehome.ui.monitor.ChartHelper
import com.example.safehome.ui.notification.NotificationActivity
import com.example.safehome.ui.settings.SettingsActivity
import com.example.safehome.ui.notification.NotificationViewModel
import com.example.safehome.ui.notification.NotificationViewModelFactory
import com.example.safehome.ui.notification.NotificationAdapter
import com.google.android.material.button.MaterialButton
import com.github.mikephil.charting.charts.LineChart
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var monitorViewModel: MonitorViewModel
    private lateinit var notificationViewModel: NotificationViewModel
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
    private lateinit var devicesTabAdapter: DeviceAdapter
    private lateinit var notificationsTabAdapter: NotificationAdapter
    private lateinit var flameTimelineAdapter: FlameTimelineAdapter
    private lateinit var recentHistoryAdapter: RecentHistoryAdapter
    private lateinit var monitorDeviceSimpleAdapter: MonitorDeviceSimpleAdapter

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
        monitorViewModel = createMonitorViewModel()
        notificationViewModel = createNotificationViewModel()
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

        setupDevicesTabContent()
        setupMonitorTabContent()

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

                    // Cập nhật thẻ điểm an toàn hệ thống
                    updateSafetyCard(status, lastUpdateTime, false)
                } else {
                    // Nếu không nhận được tín hiệu mới trong 20 giây -> tự động reset về N/A
                    bindStaticSensor("Temp", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Humid", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Gas", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Air", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Buzzer", "N/A", "Mất kết nối", "#94A3B8")
                    bindStaticSensor("Fire", "N/A", "Mất kết nối", "#94A3B8")

                    // Cập nhật thẻ điểm an toàn hệ thống ngoại tuyến
                    updateSafetyCard(null, null, true, state.hasNoDevices)
                }

                // Cập nhật tổng số cảm biến
                val txtSensorCount = findViewByName<TextView>("txtSensorCount")
                val sensorCount = if (state.devices.isNotEmpty()) {
                    // Đếm số cảm biến thực tế từ thiết bị active (6 cảm biến: Temp, Humid, Gas, Air, Buzzer, Fire)
                    6
                } else {
                    0
                }
                txtSensorCount?.text = "$sensorCount cảm biến"

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

                devicesTabAdapter.submitList(state.devices)
                val recyclerDevicesTab = findViewById<RecyclerView>(R.id.recyclerDevicesTab)
                val layoutDeviceEmptyTab = findViewById<View>(R.id.layoutDeviceEmptyTab)
                val showDevicesTabEmpty = state.devices.isEmpty()
                recyclerDevicesTab?.visibility = if (showDevicesTabEmpty) View.GONE else View.VISIBLE
                layoutDeviceEmptyTab?.visibility = if (showDevicesTabEmpty) View.VISIBLE else View.GONE
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
        layoutTabSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
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

    private fun createMonitorViewModel(): MonitorViewModel {
        val tokenManager = TokenManager(applicationContext)
        val deviceApi = RetrofitClient.createDeviceApi(tokenManager)
        val deviceRepository = DeviceRepository(deviceApi)
        val factory = MonitorViewModelFactory(deviceRepository)
        return ViewModelProvider(this, factory)[MonitorViewModel::class.java]
    }

    private fun createNotificationViewModel(): NotificationViewModel {
        val tokenManager = TokenManager(applicationContext)
        val notificationApi = RetrofitClient.createNotificationApi(tokenManager)
        val fcmApi = RetrofitClient.createFcmApi(tokenManager)
        val notificationRepository = NotificationRepository(notificationApi, fcmApi)
        val factory = NotificationViewModelFactory(notificationRepository)
        return ViewModelProvider(this, factory)[NotificationViewModel::class.java]
    }

    private fun setupDevicesTabContent() {
        val recyclerDevicesTab = findViewById<RecyclerView>(R.id.recyclerDevicesTab)
        val fabAddDeviceTab = findViewById<View>(R.id.fabAddDeviceTab)
        val btnLinkDeviceTab = findViewById<MaterialButton>(R.id.btnLinkDevice)

        devicesTabAdapter = DeviceAdapter { device ->
            val intent = Intent(this, DeviceDetailActivity::class.java)
            intent.putExtra("deviceId", device.id)
            startActivity(intent)
        }

        recyclerDevicesTab?.layoutManager = LinearLayoutManager(this)
        recyclerDevicesTab?.adapter = devicesTabAdapter

        val openClaimSheet = {
            ClaimDeviceBottomSheet().show(supportFragmentManager, ClaimDeviceBottomSheet.TAG)
        }

        fabAddDeviceTab?.setOnClickListener { openClaimSheet() }
        btnLinkDeviceTab?.setOnClickListener { openClaimSheet() }

        setupNotificationsTabContent()
    }

    private fun setupNotificationsTabContent() {
        val recyclerNotificationsTab = findViewById<RecyclerView>(R.id.recyclerNotificationsTab)
        val btnNotificationsRetry = findViewById<MaterialButton>(R.id.btnNotificationsRetry)

        notificationsTabAdapter = NotificationAdapter { item ->
            notificationViewModel.markAsRead(item)
        }

        recyclerNotificationsTab?.layoutManager = LinearLayoutManager(this)
        recyclerNotificationsTab?.adapter = notificationsTabAdapter

        btnNotificationsRetry?.setOnClickListener {
            notificationViewModel.loadNotifications()
        }

        lifecycleScope.launch {
            notificationViewModel.uiState.collect { state ->
                updateNotificationsState(state)
            }
        }
    }

    private fun updateNotificationsState(state: com.example.safehome.ui.notification.NotificationUiState) {
        val progressLoading = findViewById<View>(R.id.progressNotificationsLoading)
        val layoutEmpty = findViewById<View>(R.id.layoutNotificationsEmpty)
        val layoutError = findViewById<View>(R.id.layoutNotificationsError)
        val txtError = findViewById<TextView>(R.id.txtNotificationsError)
        val recyclerNotificationsTab = findViewById<RecyclerView>(R.id.recyclerNotificationsTab)

        progressLoading?.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        layoutEmpty?.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        layoutError?.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
        txtError?.text = state.errorMessage
        recyclerNotificationsTab?.visibility = if (!state.isLoading && state.notifications.isNotEmpty()) View.VISIBLE else View.GONE

        notificationsTabAdapter.submitNotifications(state.notifications)
    }

    private fun setupMonitorTabContent() {
        flameTimelineAdapter = FlameTimelineAdapter()
        recentHistoryAdapter = RecentHistoryAdapter()
        monitorDeviceSimpleAdapter = MonitorDeviceSimpleAdapter { device ->
            monitorViewModel.selectDevice(device.id.toString(), device.name)
        }

        val recyclerFlameTimeline = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerFlameTimeline)
        val recyclerRecentHistory = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerRecentHistory)
        val recyclerMonitorDevices = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMonitorDevices)

        recyclerFlameTimeline?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerFlameTimeline?.adapter = flameTimelineAdapter

        recyclerRecentHistory?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerRecentHistory?.adapter = recentHistoryAdapter

        recyclerMonitorDevices?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerMonitorDevices?.adapter = monitorDeviceSimpleAdapter

        val btnRefresh = findViewById<MaterialButton>(R.id.btnRefresh)
        btnRefresh?.setOnClickListener {
            monitorViewModel.refresh()
        }

        val btnHeaderRefresh = findViewById<MaterialButton>(R.id.btnHeaderRefresh)
        btnHeaderRefresh?.setOnClickListener {
            monitorViewModel.refresh()
        }

        val btnBackToDeviceList = findViewById<android.widget.ImageView>(R.id.btnBackToDeviceList)
        btnBackToDeviceList?.setOnClickListener {
            monitorViewModel.clearDeviceSelection()
        }

        val swipeRefresh = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshMonitor)
        swipeRefresh?.setOnRefreshListener {
            monitorViewModel.refresh()
        }

        val gasTabGroup = findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.gasTabGroup)
        val btnTabMQ2 = findViewById<MaterialButton>(R.id.btnTabMQ2)
        val btnTabMQ135 = findViewById<MaterialButton>(R.id.btnTabMQ135)

        btnTabMQ2?.setOnClickListener {
            monitorViewModel.selectGasTab(GasTab.MQ2)
        }

        btnTabMQ135?.setOnClickListener {
            monitorViewModel.selectGasTab(GasTab.MQ135)
        }

        val btnFilter24h = findViewById<MaterialButton>(R.id.btnFilter24h)
        val btnFilter7d = findViewById<MaterialButton>(R.id.btnFilter7d)
        val btnFilter30d = findViewById<MaterialButton>(R.id.btnFilter30d)

        btnFilter24h?.setOnClickListener {
            monitorViewModel.selectTimeFilter(TimeFilter.HOURS_24)
            updateTimeFilterButtons(TimeFilter.HOURS_24)
        }

        btnFilter7d?.setOnClickListener {
            monitorViewModel.selectTimeFilter(TimeFilter.DAYS_7)
            updateTimeFilterButtons(TimeFilter.DAYS_7)
        }

        btnFilter30d?.setOnClickListener {
            monitorViewModel.selectTimeFilter(TimeFilter.DAYS_30)
            updateTimeFilterButtons(TimeFilter.DAYS_30)
        }

        lifecycleScope.launch {
            monitorViewModel.uiState.collect { state ->
                updateMonitorState(state)
                swipeRefresh?.isRefreshing = state.isLoading
                updateTimeFilterButtons(state.selectedTimeFilter)
            }
        }
    }

    private fun updateMonitorState(state: com.example.safehome.ui.monitor.MonitorUiState) {
        android.util.Log.d("MonitorState", "Updating state: hasNoDevice=${state.hasNoDevice}, isLoading=${state.isLoading}, isEmpty=${state.isEmpty}, deviceId=${state.deviceId}")

        val layoutMonitorLoading = findViewById<View>(R.id.layoutMonitorLoading)
        val cardMonitorEmpty = findViewById<View>(R.id.cardMonitorEmpty)
        val layoutMonitorContent = findViewById<View>(R.id.layoutMonitorContent)
        val layoutDeviceList = findViewById<View>(R.id.layoutDeviceList)
        val txtEmptyTitle = findViewById<TextView>(R.id.txtEmptyTitle)
        val txtEmptyDesc = findViewById<TextView>(R.id.txtEmptyDesc)
        val btnRefresh = findViewById<MaterialButton>(R.id.btnRefresh)

        android.util.Log.d("MonitorState", "Views found: loading=${layoutMonitorLoading != null}, empty=${cardMonitorEmpty != null}, content=${layoutMonitorContent != null}, deviceList=${layoutDeviceList != null}")

        // Check if user has any devices
        val hasDevices = homeViewModel.uiState.value.devices.isNotEmpty()
        android.util.Log.d("MonitorState", "User has devices: $hasDevices, device count: ${homeViewModel.uiState.value.devices.size}")

        if (!hasDevices) {
            android.util.Log.d("MonitorState", "Showing no device state")
            layoutMonitorLoading?.visibility = View.GONE
            cardMonitorEmpty?.visibility = View.VISIBLE
            layoutMonitorContent?.visibility = View.GONE
            layoutDeviceList?.visibility = View.GONE
            txtEmptyTitle?.text = "Chưa có thiết bị"
            txtEmptyDesc?.text = "Thêm thiết bị để bắt đầu giám sát."
            btnRefresh?.visibility = View.GONE
            return
        }

        // Show device list when no device is selected
        if (state.deviceId == null && !state.isLoading) {
            android.util.Log.d("MonitorState", "Showing device list")
            layoutMonitorLoading?.visibility = View.GONE
            cardMonitorEmpty?.visibility = View.GONE
            layoutMonitorContent?.visibility = View.GONE
            layoutDeviceList?.visibility = View.VISIBLE
            
            // Update device list with devices from homeViewModel
            val devices = homeViewModel.uiState.value.devices
            monitorDeviceSimpleAdapter.submitDevices(devices)
            return
        }

        if (state.isLoading) {
            android.util.Log.d("MonitorState", "Showing loading state")
            layoutMonitorLoading?.visibility = View.VISIBLE
            cardMonitorEmpty?.visibility = View.GONE
            layoutMonitorContent?.visibility = View.GONE
            layoutDeviceList?.visibility = View.GONE
            return
        }

        if (state.errorMessage != null) {
            android.util.Log.d("MonitorState", "Showing error state: ${state.errorMessage}")
            Toast.makeText(this, state.errorMessage, Toast.LENGTH_SHORT).show()
            layoutMonitorLoading?.visibility = View.GONE
            cardMonitorEmpty?.visibility = View.VISIBLE
            layoutMonitorContent?.visibility = View.GONE
            layoutDeviceList?.visibility = View.GONE
            txtEmptyTitle?.text = "Lỗi tải dữ liệu"
            txtEmptyDesc?.text = state.errorMessage
            btnRefresh?.visibility = View.VISIBLE
            return
        }

        if (state.isEmpty) {
            android.util.Log.d("MonitorState", "Showing empty data state")
            layoutMonitorLoading?.visibility = View.GONE
            cardMonitorEmpty?.visibility = View.VISIBLE
            layoutMonitorContent?.visibility = View.GONE
            layoutDeviceList?.visibility = View.GONE
            txtEmptyTitle?.text = "Chưa có dữ liệu"
            txtEmptyDesc?.text = "Thiết bị chưa gửi dữ liệu cảm biến."
            btnRefresh?.visibility = View.VISIBLE
            return
        }

        android.util.Log.d("MonitorState", "Showing content with ${state.history.size} records")
        layoutMonitorLoading?.visibility = View.GONE
        cardMonitorEmpty?.visibility = View.GONE
        layoutMonitorContent?.visibility = View.VISIBLE
        layoutDeviceList?.visibility = View.GONE

        updateMonitorOverview(state)
        updateMonitorCharts(state)
        updateFlameTimeline(state)
        updateRecentHistory(state)
    }

    private fun updateMonitorOverview(state: com.example.safehome.ui.monitor.MonitorUiState) {
        val latestRecord = state.history.firstOrNull() ?: return

        val txtMonitorDeviceName = findViewById<TextView>(R.id.txtMonitorDeviceName)
        val txtMonitorLastUpdate = findViewById<TextView>(R.id.txtMonitorLastUpdate)
        val txtMonitorStatus = findViewById<TextView>(R.id.txtMonitorStatus)
        val cardMonitorStatus = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardMonitorStatus)
        val txtMonitorCurrentTemp = findViewById<TextView>(R.id.txtMonitorCurrentTemp)
        val txtMonitorCurrentHumid = findViewById<TextView>(R.id.txtMonitorCurrentHumid)
        val txtMonitorCurrentMQ2 = findViewById<TextView>(R.id.txtMonitorCurrentMQ2)
        val txtMonitorCurrentMQ135 = findViewById<TextView>(R.id.txtMonitorCurrentMQ135)

        txtMonitorDeviceName?.text = state.deviceName ?: "Thiết bị"

        try {
            val date = java.util.Date(java.time.Instant.parse(latestRecord.createdAt).toEpochMilli())
            val timeFormat = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.getDefault())
            txtMonitorLastUpdate?.text = "Cập nhật: ${timeFormat.format(date)}"
        } catch (e: Exception) {
            txtMonitorLastUpdate?.text = "Cập nhật: --"
        }

        val systemStatus = latestRecord.status.system
        txtMonitorStatus?.text = systemStatus

        when (systemStatus) {
            "SAFE" -> {
                cardMonitorStatus?.setCardBackgroundColor(android.graphics.Color.parseColor("#D1FAE5"))
                txtMonitorStatus?.setTextColor(android.graphics.Color.parseColor("#065F46"))
            }
            "WARNING" -> {
                cardMonitorStatus?.setCardBackgroundColor(android.graphics.Color.parseColor("#FEF3C7"))
                txtMonitorStatus?.setTextColor(android.graphics.Color.parseColor("#92400E"))
            }
            "DANGER" -> {
                cardMonitorStatus?.setCardBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
                txtMonitorStatus?.setTextColor(android.graphics.Color.parseColor("#991B1B"))
            }
            else -> {
                cardMonitorStatus?.setCardBackgroundColor(android.graphics.Color.parseColor("#F1F5F9"))
                txtMonitorStatus?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            }
        }

        txtMonitorCurrentTemp?.text = "${latestRecord.sensor.temperature}°C"
        txtMonitorCurrentHumid?.text = "${latestRecord.sensor.humidity}%"
        txtMonitorCurrentMQ2?.text = String.format("%.0f", (latestRecord.sensor.mq2Raw.toDouble() / 4095.0) * 1000)
        txtMonitorCurrentMQ135?.text = String.format("%.0f", (latestRecord.sensor.mq135Raw.toDouble() / 4095.0) * 1000)
    }

    private fun updateMonitorCharts(state: com.example.safehome.ui.monitor.MonitorUiState) {
        val history = state.history
        android.util.Log.d("MonitorCharts", "Updating charts with ${history.size} records")
        if (history.isEmpty()) {
            android.util.Log.d("MonitorCharts", "History is empty, skipping chart update")
            return
        }

        val chartTemperature = findViewById<LineChart>(R.id.chartTemperature)
        val chartHumidity = findViewById<LineChart>(R.id.chartHumidity)
        val chartGas = findViewById<LineChart>(R.id.chartGas)

        android.util.Log.d("MonitorCharts", "Chart views found: temp=${chartTemperature != null}, humid=${chartHumidity != null}, gas=${chartGas != null}")

        val tempEntries = history.mapIndexed { index, record ->
            val temp = record.sensor.temperature.toFloat()
            android.util.Log.d("MonitorCharts", "Temp entry $index: temp=$temp, createdAt=${record.createdAt}")
            com.github.mikephil.charting.data.Entry(index.toFloat(), temp)
        }

        val humidEntries = history.mapIndexed { index, record ->
            val humid = record.sensor.humidity.toFloat()
            android.util.Log.d("MonitorCharts", "Humid entry $index: humid=$humid, createdAt=${record.createdAt}")
            com.github.mikephil.charting.data.Entry(index.toFloat(), humid)
        }

        val gasEntries = when (state.selectedGasTab) {
            GasTab.MQ2 -> history.mapIndexed { index, record ->
                val gasValue = ((record.sensor.mq2Raw.toDouble() / 4095.0) * 1000).toFloat()
                android.util.Log.d("MonitorCharts", "Gas MQ2 entry $index: gas=$gasValue, createdAt=${record.createdAt}")
                com.github.mikephil.charting.data.Entry(index.toFloat(), gasValue)
            }
            GasTab.MQ135 -> history.mapIndexed { index, record ->
                val gasValue = ((record.sensor.mq135Raw.toDouble() / 4095.0) * 1000).toFloat()
                android.util.Log.d("MonitorCharts", "Gas MQ135 entry $index: gas=$gasValue, createdAt=${record.createdAt}")
                com.github.mikephil.charting.data.Entry(index.toFloat(), gasValue)
            }
        }

        android.util.Log.d("MonitorCharts", "Entries created: temp=${tempEntries.size}, humid=${humidEntries.size}, gas=${gasEntries.size}")
        android.util.Log.d("MonitorCharts", "History size: ${history.size}")
        android.util.Log.d("MonitorCharts", "About to create timestamps...")
        
        // Store original timestamps for X-axis display
        val timestamps = history.map { 
            android.util.Log.d("MonitorCharts", "Processing record: ${it.createdAt}")
            ChartHelper.formatTimestamp(it.createdAt) 
        }
        android.util.Log.d("MonitorCharts", "Timestamps created: ${timestamps.size}, first=${timestamps.firstOrNull()}, last=${timestamps.lastOrNull()}")
        
        // Post chart updates to run after layout is complete
        chartTemperature?.post {
            try {
                android.util.Log.d("MonitorCharts", "=== Inside post block for temp chart ===")
                android.util.Log.d("MonitorCharts", "Chart dimensions: temp width=${chartTemperature?.width}, height=${chartTemperature?.height}")
                
                // Direct chart setup without ChartHelper
                chartTemperature.description.isEnabled = false
                chartTemperature.setTouchEnabled(true)
                chartTemperature.isDragEnabled = true
                chartTemperature.setScaleEnabled(true)
                chartTemperature.setPinchZoom(true)
                chartTemperature.setDrawGridBackground(false)
                chartTemperature.legend.isEnabled = false
                
                // Configure X-axis
                chartTemperature.xAxis.apply {
                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = android.graphics.Color.parseColor("#64748B")
                    textSize = 10f
                    setLabelCount(6, false) // Show maximum 6 labels
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            if (index >= 0 && index < timestamps.size) {
                                val timestamp = timestamps[index]
                                val date = java.util.Date(timestamp)
                                val format = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                                return format.format(date)
                            }
                            return ""
                        }
                    }
                }
                
                // Configure Y-axis with auto-scaling and padding
                val tempValues = tempEntries.map { it.y }
                val tempMin = tempValues.minOrNull() ?: 0f
                val tempMax = tempValues.maxOrNull() ?: 0f
                val tempPadding = (tempMax - tempMin) * 0.1f // 10% padding
                
                chartTemperature.axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = android.graphics.Color.parseColor("#E2E8F0")
                    gridLineWidth = 0.5f
                    textColor = android.graphics.Color.parseColor("#64748B")
                    textSize = 10f
                    setLabelCount(5, false)
                    axisMinimum = tempMin - tempPadding
                    axisMaximum = tempMax + tempPadding
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format(java.util.Locale.getDefault(), "%.1f°C", value)
                        }
                    }
                }
                chartTemperature.axisRight.isEnabled = false
                
                // Set custom marker view with timestamp lookup
                android.util.Log.d("MonitorCharts", "Creating marker view with ${timestamps.size} timestamps")
                val markerView = com.example.safehome.ui.monitor.ChartHelper.ChartMarkerView(
                    chartTemperature.context,
                    "°C",
                    timestamps
                )
                android.util.Log.d("MonitorCharts", "Marker view instance created: $markerView")
                chartTemperature.marker = markerView
                android.util.Log.d("MonitorCharts", "Marker view set to chart, current marker: ${chartTemperature.marker}")

                // CHECK 1: Verify marker is not null
                android.util.Log.d("MARKER", "marker = ${chartTemperature.marker}")

                // CHECK 5: Verify marker attached
                android.util.Log.d("MARKER", "marker attached = ${chartTemperature.marker != null}")

                // Configure highlight behavior
                chartTemperature.setHighlightPerDragEnabled(true)
                chartTemperature.isHighlightPerDragEnabled = true
                chartTemperature.setDrawMarkers(true) // Enable marker drawing
                
                val dataSet = com.github.mikephil.charting.data.LineDataSet(tempEntries, "Temperature").apply {
                    color = android.graphics.Color.parseColor("#EF4444")
                    setCircleColor(android.graphics.Color.parseColor("#EF4444"))
                    lineWidth = 2.5f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    setDrawValues(false)
                    mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
                    cubicIntensity = 0.2f
                    setDrawFilled(true)
                    setFillColor(android.graphics.Color.argb(80, 239, 68, 68)) // Light red gradient
                    highLightColor = android.graphics.Color.parseColor("#94A3B8") // Light gray for crosshair
                    setDrawHighlightIndicators(true)
                    setHighlightLineWidth(1f)
                    setDrawHorizontalHighlightIndicator(false)
                    setDrawVerticalHighlightIndicator(true)
                }
                
                val lineData = com.github.mikephil.charting.data.LineData(dataSet)
                android.util.Log.d("MonitorCharts", "Setting temp chart data with ${lineData.entryCount} entries")
                chartTemperature.data = lineData

                // CHECK 3: Check highlighted after setData
                android.util.Log.d("MARKER", "chart.highlighted = ${chartTemperature.highlighted}")

                android.util.Log.d("MonitorCharts", "Temp chart data set, visible range: ${chartTemperature.visibleXRange}")
                chartTemperature.notifyDataSetChanged()
                chartTemperature.invalidate()
                android.util.Log.d("MonitorCharts", "Temp chart invalidated, starting animation")
                chartTemperature.animateX(1000)
                
                android.util.Log.d("MonitorCharts", "=== Direct temp chart update complete ===")
            } catch (e: Exception) {
                android.util.Log.e("MonitorCharts", "Error updating temp chart", e)
            }
        }
        
        chartHumidity?.post {
            try {
                android.util.Log.d("MonitorCharts", "=== Inside post block for humid chart ===")
                android.util.Log.d("MonitorCharts", "Chart dimensions: humid width=${chartHumidity?.width}, height=${chartHumidity?.height}")
                
                // Direct chart setup without ChartHelper
                chartHumidity.description.isEnabled = false
                chartHumidity.setTouchEnabled(true)
                chartHumidity.isDragEnabled = true
                chartHumidity.setScaleEnabled(true)
                chartHumidity.setPinchZoom(true)
                chartHumidity.setDrawGridBackground(false)
                chartHumidity.legend.isEnabled = false
                
                // Configure X-axis
                chartHumidity.xAxis.apply {
                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 86400000f // 1 day in milliseconds
                    textColor = android.graphics.Color.parseColor("#64748B")
                    textSize = 10f
                    setLabelCount(6, false) // Show maximum 6 labels
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val date = java.util.Date(value.toLong())
                            val format = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                            return format.format(date)
                        }
                    }
                }
                
                // Configure Y-axis with auto-scaling and padding
                val humidValues = humidEntries.map { it.y }
                val humidMin = humidValues.minOrNull() ?: 0f
                val humidMax = humidValues.maxOrNull() ?: 0f
                val humidPadding = (humidMax - humidMin) * 0.1f // 10% padding
                
                chartHumidity.axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = android.graphics.Color.parseColor("#E2E8F0")
                    gridLineWidth = 0.5f
                    textColor = android.graphics.Color.parseColor("#64748B")
                    textSize = 10f
                    setLabelCount(5, false)
                    axisMinimum = humidMin - humidPadding
                    axisMaximum = humidMax + humidPadding
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format(java.util.Locale.getDefault(), "%.1f%%", value)
                        }
                    }
                }
                chartHumidity.axisRight.isEnabled = false
                
                // Set custom marker view with timestamp lookup
                android.util.Log.d("MonitorCharts", "Creating marker view for humidity with ${timestamps.size} timestamps")
                val markerView = com.example.safehome.ui.monitor.ChartHelper.ChartMarkerView(
                    chartHumidity.context,
                    "%",
                    timestamps
                )
                chartHumidity.marker = markerView
                android.util.Log.d("MonitorCharts", "Marker view set to humidity chart")
                
                // Configure highlight behavior
                chartHumidity.setHighlightPerDragEnabled(true)
                chartHumidity.isHighlightPerDragEnabled = true
                chartHumidity.setDrawMarkers(true) // Enable marker drawing
                
                val dataSet = com.github.mikephil.charting.data.LineDataSet(humidEntries, "Humidity").apply {
                    color = android.graphics.Color.parseColor("#3B82F6")
                    setCircleColor(android.graphics.Color.parseColor("#3B82F6"))
                    lineWidth = 2.5f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    setDrawValues(false)
                    mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
                    cubicIntensity = 0.2f
                    setDrawFilled(true)
                    setFillColor(android.graphics.Color.argb(80, 59, 130, 246)) // Light blue gradient
                    highLightColor = android.graphics.Color.parseColor("#94A3B8") // Light gray for crosshair
                    setDrawHighlightIndicators(true)
                    setHighlightLineWidth(1f)
                    setDrawHorizontalHighlightIndicator(false)
                    setDrawVerticalHighlightIndicator(true)
                }
                
                val lineData = com.github.mikephil.charting.data.LineData(dataSet)
                chartHumidity.data = lineData
                chartHumidity.notifyDataSetChanged()
                chartHumidity.invalidate()
                chartHumidity.animateX(1000)
                
                android.util.Log.d("MonitorCharts", "=== Direct humid chart update complete ===")
            } catch (e: Exception) {
                android.util.Log.e("MonitorCharts", "Error updating humid chart", e)
            }
        }
        
        chartGas?.post {
            try {
                android.util.Log.d("MonitorCharts", "=== Inside post block for gas chart ===")
                android.util.Log.d("MonitorCharts", "Chart dimensions: gas width=${chartGas?.width}, height=${chartGas?.height}")
                
                // Direct chart setup without ChartHelper
                chartGas.description.isEnabled = false
                chartGas.setTouchEnabled(true)
                chartGas.isDragEnabled = true
                chartGas.setScaleEnabled(true)
                chartGas.setPinchZoom(true)
                chartGas.setDrawGridBackground(false)
                chartGas.legend.isEnabled = false
                
                // Configure X-axis
                chartGas.xAxis.apply {
                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = android.graphics.Color.parseColor("#64748B")
                    textSize = 10f
                    setLabelCount(6, false) // Show maximum 6 labels
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            if (index >= 0 && index < timestamps.size) {
                                val timestamp = timestamps[index]
                                val date = java.util.Date(timestamp)
                                val format = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                                return format.format(date)
                            }
                            return ""
                        }
                    }
                }
                
                // Configure Y-axis with auto-scaling and padding
                val gasValues = gasEntries.map { it.y }
                val gasMin = gasValues.minOrNull() ?: 0f
                val gasMax = gasValues.maxOrNull() ?: 0f
                val gasPadding = (gasMax - gasMin) * 0.1f // 10% padding
                
                chartGas.axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = android.graphics.Color.parseColor("#E2E8F0")
                    gridLineWidth = 0.5f
                    textColor = android.graphics.Color.parseColor("#64748B")
                    textSize = 10f
                    setLabelCount(5, false)
                    axisMinimum = gasMin - gasPadding
                    axisMaximum = gasMax + gasPadding
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format(java.util.Locale.getDefault(), "%.0f ppm", value)
                        }
                    }
                }
                chartGas.axisRight.isEnabled = false
                
                // Set custom marker view with timestamp lookup
                android.util.Log.d("MonitorCharts", "Creating marker view for gas with ${timestamps.size} timestamps")
                val markerView = com.example.safehome.ui.monitor.ChartHelper.ChartMarkerView(
                    chartGas.context,
                    " ppm",
                    timestamps
                )
                chartGas.marker = markerView
                android.util.Log.d("MonitorCharts", "Marker view set to gas chart")
                
                // Configure highlight behavior
                chartGas.setHighlightPerDragEnabled(true)
                chartGas.isHighlightPerDragEnabled = true
                chartGas.setDrawMarkers(true) // Enable marker drawing
                
                val dataSet = com.github.mikephil.charting.data.LineDataSet(gasEntries, "Gas").apply {
                    color = android.graphics.Color.parseColor("#D97706")
                    setCircleColor(android.graphics.Color.parseColor("#D97706"))
                    lineWidth = 2.5f
                    circleRadius = 4f
                    setDrawCircleHole(false)
                    setDrawValues(false)
                    mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
                    cubicIntensity = 0.2f
                    setDrawFilled(true)
                    setFillColor(android.graphics.Color.argb(80, 217, 119, 6)) // Light orange gradient
                    highLightColor = android.graphics.Color.parseColor("#94A3B8") // Light gray for crosshair
                    setDrawHighlightIndicators(true)
                    setHighlightLineWidth(1f)
                    setDrawHorizontalHighlightIndicator(false)
                    setDrawVerticalHighlightIndicator(true)
                }
                
                val lineData = com.github.mikephil.charting.data.LineData(dataSet)
                chartGas.data = lineData
                chartGas.notifyDataSetChanged()
                chartGas.invalidate()
                chartGas.animateX(1000)
                
                android.util.Log.d("MonitorCharts", "=== Direct gas chart update complete ===")
            } catch (e: Exception) {
                android.util.Log.e("MonitorCharts", "Error updating gas chart", e)
            }
        }

        val tempValues = history.map { it.sensor.temperature }
        val humidValues = history.map { it.sensor.humidity }
        val gasValues = when (state.selectedGasTab) {
            GasTab.MQ2 -> history.map { (it.sensor.mq2Raw.toDouble() / 4095.0) * 1000 }
            GasTab.MQ135 -> history.map { (it.sensor.mq135Raw.toDouble() / 4095.0) * 1000 }
        }

        val (tempMin, tempMax) = ChartHelper.getMinMax(tempValues)
        val (humidMin, humidMax) = ChartHelper.getMinMax(humidValues)
        val (gasMin, gasMax) = ChartHelper.getMinMax(gasValues)
    }

    private fun updateFlameTimeline(state: com.example.safehome.ui.monitor.MonitorUiState) {
        val history = state.history
        
        // Filter to show only state change events
        val flameEvents = mutableListOf<FlameEvent>()
        var lastFlameState: Boolean? = null
        
        for (record in history.reversed()) {
            val currentFlameState = record.sensor.flameDetected
            if (lastFlameState == null || currentFlameState != lastFlameState) {
                // State changed or this is the first record
                flameEvents.add(FlameEvent(record.createdAt, currentFlameState, true))
                lastFlameState = currentFlameState
            }
        }

        val txtFlameEmpty = findViewById<TextView>(R.id.txtFlameEmpty)
        val recyclerFlameTimeline = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerFlameTimeline)

        if (flameEvents.isEmpty()) {
            txtFlameEmpty?.visibility = View.VISIBLE
            recyclerFlameTimeline?.visibility = View.GONE
        } else {
            txtFlameEmpty?.visibility = View.GONE
            recyclerFlameTimeline?.visibility = View.VISIBLE
            flameTimelineAdapter.submitEvents(flameEvents)
        }
    }

    private fun updateRecentHistory(state: com.example.safehome.ui.monitor.MonitorUiState) {
        recentHistoryAdapter.submitList(state.history.take(10))
    }

    private fun updateTimeFilterButtons(selectedFilter: TimeFilter) {
        val btnFilter24h = findViewById<MaterialButton>(R.id.btnFilter24h)
        val btnFilter7d = findViewById<MaterialButton>(R.id.btnFilter7d)
        val btnFilter30d = findViewById<MaterialButton>(R.id.btnFilter30d)

        val selectedColor = android.graphics.Color.parseColor("#3B82F6")
        val unselectedColor = android.graphics.Color.parseColor("#64748B")
        val selectedBg = android.graphics.Color.parseColor("#DBEAFE")
        val unselectedBg = android.graphics.Color.parseColor("#F1F5F9")

        when (selectedFilter) {
            TimeFilter.HOURS_24 -> {
                btnFilter24h?.setTextColor(selectedColor)
                btnFilter24h?.backgroundTintList = android.content.res.ColorStateList.valueOf(selectedBg)
                btnFilter7d?.setTextColor(unselectedColor)
                btnFilter7d?.backgroundTintList = android.content.res.ColorStateList.valueOf(unselectedBg)
                btnFilter30d?.setTextColor(unselectedColor)
                btnFilter30d?.backgroundTintList = android.content.res.ColorStateList.valueOf(unselectedBg)
            }
            TimeFilter.DAYS_7 -> {
                btnFilter24h?.setTextColor(unselectedColor)
                btnFilter24h?.backgroundTintList = android.content.res.ColorStateList.valueOf(unselectedBg)
                btnFilter7d?.setTextColor(selectedColor)
                btnFilter7d?.backgroundTintList = android.content.res.ColorStateList.valueOf(selectedBg)
                btnFilter30d?.setTextColor(unselectedColor)
                btnFilter30d?.backgroundTintList = android.content.res.ColorStateList.valueOf(unselectedBg)
            }
            TimeFilter.DAYS_30 -> {
                btnFilter24h?.setTextColor(unselectedColor)
                btnFilter24h?.backgroundTintList = android.content.res.ColorStateList.valueOf(unselectedBg)
                btnFilter7d?.setTextColor(unselectedColor)
                btnFilter7d?.backgroundTintList = android.content.res.ColorStateList.valueOf(unselectedBg)
                btnFilter30d?.setTextColor(selectedColor)
                btnFilter30d?.backgroundTintList = android.content.res.ColorStateList.valueOf(selectedBg)
            }
        }
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
        if (tabIndex == 4) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

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
        val layoutMonitorTabContent = findViewById<View>(R.id.layoutMonitorTabContent) ?: return
        val layoutDevicesTabContent = findViewById<View>(R.id.layoutDevicesTabContent) ?: return
        val layoutNotificationsTabContent = findViewById<View>(R.id.layoutNotificationsTabContent) ?: return
        val cardPlaceholderPanel = findViewById<View>(R.id.cardPlaceholderPanel) ?: return
        val btnNotifications = findViewById<View>(R.id.btnNotifications) ?: return
        val btnLogout = findViewById<View>(R.id.btnLogout) ?: return
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

        layoutMonitorTabContent.visibility = View.GONE
        layoutDevicesTabContent.visibility = View.GONE
        layoutNotificationsTabContent.visibility = View.GONE
        cardPlaceholderPanel.visibility = View.VISIBLE
        btnNotifications.visibility = View.VISIBLE
        btnLogout.visibility = View.VISIBLE

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
                layoutPlaceholderContent.visibility = View.GONE
                layoutMonitorTabContent.visibility = View.VISIBLE

                // Force update to show device list
                updateMonitorState(monitorViewModel.uiState.value)
            }
            2 -> {
                viewDevicesActiveBg.visibility = View.VISIBLE
                imgTabDevices.setColorFilter(blueDeepColor)
                txtTabDevices.setTextColor(blueDeepColor)
                txtTabDevices.setTypeface(null, android.graphics.Typeface.BOLD)

                homeScrollView.visibility = View.GONE
                layoutPlaceholderContent.visibility = View.VISIBLE
                layoutDevicesTabContent.visibility = View.VISIBLE
                cardPlaceholderPanel.visibility = View.GONE
                btnNotifications.visibility = View.GONE
                btnLogout.visibility = View.GONE
            }
            3 -> {
                viewAlertsActiveBg.visibility = View.VISIBLE
                imgTabAlerts.setColorFilter(blueDeepColor)
                txtTabAlerts.setTextColor(blueDeepColor)
                txtTabAlerts.setTypeface(null, android.graphics.Typeface.BOLD)

                homeScrollView.visibility = View.GONE
                layoutPlaceholderContent.visibility = View.GONE
                layoutNotificationsTabContent.visibility = View.VISIBLE
                cardPlaceholderPanel.visibility = View.GONE
                btnNotifications.visibility = View.GONE
                btnLogout.visibility = View.GONE
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

    private fun updateSafetyCard(
        status: com.example.safehome.data.remote.StatusDataDto?,
        lastUpdateTime: String?,
        isOffline: Boolean,
        hasNoDevices: Boolean = false
    ) {
        val cardSafetyBadge = findViewByName<com.google.android.material.card.MaterialCardView>("cardSafetyBadge")
        val txtSafetyBadge = findViewByName<TextView>("txtSafetyBadge")
        val txtSafetyMessage = findViewByName<TextView>("txtSafetyMessage")
        val txtSafetyScore = findViewByName<TextView>("txtSafetyScore")
        val txtSafetyLastUpdated = findViewByName<TextView>("txtSafetyLastUpdated")

        if (hasNoDevices) {
            txtSafetyBadge?.text = "✕ NO DEVICE"
            txtSafetyBadge?.setTextColor(android.graphics.Color.parseColor("#475569"))
            cardSafetyBadge?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F1F5F9")))
            
            txtSafetyMessage?.text = "Chưa liên kết thiết bị"
            txtSafetyScore?.text = "--%"
            txtSafetyScore?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            txtSafetyLastUpdated?.text = "Chưa kết nối"
            return
        }

        if (status == null || isOffline) {
            txtSafetyBadge?.text = "✕ OFFLINE"
            txtSafetyBadge?.setTextColor(android.graphics.Color.parseColor("#475569"))
            cardSafetyBadge?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F1F5F9")))
            
            txtSafetyMessage?.text = "Mất kết nối với thiết bị"
            txtSafetyScore?.text = "--%"
            txtSafetyScore?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            txtSafetyLastUpdated?.text = "Ngoại tuyến"
            return
        }

        val systemStatus = status.system
        val isSafe = systemStatus == "SAFE"
        val isWarning = systemStatus == "WARNING"
        val isDanger = systemStatus == "DANGER"

        txtSafetyBadge?.text = when {
            isSafe -> "✓ SAFE"
            isWarning -> "⚠ WARNING"
            isDanger -> "✕ DANGER"
            else -> "? UNKNOWN"
        }

        val badgeTextColor = when {
            isSafe -> "#065F46"
            isWarning -> "#92400E"
            isDanger -> "#991B1B"
            else -> "#475569"
        }
        txtSafetyBadge?.setTextColor(android.graphics.Color.parseColor(badgeTextColor))

        val badgeBgColor = when {
            isSafe -> "#D1FAE5"
            isWarning -> "#FEF3C7"
            isDanger -> "#FEE2E2"
            else -> "#F1F5F9"
        }
        cardSafetyBadge?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(badgeBgColor)))

        txtSafetyMessage?.text = when {
            isSafe -> "Nhà của bạn đang được bảo vệ"
            isWarning -> "Hệ thống ghi nhận cảnh báo"
            isDanger -> "Cảnh báo nguy hiểm khẩn cấp!"
            else -> "Trạng thái không xác định"
        }

        val score = when (systemStatus) {
            "SAFE" -> 98
            "WARNING" -> 65
            "DANGER" -> 25
            else -> 50
        }
        txtSafetyScore?.text = "$score%"
        
        val scoreColor = when {
            isSafe -> "#10B981"
            isWarning -> "#F59E0B"
            isDanger -> "#EF4444"
            else -> "#64748B"
        }
        txtSafetyScore?.setTextColor(android.graphics.Color.parseColor(scoreColor))

        txtSafetyLastUpdated?.text = "Cập nhật ${formatTimeAgo(lastUpdateTime)}"
    }

    private fun formatTimeAgo(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "chưa cập nhật"
        return try {
            val createdInstant = java.time.Instant.parse(isoString)
            val now = java.time.Instant.now()
            val diffSeconds = java.time.Duration.between(createdInstant, now).seconds
            val absSeconds = java.lang.Math.abs(diffSeconds)
            when {
                absSeconds < 60 -> "$absSeconds giây trước"
                absSeconds < 3600 -> "${absSeconds / 60} phút trước"
                absSeconds < 86400 -> "${absSeconds / 3600} giờ trước"
                else -> "${absSeconds / 86400} ngày trước"
            }
        } catch (e: Exception) {
            "vừa xong"
        }
    }

}
