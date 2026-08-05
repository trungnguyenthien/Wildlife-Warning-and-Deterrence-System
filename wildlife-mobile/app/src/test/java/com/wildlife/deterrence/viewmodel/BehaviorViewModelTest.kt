package com.wildlife.deterrence.viewmodel

import com.wildlife.deterrence.data.TokenManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class BehaviorViewModelTest {

    private val tokenManager = TokenManager(null)

    @Test
    fun TC_BEHAVIOR_LOAD_SPECIES_LIST() {
        val viewModel = BehaviorViewModel(tokenManager)
        val state = viewModel.speciesListState.value

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(6, state.speciesList.size)
        assertEquals("voi", state.speciesList[0].id)
        assertEquals("Voi", state.speciesList[0].name)
        assertEquals("Cao", state.speciesList[0].dangerLevel)
    }

    @Test
    fun TC_BEHAVIOR_GET_DEFAULT_CONFIG_BY_DANGER_LEVEL() {
        val viewModel = BehaviorViewModel(tokenManager)

        // Cọp (Hổ) -> Hung dữ Cao -> Preset mặc định Critical
        val tigerConfig = viewModel.getConfigForSpecies("cop")
        assertEquals("critical", tigerConfig.presetType)
        assertEquals("Tiếng nổ giả lập", tigerConfig.audioType)
        assertEquals(100, tigerConfig.audioVolume)
        assertEquals("Đỏ xen trắng", tigerConfig.ledColor)

        // Nai -> Hung dữ Thấp -> Preset mặc định Medium Animal
        val deerConfig = viewModel.getConfigForSpecies("nai")
        assertEquals("medium_animal", deerConfig.presetType)
        assertEquals("Tiếng chó sủa lớn", deerConfig.audioType)
        assertEquals(60, deerConfig.audioVolume)
        assertEquals("Trắng", deerConfig.ledColor)
    }

    @Test
    fun TC_BEHAVIOR_APPLY_PRESET() {
        val viewModel = BehaviorViewModel(tokenManager)

        val intruderPreset = viewModel.applyPreset("voi", "intruder")
        assertEquals("intruder", intruderPreset.presetType)
        assertEquals("Tiếng súng", intruderPreset.audioType)
        assertEquals(90, intruderPreset.audioVolume)
        assertEquals("Đỏ", intruderPreset.ledColor)
        assertEquals(15, intruderPreset.ledDuration)
        assertTrue(intruderPreset.silentAlertSms)

        val mediumPreset = viewModel.applyPreset("khi", "medium_animal")
        assertEquals("medium_animal", mediumPreset.presetType)
        assertEquals("Tiếng chó sủa lớn", mediumPreset.audioType)
        assertEquals(60, mediumPreset.audioVolume)
        assertFalse(mediumPreset.silentAlertSms)
    }

    @Test
    fun TC_BEHAVIOR_SAVE_AND_GET_CUSTOM_CONFIG() {
        val viewModel = BehaviorViewModel(tokenManager)

        val customConfig = BehaviorConfigUiModel(
            speciesId = "voi",
            presetType = "custom",
            audioType = "Tần số siêu âm",
            audioVolume = 45,
            ledFrequency = "2 lần/giây",
            ledColor = "Trắng",
            ledDuration = 30,
            sirenSampleId = "Mẫu 3",
            silentAlertSms = false,
            silentAlertPush = true
        )

        viewModel.saveConfigForSpecies("voi", customConfig)

        val loadedConfig = viewModel.getConfigForSpecies("voi")
        assertEquals("custom", loadedConfig.presetType)
        assertEquals("Tần số siêu âm", loadedConfig.audioType)
        assertEquals(45, loadedConfig.audioVolume)
        assertEquals(30, loadedConfig.ledDuration)
        assertFalse(loadedConfig.silentAlertSms)
        assertTrue(loadedConfig.silentAlertPush)
    }
}
