package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class CameraDetailUiState(
    val cameraId: String,
    val name: String,
    val isOnline: Boolean,
    val liveSnapshot: SnapshotUiModel?,
    val currentAnalysis: AnalysisUiModel?,
    val historyItems: List<DetectionHistoryItem> = emptyList(),
    val selectedDateFilter: String = "today", // "today" | "custom"
    val customDate: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SnapshotUiModel(
    val url: String,
    val capturedAt: String,
    val timeAgoText: String,
    val camId: String,
    val motionDetected: Boolean,
    val gpsText: String,
    val sdCardSpace: String,
    val zoomText: String
)

data class AnalysisUiModel(
    val speciesName: String,
    val speciesNameEn: String,
    val dangerLevel: String, // "high" | "medium" | "low"
    val estimatedCount: Int,
    val confidencePercent: Int
)

data class DetectionHistoryItem(
    val id: String,
    val thumbnailUrl: String?,
    val speciesName: String?,
    val speciesNameEn: String?,
    val estimatedCount: Int?,
    val confidencePercent: Int?,
    val recordedTime: String,
    val recordedDateLabel: String
)

class CameraDetailViewModel(
    private val cameraId: String,
    private val tokenManager: TokenManager,
    private val cameraApi: CameraApi = NetworkClient.cameraApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraDetailUiState(cameraId = cameraId, name = "", isOnline = false, liveSnapshot = null, currentAnalysis = null))
    val uiState: StateFlow<CameraDetailUiState> = _uiState.asStateFlow()

    init {
        loadCameraDetail()
        loadHistory()
    }

    fun loadCameraDetail(isSilent: Boolean = false) {
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
                val detail = cameraApi.getCameraDetail(authHeader, cameraId)
                
                // Map current analysis
                val analysis = detail.currentDetection?.let { current ->
                    val mainDet = current.detections.firstOrNull()
                    if (mainDet != null) {
                        // Xác định danger level từ API
                        val danger = when (mainDet.dangerLevel.uppercase()) {
                            "CRITICAL", "HIGH" -> "high"
                            "MEDIUM" -> "medium"
                            else -> "low"
                        }

                        AnalysisUiModel(
                            speciesName = mainDet.displayName,
                            speciesNameEn = mainDet.speciesId,
                            dangerLevel = danger,
                            estimatedCount = current.detections.size,
                            confidencePercent = (mainDet.confidence * 100).toInt()
                        )
                    } else {
                        null
                    }
                }

                // Map snapshot model với mock metadata kĩ thuật cho giao diện sinh động
                val snapshotModel = detail.snapshot?.let { snap ->
                    val gps = "${detail.location.lat}, ${detail.location.lng}"
                    // Mock metadata
                    val sdSpace = "58.4 GB / 64 GB"
                    val zoom = "1.0x"
                    val hasMotion = detail.currentDetection != null
                    
                    SnapshotUiModel(
                        url = snap.url,
                        capturedAt = formatFullTimestamp(snap.capturedAt),
                        timeAgoText = getTimeAgoText(snap.capturedAt),
                        camId = detail.id,
                        motionDetected = hasMotion,
                        gpsText = gps,
                        sdCardSpace = sdSpace,
                        zoomText = zoom
                    )
                }

                _uiState.value = _uiState.value.copy(
                    name = detail.name,
                    isOnline = detail.status.uppercase() == "ONLINE",
                    liveSnapshot = snapshotModel,
                    currentAnalysis = analysis,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi tải chi tiết camera: ${e.message}"
                )
            }
        }
    }

    fun loadHistory() {
        val token = tokenManager.getToken() ?: return
        val authHeader = "Bearer $token"

        val dateQuery = if (_uiState.value.selectedDateFilter == "today") {
            // Hôm nay định dạng YYYY-MM-DD
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(System.currentTimeMillis())
        } else {
            val custom = _uiState.value.customDate
            if (custom != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(custom)
            } else {
                null
            }
        }

        viewModelScope.launch {
            try {
                val historyResponse = cameraApi.getCameraHistory(authHeader, cameraId, dateQuery)
                val items = historyResponse.map { item ->
                    DetectionHistoryItem(
                        id = item.id,
                        thumbnailUrl = item.thumbnailUrl,
                        speciesName = item.speciesName,
                        speciesNameEn = item.speciesNameEn,
                        estimatedCount = item.estimatedCount,
                        confidencePercent = item.confidencePercent,
                        recordedTime = item.recordedTime,
                        recordedDateLabel = item.recordedDateLabel
                    )
                }
                _uiState.value = _uiState.value.copy(historyItems = items)
            } catch (e: Exception) {
                // Không throw lỗi làm crash UI, chỉ log hoặc bỏ qua danh sách lịch sử
            }
        }
    }

    fun setDateFilter(filter: String, customTimeMs: Long? = null) {
        _uiState.value = _uiState.value.copy(
            selectedDateFilter = filter,
            customDate = customTimeMs
        )
        loadHistory()
    }

    fun renameCamera(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val token = tokenManager.getToken()
        if (token == null) {
            onError("Phiên đăng nhập hết hạn")
            return
        }
        val authHeader = "Bearer $token"

        viewModelScope.launch {
            try {
                val response = cameraApi.renameCamera(authHeader, cameraId, RenameCameraRequest(newName))
                _uiState.value = _uiState.value.copy(name = response.name)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Lỗi khi cập nhật tên camera")
            }
        }
    }

    fun startSseListening(context: android.content.Context) {
        val token = tokenManager.getToken() ?: return
        val url = "${NetworkClient.getServerUrl()}cameras/stream"
        NetworkClient.sseClient.startListening(
            url = url,
            token = "Bearer $token",
            onEventReceived = { event, data ->
                android.util.Log.d("SSE_Notification", "Received event in Detail: $event, data: $data")
                val isTargetCamera = data.contains("\"id\":\"$cameraId\"") || 
                                     data.contains("\"cameraId\":\"$cameraId\"")
                if (isTargetCamera) {
                    loadCameraDetail(isSilent = true)
                    loadHistory()
                }

                // Phát notification native khi có sự kiện DETECTION_ALERT
                if (event == "DETECTION_ALERT") {
                    try {
                        val json = org.json.JSONObject(data)
                        val eventId = json.optString("eventId")
                        val camId = json.optString("cameraId")
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
                                    cameraId = camId,
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
                android.util.Log.e("SSE_Notification", "SSE connection error in Detail", error)
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

    private fun formatFullTimestamp(isoString: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdfInput.parse(isoString) ?: return ""
            val sdfOutput = SimpleDateFormat("HH:mm:ss · dd/MM/yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            sdfOutput.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    private fun getTimeAgoText(capturedAtIso: String): String {
        val timeMs = parseIsoDateTime(capturedAtIso)
        if (timeMs == 0L) return "Không rõ"
        val diffMinutes = (System.currentTimeMillis() - timeMs) / 60000
        return when {
            diffMinutes <= 0 -> "Vừa xong"
            diffMinutes < 60 -> "Cách đây $diffMinutes phút"
            else -> {
                val hours = diffMinutes / 60
                "Cách đây $hours giờ"
            }
        }
    }
}
