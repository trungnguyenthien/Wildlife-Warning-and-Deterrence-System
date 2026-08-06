package com.wildlife.deterrence.viewmodel

import com.wildlife.deterrence.data.*
import junit.framework.TestCase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class StatisticsViewModelTest {

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
    fun TC_UI_STATS_FILTER_INIT() = runTest {
        val fakeCameraApi = FakeStatsCameraApi().apply {
            speciesList = listOf(
                SpeciesResponse("elephant", "Voi", "CRITICAL", false, "", 5, "", "2026-07-28T00:00:00Z"),
                SpeciesResponse("boar", "Heo rừng", "NORMAL", false, "", 2, "", "2026-07-28T00:00:00Z")
            )
            camerasList = listOf(
                CameraResponse("cam-01", "Trạm 1", LocationResponse(10.45, 106.12, "Cổng rừng"), "ONLINE", "", null)
            )
        }

        val viewModel = StatisticsViewModel(tokenManager, fakeCameraApi)
        viewModel.loadFilterOptions()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)

        // Verify species options dropdown
        assertEquals(3, state.speciesOptions.size) // Tất cả loài + 2 loài
        assertEquals("Tất cả loài", state.speciesOptions[0].label)
        assertEquals("elephant", state.speciesOptions[1].id)
        assertEquals("Voi", state.speciesOptions[1].label)

        // Verify camera options dropdown
        assertEquals(2, state.cameraOptions.size) // Tất cả camera + 1 camera
        assertEquals("Tất cả camera", state.cameraOptions[0].label)
        assertEquals("cam-01", state.cameraOptions[1].id)
        assertEquals("Trạm 1", state.cameraOptions[1].label)
    }

    @Test
    fun TC_UI_STATS_FILTER_CHANGE() = runTest {
        var summaryCalledCount = 0
        val fakeCameraApi = object : FakeStatsCameraApi() {
            override suspend fun getStatsSummary(
                token: String,
                startDate: String,
                endDate: String,
                cameraId: String?,
                speciesId: String?
            ): StatsSummaryResponse {
                summaryCalledCount++
                return StatsSummaryResponse(emptyList(), emptyList(), emptyList())
            }
        }

        val viewModel = StatisticsViewModel(tokenManager, fakeCameraApi)
        // Reset count vì init đã tự gọi
        summaryCalledCount = 0

        viewModel.onTimeRangeChanged("today")
        viewModel.onSpeciesFilterChanged("elephant")
        viewModel.onCameraFilterChanged("cam-01")

        // Mỗi lần đổi filter sẽ tự gọi refreshStatsSummary
        assertEquals(3, summaryCalledCount)
        assertEquals("today", viewModel.uiState.value.selectedTimeRange)
        assertEquals("elephant", viewModel.uiState.value.selectedSpeciesId)
        assertEquals("cam-01", viewModel.uiState.value.selectedCameraId)
    }

    @Test
    fun TC_UI_STATS_ALERTS_LOAD() = runTest {
        val fakeCameraApi = FakeStatsCameraApi().apply {
            alertsFeed = listOf(
                AlertResponse("alt-01", "ANIMAL_RARE", "Phát hiện Voi tại Cam 1", "CRITICAL", "cam-01", "Trạm 1", "evt-01", "2026-07-28T10:30:00Z", false),
                AlertResponse("alt-02", "INTRUDER", "Phát hiện Heo rừng tại Cam 2", "NORMAL", "cam-02", "Trạm 2", "evt-02", "2026-07-28T11:45:00Z", false)
            )
        }

        val viewModel = StatisticsViewModel(tokenManager, fakeCameraApi)
        viewModel.loadWeeklyDetections(page = 0)

        val state = viewModel.uiState.value
        assertEquals(2, state.weeklyDetections.size)

        val item1 = state.weeklyDetections[0]
        assertEquals("alt-01", item1.id)
        assertEquals("Voi", item1.speciesName)
        assertEquals("high", item1.dangerLevel)
        assertEquals("CAM-01", item1.cameraCode)
        assertEquals("17:30", item1.time)

        val item2 = state.weeklyDetections[1]
        assertEquals("alt-02", item2.id)
        assertEquals("Heo rừng", item2.speciesName)
        assertEquals("normal", item2.dangerLevel)
    }

    @Test
    fun TC_UI_STATS_CHART_PEAK() = runTest {
        val fakeCameraApi = FakeStatsCameraApi().apply {
            statsSummary = StatsSummaryResponse(
                trendData = listOf(
                    TrendPointResponse("2026-07-22", 2),
                    TrendPointResponse("2026-07-23", 10), // Peak value
                    TrendPointResponse("2026-07-24", 3)
                ),
                speciesBreakdown = emptyList(),
                heatmapData = emptyList()
            )
        }

        val viewModel = StatisticsViewModel(tokenManager, fakeCameraApi)
        viewModel.refreshStatsSummary()

        val state = viewModel.uiState.value
        assertEquals(3, state.dailyFrequency.size)
        assertEquals(5.0, state.averageFrequency) // (2 + 10 + 3) / 3 = 5.0

        // Kiểm tra Peak
        assertFalse(state.dailyFrequency[0].isPeak) // count = 2 < peakThreshold
        assertTrue(state.dailyFrequency[1].isPeak)  // count = 10 (Max và > threshold)
        assertFalse(state.dailyFrequency[2].isPeak) // count = 3 < peakThreshold
    }

    @Test
    fun TC_UI_STATS_LOAD_FAILURE() = runTest {
        val fakeCameraApi = FakeStatsCameraApi().apply {
            shouldSucceed = false
        }

        val viewModel = StatisticsViewModel(tokenManager, fakeCameraApi)
        // Gọi manual trigger để thấy ErrorState vì init được bọc try-catch
        viewModel.refreshStatsSummary()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Lỗi đồng bộ dữ liệu"))
    }
}

