package com.example.safehome.data.remote

import com.example.safehome.data.local.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://smarthome-backend-1-4oly.onrender.com/"

    fun createAuthApi(tokenManager: TokenManager): AuthApi {
        return createRetrofit(tokenManager).create(AuthApi::class.java)
    }

    fun createNotificationApi(tokenManager: TokenManager): NotificationApi {
        return createRetrofit(tokenManager).create(NotificationApi::class.java)
    }

    fun createFcmApi(tokenManager: TokenManager): FcmApi {
        return createRetrofit(tokenManager).create(FcmApi::class.java)
    }

    private fun createRetrofit(tokenManager: TokenManager): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createDeviceApi(tokenManager: TokenManager): DeviceApi {
        return createRetrofit(tokenManager).create(DeviceApi::class.java)
    }
}
