# SafeHome Android Project Context

## 1. Purpose

SafeHome la app Android Kotlin cho du an SafeHome AIoT. App dung de dang nhap, dang ky, luu phien nguoi dung bang access token, va hien thi thong tin nguoi dung hien tai tu backend.

Project hien tai tap trung vao flow auth co ban:

- Dang ky tai khoan that qua backend.
- Dang nhap qua backend va nhan `accessToken`.
- Luu `accessToken` bang DataStore.
- Mo app lai thi kiem tra token bang `/api/user/me`.
- Vao Home neu token hop le, quay lai Login neu chua co token hoac token sai/het han.
- Dang xuat thi xoa token va clear back stack ve Login.

## 2. Project Overview

Day la Android app truyen thong dung XML layout, AppCompat va Material Components. Project khong dung Jetpack Compose.

Thong tin chinh:

- Root project: `D:\SafeHome`
- Module app: `app`
- Package/application id: `com.example.safehome`
- Main launcher hien tai: `LoginActivity`
- Man hinh auth: `LoginActivity`, `RegisterActivity`
- Man hinh sau dang nhap: `HomeActivity`
- Backend base URL: `https://smarthome-backend-lvin.onrender.com/`
- API docs da xac minh: `https://smarthome-backend-lvin.onrender.com/api-docs/`

## 3. Architecture Layers

Project dang chia theo cac lop don gian:

- Activity layer: nhan input tu UI, observe `AuthUiState`, dieu huong giua Login/Register/Home.
- ViewModel layer: gom logic trang thai dang nhap, dang ky, kiem tra phien, dang xuat.
- Repository layer: goi API qua Retrofit, xu ly response/error, luu/xoa token thong qua `TokenManager`.
- Remote/API layer: khai bao Retrofit endpoints va DTO models.
- Local storage layer: luu `accessToken` bang Android DataStore Preferences.

Luong phu thuoc chinh:

```text
Activity -> AuthViewModel -> AuthRepository -> AuthApi/TokenManager
AuthApi -> Retrofit + OkHttp + AuthInterceptor
TokenManager -> DataStore Preferences
```

## 4. Technology / Dependencies

Project su dung:

- Kotlin Android
- XML layouts
- AppCompat
- Material Components
- Retrofit 2.11.0
- Gson converter 2.11.0
- OkHttp 4.12.0
- DataStore Preferences 1.2.1
- Lifecycle ViewModel KTX 2.8.7
- Lifecycle Runtime KTX 2.8.7
- ConstraintLayout 2.2.0

Khong su dung Compose. Khong con dung Lottie, Shimmer hoac BlurView trong trang thai hien tai.

Repositories Gradle hien tai:

- `google()`
- `mavenCentral()`
- `gradlePluginPortal()` trong `pluginManagement`

## 5. Important Files / Structure

### Manifest

- `app/src/main/AndroidManifest.xml`
  - Khai bao quyen `android.permission.INTERNET`.
  - Khai bao `.LoginActivity` la launcher voi `MAIN` + `LAUNCHER`.
  - Khai bao `.RegisterActivity` va `.HomeActivity`.
  - Theme app: `@style/Theme.SafeHome`.

### Activities

- `app/src/main/java/com/example/safehome/LoginActivity.kt`
  - Man hinh dang nhap.
  - Dung `AuthViewModel`.
  - Goi login qua ViewModel.
  - Mo `RegisterActivity` khi bam link dang ky.
  - Nhan extra `register_success` de hien thong bao dang ky thanh cong.
  - Dang nhap thanh cong thi mo `HomeActivity` va clear back stack.

- `app/src/main/java/com/example/safehome/RegisterActivity.kt`
  - Man hinh dang ky.
  - Input: first name, last name, email, password, confirm password.
  - Validate khong de trong va password confirm phai trung.
  - Goi API signup qua ViewModel.
  - Dang ky thanh cong thi quay ve Login, khong tu dang nhap vi backend tra `204 No Content`.

