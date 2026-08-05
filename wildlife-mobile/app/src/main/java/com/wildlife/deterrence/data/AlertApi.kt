package com.wildlife.deterrence.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

data class AlertDetailResponse(
    val alertId: String,
    val title: String,
    val alertType: String, // "animal" | "intrusion"
    val imageUrl: String?,
    val speciesName: String?,
    val speciesNameEn: String?,
    val cameraCode: String,
    val cameraName: String,
    val dangerLevel: String,
    val confidencePercent: Int?,
    val estimatedCount: Int?,
    val recordedAt: String, // "HH:mm:ss · dd/MM/yyyy"
    val gpsCoordinate: String?
)

interface AlertApi {
    @GET("alerts/{alertId}")
    suspend fun getAlertDetail(
        @Header("Authorization") token: String,
        @Path("alertId") alertId: String
    ): AlertDetailResponse

    @GET("alerts/feed")
    suspend fun getAlertsFeed(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<AlertResponse>
}
