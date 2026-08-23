package com.wildlife.deterrence.viewmodel

import com.wildlife.deterrence.data.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BehaviorViewModelTest {

    private val tokenManager = TokenManager(null).apply {
        saveToken("test-token")
    }

    private val fakeCameraApi = object : FakeCameraApi() {
        override suspend fun getSpecies(token: String): List<SpeciesResponse> {
            return listOf(
                SpeciesResponse("voi", "Voi", "CRITICAL", false, "", 5, "", "2026-07-28T00:00:00Z"),
                SpeciesResponse("cop", "Cọp", "HIGH", false, "", 5, "", "2026-07-28T00:00:00Z"),
                SpeciesResponse("nai", "Nai", "LOW", false, "", 5, "", "2026-07-28T00:00:00Z"),
                SpeciesResponse("khi", "Khỉ", "MEDIUM", false, "", 5, "", "2026-07-28T00:00:00Z"),
                SpeciesResponse("heo", "Heo", "MEDIUM", false, "", 5, "", "2026-07-28T00:00:00Z"),
                SpeciesResponse("nguoi", "Người", "LOW", true, "", 5, "", "2026-07-28T00:00:00Z")
            )
        }

        override suspend fun getCameras(token: String): List<CameraResponse> {
            return listOf(
                CameraResponse(
                    id = "cam-1",
                    name = "Camera Trạm Đông",
                    location = LocationResponse(10.0, 106.0, "Cửa Rừng"),
                    status = "ONLINE",
                    liveFeedUrl = "http://...",
                    snapshot = null
                )
            )
        }

        override suspend fun testDevice(token: String, cameraId: String, deviceKey: String, body: TestDeviceRequest): retrofit2.Response<Unit> {
            return retrofit2.Response.success(Unit)
        }
    }

    private val fakeConfigApi = FakeConfigApi()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun TC_BEHAVIOR_LOAD_SPECIES_LIST() {
        val viewModel = BehaviorViewModel(tokenManager, fakeCameraApi, fakeConfigApi)
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
        val viewModel = BehaviorViewModel(tokenManager, fakeCameraApi, fakeConfigApi)

        // Cọp (Hổ) -> Hung dữ Cao -> Preset mặc định Critical
        val tigerConfig = viewModel.getConfigForSpecies("cop")
        assertEquals("critical", tigerConfig.presetType)
        assertEquals("Tiếng súng", tigerConfig.audioType)
        assertEquals(100, tigerConfig.audioVolume)
        assertEquals("Đỏ xen trắng", tigerConfig.ledColor)

        // Nai -> Hung dữ Thấp -> Preset mặc định Medium Animal
        val deerConfig = viewModel.getConfigForSpecies("nai")
        assertEquals("medium_animal", deerConfig.presetType)
        assertEquals("Tiếng chó sủa lớn", deerConfig.audioType)
        assertEquals(50, deerConfig.audioVolume)
        assertEquals("Vàng", deerConfig.ledColor)
    }

    @Test
    fun TC_BEHAVIOR_APPLY_PRESET() {
        val viewModel = BehaviorViewModel(tokenManager, fakeCameraApi, fakeConfigApi)

        val intruderPreset = viewModel.applyPreset("voi", "intruder")
        assertEquals("intruder", intruderPreset.presetType)
        assertEquals("Tiếng nổ giả lập", intruderPreset.audioType)
        assertEquals(90, intruderPreset.audioVolume)
        assertEquals("Đỏ", intruderPreset.ledColor)
        assertEquals(15, intruderPreset.ledDuration)
        assertFalse(intruderPreset.silentAlertSms)

        val mediumPreset = viewModel.applyPreset("khi", "medium_animal")
        assertEquals("medium_animal", mediumPreset.presetType)
        assertEquals("Tiếng chó sủa lớn", mediumPreset.audioType)
        assertEquals(50, mediumPreset.audioVolume)
        assertFalse(mediumPreset.silentAlertSms)
    }

    @Test
    fun TC_BEHAVIOR_SAVE_AND_GET_CUSTOM_CONFIG() {
        val viewModel = BehaviorViewModel(tokenManager, fakeCameraApi, fakeConfigApi)

        val customConfig = BehaviorConfigUiModel(
            speciesId = "voi",
            presetType = "custom",
            audioType = "Tiếng gầm",
            audioVolume = 45,
            ledFrequency = "2 lần/giây",
            ledColor = "Trắng",
            ledDuration = 30,
            sirenSampleId = "Tiếng Voi",
            silentAlertSms = false,
            silentAlertPush = true
        )

        viewModel.saveConfigForSpecies("voi", customConfig)

        val loadedConfig = viewModel.getConfigForSpecies("voi")
        assertEquals("custom", loadedConfig.presetType)
        assertEquals("Tiếng gầm", loadedConfig.audioType)
        assertEquals(45, loadedConfig.audioVolume)
        assertEquals(30, loadedConfig.ledDuration)
        assertFalse(loadedConfig.silentAlertSms)
        assertTrue(loadedConfig.silentAlertPush)
    }

    @Test
    fun TC_BEHAVIOR_TEST_AUDIO_AT_STATION() {
        val viewModel = BehaviorViewModel(tokenManager, fakeCameraApi, fakeConfigApi)
        
        // Đợi init load cameras xong, kiểm tra selected camera
        assertNotNull(viewModel.selectedCameraForTest.value)
        assertEquals("cam-1", viewModel.selectedCameraForTest.value?.id)

        var successCalled = false
        var errorCalled = false

        viewModel.testAudioAtStation(
            speciesId = "voi",
            onSuccess = { successCalled = true },
            onError = { errorCalled = true }
        )

        assertTrue(successCalled)
        assertFalse(errorCalled)
    }
}

