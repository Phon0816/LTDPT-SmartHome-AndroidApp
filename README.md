# SafeHome Android App

Ứng dụng Android Smart Home IoT cho phép người dùng quản lý, giám sát và điều khiển các thiết bị IoT thông qua giao diện di động thân thiện.

## 📋 Tổng quan dự án

SafeHome là ứng dụng Android được xây dựng bằng Kotlin, sử dụng kiến trúc MVVM với XML layout truyền thống. Ứng dụng kết nối với backend qua REST API và hỗ trợ các tính năng chính:

- 🔐 **Xác thực người dùng**: Đăng ký, đăng nhập, quản lý phiên với JWT token
- 🏠 **Quản lý thiết bị**: Liên kết thiết bị IoT mới, xem danh sách thiết bị
- 📊 **Giám sát cảm biến**: Theo dõi nhiệt độ, độ ẩm, khí gas (MQ2, MQ135), phát hiện lửa
- 💡 **Điều khiển thiết bị**: Bật/tắt LED, điều khiển còi báo động
- 🔔 **Thông báo đẩy**: Nhận cảnh báo thời gian thực qua Firebase Cloud Messaging
- 📈 **Biểu đồ & lịch sử**: Xem lịch sử hoạt động thiết bị với biểu đồ trực quan
- 🎨 **Giao diện hiện đại**: Design theo phong cách glassmorphism, clean và modern

## 🛠️ Công nghệ & Dependencies

### Core Technologies
- **Kotlin** - Ngôn ngữ lập trình chính
- **XML Layouts** - Giao diện người dùng (không sử dụng Jetpack Compose)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36 (Android 14)

### Main Dependencies
```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Data Storage
implementation("androidx.datastore:datastore-preferences:1.2.1")

// Architecture Components
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

// UI Components
implementation("androidx.constraintlayout:constraintlayout:2.2.0")
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
implementation(libs.material)

// Firebase
implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
implementation("com.google.firebase:firebase-messaging")

// Charts
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
```

## 📁 Cấu trúc dự án

```
app/src/main/
├── AndroidManifest.xml                    # Khai báo quyền, Activity, Service
├── java/com/example/safehome/
│   ├── LoginActivity.kt                   # Màn hình đăng nhập
│   ├── RegisterActivity.kt                # Màn hình đăng ký
│   ├── HomeActivity.kt                    # Màn hình chính với footer navigation
│   ├── data/
│   │   ├── local/
│   │   │   └── TokenManager.kt             # Quản lý JWT token với DataStore
│   │   ├── remote/
│   │   │   ├── AuthApi.kt                 # API xác thực
│   │   │   ├── DeviceApi.kt               # API thiết bị
│   │   │   ├── NotificationApi.kt         # API thông báo
│   │   │   ├── FcmApi.kt                  # API Firebase Cloud Messaging
│   │   │   ├── AuthModels.kt              # DTO cho auth
│   │   │   ├── DeviceModels.kt            # DTO cho thiết bị
│   │   │   ├── NotificationModels.kt      # DTO cho thông báo
│   │   │   ├── AuthInterceptor.kt         # Interceptor thêm Bearer token
│   │   │   └── RetrofitClient.kt          # Retrofit client singleton
│   │   └── repository/
│   │       ├── AuthRepository.kt          # Repository xác thực
│   │       ├── DeviceRepository.kt        # Repository thiết bị
│   │       └── NotificationRepository.kt  # Repository thông báo
│   ├── firebase/
│   │   ├── FcmTokenManager.kt             # Quản lý FCM token
│   │   └── SafeHomeFirebaseMessagingService.kt  # Service nhận thông báo đẩy
│   ├── ui/
│   │   ├── auth/
│   │   │   ├── AuthViewModel.kt           # ViewModel auth
│   │   │   └── AuthViewModelFactory.kt    # Factory cho AuthViewModel
│   │   ├── home/
│   │   │   ├── HomeViewModel.kt           # ViewModel màn hình chính
│   │   │   ├── HomeViewModelFactory.kt    # Factory cho HomeViewModel
│   │   │   └── ClaimDeviceBottomSheet.kt  # Bottom sheet liên kết thiết bị
│   │   ├── device/
│   │   │   ├── DeviceActivity.kt          # Màn hình quản lý thiết bị
│   │   │   ├── DeviceDetailActivity.kt    # Màn hình chi tiết thiết bị
│   │   │   ├── DeviceHistoryActivity.kt   # Màn hình lịch sử thiết bị
│   │   │   ├── DeviceViewModel.kt         # ViewModel thiết bị
│   │   │   └── DeviceAdapter.kt           # Adapter danh sách thiết bị
│   │   ├── monitor/
│   │   │   ├── MonitorViewModel.kt        # ViewModel giám sát
│   │   │   ├── ChartHelper.kt             # Helper vẽ biểu đồ
│   │   │   └── MonitorDeviceAdapter.kt    # Adapter giám sát thiết bị
│   │   └── notification/
│   │       ├── NotificationActivity.kt     # Màn hình danh sách thông báo
│   │       ├── NotificationViewModel.kt  # ViewModel thông báo
│   │       └── NotificationAdapter.kt     # Adapter danh sách thông báo
│   └── utils/
│       └── NotificationHelper.kt          # Helper tạo notification channel
└── res/
    ├── drawable/                           # Icons, backgrounds, drawables
    ├── layout/                             # XML layouts
    ├── values/                             # Colors, strings, themes
    └── xml/                                # Network security config
```

