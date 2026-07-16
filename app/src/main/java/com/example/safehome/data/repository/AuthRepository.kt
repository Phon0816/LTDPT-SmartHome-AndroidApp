package com.example.safehome.data.repository

import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.AuthApi
import com.example.safehome.data.remote.LoginRequest
import com.example.safehome.data.remote.RegisterRequest
import com.example.safehome.data.remote.UserDto
import com.example.safehome.data.remote.ChangePasswordRequest
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

data class AuthActionResult(
    val success: Boolean,
    val message: String
)

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {

    suspend fun login(email: String, password: String): AuthActionResult {
        return try {
            val response = authApi.login(LoginRequest(email = email, password = password))

            if (response.isSuccessful) {
                val accessToken = response.body()?.accessToken

                if (!accessToken.isNullOrBlank()) {
                    tokenManager.saveAccessToken(accessToken)
                    AuthActionResult(true, "Đăng nhập thành công")
                } else {
                    AuthActionResult(false, "Backend không trả accessToken")
                }
            } else {
                AuthActionResult(false, loginErrorMessage(response.code()))
            }
        } catch (e: Exception) {
            AuthActionResult(false, "Lỗi kết nối API: ${e.message}")
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthActionResult {
        return try {
            val response = authApi.register(
                RegisterRequest(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    password = password
                )
            )

            if (response.isSuccessful || response.code() == 204) {
                AuthActionResult(true, "Đăng ký thành công, vui lòng đăng nhập")
            } else {
                AuthActionResult(false, registerErrorMessage(response.code()))
            }
        } catch (e: Exception) {
            AuthActionResult(false, "Lỗi kết nối API: ${e.message}")
        }
    }

    suspend fun getCurrentUser(): UserDto? {
        return try {
            val response = authApi.getMe()

            if (response.isSuccessful) {
                response.body()?.user
            } else {
                if (response.code() == 401 || response.code() == 403) {
                    tokenManager.clearAccessToken()
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateProfile(fullName: String?, avatarFile: java.io.File?): AuthActionResult {
        return try {
            val namePart = fullName?.let {
                MultipartBody.Part.createFormData("fullName", it)
            }
            val avatarPart = avatarFile?.let {
                val requestFile = RequestBody.create(
                    "image/*".toMediaTypeOrNull(),
                    it
                )
                MultipartBody.Part.createFormData("avatar", it.name, requestFile)
            }

            val response = authApi.updateProfile(namePart, avatarPart)
            if (response.isSuccessful) {
                AuthActionResult(true, "Cập nhật thông tin thành công")
            } else {
                AuthActionResult(false, "Cập nhật thất bại: code ${response.code()}")
            }
        } catch (e: Exception) {
            AuthActionResult(false, "Lỗi kết nối: ${e.message}")
        }
    }

    suspend fun changePassword(oldPass: String, newPass: String): AuthActionResult {
        return try {
            val response = authApi.changePassword(ChangePasswordRequest(oldPass, newPass))
            if (response.isSuccessful) {
                AuthActionResult(true, "Thay đổi mật khẩu thành công")
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Mật khẩu cũ và mới không hợp lệ"
                    401 -> "Mật khẩu cũ không chính xác"
                    else -> "Thay đổi mật khẩu thất bại"
                }
                AuthActionResult(false, errorMsg)
            }
        } catch (e: Exception) {
            AuthActionResult(false, "Lỗi kết nối: ${e.message}")
        }
    }

    suspend fun deleteAccount(): AuthActionResult {
        return try {
            val response = authApi.deleteAccount()
            if (response.isSuccessful) {
                tokenManager.clearAccessToken()
                AuthActionResult(true, "Xóa tài khoản thành công")
            } else {
                AuthActionResult(false, "Không thể xóa tài khoản: code ${response.code()}")
            }
        } catch (e: Exception) {
            AuthActionResult(false, "Lỗi kết nối: ${e.message}")
        }
    }

    suspend fun logout() {
        tokenManager.clearAccessToken()
    }

    private fun loginErrorMessage(code: Int): String {
        return when (code) {
            400 -> "Thiếu email hoặc mật khẩu"
            401 -> "Email hoặc mật khẩu không đúng"
            500 -> "Lỗi hệ thống, vui lòng thử lại sau"
            else -> "Đăng nhập thất bại ($code)"
        }
    }

    private fun registerErrorMessage(code: Int): String {
        return when (code) {
            401 -> "Thiếu thông tin bắt buộc"
            409 -> "Email đã tồn tại"
            500 -> "Lỗi hệ thống, vui lòng thử lại sau"
            else -> "Đăng ký thất bại ($code)"
        }
    }
}