class FakeConfigApi : ConfigApi {
    override suspend fun getAudioSamples(): AudioSamplesResponse {
        return AudioSamplesResponse(
            animalDeterrentSounds = listOf(
                AudioSampleItem("A_gunshot", "Tiếng súng"),
                AudioSampleItem("A_growl", "Tiếng gầm"),
                AudioSampleItem("A_dog_bark", "Tiếng chó sủa lớn"),
                AudioSampleItem("A_explosion", "Tiếng nổ giả lập")
            ),
            citizenAlertSounds = listOf(
                AlertSoundItem("deer", "Tiếng Nai", "https://.../deer.mp3"),
                AlertSoundItem("elephant", "Tiếng Voi", "https://.../elephant.mp3"),
                AlertSoundItem("monkey", "Tiếng Khỉ", "https://.../monkey.mp3"),
                AlertSoundItem("tiger", "Tiếng Hổ", "https://.../tiger.mp3"),
                AlertSoundItem("wild_boar", "Tiếng Lợn rừng", "https://.../wild_boar.mp3")
            )
        )
    }

    override suspend fun getAlertSounds(token: String): List<AlertSoundItem> = listOf()
    override suspend fun getConfigs(token: String): List<ResponseConfigData> = emptyList()
    override suspend fun getConfigDetail(token: String, speciesId: String): ResponseConfigData {
        throw NotImplementedError()
    }
    override suspend fun saveConfig(
        token: String,
        speciesId: String,
        body: SaveResponseConfigRequest
    ): ResponseConfigData {
        return ResponseConfigData(
            id = "cfg-123",
            userId = "user-123",
            speciesId = speciesId,
            ledFlash = body.ledFlash,
            ledColor = body.ledColor,
            ledIntensity = body.ledIntensity,
            speakerWarn = body.speakerWarn,
            audioSampleId = body.audioSampleId,
            audioIntensity = body.audioIntensity,
            silentAlert = body.silentAlert,
            ledFlashRate = body.ledFlashRate,
            speakerSampleId = body.speakerSampleId
        )
    }
    override suspend fun resetConfig(token: String, speciesId: String): retrofit2.Response<Unit> {
        return retrofit2.Response.success(Unit)
    }
}
