package com.example.safehome.data.remote

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String
)

data class MeResponse(
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val fullName: String,
    val email: String,
    val role: String,
    val avatar: String? = null,
    val createdAt: String
)

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
