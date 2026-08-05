package com.wildlife.deterrence.data

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.Path

data class SmsRecipientResponse(
    val id: String,
    val userId: String,
    val fullName: String,
    val phoneNumber: String,
    val relation: String,
    val createdAt: String
)

data class SmsRecipientRequest(
    val fullName: String,
    val phoneNumber: String,
    val relation: String
)

interface SmsApi {
    @GET("users/me/sms-recipients")
    suspend fun getSmsRecipients(
        @Header("Authorization") token: String
    ): List<SmsRecipientResponse>

    @POST("users/me/sms-recipients")
    suspend fun addSmsRecipient(
        @Header("Authorization") token: String,
        @Body request: SmsRecipientRequest
    ): SmsRecipientResponse

    @DELETE("users/me/sms-recipients/{recipientId}")
    suspend fun deleteSmsRecipient(
        @Header("Authorization") token: String,
        @Path("recipientId") recipientId: String
    ): retrofit2.Response<Unit>
}
