package com.wildlife.deterrence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.TokenManager
import com.wildlife.deterrence.data.PushTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

object DeepLinkHandler {
    var pendingDestination by androidx.compose.runtime.mutableStateOf<androidx.navigation3.runtime.NavKey?>(null)
}

object NotificationState {
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _realtimeAlertEvent = MutableSharedFlow<WildlifeNotificationPayload>(extraBufferCapacity = 1)
    val realtimeAlertEvent = _realtimeAlertEvent.asSharedFlow()

    fun triggerRealtimeUpdate(payload: WildlifeNotificationPayload) {
        _realtimeAlertEvent.tryEmit(payload)
    }

    fun setUnreadCount(count: Int) {
        _unreadCount.value = count
    }

    fun increment() {
        _unreadCount.value += 1
    }

    fun clear() {
        _unreadCount.value = 0
    }
}

object NotificationChannels {
    const val CHANNEL_CRITICAL = "channel_critical_v3"
    const val CHANNEL_DEFAULT = "channel_default_v3"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            // Channel Critical (Rung + Bypass DND + Alarm Sound, importance HIGH)
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL, "Cảnh báo nguy khẩn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Phát hiện động vật nguy cấp cần xử lý ngay"
                enableVibration(true)
                setBypassDnd(true)
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                setSound(soundUri, audioAttributes)
            }

            // Channel Default (importance HIGH)
            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT, "Thông báo thường",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo hệ thống, cập nhật thiết bị"
                enableVibration(true)
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }

            manager.createNotificationChannel(criticalChannel)
            manager.createNotificationChannel(defaultChannel)
        }
    }
}

data class WildlifeNotificationPayload(
    val type: String,
    val title: String,
    val body: String,
    val cameraId: String?,
    val eventId: String?,
    val speciesName: String?,
    val riskScore: Int?,
    val dangerLevel: String?,
    val alertId: String?,
    val timestamp: Long
)

object NotificationBuilder {
    fun showNotification(context: Context, payload: WildlifeNotificationPayload) {
        if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            android.util.Log.w("Notification", "Notifications are disabled for this app")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", payload.type)
            putExtra("eventId", payload.eventId)
            putExtra("alertId", payload.eventId)
            putExtra("cameraId", payload.cameraId)
            putExtra("speciesName", payload.speciesName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            payload.eventId?.hashCode() ?: payload.cameraId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isCritical = payload.dangerLevel == "CRITICAL" || payload.type == "animal.escalated" || payload.type == "danger_alert"
        val channelId = if (isCritical) NotificationChannels.CHANNEL_CRITICAL else NotificationChannels.CHANNEL_DEFAULT

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // Sử dụng notification mặc định của hệ thống Android (không dùng Custom RemoteViews)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isCritical) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_dialog_info)
            .setContentTitle(payload.title.ifEmpty { if (isCritical) "CẢNH BÁO NGUY HIỂM" else "HỆ THỐNG CẬP NHẬT" })
            .setContentText(payload.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))

        if (isCritical) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setColor(0xFFC62828.toInt())
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setColor(0xFF2E7D32.toInt())
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = UUID.randomUUID().hashCode()
        manager.notify(notificationId, builder.build())
    }
}

class WildlifeFirebaseMessagingService : FirebaseMessagingService() {

    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    override fun onMessageReceived(message: RemoteMessage) {
        android.util.Log.d("FCM", "Message received: ${message.data}")
        val payload = parsePayload(message, message.data)
        NotificationState.increment() // Tăng badge count
        NotificationState.triggerRealtimeUpdate(payload)
        
        // Chỉ gửi popup thông báo hệ thống khi ứng dụng đang ở background
        if (!isAppInForeground(applicationContext)) {
            NotificationBuilder.showNotification(applicationContext, payload)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM", "New token generated: $token")
        val tokenManager = TokenManager(applicationContext)
        val jwtToken = tokenManager.getToken()
        if (jwtToken != null) {
            val authHeader = "Bearer $jwtToken"
            val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = NetworkClient.authApi.registerPushToken(
                        authHeader = authHeader,
                        request = PushTokenRequest(
                            fcmToken = token,
                            deviceModel = deviceModel,
                            osVersion = osVersion
                        )
                    )
                    if (response.isSuccessful) {
                        android.util.Log.d("FCM", "Push token registered successfully via onNewToken")
                    } else {
                        android.util.Log.e("FCM", "Push token registration failed via onNewToken: ${response.code()}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FCM", "Error registering push token via onNewToken", e)
                }
            }
        }
    }

    private fun parsePayload(message: RemoteMessage, data: Map<String, String>): WildlifeNotificationPayload {
        val eventId = data["eventId"] ?: data["alertId"]
        val notificationTitle = message.notification?.title ?: data["title"] ?: ""
        val notificationBody = message.notification?.body ?: data["body"] ?: ""
        return WildlifeNotificationPayload(
            type = data["type"] ?: "system.alert",
            title = notificationTitle,
            body = notificationBody,
            cameraId = data["cameraId"],
            eventId = eventId,
            speciesName = data["speciesName"],
            riskScore = data["riskScore"]?.toIntOrNull(),
            dangerLevel = data["dangerLevel"],
            alertId = eventId,
            timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}