- `app/src/main/java/com/example/safehome/HomeActivity.kt`
  - Man hinh sau dang nhap.
  - Hien thi thong tin user that tu `/api/user/me`.
  - Co nut dang xuat.
  - Dang xuat xoa token va quay lai Login, clear back stack.

### Auth UI State / ViewModel

- `app/src/main/java/com/example/safehome/ui/auth/AuthViewModel.kt`
  - Chua `AuthUiState`.
  - Quan ly `isLoading`, `isLoggedIn`, `currentUser`, `errorMessage`, `successMessage`.
  - Co cac ham `checkSession()`, `login()`, `register()`, `logout()`.
  - `init` tu dong goi `checkSession()`.

- `app/src/main/java/com/example/safehome/ui/auth/AuthViewModelFactory.kt`
  - Tao `AuthViewModel` voi `AuthRepository` va `TokenManager`.

### Repository / API / Models

- `app/src/main/java/com/example/safehome/data/repository/AuthRepository.kt`
  - Goi login/register/me qua `AuthApi`.
  - Luu token sau khi login thanh cong.
  - Xoa token khi logout.
  - Xoa token neu `/api/user/me` tra 401/403.
  - Co `try/catch` de tranh crash khi loi mang/backend.

- `app/src/main/java/com/example/safehome/data/remote/AuthApi.kt`
  - Khai bao Retrofit endpoints:
    - `POST api/auth/signin`
    - `POST api/auth/signup`
    - `GET api/user/me`

- `app/src/main/java/com/example/safehome/data/remote/AuthModels.kt`
  - DTO auth hien tai:
    - `LoginRequest`
    - `RegisterRequest`
    - `LoginResponse`
    - `MeResponse`
    - `UserDto`

- `app/src/main/java/com/example/safehome/data/remote/RetrofitClient.kt`
  - Tao Retrofit client.
  - Base URL: `https://smarthome-backend-lvin.onrender.com/`
  - Gan `AuthInterceptor` vao OkHttpClient.

- `app/src/main/java/com/example/safehome/data/remote/AuthInterceptor.kt`
  - Lay token tu `TokenManager`.
  - Neu co token thi them header `Authorization: Bearer <token>`.

- `app/src/main/java/com/example/safehome/data/local/TokenManager.kt`
  - Luu `accessToken` bang DataStore Preferences.
  - Key: `access_token`.
  - Ham chinh: `saveAccessToken`, `getAccessToken`, `clearAccessToken`.

### Layout / UI

- `app/src/main/res/layout/activity_login.xml`
  - Login UI.
  - Cac id quan trong khong duoc doi neu khong cap nhat Activity:
    - `edtEmail`
    - `edtPassword`
    - `btnLogin`
    - `txtRegisterLink`

- `app/src/main/res/layout/activity_register.xml`
  - Register UI.
  - Cac id dang duoc `RegisterActivity` dung:
    - `edtFirstName`
    - `edtLastName`
    - `edtRegisterEmail`
    - `edtRegisterPassword`
    - `edtConfirmPassword`
    - `btnRegister`
    - `txtLoginLink`

- `app/src/main/res/layout/activity_home.xml`
  - Home UI.
  - Cac id dang duoc `HomeActivity` dung:
    - `txtStatus`
    - `txtName`
    - `txtEmail`
    - `txtRole`
    - `btnLogout`

### Drawables / Theme

Auth UI dang dung cac drawable XML rieng:

- `bg_auth_screen.xml`
- `bg_auth_glass_card.xml`
- `bg_auth_input.xml`
- `bg_auth_button.xml`
- `bg_auth_link_chip.xml`
- `bg_card_highlight.xml`
- `bg_logo_glass.xml`
- `ic_safehome_mark.xml`
- `ic_email_safehome.xml`
- `ic_lock_safehome.xml`
- `ic_person_safehome.xml`

