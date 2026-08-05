package com.wildlife.deterrence.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildlife.deterrence.viewmodel.AlertFeedItem
import com.wildlife.deterrence.viewmodel.CameraActivitySummary
import com.wildlife.deterrence.viewmodel.DailyFrequencyPoint
import com.wildlife.deterrence.viewmodel.FilterOption
import com.wildlife.deterrence.viewmodel.HeatmapPoint
import com.wildlife.deterrence.viewmodel.StatisticsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsTab(
    viewModel: StatisticsViewModel,
    onBackClick: () -> Unit,
    onAlertClick: (String, String?) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val primaryGreen = Color(0xFF2C4C2C)

    // Trạng thái hiển thị menu 3 chấm trên Card Tần suất xuất hiện
    var showChartOptions by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thống kê",
                        color = primaryGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = primaryGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.dailyFrequency.isEmpty() && uiState.weeklyDetections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // b. Thanh bộ lọc (statistics_filter_bar)
                item {
                    StatisticsFilterBar(
                        uiState = uiState,
                        onTimeRangeSelected = { range ->
                            if (range == "custom") {
                                // Mở DatePickerDialog để chọn ngày
                                showDateRangePicker(context) { from, to ->
                                    viewModel.onCustomTimeRangeChanged(from, to)
                                    viewModel.onTimeRangeChanged("custom")
                                }
                            } else {
                                viewModel.onTimeRangeChanged(range)
                            }
                        },
                        onSpeciesSelected = { id -> viewModel.onSpeciesFilterChanged(id) },
                        onCameraSelected = { id -> viewModel.onCameraFilterChanged(id) }
                    )
                }

                // c. Section "Phát hiện trong tuần" (weekly_detections_section)
                item {
                    Text(
                        text = "Phát hiện trong tuần",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (uiState.weeklyDetections.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Không có phát hiện nào ghi nhận trong khoảng thời gian này.",
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column {
                                uiState.weeklyDetections.take(3).forEachIndexed { index, detection ->
                                    WeeklyDetectionItemInsideCard(
                                        item = detection,
                                        onClick = { onAlertClick(detection.id, detection.speciesName) }
                                    )
                                    if (index < uiState.weeklyDetections.take(3).size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                // Nút Xem tất cả ở đáy Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F5F5))
                                        .clickable { onViewAllClick() }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Xem tất cả (${uiState.totalDetectionsCount})",
                                        color = primaryGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // d. Section "Phân tích theo từng camera" (per_camera_analysis_section)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Card con "Tần suất xuất hiện"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Tần suất xuất hiện",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        val timeLabel = when (uiState.selectedTimeRange) {
                                            "today" -> "Hôm nay"
                                            "7d" -> "7 ngày qua"
                                            "30d" -> "30 ngày qua"
                                            "custom" -> "Tùy chỉnh"
                                            else -> "7 ngày qua"
                                        }
                                        Text(
                                            text = timeLabel,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box {
                                        IconButton(onClick = { showChartOptions = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Tùy chọn",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showChartOptions,
                                            onDismissRequest = { showChartOptions = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Xem chi tiết") },
                                                onClick = { showChartOptions = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Xuất dữ liệu") },
                                                onClick = { showChartOptions = false }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Biểu đồ cột Bar Chart tự vẽ
                                if (uiState.dailyFrequency.isNotEmpty()) {
                                    BarChart(
                                        points = uiState.dailyFrequency,
                                        average = uiState.averageFrequency,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Không đủ dữ liệu vẽ biểu đồ",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Danh sách CAMERA HOẠT ĐỘNG MẠNH
                        Text(
                            text = "CAMERA HOẠT ĐỘNG MẠNH",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGreen,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        if (uiState.topActiveCameras.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Không có camera nào ghi nhận hoạt động.",
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    uiState.topActiveCameras.forEachIndexed { index, cam ->
                                        ActiveCameraRow(cam = cam)
                                        if (index < uiState.topActiveCameras.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // e. Section "Bản đồ hoạt động" (activity_heatmap_map)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Bản đồ hoạt động",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Mô phỏng bản đồ nhiệt vẽ bằng Canvas
                        HeatmapCanvas(
                            cameras = viewModel.uiState.value.cameraOptions.filter { it.id.isNotEmpty() }.map {
                                // Tạo CameraResponse mock từ option để vẽ trạm tương đương
                                // Chúng ta dùng tọa độ giả lập cố định dựa trên camera ID để phân bố đẹp trên bản đồ
                                val lat = when (it.id) {
                                    "cam-001" -> 10.450
                                    "cam-002" -> 10.455
                                    "cam-003" -> 10.460
                                    else -> 10.452 + (it.id.hashCode() % 10) * 0.002
                                }
                                val lng = when (it.id) {
                                    "cam-001" -> 106.120
                                    "cam-002" -> 106.128
                                    "cam-003" -> 106.124
                                    else -> 106.122 + (it.id.hashCode() % 10) * 0.002
                                }
                                com.wildlife.deterrence.data.CameraResponse(
                                    id = it.id,
                                    name = it.label,
                                    location = com.wildlife.deterrence.data.LocationResponse(lat, lng, ""),
                                    status = "ONLINE",
                                    liveFeedUrl = "",
                                    snapshot = null
                                )
                            },
                            heatmapPoints = uiState.heatmapPoints.map {
                                // Nếu heatmapPoint từ API chưa có dữ liệu hợp lệ, dùng lat lng tương thích
                                if (it.lat == 0.0) {
                                    HeatmapPoint(10.450, 106.120, it.intensity)
                                } else it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsFilterBar(
    uiState: com.wildlife.deterrence.viewmodel.StatisticsUiState,
    onTimeRangeSelected: (String) -> Unit,
    onSpeciesSelected: (String) -> Unit,
    onCameraSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time range filter
        val timeLabel = when (uiState.selectedTimeRange) {
            "today" -> "Hôm nay"
            "7d" -> "7 ngày"
            "30d" -> "30 ngày"
            "custom" -> "Tùy chỉnh"
            else -> "7 ngày"
        }
        FilterPill(
            label = timeLabel,
            icon = Icons.Default.CalendarToday,
            options = listOf(
                FilterOption("today", "Hôm nay"),
                FilterOption("7d", "7 ngày"),
                FilterOption("30d", "30 ngày"),
                FilterOption("custom", "Tùy chỉnh")
            ),
            selectedId = uiState.selectedTimeRange,
            onOptionSelected = onTimeRangeSelected
        )

        // Species filter
        val selectedSpecies = uiState.speciesOptions.find { it.id == uiState.selectedSpeciesId }?.label ?: "Tất cả loài"
        FilterPill(
            label = selectedSpecies,
            icon = Icons.Default.Pets,
            options = uiState.speciesOptions,
            selectedId = uiState.selectedSpeciesId ?: "",
            onOptionSelected = onSpeciesSelected
        )

        // Camera filter
        val selectedCamera = uiState.cameraOptions.find { it.id == uiState.selectedCameraId }?.label ?: "Tất cả camera"
        FilterPill(
            label = selectedCamera,
            icon = Icons.Default.Videocam,
            options = uiState.cameraOptions,
            selectedId = uiState.selectedCameraId ?: "",
            onOptionSelected = onCameraSelected
        )
    }
}

@Composable
fun FilterPill(
    label: String,
    icon: ImageVector?,
    options: List<FilterOption>,
    selectedId: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Nút active khi selectedId khác "all"
    val isActive = selectedId != "all" && selectedId.isNotEmpty()
    val primaryGreen = Color(0xFF2C4C2C)
    
    val backgroundColor = if (isActive) primaryGreen else MaterialTheme.colorScheme.surface
    val contentColor = if (isActive) Color.White else Color.Black
    val borderColor = if (isActive) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Box {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(backgroundColor)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.Gray
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label, fontSize = 13.sp) },
                    onClick = {
                        onOptionSelected(opt.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun WeeklyDetectionItemInsideCard(
    item: AlertFeedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon tròn bên trái: ⚠️ (nền đỏ nhạt) nếu nguy hiểm cao, 🐾 (nền xám nhạt) nếu thường
        val isHigh = item.dangerLevel == "high"
        val iconBg = if (isHigh) Color(0xFFFEEBEE) else Color(0xFFF5F5F5)

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (isHigh) {
                Text("⚠️", fontSize = 16.sp)
            } else {
                Text("🐾", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.speciesName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.cameraCode} • ${item.locationName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Giờ và Ngày xếp chồng bên phải
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.time,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.date,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyDetectionItem(
    item: AlertFeedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon tròn bên trái: ⚠️ (nền đỏ nhạt) nếu là loài nguy hiểm cao, 🐾 (nền cam nhạt) nếu mức thường.
            val isHigh = item.dangerLevel == "high"
            val iconBg = if (isHigh) Color(0xFFFEEBEE) else Color(0xFFFFF3E0)
            val iconTint = if (isHigh) Color(0xFFC62828) else Color(0xFFEF6C00)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                if (isHigh) {
                    Text("⚠️", fontSize = 16.sp)
                } else {
                    Text("🐾", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.speciesName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.cameraCode} • ${item.locationName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Giờ và Ngày xếp chồng bên phải
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.time,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = item.date,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActiveCameraRow(
    cam: CameraActivitySummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Chấm tròn đỏ/xanh lá tùy hoạt động
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (cam.isHighActivity) Color(0xFFC62828) else Color(0xFF4CAF50))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = cam.cameraName,
                fontSize = 13.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }

        // Badge số lượng "X phát hiện"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${cam.detectionCount} phát hiện",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BarChart(
    points: List<DailyFrequencyPoint>,
    average: Double,
    modifier: Modifier = Modifier
) {
    val maxCount = points.maxOfOrNull { it.count } ?: 0
    val displayMax = Math.max(maxCount, 8)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 35.dp.toPx()
        val paddingRight = 45.dp.toPx()
        val paddingTop = 20.dp.toPx()
        val paddingBottom = 25.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Vẽ các cột
        if (points.isNotEmpty()) {
            val barWidth = (chartWidth / points.size) * 0.5f
            val stepX = chartWidth / points.size

            points.forEachIndexed { index, pt ->
                val barHeight = (pt.count.toFloat() / displayMax) * chartHeight
                val x = paddingLeft + (index * stepX) + (stepX - barWidth) / 2
                val y = paddingTop + chartHeight - barHeight

                val barColor = if (pt.isPeak) Color(0xFFC62828) else Color(0xFF4CAF50)

                // Vẽ cột bo góc nhẹ ở đỉnh
                drawRoundRect(
                    color = barColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Vẽ label dưới cột (T2–CN)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText(
                        pt.dayLabel,
                        x + barWidth / 2,
                        paddingTop + chartHeight + 16.dp.toPx(),
                        paint
                    )

                    // Vẽ số count nhỏ trên đỉnh cột nếu count > 0
                    if (pt.count > 0) {
                        paint.color = if (pt.isPeak) android.graphics.Color.RED else android.graphics.Color.DKGRAY
                        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        drawText(
                            pt.count.toString(),
                            x + barWidth / 2,
                            y - 6.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }

        // Vẽ đường trung bình (nét đứt)
        if (average > 0) {
            val avgY = paddingTop + chartHeight - ((average.toFloat() / displayMax) * chartHeight)
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            drawLine(
                color = Color.Gray.copy(alpha = 0.8f),
                start = androidx.compose.ui.geometry.Offset(paddingLeft, avgY),
                end = androidx.compose.ui.geometry.Offset(paddingLeft + chartWidth, avgY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect
            )

            // Vẽ label "TB: X.X" ở cuối đường nét đứt
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                drawText(
                    String.format("TB: %.1f", average),
                    paddingLeft + chartWidth + 5.dp.toPx(),
                    avgY + 4.dp.toPx(),
                    paint
                )
            }
        }
    }
}

@Composable
fun HeatmapCanvas(
    cameras: List<com.wildlife.deterrence.data.CameraResponse>,
    heatmapPoints: List<HeatmapPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F5E9)) // Nền rừng xanh nhạt
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Vẽ sông ngòi uốn lượn cách điệu
            val riverPath = Path().apply {
                moveTo(0f, height * 0.3f)
                cubicTo(width * 0.3f, height * 0.2f, width * 0.6f, height * 0.8f, width, height * 0.7f)
            }
            drawPath(
                path = riverPath,
                color = Color(0xFFBBDEFB),
                style = Stroke(width = 8.dp.toPx())
            )

            if (cameras.isNotEmpty()) {
                val lats = cameras.map { it.location.lat }
                val lngs = cameras.map { it.location.lng }
                val minLat = lats.minOrNull() ?: 0.0
                val maxLat = lats.maxOrNull() ?: 1.0
                val minLng = lngs.minOrNull() ?: 0.0
                val maxLng = lngs.maxOrNull() ?: 1.0

                val latRange = if (maxLat - minLat == 0.0) 1.0 else maxLat - minLat
                val lngRange = if (maxLng - minLng == 0.0) 1.0 else maxLng - minLng

                // 1. Vẽ các điểm Heatmap tỏa ra
                heatmapPoints.forEach { pt ->
                    val normX = 0.2f + 0.6f * ((pt.lng - minLng) / lngRange).toFloat()
                    val normY = 0.2f + 0.6f * (1f - ((pt.lat - minLat) / latRange).toFloat())

                    val canvasX = normX * width
                    val canvasY = normY * height

                    val maxIntensity = heatmapPoints.maxOfOrNull { it.intensity } ?: 1.0
                    val intensityRatio = (pt.intensity / maxIntensity).toFloat()
                    val radius = (35.dp.toPx() + intensityRatio * 45.dp.toPx())

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE53935).copy(alpha = 0.55f * intensityRatio),
                                Color(0xFFE53935).copy(alpha = 0.2f * intensityRatio),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(canvasX, canvasY),
                            radius = radius
                        ),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(canvasX, canvasY)
                    )
                }

                // 2. Vẽ các trạm camera
                cameras.forEach { cam ->
                    val normX = 0.2f + 0.6f * ((cam.location.lng - minLng) / lngRange).toFloat()
                    val normY = 0.2f + 0.6f * (1f - ((cam.location.lat - minLat) / latRange).toFloat())

                    val canvasX = normX * width
                    val canvasY = normY * height

                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(canvasX, canvasY)
                    )
                    drawCircle(
                        color = Color(0xFF2C4C2C),
                        radius = 5.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(canvasX, canvasY)
                    )

                    // Nhãn tên camera
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 8.sp.toPx()
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            cam.name.take(7),
                            canvasX,
                            canvasY - 9.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }

        // Legend
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mật độ cao", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// Dialog picker ngày bắt đầu và kết thúc dùng API mặc định
private fun showDateRangePicker(
    context: android.content.Context,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(context, { _, startYear, startMonth, startDay ->
        val startCal = Calendar.getInstance().apply {
            set(startYear, startMonth, startDay, 0, 0, 0)
        }
        val fromTimestamp = startCal.timeInMillis

        DatePickerDialog(context, { _, endYear, endMonth, endDay ->
            val endCal = Calendar.getInstance().apply {
                set(endYear, endMonth, endDay, 23, 59, 59)
            }
            val toTimestamp = endCal.timeInMillis
            onDateRangeSelected(fromTimestamp, toTimestamp)
        }, year, month, day).apply {
            setTitle("Chọn ngày kết thúc")
            show()
        }
    }, year, month, day).apply {
        setTitle("Chọn ngày bắt đầu")
        show()
    }
}
