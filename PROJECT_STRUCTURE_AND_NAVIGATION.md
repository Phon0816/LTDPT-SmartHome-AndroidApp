# Hướng Dẫn Cấu Trúc Thư Mục và Cơ Chế Điều Hướng (Footer Navigation)

Tài liệu này cung cấp chi tiết tuyệt đối về cấu trúc thư mục, chức năng từng file, cách xây dựng các Activity và cách triển khai Footer Bottom Navigation điều hướng qua lại giữa các màn hình trong ứng dụng **SafeHome**.

---

## 1. Cấu Trúc Thư Mục Chi Tiết Của Dự Án

Dưới đây là cây thư mục đầy đủ của module `app/src/main` cùng chức năng chi tiết của từng tệp tin, không lược bớt.

```text
app/src/main/
├── AndroidManifest.xml (Khai báo quyền INTERNET, POST_NOTIFICATIONS, các Activity và Service của Firebase)
├── java/com/example/safehome/
│   ├── LoginActivity.kt (Màn hình đăng nhập, xác thực thông tin user)
│   ├── RegisterActivity.kt (Màn hình đăng ký tài khoản mới)
│   ├── HomeActivity.kt (Màn hình chính hiển thị cảm biến, danh sách thiết bị và Footer Navigation)
│   │
│   ├── data/
│   │   ├── SensorItem.kt (Data class đại diện cho một phần tử cảm biến hiển thị UI)
│   │   │
│   │   ├── local/
│   │   │   └── TokenManager.kt (Quản lý lưu trữ JWT Access Token bằng Jetpack DataStore Preferences)
│   │   │
│   │   ├── remote/
│   │   │   ├── AuthApi.kt (Các endpoint Retrofit phục vụ auth: signin, signup, getMe)
│   │   │   ├── AuthInterceptor.kt (OkHttp Interceptor tự động thêm header Authorization: Bearer <token>)
│   │   │   ├── AuthModels.kt (Các DTO request/response cho luồng authentication)
│   │   │   ├── DeviceApi.kt (Các endpoint Retrofit quản lý thiết bị: getDevices, claim, led control)
│   │   │   ├── DeviceModels.kt (Các DTO mô tả cấu trúc thiết bị, cảm biến, trạng thái)
│   │   │   ├── FcmApi.kt (Các endpoint đăng ký/hủy đăng ký FCM token với backend)
│   │   │   ├── NotificationApi.kt (Các endpoint quản lý thông báo: danh sách, đếm chưa đọc, đánh dấu đã đọc)
│   │   │   ├── NotificationModels.kt (Các DTO mô tả thông báo, lý do cảnh báo, token)
│   │   │   └── RetrofitClient.kt (Khởi tạo Retrofit, cấu hình OkHttpClient chung với interceptor)
│   │   │
│   │   └── repository/
│   │       ├── AuthRepository.kt (Repository trung gian quản lý logic auth và lưu token)
│   │       ├── DeviceRepository.kt (Repository trung gian lấy thông tin thiết bị và điều khiển đèn LED)
│   │       └── NotificationRepository.kt (Repository trung gian xử lý danh sách thông báo và đăng ký/hủy FCM)
│   │
│   ├── firebase/
│   │   ├── FcmTokenManager.kt (Quản lý lấy token từ Firebase, đăng ký và hủy đăng ký token trên backend)
│   │   └── SafeHomeFirebaseMessagingService.kt (Service kế thừa FirebaseMessagingService để nhận thông báo đẩy và đẩy lên thanh thông báo hệ thống)
│   │
│   ├── ui/
│   │   ├── HomeAdapter.kt (Adapter xử lý hiển thị danh sách nếu sử dụng RecyclerView cho cảm biến)
│   │   │
│   │   ├── auth/
│   │   │   ├── AuthViewModel.kt (ViewModel quản lý trạng thái đăng nhập, đăng ký, đăng xuất)
│   │   │   └── AuthViewModelFactory.kt (Factory khởi tạo AuthViewModel kèm repository)
│   │   │
│   │   ├── home/
│   │   │   ├── ClaimDeviceBottomSheet.kt (Hộp thoại vuốt lên từ bên dưới để liên kết thiết bị IoT mới bằng code)
│   │   │   ├── HomeViewModel.kt (ViewModel quản lý tải thiết bị, điều khiển LED, và vòng lặp poll dữ liệu cảm biến)
│   │   │   └── HomeViewModelFactory.kt (Factory khởi tạo HomeViewModel)
│   │   │
│   │   └── notification/
│   │       ├── NotificationActivity.kt (Màn hình hiển thị danh sách nhật ký cảnh báo và điều hướng footer)
│   │       ├── NotificationAdapter.kt (Adapter hiển thị danh sách thông báo trong RecyclerView)
│   │       ├── NotificationViewModel.kt (ViewModel quản lý tải danh sách thông báo, đánh dấu đã đọc)
│   │       └── NotificationViewModelFactory.kt (Factory khởi tạo NotificationViewModel)
│   │
│   └── utils/
│       └── NotificationHelper.kt (Hỗ trợ tạo Notification Channel và hiển thị thông báo đẩy cục bộ trên Android)
│
└── res/
    ├── drawable/ (Chứa toàn bộ icon PNG/XML và các hình nền bo góc glassmorphism)
    │   ├── bg_auth_button.xml (Hình nền nút bấm bo góc có hiệu ứng gradient)
    │   ├── bg_auth_glass_card.xml (Hình nền hiệu ứng kính trong suốt cho thẻ nhập liệu)
    │   ├── bg_auth_input.xml (Hình nền viền mờ cho EditText nhập liệu)
    │   ├── bg_auth_link_chip.xml (Hình nền nút liên kết chuyển trang dạng chip)
    │   ├── bg_auth_screen.xml (Background chính của màn hình đăng nhập/đăng ký)
    │   ├── bg_card_highlight.xml (Hình nền bo góc cho thẻ cảnh báo nguy hiểm)
    │   ├── bg_logo_glass.xml (Background mờ cho logo ứng dụng)
    │   ├── bg_notification_badge_warning.xml (Badge hiển thị số lượng thông báo)
    │   ├── bg_notification_icon_danger.xml (Background tròn đỏ cho icon cảnh báo cháy/gas rò rỉ)
    │   ├── bg_notification_icon_info.xml (Background tròn xanh lam cho tin nhắn hệ thống)
    │   ├── bg_notification_icon_warning.xml (Background tròn cam cho cảnh báo độ ẩm/nhiệt độ)
    │   ├── bg_notification_round_button.xml (Background nút bấm bo tròn màn thông báo)
    │   ├── bg_notification_unread_dot.xml (Chấm xanh báo hiệu tin chưa đọc)
    │   ├── bg_placeholder_glass_card.xml (Card hiển thị nội dung trống hoặc màn hình chưa có thiết bị)
    │   ├── bg_safety_card_gradient.xml (Hình nền thẻ thông tin an toàn của cảm biến)
    │   ├── bg_soft_blue_circle.xml (Hình nền tròn màu xanh cho UI cũ)
    │   ├── bg_soft_green_circle.xml (Hình nền tròn màu xanh lá cho UI cũ)
    │   ├── ic_aqi.xml (Icon đại diện chất lượng không khí)
    │   ├── ic_buzzer.png (Icon còi báo động)
    │   ├── ic_clock.xml (Icon đồng hồ chỉ thời gian thông báo)
    │   ├── ic_email_safehome.xml (Icon hòm thư trong input nhập tài khoản)
    │   ├── ic_fire.png (Icon ngọn lửa cảnh báo cháy)
    │   ├── ic_footer_alerts.png (Icon hòm thư cảnh báo ở footer)
    │   ├── ic_footer_devices.png (Icon quản lý thiết bị ở footer)
    │   ├── ic_footer_home.png (Icon ngôi nhà ở footer)
    │   ├── ic_footer_monitor.png (Icon đồ thị giám sát ở footer)
    │   ├── ic_footer_settings.png (Icon cài đặt ở footer)
    │   ├── ic_gas.png (Icon khí gas rò rỉ)
    │   ├── ic_humidity.png (Icon độ ẩm môi trường)
    │   ├── ic_lightbulb.xml (Icon bóng đèn điều khiển LED)
    │   ├── ic_lock_safehome.xml (Icon khóa trong ô mật khẩu)
    │   ├── ic_notifications.xml (Icon chuông thông báo)
    │   ├── ic_notification_back.xml (Nút quay lại trên màn hình thông báo)
    │   ├── ic_notification_bell.xml (Icon chuông hiển thị thông báo hệ thống)
    │   ├── ic_notification_clock.xml (Icon thời gian tạo thông báo)
    │   ├── ic_notification_danger.xml (Icon cảnh báo nguy hiểm khẩn cấp)
    │   ├── ic_notification_info.xml (Icon thông tin hướng dẫn)
    │   ├── ic_notification_warning.xml (Icon cảnh báo mức trung bình)
    │   ├── ic_person_safehome.xml (Icon hình người trong nhập Họ tên)
    │   ├── ic_room.xml (Icon đại diện cho căn phòng)
    │   ├── ic_safehome_mark.xml (Logo nhận diện SafeHome)
    │   ├── ic_shield_house.xml (Icon khiên bảo vệ ngôi nhà)
    │   ├── ic_temp.png (Icon nhiệt độ)
    │   ├── ic_wind.png (Icon chất lượng không khí)
    │   └── img_shield_house.png (Ảnh khiên bảo vệ ngôi nhà định dạng PNG)
    │
    ├── layout/ (Các file XML khai báo giao diện)
    │   ├── activity_login.xml (Layout màn hình Login)
    │   ├── activity_register.xml (Layout màn hình Register)
    │   ├── activity_home.xml (Layout màn hình chính chứa các ô cảm biến, danh sách thiết bị và Footer thanh điều hướng)
    │   ├── activity_notification.xml (Layout màn hình hiển thị danh sách nhật ký thông báo)
    │   ├── bottom_sheet_claim_device.xml (Layout vuốt thêm thiết bị mới)
    │   ├── item_device_room.xml (Layout của từng thiết bị trong phòng cùng 5 nút điều khiển LED)
    │   ├── item_notification.xml (Layout của một dòng thông báo cảnh báo)
    │   ├── item_sensor.xml (Layout dạng thẻ cho một cảm biến)
    │   └── layout-land/ (Chứa item_sensor.xml tối ưu cho màn hình nằm ngang)
    │
    ├── values/ (Chứa biến màu, chuỗi văn bản và cấu hình giao diện chung)
    │   ├── colors.xml (Bảng màu định danh: Primary, BlueDeep, Mint, Grey)
    │   ├── strings.xml (Các từ khóa ngôn ngữ hiển thị trên UI)
    │   └── themes.xml (Cấu hình style sáng/tối cho toàn bộ ứng dụng)
    └── values-night/
        └── themes.xml (Cấu hình theme khi hệ thống bật chế độ Dark Mode)
```