open class FakeStatsCameraApi : CameraApi {
    var shouldSucceed = true
    var speciesList: List<SpeciesResponse> = emptyList()
    var camerasList: List<CameraResponse> = emptyList()
    var alertsFeed: List<AlertResponse> = emptyList()
    var statsSummary = StatsSummaryResponse(emptyList(), emptyList(), emptyList())

    override suspend fun getSpecies(token: String): List<SpeciesResponse> {
        if (!shouldSucceed) throw Exception("Network error")
        return speciesList
    }

    override suspend fun getCameras(token: String): List<CameraResponse> {
        if (!shouldSucceed) throw Exception("Network error")
        return camerasList
    }

    override suspend fun getCameraDetail(token: String, cameraId: String): CameraDetailResponse {
        throw NotImplementedError()
    }

    override suspend fun getCameraHistory(token: String, cameraId: String, date: String?): List<DetectionHistoryItemResponse> {
        throw NotImplementedError()
    }

    override suspend fun renameCamera(token: String, cameraId: String, body: RenameCameraRequest): CameraResponse {
        throw NotImplementedError()
    }

    override suspend fun getAlertsFeed(token: String, page: Int, size: Int): List<AlertResponse> {
        if (!shouldSucceed) throw Exception("Network error")
        return alertsFeed
    }

    override suspend fun readAlert(token: String, alertId: String): retrofit2.Response<Unit> {
        throw NotImplementedError()
    }

    override suspend fun getStatsSummary(
        token: String,
        startDate: String,
        endDate: String,
        cameraId: String?,
        speciesId: String?
    ): StatsSummaryResponse {
        if (!shouldSucceed) throw Exception("Network error")
        return statsSummary
    }

    override suspend fun testDevice(
        token: String,
        cameraId: String,
        deviceKey: String,
        body: TestDeviceRequest
    ): retrofit2.Response<Unit> {
        return retrofit2.Response.success(Unit)
    }
}
