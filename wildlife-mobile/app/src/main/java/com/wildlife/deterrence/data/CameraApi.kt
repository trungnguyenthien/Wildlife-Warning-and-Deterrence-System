package com.wildlife.deterrence.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

data class CameraResponse(
    val id: String,
    val name: String,
    val location: LocationResponse,
    val status: String,
    val liveFeedUrl: String,
    val snapshot: SnapshotResponse?
)

data class LocationResponse(
    val lat: Double,
    val lng: Double,
    val address: String
)

data class SnapshotResponse(
    val url: String,
    val capturedAt: String
)

data class AlertResponse(
    val id: String,
    val type: String,
    val title: String,
    val dangerLevel: String,
    val cameraId: String,
    val cameraName: String,
    val eventId: String,
    val createdAt: String,
    val isRead: Boolean
)

data class RenameCameraRequest(
    val name: String
)

data class CameraDetailResponse(
    val id: String,
    val name: String,
    val location: LocationResponse,
    val status: String,
    val liveFeedUrl: String,
    val snapshot: SnapshotResponse?,
    val currentDetection: CurrentDetectionResponse?
)

data class CurrentDetectionResponse(
    val eventId: String,
    val detectedAt: String,
    val detections: List<DetectionResponse>
)

data class DetectionResponse(
    val speciesId: String,
    val displayName: String,
    val confidence: Double
)

data class DetectionHistoryItemResponse(
    val id: String,
    val thumbnailUrl: String?,
    val speciesName: String?,
    val speciesNameEn: String?,
    val estimatedCount: Int?,
    val confidencePercent: Int?,
    val recordedTime: String,
    val recordedDateLabel: String
)

interface CameraApi {
    @GET("cameras")
    suspend fun getCameras(
        @Header("Authorization") token: String
    ): List<CameraResponse>

    @GET("cameras/{cameraId}")
    suspend fun getCameraDetail(
        @Header("Authorization") token: String,
        @Path("cameraId") cameraId: String
    ): CameraDetailResponse

    @GET("cameras/{cameraId}/history")
    suspend fun getCameraHistory(
        @Header("Authorization") token: String,
        @Path("cameraId") cameraId: String,
        @Query("date") date: String? = null
    ): List<DetectionHistoryItemResponse>

    @PATCH("cameras/{cameraId}")
    suspend fun renameCamera(
        @Header("Authorization") token: String,
        @Path("cameraId") cameraId: String,
        @Body body: RenameCameraRequest
    ): CameraResponse

    @GET("alerts/feed")
    suspend fun getAlertsFeed(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 50
    ): List<AlertResponse>

    @POST("alerts/feed/{alertId}/read")
    suspend fun readAlert(
        @Header("Authorization") token: String,
        @Path("alertId") alertId: String
    ): retrofit2.Response<Unit>

    @GET("species")
    suspend fun getSpecies(
        @Header("Authorization") token: String
    ): List<SpeciesResponse>

    @GET("stats/summary")
    suspend fun getStatsSummary(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("cameraId") cameraId: String? = null,
        @Query("speciesId") speciesId: String? = null
    ): StatsSummaryResponse

    @POST("cameras/{cameraId}/devices/{deviceKey}/test")
    suspend fun testDevice(
        @Header("Authorization") token: String,
        @Path("cameraId") cameraId: String,
        @Path("deviceKey") deviceKey: String,
        @Body body: TestDeviceRequest
    ): retrofit2.Response<Unit>
}

data class TestDeviceRequest(
    val durationSeconds: Int,
    val intensity: Int
)

data class SpeciesResponse(
    val id: String,
    val displayName: String,
    val dangerLevel: String,
    val isHuman: Boolean,
    val htmlDescription: String,
    val aggressionLevel: Int,
    val recommendAction: String,
    val createdAt: String
)

data class TrendPointResponse(
    val date: String,
    val count: Int
)

data class SpeciesBreakdownResponse(
    val speciesId: String,
    val displayName: String,
    val count: Int
)

data class HeatmapPointResponse(
    val lat: Double,
    val lng: Double,
    val intensity: Double
)

data class StatsSummaryResponse(
    val trendData: List<TrendPointResponse>,
    val speciesBreakdown: List<SpeciesBreakdownResponse>,
    val heatmapData: List<HeatmapPointResponse>
)

