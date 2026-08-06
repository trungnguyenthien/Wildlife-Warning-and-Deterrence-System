package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.CameraApi
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class StatisticsUiState(
    val speciesOptions: List<FilterOption> = emptyList(),
    val cameraOptions: List<FilterOption> = emptyList(),
    val selectedTimeRange: String = "7d", // "today" | "7d" | "30d" | "custom"
    val customFrom: Long? = null,
    val customTo: Long? = null,
    val selectedSpeciesId: String? = null, // null = "Tất cả loài"
    val selectedCameraId: String? = null,  // null = "Tất cả camera"

    val weeklyDetections: List<AlertFeedItem> = emptyList(),
    val totalDetectionsCount: Int = 0,

    val dailyFrequency: List<DailyFrequencyPoint> = emptyList(),
    val averageFrequency: Double = 0.0,
    val topActiveCameras: List<CameraActivitySummary> = emptyList(),
    val heatmapPoints: List<HeatmapPoint> = emptyList(),

    val isLoading: Boolean = false,
    val error: String? = null
)

data class FilterOption(val id: String, val label: String)

data class AlertFeedItem(
    val id: String,
    val speciesName: String,
    val dangerLevel: String, // "high" | "normal"
    val cameraCode: String,
    val locationName: String,
    val time: String,   // HH:mm
    val date: String,     // dd/MM
    val speciesId: String? = null
)

data class DailyFrequencyPoint(val dayLabel: String, val count: Int, val isPeak: Boolean)

data class CameraActivitySummary(val cameraId: String, val cameraName: String, val detectionCount: Int, val isHighActivity: Boolean)

data class HeatmapPoint(val lat: Double, val lng: Double, val intensity: Double)