## 🏗️ Kiến trúc ứng dụng

### MVVM Architecture
```
Activity/Fragment → ViewModel → Repository → API/Local Storage
```

- **Activity/Fragment**: Hiển thị UI, xử lý user interaction, observe ViewModel state
- **ViewModel**: Quản lý business logic, state, gọi Repository
- **Repository**: Trung gian giữa ViewModel và data sources (API, local storage)
- **API/Local Storage**: Retrofit cho network calls, DataStore cho local storage

## 🧠 Logic Nghiệp Vụ Chi Tiết

### 1. Authentication Logic

#### Session Check Flow
```
App mở → AuthViewModel.init() → checkSession()
  ↓
Đọc token từ DataStore
  ↓
Nếu token rỗng → isLoggedIn = false → Ở lại Login
  ↓
Nếu token có → Gọi API /api/user/me
  ↓
Success → isLoggedIn = true → Chuyển sang Home
  ↓
401/403 → Xóa token → isLoggedIn = false → Ở lại Login
```

#### Login Flow
```
User nhập email/password → LoginActivity.validate()
  ↓
AuthViewModel.login(email, password)
  ↓
AuthRepository.login() → POST /api/auth/signin
  ↓
Success → Lưu accessToken vào DataStore
  ↓
Gọi /api/user/me để lấy user info
  ↓
Success → isLoggedIn = true → Chuyển Home
  ↓
Error → Hiển thị error message
```

#### Register Flow
```
User nhập thông tin → RegisterActivity.validate()
  ↓
AuthViewModel.register(firstName, lastName, email, password)
  ↓
AuthRepository.register() → POST /api/auth/signup
  ↓
Success (204 No Content) → Quay về Login
  ↓
Hiển thị "Đăng ký thành công, vui lòng đăng nhập"
```

#### Error Handling
- **400**: Thiếu email hoặc mật khẩu
- **401**: Email hoặc mật khẩu không đúng
- **409**: Email đã tồn tại
- **500**: Lỗi hệ thống

### 2. Device Management Logic

#### Real-time Polling
```
HomeViewModel.loadDeviceHistory(deviceId)
  ↓
Bắt đầu coroutine loop với delay 3 giây
  ↓
Mỗi 3 giây → Gọi /api/user/devices
  ↓
Cập nhật activeDevice với sensor data mới
  ↓
Nếu đang điều khiển (isControlling = true) → Bỏ qua poll
  ↓
Nếu điều khiển xong → Đợi 4 giây rồi resume polling
```

#### LED Control với Optimistic Update
```
User toggle LED → HomeViewModel.controlDeviceLed()
  ↓
1. UPDATE UI NGAY (Optimistic Update)
  - Cập nhật state trong memory
  - Hiển thị LED mới lên UI
  ↓
2. Gửi lệnh xuống ESP32
  - POST /api/user/devices/{id}/control
  - isControlling = true (dừng polling)
  ↓
3. Xử lý response
  - Success → Giữ nguyên state mới
  - Error → Revert về state cũ
  ↓
4. Resume polling sau 4 giây
  - isControlling = false
```

#### Device Claim Flow
```
User nhập deviceCode, deviceSecret, deviceName
  ↓
HomeViewModel.claimDevice()
  ↓
POST /api/user/claim
  ↓
Success → Tự động loadDevices() → Cập nhật danh sách
  ↓
Error theo status code:
  - 400/401: Mã thiết bị hoặc mật khẩu không đúng
  - 404: Không tìm thấy thiết bị
  - 409: Thiết bị đã được liên kết với tài khoản khác
```

### 3. Notification Logic