---

## 2. Cách Tạo Và Thiết Kế Một Trang Activity

Mỗi trang Activity trong ứng dụng SafeHome được xây dựng theo chuẩn kiến trúc MVVM bằng cách kết hợp XML Layout cổ điển, ViewModel quản lý trạng thái, và Coroutine Flow để cập nhật giao diện thời gian thực.

### Quy trình các bước thực hiện:

#### Bước 1: Khai báo giao diện trong thư mục `layout/`
Mỗi Activity cần một file layout XML tương ứng (ví dụ: `activity_home.xml`).
* Sử dụng `ConstraintLayout` làm thẻ cha để bố trí linh hoạt.
* Đặt ID rõ ràng cho các view tương tác bằng cú pháp: `android:id="@+id/idCuaView"` (ví dụ: `edtEmail`, `btnLogin`, `txtStatus`).

#### Bước 2: Thiết lập Activity Class kế thừa `AppCompatActivity`
Trong phương thức `onCreate()`, thực hiện ánh xạ view và khởi tạo ViewModel:
```kotlin
class LoginActivity : AppCompatActivity() {
    private lateinit var authViewModel: AuthViewModel
    private lateinit var btnLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Ánh xạ layout XML

        // Khởi tạo ViewModel qua Factory để truyền tham số Repository
        val tokenManager = TokenManager(applicationContext)
        val authApi = RetrofitClient.createAuthApi(tokenManager)
        val authRepository = AuthRepository(authApi, tokenManager)
        val factory = AuthViewModelFactory(authRepository, tokenManager)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        // Ánh xạ View bằng findViewById
        btnLogin = findViewById(R.id.btnLogin)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)

        // Đăng ký sự kiện Click
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            authViewModel.login(email, "password")
        }

        // Lắng nghe trạng thái từ ViewModel để cập nhật giao diện
        lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                // Cập nhật trạng thái loading, lỗi hoặc chuyển màn hình
                btnLogin.isEnabled = !state.isLoading
                if (state.isLoggedIn) {
                    openHome()
                }
            }
        }
    }
}
```

