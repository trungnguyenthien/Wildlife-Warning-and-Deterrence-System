package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.TokenManager
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.ResponseConfigData
import com.wildlife.deterrence.data.SaveResponseConfigRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BehaviorConfigUiModel(
    val speciesId: String,
    val presetType: String, // "intruder" | "medium_animal" | "critical" | "custom"
    val audioType: String, // "Tiếng súng" | "Tiếng gầm" | "Tiếng chó sủa lớn" | "Tiếng nổ giả lập" | "Tần số siêu âm"
    val audioVolume: Int, // 1-100
    val ledFrequency: String, // "2 lần/giây" | "4 lần/giây" | "Nhấp nháy ngẫu nhiên"
    val ledColor: String, // "Đỏ" | "Trắng" | "Đỏ xen trắng"
    val ledDuration: Int, // 1-60 giây
    val sirenSampleId: String, // "Mẫu 1" | "Mẫu 2" | "Mẫu 3"
    val silentAlertSms: Boolean,
    val silentAlertPush: Boolean
)

data class SpeciesInfoUiModel(
    val id: String,
    val name: String,
    val dangerLevel: String // "Cao" | "Trung bình" | "Thấp"
)

data class BehaviorSpeciesListUiState(
    val speciesList: List<SpeciesInfoUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BehaviorViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _speciesListState = MutableStateFlow(BehaviorSpeciesListUiState())
    val speciesListState: StateFlow<BehaviorSpeciesListUiState> = _speciesListState.asStateFlow()

    // Lưu trữ cấu hình trong bộ nhớ session để bảo toàn dữ liệu
    private val _configsMap = mutableMapOf<String, BehaviorConfigUiModel>()

    init {
        loadSpeciesList()
    }

    fun loadSpeciesList() {
        _speciesListState.value = BehaviorSpeciesListUiState(isLoading = true)

        val token = tokenManager.getToken()
        if (token == null) {
            _speciesListState.value = BehaviorSpeciesListUiState(
                error = "Chưa đăng nhập hệ thống",
                isLoading = false
            )
            return
        }

        viewModelScope.launch {
            try {
                // 1. Tải danh mục loài từ API
                val responseList = NetworkClient.cameraApi.getSpecies("Bearer $token")
                val mappedList = responseList.map { s ->
                    SpeciesInfoUiModel(
                        id = s.id,
                        name = s.displayName,
                        dangerLevel = getDangerLevelLabel(s.dangerLevel)
                    )
                }

                // 2. Tải danh sách cấu hình hiện tại của user từ API
                val responseConfigs = NetworkClient.configApi.getConfigs("Bearer $token")
                responseConfigs.forEach { config ->
                    _configsMap[config.speciesId] = mapBackendConfigToUi(config)
                }

                _speciesListState.value = BehaviorSpeciesListUiState(
                    speciesList = mappedList,
                    isLoading = false
                )
            } catch (e: Exception) {
                _speciesListState.value = BehaviorSpeciesListUiState(
                    error = "Lỗi kết nối API: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun getConfigForSpecies(speciesId: String): BehaviorConfigUiModel {
        return _configsMap[speciesId] ?: createDefaultConfigForSpecies(speciesId)
    }

    fun saveConfigForSpecies(speciesId: String, config: BehaviorConfigUiModel) {
        _configsMap[speciesId] = config

        val token = tokenManager.getToken() ?: return
        viewModelScope.launch {
            try {
                val req = SaveResponseConfigRequest(
                    ledFlash = config.presetType != "custom" || config.ledFrequency != "Không",
                    ledColor = mapLedColorToBackend(config.ledColor),
                    ledIntensity = config.ledDuration,
                    speakerWarn = !config.silentAlertSms,
                    audioSampleId = mapAudioTypeToId(config.audioType),
                    audioIntensity = config.audioVolume,
                    silentAlert = config.silentAlertSms
                )
                NetworkClient.configApi.saveConfig(
                    token = "Bearer $token",
                    speciesId = speciesId,
                    body = req
                )
            } catch (e: Exception) {
                System.err.println("Lỗi khi lưu cấu hình lên server: ${e.message}")
            }
        }
    }

    fun applyPreset(speciesId: String, presetType: String): BehaviorConfigUiModel {
        return when (presetType) {
            "intruder" -> BehaviorConfigUiModel(
                speciesId = speciesId,
                presetType = "intruder",
                audioType = "Tiếng súng",
                audioVolume = 90,
                ledFrequency = "4 lần/giây",
                ledColor = "Đỏ",
                ledDuration = 15,
                sirenSampleId = "Mẫu 1",
                silentAlertSms = true,
                silentAlertPush = true
            )
            "medium_animal" -> BehaviorConfigUiModel(
                speciesId = speciesId,
                presetType = "medium_animal",
                audioType = "Tiếng chó sủa lớn",
                audioVolume = 60,
                ledFrequency = "2 lần/giây",
                ledColor = "Trắng",
                ledDuration = 10,
                sirenSampleId = "Mẫu 2",
                silentAlertSms = false,
                silentAlertPush = true
            )
            "critical" -> BehaviorConfigUiModel(
                speciesId = speciesId,
                presetType = "critical",
                audioType = "Tiếng nổ giả lập",
                audioVolume = 100,
                ledFrequency = "Nhấp nháy ngẫu nhiên",
                ledColor = "Đỏ xen trắng",
                ledDuration = 20,
                sirenSampleId = "Mẫu 1",
                silentAlertSms = true,
                silentAlertPush = true
            )
            else -> getConfigForSpecies(speciesId).copy(presetType = "custom")
        }
    }

    private fun createDefaultConfigForSpecies(speciesId: String): BehaviorConfigUiModel {
        val species = _speciesListState.value.speciesList.find { it.id == speciesId }
        val defaultPreset = when (species?.dangerLevel) {
            "Cao" -> "critical"
            "Trung bình" -> "medium_animal"
            else -> "medium_animal" // Thấp
        }
        return applyPreset(speciesId, defaultPreset)
    }

    // Các hàm ánh xạ tiện ích giữa UI và Backend API

    private fun getDangerLevelLabel(dangerLevel: String): String {
        return when (dangerLevel) {
            "CRITICAL", "HIGH" -> "Cao"
            "MEDIUM" -> "Trung bình"
            else -> "Thấp"
        }
    }

    private fun mapAudioTypeToId(type: String): String? {
        return when (type) {
            "Tiếng súng", "Tiếng súng nổ đe dọa" -> "A_gunshot"
            "Tiếng chó sủa lớn", "Tiếng chó sủa dữ dội", "Tiếng gầm" -> "A_dog_bark"
            "Tiếng nổ giả lập", "Tiếng còi hú khẩn cấp" -> "A_alarm_siren"
            "Phát loa cảnh báo" -> "S_warn_citizen"
            else -> null
        }
    }

    private fun mapAudioIdToType(id: String?): String {
        return when (id) {
            "A_gunshot" -> "Tiếng súng"
            "A_dog_bark" -> "Tiếng chó sủa lớn"
            "A_alarm_siren" -> "Tiếng nổ giả lập"
            "S_warn_citizen" -> "Phát loa cảnh báo"
            else -> "Không có"
        }
    }

    private fun mapLedColorToBackend(color: String): String? {
        return when (color) {
            "Đỏ" -> "RED"
            "Trắng" -> "WHITE"
            "Đỏ xen trắng" -> "STROBE"
            "Vàng" -> "YELLOW"
            else -> null
      }
    }

    private fun mapBackendToLedColor(color: String?): String {
        return when (color) {
            "RED" -> "Đỏ"
            "WHITE" -> "Trắng"
            "STROBE" -> "Đỏ xen trắng"
            "YELLOW" -> "Vàng"
            else -> "Trắng"
        }
    }

    private fun mapBackendConfigToUi(data: ResponseConfigData): BehaviorConfigUiModel {
        val audioType = mapAudioIdToType(data.audioSampleId)
        val ledColor = mapBackendToLedColor(data.ledColor)

        val presetType = if (data.id == null) {
            "critical"
        } else {
            if (data.ledFlash && data.ledColor == "STROBE" && data.audioSampleId == "A_gunshot" && data.audioIntensity == 90) {
                "critical"
            } else if (data.ledColor == "YELLOW" && data.audioSampleId == "A_dog_bark") {
                "medium_animal"
            } else if (data.ledColor == "RED" && data.audioSampleId == "A_alarm_siren") {
                "intruder"
            } else {
                "custom"
            }
        }

        return BehaviorConfigUiModel(
            speciesId = data.speciesId,
            presetType = presetType,
            audioType = audioType,
            audioVolume = data.audioIntensity,
            ledFrequency = if (data.ledFlash) "4 lần/giây" else "2 lần/giây",
            ledColor = ledColor,
            ledDuration = data.ledIntensity,
            sirenSampleId = "Mẫu 1",
            silentAlertSms = data.silentAlert,
            silentAlertPush = true
        )
    }
}
