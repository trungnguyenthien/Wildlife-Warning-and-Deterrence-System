package com.wildlife.deterrence

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.TestDeviceRequest
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeterrenceActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notificationId", 0)
        val cameraId = intent.getStringExtra("cameraId") ?: return
        val eventId = intent.getStringExtra("eventId") ?: ""
        val title = intent.getStringExtra("title") ?: "CẢNH BÁO NGUY HIỂM"
        val body = intent.getStringExtra("body") ?: ""
        val type = intent.getStringExtra("type") ?: "danger_alert"
        val dangerLevel = intent.getStringExtra("dangerLevel") ?: "CRITICAL"
        val speciesName = intent.getStringExtra("speciesName") ?: ""

        val tokenManager = TokenManager(context.applicationContext)
        val token = tokenManager.getToken()

        // 1. Kích hoạt xua đuổi thời gian thực qua API chạy ngầm
        if (token != null) {
            val authHeader = "Bearer $token"
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = NetworkClient.cameraApi.testDevice(
                        token = authHeader,
                        cameraId = cameraId,
                        deviceKey = "speaker",
                        body = TestDeviceRequest(durationSeconds = 60, intensity = 100)
                    )
                    if (response.isSuccessful) {
                        android.util.Log.d("DeterrenceReceiver", "Triggered speaker sound successfully for camera $cameraId")
                    } else {
                        android.util.Log.e("DeterrenceReceiver", "Failed to trigger speaker sound: ${response.code()}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DeterrenceReceiver", "Error triggering speaker sound", e)
                }
            }
        } else {
            android.util.Log.w("DeterrenceReceiver", "No token found, skipping API trigger")
        }

        // 2. Cập nhật giao diện notification sang trạng thái đã kích hoạt
        val packageName = context.packageName
        val collapsedViews = RemoteViews(packageName, R.layout.custom_notification_collapsed)
        val expandedViews = RemoteViews(packageName, R.layout.custom_notification_expanded)

        val isCritical = dangerLevel == "CRITICAL" || type == "animal.escalated" || type == "danger_alert"
        val channelId = if (isCritical) NotificationChannels.CHANNEL_CRITICAL else NotificationChannels.CHANNEL_DEFAULT

        // Cấu hình lại text & màu sắc như cũ
        collapsedViews.setTextViewText(R.id.notification_title, title)
        collapsedViews.setTextViewText(R.id.notification_body, body)
        collapsedViews.setTextColor(R.id.notification_title, if (isCritical) 0xFFBA1A1A.toInt() else 0xFF059669.toInt())

        expandedViews.setTextViewText(R.id.notification_title, title)
        expandedViews.setTextViewText(R.id.notification_body, body)
        expandedViews.setTextColor(R.id.notification_title, if (isCritical) 0xFFBA1A1A.toInt() else 0xFF059669.toInt())

        if (isCritical) {
            collapsedViews.setInt(R.id.notification_indicator, "setBackgroundColor", 0xFFBA1A1A.toInt())
            collapsedViews.setInt(R.id.notification_icon_container, "setBackgroundResource", R.drawable.bg_notification_icon_red)
            collapsedViews.setImageViewResource(R.id.notification_icon, android.R.drawable.ic_dialog_alert)
            collapsedViews.setInt(R.id.notification_icon, "setColorFilter", 0xFFBA1A1A.toInt())

            expandedViews.setInt(R.id.notification_indicator, "setBackgroundColor", 0xFFBA1A1A.toInt())
            expandedViews.setInt(R.id.notification_icon_container, "setBackgroundResource", R.drawable.bg_notification_icon_red)
            expandedViews.setImageViewResource(R.id.notification_icon, android.R.drawable.ic_dialog_alert)
            expandedViews.setInt(R.id.notification_icon, "setColorFilter", 0xFFBA1A1A.toInt())
        } else {
            collapsedViews.setInt(R.id.notification_indicator, "setBackgroundColor", 0xFF059669.toInt())
            collapsedViews.setInt(R.id.notification_icon_container, "setBackgroundResource", R.drawable.bg_notification_icon_green)
            collapsedViews.setImageViewResource(R.id.notification_icon, android.R.drawable.ic_dialog_info)
            collapsedViews.setInt(R.id.notification_icon, "setColorFilter", 0xFF059669.toInt())

            expandedViews.setInt(R.id.notification_indicator, "setBackgroundColor", 0xFF059669.toInt())
            expandedViews.setInt(R.id.notification_icon_container, "setBackgroundResource", R.drawable.bg_notification_icon_green)
            expandedViews.setImageViewResource(R.id.notification_icon, android.R.drawable.ic_dialog_info)
            expandedViews.setInt(R.id.notification_icon, "setColorFilter", 0xFF059669.toInt())
        }

        // Cập nhật Action Button thành màu xám và ghi Đã kích hoạt
        expandedViews.setViewVisibility(R.id.notification_action_button, View.VISIBLE)
        expandedViews.setTextViewText(R.id.notification_action_button, "📢  ĐÃ KÍCH HOẠT XUA ĐUỔI")
        expandedViews.setInt(R.id.notification_action_button, "setBackgroundResource", R.drawable.bg_notification_button_grey)
        expandedViews.setOnClickPendingIntent(R.id.notification_action_button, null)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", type)
            putExtra("eventId", eventId)
            putExtra("alertId", eventId)
            putExtra("cameraId", cameraId)
            putExtra("speciesName", speciesName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode() ?: cameraId.hashCode() ?: 0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isCritical) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_dialog_info)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(if (isCritical) 0xFFC62828.toInt() else 0xFF2E7D32.toInt())

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}
