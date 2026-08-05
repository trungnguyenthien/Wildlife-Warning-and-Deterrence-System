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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AlertDetailViewModelTest {

    private val tokenManager = TokenManager(null).apply {
        saveToken("test-jwt-token")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun TC_UI_ALERTDETAIL_LOAD_SUCCESS() = runTest {
        val fakeAlertApi = object : FakeAlertApi() {
            override suspend fun getAlertDetail(token: String, alertId: String): AlertDetailResponse {
                return AlertDetailResponse(
                    alertId = "alt-991",
                    title = "Phát hiện HỔ tại Cam 2",
                    alertType = "animal",
                    imageUrl = "https://cdn.example.com/tiger.jpg",
                    speciesName = "Hổ Đông Nam Á",
                    speciesNameEn = "Indochinese Tiger",
                    cameraCode = "cam-002",
                    cameraName = "Trạm Rìa Rừng Cổng Bắc",
                    dangerLevel = "CRITICAL",
                    confidencePercent = 99,
                    estimatedCount = 1,
                    recordedAt = "10:15:00 · 22/07/2026",
                    gpsCoordinate = "10.460, 106.124"
                )
            }
        }

        val viewModel = AlertDetailViewModel("alt-991", "Hổ Đông Nam Á", tokenManager, fakeAlertApi)
        viewModel.loadAlertDetail()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("alt-991", state.alertId)
        assertEquals("Phát hiện HỔ tại Cam 2", state.title)
        assertEquals("animal", state.alertType)
        assertEquals("https://cdn.example.com/tiger.jpg", state.imageUrl)
        assertEquals("Hổ Đông Nam Á", state.speciesName)
        assertEquals("Indochinese Tiger", state.speciesNameEn)
        assertEquals("cam-002", state.cameraCode)
        assertEquals("Trạm Rìa Rừng Cổng Bắc", state.cameraName)
        assertEquals("CRITICAL", state.dangerLevel)
        assertEquals(99, state.confidencePercent)
        assertEquals(1, state.estimatedCount)
        assertEquals("10:15:00 · 22/07/2026", state.recordedAt)
        assertEquals("10.460, 106.124", state.gpsCoordinate)
    }

    @Test
    fun TC_UI_ALERTDETAIL_ANIMAL_VS_INTRUSION() = runTest {
        val fakeAlertApi = object : FakeAlertApi() {
            override suspend fun getAlertDetail(token: String, alertId: String): AlertDetailResponse {
                return AlertDetailResponse(
                    alertId = "alt-992",
                    title = "Cảnh báo: Phát hiện đối tượng xâm nhập tại Trạm Biên Phòng",
                    alertType = "intrusion",
                    imageUrl = "https://cdn.example.com/intruder.jpg",
                    speciesName = "Người Lạ",
                    speciesNameEn = "Intruder / Unknown Person",
                    cameraCode = "cam-005",
                    cameraName = "Trạm Biên Phòng",
                    dangerLevel = "HIGH",
                    confidencePercent = 95,
                    estimatedCount = 1,
                    recordedAt = "11:20:00 · 22/07/2026",
                    gpsCoordinate = "10.480, 106.140"
                )
            }
        }

        val viewModel = AlertDetailViewModel("alt-992", "Người Lạ", tokenManager, fakeAlertApi)
        viewModel.loadAlertDetail()

        val state = viewModel.uiState.value
        assertEquals("intrusion", state.alertType)
        assertEquals("Người Lạ", state.speciesName)
    }

    @Test
    fun TC_UI_ALERTDETAIL_API_404_HANDLED() = runTest {
        val fakeAlertApi = object : FakeAlertApi() {
            override suspend fun getAlertDetail(token: String, alertId: String): AlertDetailResponse {
                throw RuntimeException("404 Not Found")
            }
        }

        val viewModel = AlertDetailViewModel("alt-failed-api", "Voi Rừng", tokenManager, fakeAlertApi)
        viewModel.loadAlertDetail()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("alt-failed-api", state.alertId)
        assertNotNull(state.speciesName)
        assertNull(state.imageUrl)
    }
}

open class FakeAlertApi : AlertApi {
    override suspend fun getAlertDetail(token: String, alertId: String): AlertDetailResponse {
        throw NotImplementedError()
    }

    override suspend fun getAlertsFeed(token: String, page: Int, size: Int): List<AlertResponse> {
        throw NotImplementedError()
    }
}