#### Load Notifications
```
NotificationViewModel.init()
  ↓
loadNotifications()
  ↓
GET /api/notifications
  ↓
GET /api/notifications/unread-count
  ↓
Map DTO → NotificationItem với type (Danger/Warning/Info)
  ↓
Cập nhật UI với danh sách và badge count
```

#### Mark as Read Flow
```
User tap notification → markAsRead(item)
  ↓
PATCH /api/notifications/{id}/read
  ↓
Success → Cập nhật isRead = true locally
  ↓
Giảm unreadCount (nếu trước đó chưa đọc)
  ↓
Error → Hiển thị action message
```

#### Mark All as Read
```
User tap "Đánh dấu tất cả đã đọc"
  ↓
markAllAsRead()
  ↓
PATCH /api/notifications/read-all
  ↓
Success → Set tất cả isRead = true
  ↓
Set unreadCount = 0
```

### 4. FCM Token Management

#### Token Registration
```
User đăng nhập thành công
  ↓
FcmTokenManager.syncTokenIfLoggedIn()
  ↓
Lấy FCM token từ Firebase
  ↓
POST /api/fcm/register với deviceName
  ↓
Success → Token đã đăng ký để nhận push notification
```

#### Token Unregistration
```
User đăng xuất
  ↓
FcmTokenManager.unregisterCurrentToken()
  ↓
POST /api/fcm/unregister
  ↓
Success → Không còn nhận push notification
```

### 5. Monitor Logic

#### Time Filter
```
User chọn filter (24h / 7 ngày / 30 ngày)
  ↓
MonitorViewModel.selectTimeFilter(filter)
  ↓
Tính cutoffTime dựa trên Instant.now()
  ↓
Filter fullHistory theo createdAt > cutoffTime
  ↓
Cập nhật UI với filtered data
```

#### Gas Tab Switching
```
User chọn tab MQ2 hoặc MQ135
  ↓
MonitorViewModel.selectGasTab(tab)
  ↓
Cập nhật selectedGasTab
  ↓
Chart vẽ lại với gas data tương ứng
```

### 6. Data Flow Summary

#### Authentication Flow
- User đăng nhập → API call → Lưu JWT token vào DataStore
- Mở app → Kiểm tra token → Gọi `/api/user/me` → Valid thì vào Home
- Token hết hạn/invalid → Xóa token → Quay về Login

#### Device Management Flow
- Lấy danh sách thiết bị từ `/api/user/devices`
- Liên kết thiết bị mới với code qua `/api/user/claim`
- Điều khiển LED qua `/api/user/devices/{id}/control`
- Polling real-time mỗi 3 giây

#### Notification Flow
- Firebase gửi FCM token → Đăng ký với backend qua `/api/fcm/register`
- Backend gửi push notification → FirebaseMessagingService nhận → Hiển thị notification
- Lấy danh sách thông báo từ `/api/notifications`

## 🔌 API Endpoints

### Base URL
```
https://smarthome-backend-1-4oly.onrender.com/
```

### Authentication
- `POST /api/auth/signin` - Đăng nhập
- `POST /api/auth/signup` - Đăng ký
- `GET /api/user/me` - Lấy thông tin user hiện tại

### Devices
- `GET /api/user/devices` - Lấy danh sách thiết bị
- `GET /api/user/devices/{id}` - Lấy chi tiết thiết bị
- `GET /api/user/devices/{id}/history` - Lấy lịch sử thiết bị
- `POST /api/user/claim` - Liên kết thiết bị mới
- `POST /api/user/devices/{id}/control` - Điều khiển thiết bị (LED, buzzer)

### Notifications
- `GET /api/notifications` - Lấy danh sách thông báo
- `GET /api/notifications/unread-count` - Lấy số lượng thông báo chưa đọc
- `PATCH /api/notifications/{id}/read` - Đánh dấu đã đọc
- `PATCH /api/notifications/read-all` - Đánh dấu tất cả đã đọc

### FCM
- `POST /api/fcm/register` - Đăng ký FCM token
- `POST /api/fcm/unregister` - Hủy đăng ký FCM token

## 🚀 Cài đặt và chạy

### Yêu cầu
- Android Studio Hedgehog hoặc mới hơn
- JDK 11 trở lên
- Android SDK với API Level 26 trở lên
- Gradle 8.0+

### Các bước cài đặt

1. **Clone repository**:
```bash
git clone <repository-url>
cd LTDPT-SmartHome-AndroidApp
```

2. **Mở project trong Android Studio**:
   - File → Open → Chọn thư mục project