class StatisticsViewModel(
    private val tokenManager: TokenManager,
    private val cameraApi: CameraApi = NetworkClient.cameraApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20

    // Cache camera list details to resolve names from locations
    private var rawCameras: List<com.wildlife.deterrence.data.CameraResponse> = emptyList()

    init {
        loadFilterOptions()
    }

    fun loadFilterOptions() {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Không có phiên đăng nhập. Vui lòng đăng nhập lại.")
            return
        }
        val authHeader = "Bearer $token"

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Sử dụng coroutineScope để exception ném ra được bọc và bắt gọn bởi try-catch
                val (speciesList, camerasList) = kotlinx.coroutines.coroutineScope {
                    val speciesDeferred = async { cameraApi.getSpecies(authHeader) }
                    val camerasDeferred = async { cameraApi.getCameras(authHeader) }
                    Pair(speciesDeferred.await(), camerasDeferred.await())
                }
                rawCameras = camerasList

                val speciesOpts = listOf(FilterOption("", "Tất cả loài")) + speciesList.map {
                    FilterOption(it.id, it.displayName)
                }

                val cameraOpts = listOf(FilterOption("", "Tất cả camera")) + camerasList.map {
                    FilterOption(it.id, it.name)
                }

                _uiState.value = _uiState.value.copy(
                    speciesOptions = speciesOpts,
                    cameraOptions = cameraOpts
                )

                // Sau khi load xong filters, thực hiện load dữ liệu thống kê & cảnh báo
                refreshStatsSummary()
                loadWeeklyDetections(page = 0)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi khởi tạo bộ lọc: ${e.message}"
                )
            }
        }
    }

    fun onTimeRangeChanged(range: String) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
        refreshStatsSummary()
    }

    fun onCustomTimeRangeChanged(from: Long?, to: Long?) {
        _uiState.value = _uiState.value.copy(customFrom = from, customTo = to)
        if (from != null && to != null) {
            refreshStatsSummary()
        }
    }

    fun onSpeciesFilterChanged(id: String?) {
        _uiState.value = _uiState.value.copy(selectedSpeciesId = if (id.isNullOrEmpty()) null else id)
        refreshStatsSummary()
    }

    fun onCameraFilterChanged(id: String?) {
        _uiState.value = _uiState.value.copy(selectedCameraId = if (id.isNullOrEmpty()) null else id)
        refreshStatsSummary()
    }

    fun refreshStatsSummary() {
        val token = tokenManager.getToken() ?: return
        val authHeader = "Bearer $token"

        val dates = calculateDateRange(
            range = _uiState.value.selectedTimeRange,
            customFrom = _uiState.value.customFrom,
            customTo = _uiState.value.customTo
        )

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val summary = cameraApi.getStatsSummary(
                    token = authHeader,
                    startDate = dates.first,
                    endDate = dates.second,
                    cameraId = _uiState.value.selectedCameraId,
                    speciesId = _uiState.value.selectedSpeciesId
                )

                // 1. Phân tích dailyFrequency
                val dailyPoints = summary.trendData.map { point ->
                    val dayLabel = formatDayLabel(point.date)
                    DailyFrequencyPoint(
                        dayLabel = dayLabel,
                        count = point.count,
                        isPeak = false // Sẽ được tính bên dưới
                    )
                }

                // Tính toán isPeak dựa trên logic: max và trung bình + độ lệch chuẩn
                val avgFreq = if (dailyPoints.isNotEmpty()) dailyPoints.map { it.count }.average() else 0.0
                val maxVal = if (dailyPoints.isNotEmpty()) dailyPoints.maxOf { it.count } else 0
                val variance = if (dailyPoints.isNotEmpty()) {
                    dailyPoints.map { Math.pow(it.count - avgFreq, 2.0) }.average()
                } else 0.0
                val stdDev = Math.sqrt(variance)
                val peakThreshold = avgFreq + stdDev

                val mappedDailyPoints = dailyPoints.map { pt ->
                    val isPeakVal = (maxVal > 0 && pt.count == maxVal) || (stdDev > 0 && pt.count > peakThreshold)
                    pt.copy(isPeak = isPeakVal)
                }

                // 2. Tính toán topActiveCameras dựa trên heatmapData khớp với rawCameras
                val cameraSummaryList = summary.heatmapData.map { heat ->
                    val matchedCam = rawCameras.find { cam ->
                        Math.abs(cam.location.lat - heat.lat) < 0.001 &&
                                Math.abs(cam.location.lng - heat.lng) < 0.001
                    }
                    val camId = matchedCam?.id ?: "CAM-${heat.lat}_${heat.lng}"
                    val camName = matchedCam?.name ?: "Trạm ${heat.lat}, ${heat.lng}"
                    CameraActivitySummary(
                        cameraId = camId,
                        cameraName = camName,
                        detectionCount = heat.intensity.toInt(),
                        isHighActivity = false
                    )
                }.sortedByDescending { it.detectionCount }

                // Đánh dấu camera hoạt động mạnh nhất
                val finalCameraSummaryList = cameraSummaryList.mapIndexed { idx, item ->
                    item.copy(isHighActivity = idx == 0 && item.detectionCount > 0)
                }

                // 3. Mapped HeatmapPoints
                val finalHeatmapPoints = summary.heatmapData.map {
                    HeatmapPoint(lat = it.lat, lng = it.lng, intensity = it.intensity)
                }

                val totalCount = summary.trendData.sumOf { it.count }

                _uiState.value = _uiState.value.copy(
                    dailyFrequency = mappedDailyPoints,
                    averageFrequency = avgFreq,
                    topActiveCameras = finalCameraSummaryList,
                    heatmapPoints = finalHeatmapPoints,
                    totalDetectionsCount = totalCount,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi đồng bộ dữ liệu: ${e.message}"
                )
            }
        }
    }

    fun loadWeeklyDetections(page: Int = 0) {
        val token = tokenManager.getToken() ?: return
        val authHeader = "Bearer $token"

        currentPage = page
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Retrofit API sử dụng 1-indexed page, Client dùng 0-indexed page
                val apiPage = currentPage + 1
                val response = cameraApi.getAlertsFeed(authHeader, page = apiPage, size = pageSize)

                val alertItems = response.map { alt ->
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

                _uiState.value = _uiState.value.copy(
                    weeklyDetections = if (page == 0) alertItems else _uiState.value.weeklyDetections + alertItems,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi tải bản tin: ${e.message}"
                )
            }
        }
    }

    fun loadMoreDetections() {
        val nextPage = currentPage + 1
        loadWeeklyDetections(page = nextPage)
    }

    // Helper to calculate start & end dates in "YYYY-MM-DD"
    private fun calculateDateRange(range: String, customFrom: Long?, customTo: Long?): Pair<String, String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val end = sdf.format(cal.time)

        return when (range) {
            "today" -> Pair(end, end)
            "7d" -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                val start = sdf.format(cal.time)
                Pair(start, end)
            }
            "30d" -> {
                cal.add(Calendar.DAY_OF_YEAR, -29)
                val start = sdf.format(cal.time)
                Pair(start, end)
            }
            "custom" -> {
                val start = customFrom?.let { sdf.format(Date(it)) } ?: end
                val customEnd = customTo?.let { sdf.format(Date(it)) } ?: end
                Pair(start, customEnd)
            }
            else -> Pair(end, end)
        }
    }

    // Format YYYY-MM-DD date string to Vietnamese Day of Week
    private fun formatDayLabel(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(dateStr)
            val cal = Calendar.getInstance().apply { time = date ?: Date() }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                Calendar.SUNDAY -> "CN"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    // Parse species name from title, e.g. "Phát hiện Voi tại..." -> "Voi"
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
