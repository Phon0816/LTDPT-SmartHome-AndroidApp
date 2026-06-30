package com.example.safehome.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.UserDto
import com.example.safehome.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val currentUser: UserDto? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val token = tokenManager.getAccessToken()

                if (token.isNullOrBlank()) {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = false,
                        currentUser = null
                    )
                    return@launch
                }

                val user = authRepository.getCurrentUser()

                if (user != null) {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = user
                    )
                } else {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = false,
                        currentUser = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isLoggedIn = false,
                    currentUser = null,
                    errorMessage = "Không thể kiểm tra phiên đăng nhập: ${e.message}"
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val loginResult = authRepository.login(email, password)

            if (loginResult.success) {
                val user = authRepository.getCurrentUser()

                if (user != null) {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = user,
                        successMessage = loginResult.message
                    )
                } else {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isLoggedIn = false,
                        errorMessage = "Không lấy được thông tin người dùng"
                    )
                }
            } else {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isLoggedIn = false,
                    errorMessage = loginResult.message
                )
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val registerResult = authRepository.register(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password
            )

            _uiState.value = if (registerResult.success) {
                AuthUiState(
                    isLoading = false,
                    isLoggedIn = false,
                    successMessage = registerResult.message
                )
            } else {
                AuthUiState(
                    isLoading = false,
                    isLoggedIn = false,
                    errorMessage = registerResult.message
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } finally {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    isLoggedIn = false,
                    currentUser = null
                )
            }
        }
    }
}