3. **Cấu hình Firebase** (nếu cần):
   - Thêm file `google-services.json` vào `app/`
   - File này đã có sẵn trong project

4. **Sync Gradle**:
   - Android Studio sẽ tự động sync Gradle
   - Hoặc chạy: `File → Sync Project with Gradle Files`

5. **Chạy ứng dụng**:
   - Chọn emulator hoặc device
   - Click Run button hoặc nhấn `Shift + F10`

### Build APK
```bash
# Debug APK
./gradlew :app:assembleDebug

# Release APK
./gradlew :app:assembleRelease
```

APK sẽ được tạo trong `app/build/outputs/apk/`

## 📱 Tính năng chính

### 1. Xác thực (Authentication)
- Đăng ký tài khoản mới với email, password
- Đăng nhập với email/password
- Tự động lưu phiên đăng nhập với JWT token
- Tự động kiểm tra token khi mở app
- Đăng xuất và xóa token

### 2. Màn hình chính (Home)
- Footer navigation với 5 tab:
  - **Trang chủ**: Hiển thị tổng quan cảm biến
  - **Giám sát**: Biểu đồ và thống kê
  - **Thiết bị**: Quản lý danh sách thiết bị
  - **Cảnh báo**: Danh sách thông báo
  - **Cài đặt**: Cấu hình ứng dụng
- Hiển thị trạng thái cảm biến real-time
- Thông báo badge cho cảnh báo chưa đọc

### 3. Quản lý thiết bị (Devices)
- Xem danh sách thiết bị đã liên kết
- Liên kết thiết bị mới với device code
- Xem chi tiết thiết bị (sensor data, control status)
- Điều khiển LED (5 channels)
- Điều khiển còi báo động (buzzer)
- Xem lịch sử hoạt động thiết bị

### 4. Giám sát (Monitor)
- Biểu đồ nhiệt độ, độ ẩm theo thời gian
- Biểu đồ nồng độ khí gas (MQ2, MQ135)
- Timeline phát hiện lửa
- So sánh dữ liệu giữa các thiết bị

### 5. Thông báo (Notifications)
- Danh sách cảnh báo và thông báo hệ thống
- Đánh dấu đã đọc từng thông báo
- Đánh dấu tất cả đã đọc
- Badge số lượng thông báo chưa đọc
- Phân loại: Danger (cháy/gas), Warning (nhiệt độ/độ ẩm), Info (hệ thống)

### 6. Firebase Cloud Messaging
- Tự động đăng ký FCM token khi đăng nhập
- Hủy đăng ký khi đăng xuất
- Nhận thông báo đẩy real-time
- Hiển thị notification trên system tray

## 🎨 Giao diện người dùng

### Design System
- **Color Palette**: Xanh dương (#2563EB), Mint xanh lá, Trắng, Xám
- **Style**: Glassmorphism, bo góc lớn, shadow mềm
- **Typography**: Clean, modern, dễ đọc
- **Components**: Material Design 3

### Screens
- **Login/Register**: Card glassmorphism với input fields
- **Home**: Dashboard với sensor cards và device list
- **Device Detail**: Chi tiết thiết bị với control buttons
- **Monitor**: Biểu đồ MPAndroidChart với timeline
- **Notifications**: List với badges và icons phân loại

## 🔐 Bảo mật

- JWT token lưu trữ trong DataStore Preferences (encrypted)
- HTTPS cho tất cả API calls
- AuthInterceptor tự động thêm Bearer token
- Token validation khi mở app
- Xóa token khi đăng xuất hoặc token invalid

## 📝 Lưu ý quan trọng

### Không được thay đổi
- Package name: `com.example.safehome`
- Backend base URL
- API endpoints structure
- Auth flow và token management

### Các vấn đề đã gặp
- Lottie animation gây crash → Không sử dụng
- Shimmer/BlurView gây lag → Không sử dụng
- UTF-8 encoding cho text tiếng Việt

### Best Practices
- Giữ nguyên ID của views trong XML layout
- Validate input trước khi gọi API
- Xử lý error với try-catch trong Repository
- Sử dụng Coroutine cho async operations
- Observe StateFlow trong Activity/Fragment

## 🤝 Đóng góp

Để đóng góp vào dự án:

1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push đến branch (`git push origin feature/AmazingFeature`)
5. Mở Pull Request

## 📄 License

Dự án này là phần của dự án SafeHome AIoT.

## 👥 Team

- Team SafeHome AIoT

## 📞 Liên hệ

Nếu có câu hỏi hoặc vấn đề, vui lòng liên hệ với team phát triển.

---

**SafeHome Android App** - Smart Home IoT Solution
