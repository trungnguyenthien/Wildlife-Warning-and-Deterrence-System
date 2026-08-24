package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.CameraApi
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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

    private var pollingJob: Job? = null
    private var realtimeJob: Job? = null
    private var lastKnownUpdatedAt: Long = 0L

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
                // Tải danh sách camera
                // Lưu ý: GET /cameras không trả về currentDetection,
                // nên dùng snapshot.capturedAt ≤ 30s để xác định trạng thái báo động (cách đơn giản nhất)
                val camerasResponse = cameraApi.getCameras(authHeader)

                // Sắp xếp danh sách: trạm có snapshot mới nhất lên đầu
                val sortedCameras = camerasResponse.sortedByDescending { cam ->
                    cam.snapshot?.capturedAt?.let { parseIsoDateTime(it) } ?: 0L
                }

                val now = System.currentTimeMillis()
                val ALERT_COOLDOWN_MS = 30_000L // 30 giây

                val uiModels = sortedCameras.map { cam ->
                    val snapshotTimeMs = cam.snapshot?.capturedAt?.let { parseIsoDateTime(it) } ?: 0L

                    // isAlertActive: server trả về currentDetection != null khi event xảy ra trong 30s gần nhất
                    val isAlertActive = cam.currentDetection != null

                    // Trích xuất thông tin loài từ currentDetection nếu có
                    val mainDet = cam.currentDetection?.detections?.firstOrNull()
                    val alertSpec = if (isAlertActive) mainDet?.speciesName else null
                    val alertConf = if (isAlertActive) mainDet?.confidence?.let { (it * 100).toInt() } else null

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
                        hasUnreadAlert = isAlertActive,
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

    fun startPolling(intervalMs: Long = 5000L) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                smartPoll()
                delay(intervalMs)
            }
        }
        android.util.Log.d("Polling", "Smart polling started (interval=${intervalMs}ms)")

        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            com.wildlife.deterrence.NotificationState.realtimeAlertEvent.collect {
                android.util.Log.d("Polling", "Realtime alert event received, reloading cameras...")
                loadCameras(isSilent = true)
            }
        }
    }

    private suspend fun smartPoll() {
        val token = tokenManager.getToken() ?: return
        val authHeader = "Bearer $token"
        try {
            val heartbeat = cameraApi.getCamerasHeartbeat(authHeader)
            val serverUpdatedAt = parseIsoDateTime(heartbeat.lastUpdatedAt)
            if (serverUpdatedAt > lastKnownUpdatedAt) {
                android.util.Log.d("Polling", "New update detected (server=$serverUpdatedAt > local=$lastKnownUpdatedAt), fetching cameras...")
                lastKnownUpdatedAt = serverUpdatedAt
                loadCameras(isSilent = true)
            } else {
                android.util.Log.d("Polling", "No new update, skipping GET /cameras")
            }
        } catch (e: Exception) {
            android.util.Log.e("Polling", "Heartbeat check failed", e)
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        realtimeJob?.cancel()
        realtimeJob = null
        android.util.Log.d("Polling", "Camera list polling stopped")
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
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
