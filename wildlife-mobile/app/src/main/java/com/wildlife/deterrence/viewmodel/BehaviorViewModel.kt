package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val icon: String,
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
        _speciesListState.value = BehaviorSpeciesListUiState(
            speciesList = defaultSpeciesList,
            isLoading = false
        )
    }

    fun getConfigForSpecies(speciesId: String): BehaviorConfigUiModel {
        return _configsMap[speciesId] ?: createDefaultConfigForSpecies(speciesId)
    }

    fun saveConfigForSpecies(speciesId: String, config: BehaviorConfigUiModel) {
        _configsMap[speciesId] = config
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
        val species = defaultSpeciesList.find { it.id == speciesId }
        val defaultPreset = when (species?.dangerLevel) {
            "Cao" -> "critical"
            "Trung bình" -> "medium_animal"
            else -> "medium_animal" // Thấp
        }
        return applyPreset(speciesId, defaultPreset)
    }

    companion object {
        val defaultSpeciesList = listOf(
            SpeciesInfoUiModel("voi", "Voi", "🐘", "Cao"),
            SpeciesInfoUiModel("cop", "Cọp", "🐅", "Cao"),
            SpeciesInfoUiModel("nai", "Nai", "🦌", "Thấp"),
            SpeciesInfoUiModel("khi", "Khỉ", "🐒", "Trung bình"),
            SpeciesInfoUiModel("heo_rung", "Heo rừng", "🐗", "Trung bình"),
            SpeciesInfoUiModel("ca_sau", "Cá sấu", "🐊", "Cao")
        )
    }
}
