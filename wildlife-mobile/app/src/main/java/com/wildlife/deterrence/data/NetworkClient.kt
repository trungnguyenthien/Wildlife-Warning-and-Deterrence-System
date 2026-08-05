package com.wildlife.deterrence.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    // Thay đổi sang BASE_URL Production Vercel
    const val BASE_URL = "https://wildlife-warning-and-deterrence-sys.vercel.app/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS) // Giữ kết nối SSE vô hạn không bị client tự ngắt
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val cameraApi: CameraApi = retrofit.create(CameraApi::class.java)
    val alertApi: AlertApi = retrofit.create(AlertApi::class.java)
    val smsApi: SmsApi = retrofit.create(SmsApi::class.java)
    val sseClient: SseClient = SseClient(okHttpClient)
}
