package com.wildlife.deterrence.viewmodel

import com.wildlife.deterrence.data.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
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

class AllDetectionsViewModelTest {

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
    fun TC_UI_ALLDETECTIONS_LOAD_FULL() = runTest {
        val fakeAlertApi = object : FakeAlertApi() {
            override suspend fun getAlertsFeed(token: String, page: Int, size: Int): List<AlertResponse> {
                return listOf(
                    AlertResponse(
                        id = "alt-1",
                        type = "ANIMAL_RARE",
                        title = "Phát hiện Voi tại Trạm 1",
                        dangerLevel = "CRITICAL",
                        cameraId = "cam-1",
                        cameraName = "Trạm Bờ Sông Đăk Bla",
                        eventId = "evt-1",
                        createdAt = "2026-07-30T10:00:00Z",
                        isRead = false
                    )
                )
            }
        }

        val viewModel = AllDetectionsViewModel("7d", null, null, tokenManager, fakeAlertApi)
        viewModel.loadFirstPage()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.items.size)
        assertEquals("alt-1", state.items[0].id)
        assertEquals("Voi", state.items[0].speciesName)
        assertEquals("high", state.items[0].dangerLevel)
        assertEquals("CAM-1", state.items[0].cameraCode)
    }

    @Test
    fun TC_UI_ALLDETECTIONS_PAGINATION() = runTest {
        val fakeAlertApi = object : FakeAlertApi() {
            private var callCount = 0

            override suspend fun getAlertsFeed(token: String, page: Int, size: Int): List<AlertResponse> {
                callCount++
                return if (page == 1) {
                    List(20) { index ->
                        AlertResponse(
                            id = "alt-$index",
                            type = "ANIMAL_RARE",
                            title = "Phát hiện Voi tại Trạm 1",
                            dangerLevel = "HIGH",
                            cameraId = "cam-1",
                            cameraName = "Trạm 1",
                            eventId = "evt-$index",
                            createdAt = "2026-07-30T10:00:00Z",
                            isRead = false
                        )
                    }
                } else {
                    listOf(
                        AlertResponse(
                            id = "alt-next-page",
                            type = "ANIMAL_RARE",
                            title = "Phát hiện Voi tại Trạm 1",
                            dangerLevel = "HIGH",
                            cameraId = "cam-1",
                            cameraName = "Trạm 1",
                            eventId = "evt-next",
                            createdAt = "2026-07-30T11:00:00Z",
                            isRead = false
                        )
                    )
                }
            }
        }

        val viewModel = AllDetectionsViewModel("7d", null, null, tokenManager, fakeAlertApi)
        viewModel.loadFirstPage()

        var state = viewModel.uiState.value
        assertEquals(20, state.items.size)
        assertTrue(state.hasMore)
        assertEquals(1, state.currentPage)

        viewModel.loadNextPage()

        state = viewModel.uiState.value
        assertFalse(state.isLoadingMore)
        assertEquals(21, state.items.size)
        assertEquals("alt-next-page", state.items[20].id)
        assertFalse(state.hasMore) // Do page 2 chỉ trả về 1 phần tử (ít hơn size = 20)
        assertEquals(2, state.currentPage)
    }
}
