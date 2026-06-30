package com.example.safehome

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.safehome.data.local.TokenManager
import com.example.safehome.data.remote.RetrofitClient
import com.example.safehome.data.repository.AuthRepository
import com.example.safehome.ui.auth.AuthViewModel
import com.example.safehome.ui.auth.AuthViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var btnLogin: MaterialButton
    private var lastErrorMessage: String? = null
    private var openedHome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        showRegisterSuccessIfNeeded(intent)

        authViewModel = createAuthViewModel()

        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val txtRegisterLink = findViewById<TextView>(R.id.txtRegisterLink)
        btnLogin = findViewById(R.id.btnLogin)

        txtRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.login(email, password)
        }

        lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                btnLogin.isEnabled = !state.isLoading
                btnLogin.text = if (state.isLoading) "Đang xử lý..." else "Đăng nhập"

                state.errorMessage?.let { message ->
                    if (message != lastErrorMessage) {
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                        lastErrorMessage = message
                    }
                }

                if (state.isLoggedIn && state.currentUser != null && !openedHome) {
                    openedHome = true
                    openHome()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showRegisterSuccessIfNeeded(intent)
    }

    private fun createAuthViewModel(): AuthViewModel {
        val tokenManager = TokenManager(applicationContext)
        val authApi = RetrofitClient.createAuthApi(tokenManager)
        val authRepository = AuthRepository(authApi, tokenManager)
        val factory = AuthViewModelFactory(authRepository, tokenManager)
        return ViewModelProvider(this, factory)[AuthViewModel::class.java]
    }

    private fun openHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showRegisterSuccessIfNeeded(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_REGISTER_SUCCESS, false)) {
            Toast.makeText(this, "Đăng ký thành công, vui lòng đăng nhập", Toast.LENGTH_LONG).show()
            intent.removeExtra(EXTRA_REGISTER_SUCCESS)
        }
    }

    companion object {
        const val EXTRA_REGISTER_SUCCESS = "register_success"
    }
}