#### Bước 3: Đăng ký Activity trong `AndroidManifest.xml`
Tất cả các Activity mới tạo bắt buộc phải được khai báo trong thẻ `<application>`:
```xml
<activity
    android:name=".HomeActivity"
    android:exported="false"
    android:theme="@style/Theme.SafeHome" />
```

---

## 3. Cơ Chế Hoạt Động Của Footer Navigation

Hệ thống điều hướng chân trang (Footer Bottom Navigation) của ứng dụng SafeHome giúp người dùng di chuyển qua lại giữa 5 khu vực chính của ứng dụng một cách mượt mà và trực quan nhất.

### 3.1 Giao Diện Footer XML (Phần Chân Trang)
Thanh footer này được nhúng trực tiếp ở dưới cùng của tệp [activity_home.xml](file:///C:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/res/layout/activity_home.xml) và [activity_notification.xml](file:///C:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/res/layout/activity_notification.xml).

Cấu trúc gồm 5 thẻ Layout đứng ngang đại diện cho 5 Tab:
1. **Tab Trang chủ (Home)** (Index 0) - ID layout: `layoutTabHome`
2. **Tab Giám sát (Monitor)** (Index 1) - ID layout: `layoutTabMonitor`
3. **Tab Thiết bị (Devices)** (Index 2) - ID layout: `layoutTabDevices`
4. **Tab Cảnh báo (Alerts)** (Index 3) - ID layout: `layoutTabAlerts` (Hoặc nhấn vào biểu tượng chuông `bellContainer`)
5. **Tab Cài đặt (Settings)** (Index 4) - ID layout: `layoutTabSettings`

Mỗi Tab gồm:
* Một View làm nền bo tròn đánh dấu trạng thái hoạt động: `viewXXXActiveBg` (mặc định ẩn `android:visibility="invisible"`).
* Một `ImageView` chứa icon (ví dụ: `imgTabHome`).
* Một `TextView` nhãn mô tả bên dưới (ví dụ: `txtTabHome`).

---

### 3.2 Cơ Chế Điều Hướng 2 Chế Độ (In-place & Activity Transition)
Để tối ưu hiệu năng và tránh việc tạo quá nhiều Activity chồng chéo gây tốn RAM, ứng dụng chia làm 2 cách chuyển đổi tab:

#### Chế độ 1: Chuyển đổi nội dung tại chỗ (In-place Switch)
Áp dụng cho các tab: **Trang chủ (0)**, **Giám sát (1)**, **Thiết bị (2)**, **Cài đặt (4)**.
Tất cả các tab này đều nằm chung trong [HomeActivity.kt](file:///C:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/HomeActivity.kt).

* Khi click vào các tab này, hàm `selectTab(tabIndex)` được gọi:
  ```kotlin
  private fun selectTab(tabIndex: Int) {
      // 1. Ẩn toàn bộ vạch hoạt động của 5 tab
      viewHomeActiveBg.visibility = View.INVISIBLE
      viewMonitorActiveBg.visibility = View.INVISIBLE
      // ...
      
      // 2. Chuyển đổi màu sắc biểu tượng và text nhãn về màu xám nhạt (#64748B)
      imgTabHome.setColorFilter(textMutedColor)
      txtTabHome.setTextColor(textMutedColor)
      txtTabHome.setTypeface(null, Typeface.NORMAL)
      // ...

      // 3. Kích hoạt nổi bật Tab được chọn (Tô màu xanh lam #2563EB và in đậm chữ)
      when (tabIndex) {
          0 -> {
              viewHomeActiveBg.visibility = View.VISIBLE
              imgTabHome.setColorFilter(blueDeepColor)
              txtTabHome.setTextColor(blueDeepColor)
              txtTabHome.setTypeface(null, Typeface.BOLD)

              // Hiện ScrollView chính của Trang Chủ, ẩn khung giao diện chờ (Placeholder)
              homeScrollView.visibility = View.VISIBLE
              layoutPlaceholderContent.visibility = View.GONE
          }
          1 -> {
              viewMonitorActiveBg.visibility = View.VISIBLE
              imgTabMonitor.setColorFilter(blueDeepColor)
              // ...
              // Ẩn trang chủ, hiện khung chờ của tab Giám sát
              homeScrollView.visibility = View.GONE
              layoutPlaceholderContent.visibility = View.VISIBLE
              
              // Thay thế icon và văn bản hiển thị động cho phù hợp tab
              imgPlaceholderIcon.setImageResource(R.drawable.ic_footer_monitor)
              txtPlaceholderTitle.text = "Giám Sát Hệ Thống"
              txtPlaceholderDesc.text = "Biểu đồ trực quan và thống kê cảm biến hiển thị tại đây."
          }
          // Tương tự với tab Quản lý thiết bị (2) và Cài đặt (4)...
      }
  }
  ```

#### Chế độ 2: Chuyển đổi giữa các Activity (Activity Transition)
Áp dụng khi di chuyển đến **Tab Cảnh báo (3)**. Tab này yêu cầu mở màn hình danh sách thông báo riêng biệt là [NotificationActivity.kt](file:///C:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/ui/notification/NotificationActivity.kt).

* **Từ HomeActivity sang NotificationActivity**:
  Khi click vào `layoutTabAlerts` hoặc `bellContainer` trên màn hình Home:
  ```kotlin
  val intent = Intent(this, NotificationActivity::class.java)
  startActivity(intent)
  ```
  *(Màn hình NotificationActivity sẽ mở lên đè lên trên HomeActivity)*.

* **Từ NotificationActivity quay về HomeActivity**:
  Khi người dùng đứng ở màn hình thông báo và click vào bất kỳ tab nào khác (0, 1, 2, 4) ở thanh Footer của màn hình thông báo, app gọi hàm `navigateToHome(tabIndex)`:
  ```kotlin
  private fun navigateToHome(tabIndex: Int) {
      val intent = Intent(this, HomeActivity::class.java)
      // Sử dụng FLAG_ACTIVITY_REORDER_TO_FRONT để kéo HomeActivity cũ ở dưới lên trước, tránh tạo mới trùng lặp
      intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
      intent.putExtra("SELECT_TAB", tabIndex) // Gửi kèm tham số Tab cần mở
      startActivity(intent)
      finish() // Đóng NotificationActivity hiện tại ngay lập tức để tiết kiệm bộ nhớ
  }
  ```

* **Xử lý sự kiện ở HomeActivity khi quay về**:
  Khi `HomeActivity` được kéo lên phía trước qua Flag `REORDER_TO_FRONT`, hàm `onCreate()` sẽ không chạy lại mà thay vào đó hàm `onNewIntent(intent)` sẽ hứng dữ liệu:
  ```kotlin
  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      val tabToSelect = intent.getIntExtra("SELECT_TAB", -1)
      if (tabToSelect != -1) {
          selectTab(tabToSelect) // Gọi hàm kích hoạt hiển thị đúng tab đã chọn
      }
  }
  ```

Bằng cơ chế kết hợp trên, thanh Footer tạo cảm giác như toàn bộ app nằm trên cùng một màn hình duy nhất nhưng thực chất vẫn có sự phân tách rõ ràng về mặt kiến trúc phần mềm giữa các Activity riêng biệt.

---

## 4. Hướng Dẫn Cấu Hình, Khởi Tạo và Sử Dụng Các API (Retrofit)

Hệ thống kết nối mạng trong SafeHome sử dụng thư viện **Retrofit 2** kết hợp với **OkHttpClient** để gọi các API đến backend. Dưới đây là chi tiết về vị trí các file API, cơ chế tự động đính kèm Token và cách sử dụng cụ thể trong dự án.

### 4.1 Nơi Định Nghĩa Các Endpoint API (API Interfaces)
Toàn bộ các Interface định nghĩa API nằm trong gói `com.example.safehome.data.remote`. Mỗi nhóm nghiệp vụ được chia làm các file riêng biệt:

1. **[AuthApi.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/data/remote/AuthApi.kt)**:
   * Chứa các endpoint liên quan đến xác thực và thông tin cá nhân.
   * `POST api/auth/signin` - Đăng nhập tài khoản.
   * `POST api/auth/signup` - Đăng ký tài khoản mới.
   * `GET api/user/me` - Lấy thông tin tài khoản hiện tại (Yêu cầu JWT Token).

2. **[DeviceApi.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/data/remote/DeviceApi.kt)**:
   * Chứa các endpoint quản lý thiết bị phần cứng IoT.
   * `GET api/devices` - Lấy danh sách thiết bị liên kết với người dùng.
   * `POST api/devices/claim` - Liên kết thiết bị mới vào tài khoản thông qua mã code (`deviceCode`).
   * `POST api/devices/{id}/led` - Điều khiển bật/tắt các cổng đèn LED trên mạch IoT.

3. **[FcmApi.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/data/remote/FcmApi.kt)**:
   * Quản lý đăng ký thiết bị nhận thông báo đẩy.
   * `POST api/fcm/register` - Đăng ký FCM token của máy kèm tên thiết bị.
   * `POST api/fcm/unregister` - Hủy đăng ký FCM token khi đăng xuất.

4. **[NotificationApi.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/data/remote/NotificationApi.kt)**:
   * Quản lý nhật ký và trạng thái các thông báo cảnh báo.
   * `GET api/notifications` - Tải danh sách thông báo.
   * `GET api/notifications/unread-count` - Lấy số lượng cảnh báo chưa đọc.
   * `PATCH api/notifications/{id}/read` - Đánh dấu một cảnh báo cụ thể là đã đọc.
   * `PATCH api/notifications/read-all` - Đánh dấu toàn bộ danh sách cảnh báo là đã đọc.

---

### 4.2 Cơ Chế Khởi Tạo và Tự Động Gắn JWT Token
Để tránh việc phải truyền thủ công token vào từng request gọi lên backend, SafeHome sử dụng một interceptor của OkHttp để tự động chèn header `Authorization: Bearer <Token>` vào tất cả các yêu cầu.

#### Bước 1: Trình chặn đính kèm Token - [AuthInterceptor.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/data/remote/AuthInterceptor.kt)
Mỗi khi có một request HTTP được phát đi, Interceptor này sẽ:
1. Đọc bất đồng bộ JWT Token đang lưu trữ trong DataStore qua `TokenManager`.
2. Nếu tồn tại Token, chèn thêm header `Authorization` vào request gốc.
```kotlin
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.getAccessToken() }
        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}
```

#### Bước 2: Khởi tạo HttpClient và Retrofit - [RetrofitClient.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/data/remote/RetrofitClient.kt)
Đối tượng `RetrofitClient` đóng vai trò là một Singleton Factory cung cấp các API Interface đã được cấu hình HttpClient:
* **Base URL**: `https://smarthome-backend-1-4oly.onrender.com/`
* Cấu hình chuyển đổi Json tự động bằng Gson (`GsonConverterFactory`).
* Hàm tạo API mẫu:
```kotlin
object RetrofitClient {
    private const val BASE_URL = "https://smarthome-backend-1-4oly.onrender.com/"

    private fun createRetrofit(tokenManager: TokenManager): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createDeviceApi(tokenManager: TokenManager): DeviceApi {
        return createRetrofit(tokenManager).create(DeviceApi::class.java)
    }
}
```

---

### 4.3 Cách Lấy Dữ Liệu Thông Qua Lớp Repository
Lớp Repository (nằm trong thư mục `com.example.safehome.data.repository`) đóng vai trò là trung gian cô lập các cuộc gọi mạng trực tiếp, thực hiện bắt lỗi (`try-catch`) phòng tránh crash ứng dụng khi mất kết nối và chuẩn hóa kiểu dữ liệu trả về cho ViewModel.

*Ví dụ cấu hình trong [DeviceRepository.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/data/repository/DeviceRepository.kt):*
```kotlin
class DeviceRepository(private val deviceApi: DeviceApi) {

    suspend fun getDevices(): List<DeviceDto> {
        return try {
            val response = deviceApi.getDevices()
            if (response.isSuccessful) {
                response.body()?.data.orEmpty()
            } else {
                emptyList() // Trả về danh sách rỗng nếu HTTP status lỗi (401, 500,...)
            }
        } catch (e: Exception) {
            emptyList() // Tránh crash app nếu mất mạng
        }
    }
}
```

---

### 4.4 Cách Sử Dụng API Trong ViewModel và Cập Nhật Lên UI
ViewModel gọi các hàm trong Repository thông qua luồng Coroutine của Kotlin và lưu kết quả vào `StateFlow`. Giao diện Activity chỉ cần lắng nghe sự thay đổi của StateFlow này để hiển thị dữ liệu.

#### Bước 1: Gọi API bất đồng bộ trong ViewModel - [HomeViewModel.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/ui/home/HomeViewModel.kt)
```kotlin
class HomeViewModel(private val deviceRepository: DeviceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Gọi suspend function của Repository trên background thread
            val devicesList = deviceRepository.getDevices()
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                devices = devicesList
            )
        }
    }
}
```

#### Bước 2: Quan sát và cập nhật giao diện trong Activity - [HomeActivity.kt](file:///c:/Users/Admin/IdeaProjects/SmartHomeApp/app/src/main/java/com/example/safehome/HomeActivity.kt)
```kotlin
lifecycleScope.launch {
    homeViewModel.uiState.collect { state ->
        if (state.isLoading) {
            // Hiển thị vòng xoay loading
        } else {
            // Cập nhật danh sách thiết bị lên màn hình
            updateDeviceListUi(state.devices)
        }
    }
}
```
Bằng cách phân tách rạch ròi các lớp mạng như trên, mã nguồn ứng dụng luôn giữ được tính trong sạch, dễ bảo trì, tránh tối đa việc viết logic gọi mạng trực tiếp trong các file Activity gây giật/lag giao diện người dùng.
