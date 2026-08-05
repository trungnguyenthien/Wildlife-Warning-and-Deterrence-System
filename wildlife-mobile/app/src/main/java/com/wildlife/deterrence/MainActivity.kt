package com.wildlife.deterrence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wildlife.deterrence.data.ThemeSettings
import com.wildlife.deterrence.theme.WildlifeDeterrenceTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    NotificationChannels.createChannels(applicationContext)
    handleIntent(intent)
    ThemeSettings.init(applicationContext)

    enableEdgeToEdge()
    setContent {
      val themeMode by ThemeSettings.themeMode.collectAsState()
      val isDarkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
      }

      WildlifeDeterrenceTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation()
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent?) {
    val type = intent?.getStringExtra("type")
    val eventId = intent?.getStringExtra("eventId") ?: intent?.getStringExtra("alertId")
    val cameraId = intent?.getStringExtra("cameraId")
    val speciesName = intent?.getStringExtra("speciesName")

    android.util.Log.d("MainActivity", "handleIntent: type=$type, eventId=$eventId, cameraId=$cameraId, speciesName=$speciesName")

    if (cameraId != null) {
      android.util.Log.d("MainActivity", "DeepLink to CameraDetail: $cameraId")
      DeepLinkHandler.pendingDestination = CameraDetail(cameraId)
    } else if (type != null) {
      val navKey = when (type) {
        "animal.detected", "animal.escalated", "danger_alert" -> {
          if (eventId != null) AlertDetail(eventId, speciesName) else null
        }
        "fence.activated", "fence.deactivated", "device.offline" -> {
          if (cameraId != null) CameraDetail(cameraId) else null
        }
        else -> null
      }
      if (navKey != null) {
        DeepLinkHandler.pendingDestination = navKey
      }
    } else if (eventId != null) {
      DeepLinkHandler.pendingDestination = AlertDetail(eventId, speciesName)
    }
  }
}
