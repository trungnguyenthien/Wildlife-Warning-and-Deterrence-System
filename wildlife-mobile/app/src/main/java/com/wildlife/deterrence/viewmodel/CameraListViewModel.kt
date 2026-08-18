package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.CameraApi
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class CameraListUiState(
    val stations: List<StationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long = 0L
)

data class StationUiModel(
    val id: String,
    val name: String,
    val address: String,
    val isOnline: Boolean,
    val thumbnailUrl: String?,
    val hasUnreadAlert: Boolean,
    val alertSpecies: String?,
    val alertConfidence: Int?,
    val timestampText: String,
    val offlineDurationText: String?
)

class CameraListViewModel(
    private val tokenManager: TokenManager,
    private val cameraApi: CameraApi = NetworkClient.cameraApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraListUiState())
    val uiState: StateFlow<CameraListUiState> = _uiState.asStateFlow()

    fun loadCameras(isSilent: Boolean = false) {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Không có phiên đăng nhập. Vui lòng đăng nhập lại.")
            return
        }
        val authHeader = "Bearer $token"

        if (!isSilent) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            try {
                // Tải song song danh sách camera và alerts feed để kiểm tra tin chưa đọc
                val camerasResponse = cameraApi.getCameras(authHeader)
                val alertsResponse = try {
                    cameraApi.getAlertsFeed(authHeader, page = 1, size = 50)
                } catch (e: Exception) {
                    emptyList()
                }

                // Sắp xếp danh sách: trạm có snapshot mới nhất lên đầu
                val sortedCameras = camerasResponse.sortedByDescending { cam ->
                    cam.snapshot?.capturedAt?.let { parseIsoDateTime(it) } ?: 0L
                }

                val uiModels = sortedCameras.map { cam ->
                    // Lọc danh sách alerts chưa đọc của camera này
                    val unreadAlerts = alertsResponse.filter { it.cameraId == cam.id && !it.isRead }
                    val hasUnread = unreadAlerts.isNotEmpty()
                    val latestUnreadAlert = unreadAlerts.firstOrNull()

                    // Logic cảnh báo: nhấp nháy đỏ khi có sự kiện nguy hiểm chưa xem trong vòng 30 phút
                    var hasUnreadAlertValue = false
                    var alertSpec: String? = null
                    var alertConf: Int? = null

                    if (latestUnreadAlert != null) {
                        try {
                            val alertTime = parseIsoDateTime(latestUnreadAlert.createdAt)
                            val diffMinutes = (System.currentTimeMillis() - alertTime) / 60000
                            if (diffMinutes < 30) {
                                hasUnreadAlertValue = true
                                
                                // Trích xuất tên động vật từ title (vd: "Cảnh báo: Phát hiện Voi Châu Á tại...")
                                val title = latestUnreadAlert.title
                                val prefix = "Phát hiện "
                                val suffix = " tại"
                                val startIndex = title.indexOf(prefix)
                                if (startIndex != -1) {
                                    val start = startIndex + prefix.length
                                    val end = title.indexOf(suffix, start)
                                    alertSpec = if (end != -1) {
                                        title.substring(start, end).trim()
                                    } else {
                                        title.substring(start).trim()
                                    }
                                }

                                // Trích xuất confidence từ title (ví dụ: "92%")
                                val match = Regex("(\\d+)%").find(title)
                                if (match != null) {
                                    alertConf = match.groupValues[1].toIntOrNull()
                                }
                            }
                        } catch (e: Exception) {
                            hasUnreadAlertValue = true
                        }
                    }

                    val timeText = cam.snapshot?.capturedAt?.let { formatTimestamp(it) } ?: ""
                    val isOnlineVal = cam.status.uppercase() == "ONLINE"
                    
                    // Tính thời gian ngoại tuyến
                    val offlineDur: String? = if (!isOnlineVal) {
                        val capturedAt = cam.snapshot?.capturedAt
                        if (capturedAt != null) {
                            val diffMs = System.currentTimeMillis() - parseIsoDateTime(capturedAt)
                            val diffHours = diffMs / 3600000
                            if (diffHours <= 0) {
                                "Mất kết nối dưới 1 giờ trước"
                            } else {
                                "Mất kết nối $diffHours giờ trước"
                            }
                        } else {
                            "Mất kết nối"
                        }
                    } else {
                        null
                    }

                    StationUiModel(
                        id = cam.id,
                        name = cam.name,
                        address = cam.location.address,
                        isOnline = isOnlineVal,
                        thumbnailUrl = cam.snapshot?.url,
                        hasUnreadAlert = hasUnreadAlertValue,
                        alertSpecies = alertSpec,
                        alertConfidence = alertConf,
                        timestampText = timeText,
                        offlineDurationText = offlineDur
                    )
                }

                _uiState.value = CameraListUiState(
                    stations = uiModels,
                    isLoading = false,
                    isRefreshing = false,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "Lỗi kết nối máy chủ: ${e.message}"
                )
            }
        }
    }

    fun refreshCameras() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadCameras(isSilent = true)
    }

    fun startSseListening(context: android.content.Context) {
        val token = tokenManager.getToken() ?: return
        val url = "${NetworkClient.getServerUrl()}cameras/stream"
        NetworkClient.sseClient.startListening(
            url = url,
            token = "Bearer $token",
            onEventReceived = { event, data ->
                android.util.Log.d("SSE_Notification", "Received event: $event, data: $data")
                // Khi nhận được sự kiện (ví dụ: DETECTION_ALERT), reload danh sách chạy ngầm
                loadCameras(isSilent = true)

                // Phát notification native lên thiết bị khi nhận được cảnh báo thời gian thực từ SSE
                if (event == "DETECTION_ALERT") {
                    try {
                        val json = org.json.JSONObject(data)
                        val eventId = json.optString("eventId")
                        val cameraId = json.optString("cameraId")
                        val cameraName = json.optString("cameraName")
                        val detectionsArray = json.optJSONArray("detections")

                        if (detectionsArray != null && detectionsArray.length() > 0) {
                            val firstDet = detectionsArray.getJSONObject(0)
                            val speciesId = firstDet.optString("speciesId")
                            val displayName = firstDet.optString("displayName")
                            val confidence = firstDet.optDouble("confidence", 0.0)
                            val riskScore = (confidence * 10).toInt()

                            if (speciesId.isNotEmpty()) {
                                val payload = com.wildlife.deterrence.WildlifeNotificationPayload(
                                    type = "danger_alert",
                                    title = "CẢNH BÁO NGUY HIỂM",
                                    body = "Phát hiện $displayName (Chỉ số $riskScore/10) tại $cameraName. Chạm để kích hoạt kịch bản xua đuổi.",
                                    cameraId = cameraId,
                                    eventId = eventId,
                                    speciesName = displayName,
                                    riskScore = riskScore,
                                    dangerLevel = "CRITICAL",
                                    alertId = eventId,
                                    timestamp = System.currentTimeMillis()
                                )
                                com.wildlife.deterrence.NotificationBuilder.showNotification(context, payload)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SSE_Notification", "Error parsing SSE data", e)
                    }
                }
            },
            onError = { error ->
                android.util.Log.e("SSE_Notification", "SSE connection error", error)
            }
        )
    }

    fun stopSseListening() {
        NetworkClient.sseClient.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        stopSseListening()
    }

    private fun parseIsoDateTime(isoString: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(isoString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun formatTimestamp(isoString: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdfInput.parse(isoString) ?: return ""
            val sdfOutput = SimpleDateFormat("HH:mm '·' dd/MM", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            sdfOutput.format(date)
        } catch (e: Exception) {
            ""
        }
    }
}
