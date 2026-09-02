package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.TokenManager
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.ResponseConfigData
import com.wildlife.deterrence.data.SaveResponseConfigRequest
import com.wildlife.deterrence.data.CameraApi
import com.wildlife.deterrence.data.ConfigApi
import com.wildlife.deterrence.data.AlertSoundItem
import com.wildlife.deterrence.data.AudioSampleItem
import com.wildlife.deterrence.data.AudioSamplesResponse
import com.wildlife.deterrence.data.CameraResponse
import com.wildlife.deterrence.data.TestDeviceRequest
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
    private val tokenManager: TokenManager,
    private val cameraApi: CameraApi = NetworkClient.cameraApi,
    private val configApi: ConfigApi = NetworkClient.configApi
) : ViewModel() {

    private val _speciesListState = MutableStateFlow(BehaviorSpeciesListUiState())
    val speciesListState: StateFlow<BehaviorSpeciesListUiState> = _speciesListState.asStateFlow()

    private val _deterrentSounds = MutableStateFlow<List<AudioSampleItem>>(emptyList())
    val deterrentSounds: StateFlow<List<AudioSampleItem>> = _deterrentSounds.asStateFlow()

    private val _citizenAlertSounds = MutableStateFlow<List<AlertSoundItem>>(emptyList())
    val citizenAlertSounds: StateFlow<List<AlertSoundItem>> = _citizenAlertSounds.asStateFlow()

    private val _cameras = MutableStateFlow<List<CameraResponse>>(emptyList())
    val cameras: StateFlow<List<CameraResponse>> = _cameras.asStateFlow()

    private val _selectedCameraForTest = MutableStateFlow<CameraResponse?>(null)
    val selectedCameraForTest: StateFlow<CameraResponse?> = _selectedCameraForTest.asStateFlow()

    // Lưu trữ cấu hình trong bộ nhớ session để bảo toàn dữ liệu
    private val _configsMap = mutableMapOf<String, BehaviorConfigUiModel>()

    init {
        loadSpeciesList()
    }

    fun setSelectedCameraForTest(camera: CameraResponse) {
        _selectedCameraForTest.value = camera
    }

    fun testAudioAtStation(
        speciesId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val camera = _selectedCameraForTest.value
        if (camera == null) {
            onError("Vui lòng chọn trạm camera để nghe thử.")
            return
        }
        val token = tokenManager.getToken() ?: return
        val authHeader = "Bearer $token"
        val config = getConfigForSpecies(speciesId)

        // Ánh xạ audioType -> audioSampleId
        val audioSampleId = mapAudioTypeToId(config.audioType)

        viewModelScope.launch {
            try {
                // Gửi lệnh test loa tại trạm camera đã chọn
                val response = cameraApi.testDevice(
                    token = authHeader,
                    cameraId = camera.id,
                    deviceKey = "speaker",
                    body = TestDeviceRequest(
                        durationSeconds = 5, // Phát thử 5 giây
                        intensity = config.audioVolume,
                        audioSampleId = audioSampleId
                    )
                )
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Lỗi ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Lỗi kết nối")
            }
        }
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
                // 0. Tải danh mục âm thanh mẫu từ API
                try {
                    val audioSamples = configApi.getAudioSamples()
                    _deterrentSounds.value = audioSamples.animalDeterrentSounds
                    _citizenAlertSounds.value = audioSamples.citizenAlertSounds
                } catch (e: Exception) {
                    // Không hardcode id âm thanh: khi API lỗi để danh sách trống, tránh nhét giá trị cứng
                    System.err.println("Lỗi tải danh mục âm thanh từ API: ${e.message}")
                    _deterrentSounds.value = emptyList()
                    _citizenAlertSounds.value = emptyList()
                }

                // Tải danh sách camera để phục vụ tính năng nghe thử tại trạm
                try {
                    val cameraList = cameraApi.getCameras("Bearer $token")
                    _cameras.value = cameraList
                    if (cameraList.isNotEmpty() && _selectedCameraForTest.value == null) {
                        _selectedCameraForTest.value = cameraList.first()
                    }
                } catch (e: Exception) {
                    System.err.println("Lỗi tải danh sách camera: ${e.message}")
                }

                // 1. Tải danh mục loài từ API
                val responseList = cameraApi.getSpecies("Bearer $token")
                val mappedList = responseList.map { s ->
                    SpeciesInfoUiModel(
                        id = s.id,
                        name = s.displayName,
                        dangerLevel = getDangerLevelLabel(s.dangerLevel)
                    )
                }

                // 2. Tải danh sách cấu hình hiện tại của user từ API
                val responseConfigs = configApi.getConfigs("Bearer $token")
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
                val ledFlashRate = when (config.ledFrequency) {
                    "2 lần/giây" -> "2_per_sec"
                    "4 lần/giây" -> "4_per_sec"
                    "Nhấp nháy ngẫu nhiên" -> "random"
                    else -> null
                }
                val req = SaveResponseConfigRequest(
                    ledFlash = config.presetType != "custom" || config.ledFrequency != "Không",
                    ledColor = mapLedColorToBackend(config.ledColor),
                    ledIntensity = config.ledDuration,
                    speakerWarn = !config.silentAlertSms,
                    audioSampleId = mapAudioTypeToId(config.audioType),
                    audioIntensity = config.audioVolume,
                    silentAlert = config.silentAlertSms,
                    ledFlashRate = ledFlashRate
                )
                configApi.saveConfig(
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
                audioType = mapAudioIdToType("A_explosion"),
                audioVolume = 90,
                ledFrequency = "4 lần/giây",
                ledColor = "Đỏ",
                ledDuration = 15,
                silentAlertSms = false,
                silentAlertPush = true
            )
            "medium_animal" -> BehaviorConfigUiModel(
                speciesId = speciesId,
                presetType = "medium_animal",
                audioType = mapAudioIdToType("A_dog_bark"),
                audioVolume = 50,
                ledFrequency = "2 lần/giây",
                ledColor = "Vàng",
                ledDuration = 10,
                silentAlertSms = false,
                silentAlertPush = true
            )
            "critical" -> BehaviorConfigUiModel(
                speciesId = speciesId,
                presetType = "critical",
                audioType = mapAudioIdToType("A_gunshot"),
                audioVolume = 100,
                ledFrequency = "Nhấp nháy ngẫu nhiên",
                ledColor = "Đỏ xen trắng",
                ledDuration = 20,
                silentAlertSms = false,
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
        if (type == "Không" || type == "Không có") return null
        val found = _deterrentSounds.value.find { it.name == type }
        if (found != null) return found.id
        return when (type) {
            "Tiếng súng", "Tiếng súng nổ đe dọa" -> "A_gunshot"
            "Tiếng gầm", "Tiếng gầm đe dọa" -> "A_growl"
            "Tiếng chó sủa lớn", "Tiếng chó sủa dữ dội", "Chó sủa lớn" -> "A_dog_bark"
            "Tiếng nổ giả lập", "Tiếng còi hú khẩn cấp" -> "A_explosion"
            else -> null
        }
    }

    private fun mapAudioIdToType(id: String?): String {
        if (id == null) return "Không"
        val found = _deterrentSounds.value.find { it.id == id }
        if (found != null) return found.name
        return when (id) {
            "A_gunshot" -> "Tiếng súng"
            "A_growl" -> "Tiếng gầm"
            "A_dog_bark" -> "Chó sủa lớn"
            "A_explosion" -> "Tiếng nổ giả lập"
            else -> "Không"
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
            if (data.ledFlash && data.ledColor == "STROBE" && data.audioSampleId == "A_gunshot" && data.audioIntensity == 100 && data.ledIntensity == 20 && data.ledFlashRate == "random") {
                "critical"
            } else if (data.ledColor == "YELLOW" && data.audioSampleId == "A_dog_bark" && data.audioIntensity == 50 && data.ledIntensity == 10 && data.ledFlashRate == "2_per_sec") {
                "medium_animal"
            } else if (data.ledColor == "RED" && data.audioSampleId == "A_explosion" && data.audioIntensity == 90 && data.ledIntensity == 15 && data.ledFlashRate == "4_per_sec") {
                "intruder"
            } else {
                "custom"
            }
        }

        val ledFrequency = when (data.ledFlashRate) {
            "2_per_sec" -> "2 lần/giây"
            "4_per_sec" -> "4 lần/giây"
            "random" -> "Nhấp nháy ngẫu nhiên"
            else -> if (data.ledFlash) "4 lần/giây" else "Không"
        }

        return BehaviorConfigUiModel(
            speciesId = data.speciesId,
            presetType = presetType,
            audioType = audioType,
            audioVolume = data.audioIntensity,
            ledFrequency = ledFrequency,
            ledColor = ledColor,
            ledDuration = data.ledIntensity,
            silentAlertSms = data.silentAlert,
            silentAlertPush = true
        )
    }
}
