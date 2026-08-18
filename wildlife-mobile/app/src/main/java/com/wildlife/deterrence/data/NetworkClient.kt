package com.wildlife.deterrence.data

import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    // Thay đổi sang BASE_URL Production Vercel
    const val BASE_URL = "https://wildlife-warning-and-deterrence-sys.vercel.app/"

    var customServerUrl: String? = null

    fun getServerUrl(): String {
        return customServerUrl ?: BASE_URL
    }

    class DynamicBaseUrlInterceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            var request = chain.request()
            val customUrl = customServerUrl?.toHttpUrlOrNull()
            if (customUrl != null) {
                val newUrl = request.url.newBuilder()
                    .scheme(customUrl.scheme)
                    .host(customUrl.host)
                    .port(customUrl.port)
                    .build()
                request = request.newBuilder()
                    .url(newUrl)
                    .build()
            }
            return chain.proceed(request)
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(DynamicBaseUrlInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val sseOkHttpClient: OkHttpClient = OkHttpClient.Builder()
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
    val configApi: ConfigApi = retrofit.create(ConfigApi::class.java)
    val sseClient: SseClient = SseClient(sseOkHttpClient)
}
