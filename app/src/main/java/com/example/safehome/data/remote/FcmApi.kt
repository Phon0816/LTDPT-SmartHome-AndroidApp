package com.example.safehome.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FcmApi {

    @POST("api/fcm/register")
    suspend fun register(
        @Body request: FcmRegisterRequest
    ): Response<FcmRegisterResponse>

    @POST("api/fcm/unregister")
    suspend fun unregister(
        @Body request: FcmUnregisterRequest
    ): Response<FcmUnregisterResponse>
}
