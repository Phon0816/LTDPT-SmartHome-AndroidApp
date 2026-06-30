package com.example.safehome.data.repository

import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.AuthApi
import com.example.safehome.data.remote.LoginRequest
import com.example.safehome.data.remote.RegisterRequest
import com.example.safehome.data.remote.UserDto

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
