package com.wildlife.deterrence.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
}
