package com.example.safehome.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/signin")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/signup")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<Unit>

    @GET("api/user/me")
    suspend fun getMe(): Response<MeResponse>
}
