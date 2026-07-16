package com.example.safehome.data.remote

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.Part

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

    @Multipart
    @PATCH("api/user/me")
    suspend fun updateProfile(
        @Part fullName: MultipartBody.Part? = null,
        @Part avatar: MultipartBody.Part? = null
    ): Response<MeResponse>

    @POST("api/user/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ResponseBody>

    @DELETE("api/user/me")
    suspend fun deleteAccount(): Response<ResponseBody>
}
