package com.wildlife.deterrence

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Splash : NavKey
@Serializable data object Login : NavKey
@Serializable data object Main : NavKey
@Serializable data object Register : NavKey
@Serializable data class CameraDetail(val cameraId: String) : NavKey
@Serializable data class AlertDetail(val alertId: String, val speciesName: String? = null) : NavKey
@Serializable data class AllDetections(
    val timeRange: String,
    val speciesId: String? = null,
    val cameraId: String? = null
) : NavKey

@Serializable data object SmsSetup : NavKey

@Serializable data object BehaviorSpeciesList : NavKey
@Serializable data class BehaviorConfig(val speciesId: String, val speciesName: String) : NavKey