Theme va mau:

- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml`

## 6. Verified Backend APIs

Cac API auth da duoc xac minh tu Swagger/API docs backend.

### Login

- Method/path: `POST /api/auth/signin`
- Body:

```json
{
  "email": "user@example.com",
  "password": "password"
}
```

- Response 200:

```json
{
  "accessToken": "..."
}
```

Trong Android:

- `AuthApi.login()` phai dung `api/auth/signin`.
- `LoginResponse` phai co field `accessToken`.
- Khong dung endpoint cu `api/auth/login`.

### Register

- Method/path: `POST /api/auth/signup`
- Body:

```json
{
  "firstName": "First",
  "lastName": "Last",
  "email": "user@example.com",
  "password": "password"
}
```

- Response thanh cong: `204 No Content`
- Backend khong tra token sau signup.

Trong Android:

- Dang ky thanh cong thi quay ve Login va bao nguoi dung dang nhap.
- Khong luu token gia.
- Khong tu chuyen Home neu signup khong tra token.

### Current User

- Method/path: `GET /api/user/me`
- Header bat buoc:

```text
Authorization: Bearer <accessToken>
```

- Response:

```json
{
  "user": {
    "id": 1,
    "fullName": "User Name",
    "email": "user@example.com",
    "role": "USER",
    "createdAt": "..."
  }
}
```

Trong Android:

- `AuthInterceptor` gan Bearer token tu DataStore.
- `MeResponse` co field `user`.
- `UserDto` co `id`, `fullName`, `email`, `role`, `createdAt`.

## 7. Current Auth / Session Flow

### Khi mo app

1. `LoginActivity` la launcher.
2. `AuthViewModel` duoc tao.
3. `AuthViewModel.init` goi `checkSession()`.
4. `checkSession()` doc token tu `TokenManager`.
5. Neu khong co token: `isLoggedIn = false`, o lai Login.
6. Neu co token: goi `/api/user/me`.
7. Neu lay duoc user: `isLoggedIn = true`, `currentUser != null`, Activity chuyen sang Home.
8. Neu token sai/het han hoac backend tra 401/403: Repository xoa token, ViewModel set logged out.

### Login

1. User nhap email/password.
2. `LoginActivity` validate khong de trong.
3. Goi `AuthViewModel.login(email, password)`.
4. Repository goi `POST /api/auth/signin`.
5. Neu response co `accessToken`, token duoc luu vao DataStore.
6. ViewModel goi tiep `/api/user/me` de lay user.
7. Neu co user: chuyen sang `HomeActivity`, clear back stack.
8. Neu loi: hien Toast/error message, khong crash.

### Register

1. User mo `RegisterActivity` tu link tren Login.
2. Nhap first name, last name, email, password, confirm password.
3. Activity validate field va confirm password.
4. Goi `AuthViewModel.register(...)`.
5. Repository goi `POST /api/auth/signup`.
6. Neu response thanh cong/204: quay ve Login voi extra `register_success`.
7. Login hien thong bao dang ky thanh cong, yeu cau dang nhap.

### Home

1. `HomeActivity` tao `AuthViewModel`.
2. ViewModel kiem tra session va goi `/api/user/me`.
3. Neu co user: hien `fullName`, `email`, `role`.
4. Neu logout: xoa token va quay ve Login, clear back stack.
5. Neu token khong hop le: xoa token va quay ve Login.

## 8. Database / Security

Android app khong ket noi truc tiep database. App chi goi backend API qua HTTPS.

Nhung thong tin tuyet doi khong nen dua vao Android app:

- Database URL
- Database username/password
- JWT secret
- Private API keys
- Backend environment variables

Database engine/host khong duoc xac minh truc tiep trong source Android hien tai. Neu can tai lieu database, lay tu backend repo hoac backend deployment config, khong suy luan tu Android app.

Token hien tai duoc luu bang DataStore Preferences. Day la cach don gian de chay flow hoc tap/demo. Neu len production can can nhac hardening them, nhung khong thay doi lan man neu chua co yeu cau.

## 9. Current UI Standards

UI hien tai theo huong:

- XML layout, AppCompat, Material Components.
- Tone SafeHome: xanh/trang/mint.
- Phong cach clean, modern, Apple-like nhe.
- Card auth bo goc lon, nen sang, shadow mem.
- Khong dung Compose.
- Khong dung thanh mau tim mac dinh Android cho auth UI.
- Khong dung background hinh tron/blob neu user da yeu cau bo.
- Khong dung Lottie/Shimmer/BlurView trong trang thai hien tai vi tung gay crash/lag.

Khi sua UI auth:

- Giu cac id Activity dang findViewById.
- Neu doi id thi phai sua Activity tuong ung.
- Kiem tra text tieng Viet de tranh mojibake.
- Uu tien UI gon, sang, doc ro, chay tot tren emulator Pixel 7.

## 10. Constraints Not To Break

Khong duoc pha cac rang buoc sau neu khong co yeu cau ro:

- Khong doi package name `com.example.safehome`.
- Khong doi base URL backend.
- Khong them Compose.
- Khong hardcode email/password test.
- Khong luu token gia.
- Khong tu bia endpoint API.
- Khong dung lai endpoint sai `api/auth/login`.
- Khong chuyen Register vao Home neu signup chi tra `204 No Content`.
- Khong bo `INTERNET` permission.
- Khong bo `LoginActivity` launcher neu chua co flow Splash/Session rieng.
- Khong sua app logic khi chi duoc yeu cau sua UI hoac doc project.
- Khong them thu vien visual nang neu khong that su can, vi app da tung gap crash/lag voi Lottie/Shimmer/BlurView.

## 11. Sensitive Issues / Previous Bugs

Cac van de da tung gap hoac dang can luu y:

- Lottie tung lam app crash voi loi `IllegalStateException: Unable to parse composition`. Khong nen them lai Lottie neu chua kiem tra asset JSON ky.
- Shimmer/BlurView tung lam UI lag/skipped frames tren emulator. Hien tai khong con dung.
- Android Studio/emulator tung bao install/run issue khi may ao chua boot xong. Can doi emulator boot xong roi run lai.
- Co file/comment/text tieng Viet trong source hien tai hien thi mojibake o mot so noi khi doc bang terminal. Khi sua text UI hoac message, can dam bao file luu UTF-8 dung.
- Mot so drawable cu nhu `bg_soft_blue_circle.xml`, `bg_soft_green_circle.xml`, `bg_login_screen.xml` co the con ton tai trong project. Neu khong duoc layout tham chieu thi chung chi la resource cu, khong nen xoa neu chua kiem tra usage.
- Neu `/api/user/me` tra 401/403 thi Repository phai xoa token de tranh ket o trang thai phien hong.

## 12. Current Project State

Trang thai hien tai:

- App build debug da tung chay thanh cong voi lenh:

```powershell
.\gradlew.bat :app:assembleDebug
```

- Auth endpoints da duoc cap nhat theo backend that:
  - Login: `/api/auth/signin`
  - Register: `/api/auth/signup`
  - Me: `/api/user/me`

- Register flow that da co:
  - Signup tra `204 No Content`.
  - App quay ve Login va hien thong bao thanh cong.

- Session flow da co:
  - Token luu bang DataStore.
  - Mo app goi `/api/user/me` neu co token.
  - 401/403 xoa token.

- Home flow da co:
  - Hien user that tu backend.
  - Logout xoa token va quay ve Login.

- UI auth da duoc lam lai theo huong modern/glass nhe, nhung van nen giu nguyen cac id dang dung trong Activity khi tiep tuc polish.

Neu lam viec tiep, nen doc file nay truoc khi sua code de tranh lap lai loi endpoint, token flow, hoac them thu vien UI gay crash/lag.
