package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.AlertApi
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlertDetailUiState(
    val alertId: String = "",
    val title: String = "",
    val alertType: String = "animal", // "animal" | "intrusion"
    val imageUrl: String? = null,
    val speciesName: String? = null,
    val speciesNameEn: String? = null,
    val cameraCode: String = "",
    val cameraName: String = "",
    val dangerLevel: String = "LOW",
    val confidencePercent: Int? = null,
    val estimatedCount: Int? = null,
    val recordedAt: String = "", // "HH:mm:ss · dd/MM/yyyy"
    val gpsCoordinate: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AlertDetailViewModel(
    private val alertId: String,
    private val initialSpeciesName: String?,
    private val tokenManager: TokenManager,
    private val alertApi: AlertApi = NetworkClient.alertApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertDetailUiState(alertId = alertId))
    val uiState: StateFlow<AlertDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlertDetail()
    }

    fun loadAlertDetail() {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Vui lòng đăng nhập lại.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                println("AlertDetail: Calling API: ${com.wildlife.deterrence.data.NetworkClient.BASE_URL}alerts/$alertId")
                val response = alertApi.getAlertDetail("Bearer $token", alertId)
                _uiState.value = AlertDetailUiState(
                    alertId = response.alertId,
                    title = response.title,
                    alertType = response.alertType,
                    imageUrl = response.imageUrl,
                    speciesName = response.speciesName,
                    speciesNameEn = response.speciesNameEn,
                    cameraCode = response.cameraCode,
                    cameraName = response.cameraName,
                    dangerLevel = response.dangerLevel,
                    confidencePercent = response.confidencePercent,
                    estimatedCount = response.estimatedCount,
                    recordedAt = response.recordedAt,
                    gpsCoordinate = response.gpsCoordinate,
                    isLoading = false
                )
            } catch (e: Exception) {
                // Fallback debug data if server is offline or 404
                println("AlertDetail: API failed, falling back to mock data: ${e.localizedMessage}")
                _uiState.value = getMockAlertDetail(alertId)
            }
        }
    }

    private fun getMockAlertDetail(id: String): AlertDetailUiState {
        val isIntrusion = initialSpeciesName?.contains("Người", ignoreCase = true) == true || 
                          initialSpeciesName?.contains("Intrusion", ignoreCase = true) == true ||
                          id.contains("intrusion")
        
        val isTiger = initialSpeciesName?.contains("Hổ", ignoreCase = true) == true || 
                      initialSpeciesName?.contains("Cọp", ignoreCase = true) == true ||
                      id.contains("ho") || id.contains("tiger")
        
        return when {
            isIntrusion -> {
                AlertDetailUiState(
                    alertId = id,
                    title = "Cảnh báo: Phát hiện đối tượng xâm nhập tại Trạm Biên Phòng",
                    alertType = "intrusion",
                    imageUrl = null,
                    speciesName = "Người Lạ / Đối Tượng Xâm Nhập",
                    speciesNameEn = "Intruder / Border Intrusion",
                    cameraCode = "CAM-05",
                    cameraName = "Trạm Biên Phòng Cổng Tây",
                    dangerLevel = "HIGH",
                    confidencePercent = 94,
                    estimatedCount = 1,
                    recordedAt = "10:15:30 · 30/07/2026",
                    gpsCoordinate = "14.312, 107.584",
                    isLoading = false
                )
            }
            isTiger -> {
                AlertDetailUiState(
                    alertId = id,
                    title = "Phát hiện Hổ Đông Nam Á tại Trạm 2",
                    alertType = "animal",
                    imageUrl = null,
                    speciesName = "Hổ Đông Nam Á",
                    speciesNameEn = "Panthera tigris corbetti",
                    cameraCode = "CAM-02",
                    cameraName = "Trạm quan trắc 2",
                    dangerLevel = "CRITICAL",
                    confidencePercent = 99,
                    estimatedCount = 1,
                    recordedAt = "23:45:10 · 30/07/2026",
                    gpsCoordinate = "14.315, 107.590",
                    isLoading = false
                )
            }
            else -> { // Voi Rừng
                AlertDetailUiState(
                    alertId = id,
                    title = "Phát hiện Voi Rừng tại Trạm 1",
                    alertType = "animal",
                    imageUrl = null,
                    speciesName = "Voi Rừng Tây Nguyên",
                    speciesNameEn = "Elephas maximus",
                    cameraCode = "CAM-01",
                    cameraName = "Cửa rừng phía Bắc",
                    dangerLevel = "CRITICAL",
                    confidencePercent = 98,
                    estimatedCount = 2,
                    recordedAt = "14:30:15 · 30/07/2026",
                    gpsCoordinate = "14.308, 107.579",
                    isLoading = false
                )
            }
        }
    }
}
