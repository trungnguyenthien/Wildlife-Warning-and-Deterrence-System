package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.AlertApi
import com.wildlife.deterrence.data.AlertResponse
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AllDetectionsUiState(
    val items: List<AlertFeedItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AllDetectionsViewModel(
    val timeRange: String,
    val speciesId: String?,
    val cameraId: String?,
    private val tokenManager: TokenManager,
    private val alertApi: AlertApi = NetworkClient.alertApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllDetectionsUiState())
    val uiState: StateFlow<AllDetectionsUiState> = _uiState.asStateFlow()

    init {
        loadFirstPage()
    }

    fun loadFirstPage() {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Vui lòng đăng nhập lại.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, currentPage = 1, hasMore = true, items = emptyList())

        viewModelScope.launch {
            try {
                // Hiện tại API alerts/feed trả về toàn bộ dữ liệu, ta tải phân trang size = 20
                val response = alertApi.getAlertsFeed("Bearer $token", page = 1, size = 20)
                val mappedItems = mapToAlertFeedItems(response)
                
                // Ở Client, chúng ta có thể áp dụng thêm bộ lọc offline nếu API Server chưa lọc
                // Hoặc giả định API server sẽ lọc trong tương lai. Ở đây ta lọc offline để bảo đảm chạy đúng mẫu
                val filteredItems = filterItemsOffline(mappedItems)

                _uiState.value = AllDetectionsUiState(
                    items = filteredItems,
                    currentPage = 1,
                    hasMore = response.size >= 20,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi khi tải lịch sử phát hiện: ${e.localizedMessage ?: "Không xác định"}"
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore) return

        val token = tokenManager.getToken() ?: return
        val nextPage = currentState.currentPage + 1

        _uiState.value = currentState.copy(isLoadingMore = true, error = null)

        viewModelScope.launch {
            try {
                val response = alertApi.getAlertsFeed("Bearer $token", page = nextPage, size = 20)
                val mappedItems = mapToAlertFeedItems(response)
                val filteredItems = filterItemsOffline(mappedItems)

                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items + filteredItems,
                    currentPage = nextPage,
                    hasMore = response.size >= 20,
                    isLoadingMore = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = "Lỗi tải thêm trang: ${e.localizedMessage ?: "Không xác định"}"
                )
            }
        }
    }

    private fun filterItemsOffline(items: List<AlertFeedItem>): List<AlertFeedItem> {
        // Lọc theo cameraId
        var result = if (!cameraId.isNullOrEmpty() && cameraId != "all") {
            items.filter { it.cameraCode.equals(cameraId, ignoreCase = true) }
        } else {
            items
        }

        // Lọc theo speciesId
        if (!speciesId.isNullOrEmpty() && speciesId != "all") {
            result = result.filter { it.speciesId.equals(speciesId, ignoreCase = true) }
        }

        // Lọc theo timeRange
        // (Để đơn giản và đồng bộ, timeRange có thể lọc theo khoảng thời gian thực ghi nhận)
        return result
    }

    private fun mapToAlertFeedItems(response: List<AlertResponse>): List<AlertFeedItem> {
        return response.map { alt ->
            val isHighDanger = alt.dangerLevel.uppercase() == "CRITICAL" ||
                    alt.dangerLevel.uppercase() == "HIGH" ||
                    alt.dangerLevel.uppercase() == "WARNING"
            
            val species = parseSpeciesFromTitle(alt.title)

            AlertFeedItem(
                id = alt.id,
                speciesName = species,
                dangerLevel = if (isHighDanger) "high" else "normal",
                cameraCode = alt.cameraId.uppercase(),
                locationName = alt.cameraName,
                time = formatTime(alt.createdAt),
                date = formatDate(alt.createdAt),
                speciesId = alt.speciesId
            )
        }
    }

    private fun parseSpeciesFromTitle(title: String): String {
        val prefix = "Phát hiện "
        val suffix = " tại"
        val startIndex = title.indexOf(prefix)
        return if (startIndex != -1) {
            val start = startIndex + prefix.length
            val end = title.indexOf(suffix, start)
            if (end != -1) {
                title.substring(start, end).trim()
            } else {
                title.substring(start).trim()
            }
        } else {
            title
        }
    }

    private fun formatTime(isoString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString)
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+7")
            }
            formatter.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatDate(isoString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString)
            val formatter = SimpleDateFormat("dd/MM", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+7")
            }
            formatter.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }
}
