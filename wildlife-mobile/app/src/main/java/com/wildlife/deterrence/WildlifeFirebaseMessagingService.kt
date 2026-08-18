package com.wildlife.deterrence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import kotlinx.coroutines.launch
import java.util.UUID

object DeepLinkHandler {
    var pendingDestination by androidx.compose.runtime.mutableStateOf<androidx.navigation3.runtime.NavKey?>(null)
}

object NotificationState {
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

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
    const val CHANNEL_CRITICAL = "channel_critical_v2"
    const val CHANNEL_DEFAULT = "channel_default_v2"

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

        val packageName = context.packageName
        val collapsedViews = android.widget.RemoteViews(packageName, R.layout.custom_notification_collapsed)
        val expandedViews = android.widget.RemoteViews(packageName, R.layout.custom_notification_expanded)

        // Thiết lập text & màu sắc tiêu đề theo mockup
        collapsedViews.setTextViewText(R.id.notification_title, if (isCritical) "CẢNH BÁO NGUY HIỂM" else "HỆ THỐNG CẬP NHẬT")
        collapsedViews.setTextViewText(R.id.notification_body, payload.body)
        collapsedViews.setTextColor(R.id.notification_title, if (isCritical) 0xFFBA1A1A.toInt() else 0xFF059669.toInt())

        expandedViews.setTextViewText(R.id.notification_title, if (isCritical) "CẢNH BÁO NGUY HIỂM" else "HỆ THỐNG CẬP NHẬT")
        expandedViews.setTextViewText(R.id.notification_body, payload.body)
        expandedViews.setTextColor(R.id.notification_title, if (isCritical) 0xFFBA1A1A.toInt() else 0xFF059669.toInt())

        if (isCritical) {
            // Tông đỏ cam nguy hiểm
            collapsedViews.setInt(R.id.notification_indicator, "setBackgroundColor", 0xFFBA1A1A.toInt())
            collapsedViews.setInt(R.id.notification_icon_container, "setBackgroundResource", R.drawable.bg_notification_icon_red)
            collapsedViews.setImageViewResource(R.id.notification_icon, android.R.drawable.ic_dialog_alert)
            collapsedViews.setInt(R.id.notification_icon, "setColorFilter", 0xFFBA1A1A.toInt())

            expandedViews.setInt(R.id.notification_indicator, "setBackgroundColor", 0xFFBA1A1A.toInt())
            expandedViews.setInt(R.id.notification_icon_container, "setBackgroundResource", R.drawable.bg_notification_icon_red)
            expandedViews.setImageViewResource(R.id.notification_icon, android.R.drawable.ic_dialog_alert)
            expandedViews.setInt(R.id.notification_icon, "setColorFilter", 0xFFBA1A1A.toInt())
            expandedViews.setViewVisibility(R.id.notification_action_button, android.view.View.VISIBLE)

            // Kích hoạt nút hành động xua đuổi ngay
            val actionIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("type", payload.type)
                putExtra("eventId", payload.eventId)
                putExtra("alertId", payload.eventId)
                putExtra("cameraId", payload.cameraId)
                putExtra("action_trigger", "deter")
            }
            val actionPendingIntent = PendingIntent.getActivity(
                context,
                (payload.eventId?.hashCode() ?: 0) + 1,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            expandedViews.setOnClickPendingIntent(R.id.notification_action_button, actionPendingIntent)
        } else {
            // Tông xanh lục an toàn/cập nhật
            collapsedViews.setInt(R.id.notification_indicator, "setBackgroundColor", 0xFF059669.toInt())
            collapsedViews.setInt(R.id.notification_icon_container, "setBackgroundResource", R.drawable.bg_notification_icon_green)
            collapsedViews.setImageViewResource(R.id.notification_icon, android.R.drawable.ic_dialog_info)
            collapsedViews.setInt(R.id.notification_icon, "setColorFilter", 0xFF059669.toInt())

            expandedViews.setInt(R.id.notification_indicator, "setBackgroundColor", 0xFF059669.toInt())
            expandedViews.setInt(R.id.notification_icon_container, "setBackgroundResource", R.drawable.bg_notification_icon_green)
            expandedViews.setImageViewResource(R.id.notification_icon, android.R.drawable.ic_dialog_info)
            expandedViews.setInt(R.id.notification_icon, "setColorFilter", 0xFF059669.toInt())
            expandedViews.setViewVisibility(R.id.notification_action_button, android.view.View.GONE)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isCritical) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isCritical) "CẢNH BÁO NGUY HIỂM" else "HỆ THỐNG CẬP NHẬT")
            .setContentText(payload.body)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (isCritical) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(0xFFC62828.toInt())
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(0xFF2E7D32.toInt())
        }

        val notificationId = UUID.randomUUID().hashCode()
        manager.notify(notificationId, builder.build())
    }
}

class WildlifeFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        android.util.Log.d("FCM", "Message received: ${message.data}")
        val payload = parsePayload(message, message.data)
        NotificationState.increment() // Tăng badge count
        NotificationBuilder.showNotification(applicationContext, payload)
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
