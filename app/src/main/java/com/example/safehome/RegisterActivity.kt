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

class RegisterActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var btnRegister: MaterialButton
    private var lastErrorMessage: String? = null
    private var returnedToLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authViewModel = createAuthViewModel()

        val edtFirstName = findViewById<TextInputEditText>(R.id.edtFirstName)
        val edtLastName = findViewById<TextInputEditText>(R.id.edtLastName)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtRegisterEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtRegisterPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val txtLoginLink = findViewById<TextView>(R.id.txtLoginLink)
        btnRegister = findViewById(R.id.btnRegister)

        txtLoginLink.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            val firstName = edtFirstName.text.toString().trim()
            val lastName = edtLastName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString()
            val confirmPassword = edtConfirmPassword.text.toString()

            if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.register(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password
            )
        }

        lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                btnRegister.isEnabled = !state.isLoading
                btnRegister.text = if (state.isLoading) "Đang tạo tài khoản..." else "Đăng ký"

                state.errorMessage?.let { message ->
                    if (message != lastErrorMessage) {
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_LONG).show()
                        lastErrorMessage = message
                    }
                }

                if (state.successMessage != null && !returnedToLogin) {
                    returnedToLogin = true
                    openLoginWithSuccess()
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

    private fun openLoginWithSuccess() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_REGISTER_SUCCESS, true)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
