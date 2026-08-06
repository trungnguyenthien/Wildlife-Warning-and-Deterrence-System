package com.wildlife.deterrence.data

import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

data class ResponseConfigData(
    val id: String?,
    val userId: String?,
    val speciesId: String,
    val ledFlash: Boolean,
    val ledColor: String?,
    val ledIntensity: Int, // mappings to ledDurationSeconds
    val speakerWarn: Boolean,
    val audioSampleId: String?,
    val audioIntensity: Int,
    val silentAlert: Boolean,
    val ledFlashRate: String? = null,
    val speakerSampleId: String? = null
)

data class SaveResponseConfigRequest(
    val ledFlash: Boolean,
    val ledColor: String?,
    val ledIntensity: Int,
    val speakerWarn: Boolean,
    val audioSampleId: String?,
    val audioIntensity: Int,
    val silentAlert: Boolean,
    val ledFlashRate: String? = null,
    val speakerSampleId: String? = null
)

interface ConfigApi {
    @GET("response-configs")
    suspend fun getConfigs(
        @Header("Authorization") token: String
    ): List<ResponseConfigData>

    @GET("response-configs")
    suspend fun getConfigDetail(
        @Header("Authorization") token: String,
        @Query("speciesId") speciesId: String
    ): ResponseConfigData

    @PUT("response-configs/{speciesId}")
    suspend fun saveConfig(
        @Header("Authorization") token: String,
        @Path("speciesId") speciesId: String,
        @Body body: SaveResponseConfigRequest
    ): ResponseConfigData

    @DELETE("response-configs/{speciesId}")
    suspend fun resetConfig(
        @Header("Authorization") token: String,
        @Path("speciesId") speciesId: String
    ): retrofit2.Response<Unit>
}
