package com.wildlife.deterrence.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.DELETE
import retrofit2.http.Query

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val role: String
)

data class RegisterRequest(
    val username: String,
    val fullName: String,
    val phoneNumber: String,
    val email: String?,
    val role: String,
    val password: String,
    val confirmPassword: String
)

data class RegisterResponse(
    val id: String,
    val username: String,
    val fullName: String,
    val phoneNumber: String,
    val role: String,
    val createdAt: String
)

data class PushTokenRequest(
    val fcmToken: String,
    val deviceModel: String,
    val osVersion: String
)

data class UserProfileResponse(
    val id: String,
    val username: String,
    val fullName: String,
    val phoneNumber: String,
    val role: String,
    val email: String? = null
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("devices/push-token")
    suspend fun registerPushToken(
        @Header("Authorization") authHeader: String,
        @Body request: PushTokenRequest
    ): Response<Unit>

    @DELETE("devices/push-token")
    suspend fun deletePushToken(
        @Header("Authorization") authHeader: String,
        @Query("fcmToken") fcmToken: String? = null
    ): Response<Unit>

    @GET("users/me")
    suspend fun getUserProfile(
        @Header("Authorization") authHeader: String
    ): Response<UserProfileResponse>
}
