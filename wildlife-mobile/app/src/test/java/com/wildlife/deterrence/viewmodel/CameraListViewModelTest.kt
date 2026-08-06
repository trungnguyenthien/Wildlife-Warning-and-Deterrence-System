package com.wildlife.deterrence.viewmodel

import com.wildlife.deterrence.data.AlertResponse
import com.wildlife.deterrence.data.CameraApi
import com.wildlife.deterrence.data.CameraResponse
import com.wildlife.deterrence.data.LocationResponse
import com.wildlife.deterrence.data.SnapshotResponse
import com.wildlife.deterrence.data.TokenManager
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

class CameraListViewModelTest {

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
    fun TC_UI_CAM_LOAD_SUCCESS() = runTest {
        val fakeCameraApi = FakeCameraApi().apply {
            shouldSucceed = true
            camerasList = listOf(
                CameraResponse(
                    id = "cam-01",
                    name = "Trạm 1",
                    location = LocationResponse(lat = 11.2, lng = 107.5, address = "Rìa Nam"),
                    status = "ONLINE",
                    liveFeedUrl = "http://feed/1",
                    snapshot = SnapshotResponse(url = "http://img/1.jpg", capturedAt = "2026-07-27T10:00:00Z")
                ),
                CameraResponse(
                    id = "cam-02",
                    name = "Trạm 2",
                    location = LocationResponse(lat = 11.3, lng = 107.6, address = "Rìa Bắc"),
                    status = "OFFLINE",
                    liveFeedUrl = "http://feed/2",
                    snapshot = null
                )
            )
        }

        val viewModel = CameraListViewModel(tokenManager, fakeCameraApi)
        viewModel.loadCameras()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.stations.size)

        // Camera 1 assertions
        val cam1 = state.stations[0]
        assertEquals("cam-01", cam1.id)
        assertEquals("Trạm 1", cam1.name)
        assertTrue(cam1.isOnline)
        assertEquals("http://img/1.jpg", cam1.thumbnailUrl)
        assertNotNull(cam1.timestampText)

        // Camera 2 assertions
        val cam2 = state.stations[1]
        assertEquals("cam-02", cam2.id)
        assertFalse(cam2.isOnline)
        assertNull(cam2.thumbnailUrl)
        assertEquals("", cam2.timestampText)
    }

    @Test
    fun TC_UI_CAM_LOAD_FAILURE() = runTest {
        val fakeCameraApi = FakeCameraApi().apply {
            shouldSucceed = false
        }

        val viewModel = CameraListViewModel(tokenManager, fakeCameraApi)
        viewModel.loadCameras()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.stations.isEmpty())
    }

    @Test
    fun TC_UI_CAM_SSE_TRIGGER() = runTest {
        var loadCalledCount = 0
        val fakeCameraApi = object : FakeCameraApi() {
            override suspend fun getCameras(token: String): List<CameraResponse> {
                loadCalledCount++
                return emptyList()
            }
        }

        val viewModel = CameraListViewModel(tokenManager, fakeCameraApi)
        // Gọi load lần đầu
        viewModel.loadCameras()
        assertEquals(1, loadCalledCount)

        // Giả lập nhận event từ SSE
        viewModel.refreshCameras()
        assertEquals(2, loadCalledCount)
    }

    @Test
    fun TC_UI_CAM_OFFLINE_30S() = runTest {
        val fakeCameraApi = FakeCameraApi().apply {
            camerasList = listOf(
                CameraResponse(
                    id = "cam-03",
                    name = "Trạm 3",
                    location = LocationResponse(lat = 11.4, lng = 107.7, address = "Khu Trung Tâm"),
                    status = "OFFLINE", // Giả lập trạng thái camera offline
                    liveFeedUrl = "http://feed/3",
                    snapshot = null
                )
            )
        }

        val viewModel = CameraListViewModel(tokenManager, fakeCameraApi)
        viewModel.loadCameras()

        val cam = viewModel.uiState.value.stations[0]
        assertFalse(cam.isOnline) // Đảm bảo trạng thái offline được cập nhật chính xác trên UI model
    }
}

open class FakeCameraApi : CameraApi {
    var shouldSucceed: Boolean = true
    var camerasList: List<CameraResponse> = emptyList()
    var alertsList: List<AlertResponse> = emptyList()

    override suspend fun getCameras(token: String): List<CameraResponse> {
        if (!shouldSucceed) throw Exception("API Connection failure")
        return camerasList
    }

    override suspend fun getCameraDetail(token: String, cameraId: String): com.wildlife.deterrence.data.CameraDetailResponse {
        throw Exception("Not implemented")
    }

    override suspend fun getCameraHistory(token: String, cameraId: String, date: String?): List<com.wildlife.deterrence.data.DetectionHistoryItemResponse> {
        return emptyList()
    }

    override suspend fun renameCamera(token: String, cameraId: String, body: com.wildlife.deterrence.data.RenameCameraRequest): CameraResponse {
        throw Exception("Not implemented")
    }

    override suspend fun getAlertsFeed(token: String, page: Int, size: Int): List<AlertResponse> {
        if (!shouldSucceed) throw Exception("API Connection failure")
        return alertsList
    }

    override suspend fun readAlert(token: String, alertId: String): retrofit2.Response<Unit> {
        return retrofit2.Response.success(Unit)
    }

    override suspend fun getSpecies(token: String): List<com.wildlife.deterrence.data.SpeciesResponse> {
        return emptyList()
    }

    override suspend fun getStatsSummary(
        token: String,
        startDate: String,
        endDate: String,
        cameraId: String?,
        speciesId: String?
    ): com.wildlife.deterrence.data.StatsSummaryResponse {
        return com.wildlife.deterrence.data.StatsSummaryResponse(emptyList(), emptyList(), emptyList())
    }

    override suspend fun testDevice(
        token: String,
        cameraId: String,
        deviceKey: String,
        body: com.wildlife.deterrence.data.TestDeviceRequest
    ): retrofit2.Response<Unit> {
        return retrofit2.Response.success(Unit)
    }
}
