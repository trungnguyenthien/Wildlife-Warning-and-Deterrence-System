package com.wildlife.deterrence.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wildlife.deterrence.viewmodel.*
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDetailScreen(
    viewModel: CameraDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

    val primaryGreen = Color(0xFF2C4C2C)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val headerText = remember(uiState.name, uiState.cameraId) {
                            buildAnnotatedString {
                                if (uiState.cameraId.isNotEmpty()) {
                                    withStyle(
                                        style = SpanStyle(
                                            color = primaryGreen.copy(alpha = 0.6f),
                                            fontSize = 16.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    ) {
                                        append("[${uiState.cameraId.uppercase()}] ")
                                    }
                                }
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = primaryGreen
                                    )
                                ) {
                                    append(uiState.name.ifEmpty { "Đang tải..." })
                                }
                            }
                        }
                        Text(
                            text = headerText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                        // Trạng thái Trực tuyến/Ngoại tuyến ngay dưới tên
                        Text(
                            text = if (uiState.isOnline) "🟢 Trực tuyến" else "⚪ Ngoại tuyến",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                actions = {
                    IconButton(onClick = { showEditNameDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Đổi tên",
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
        if (uiState.isLoading && uiState.name.isEmpty()) {
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
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // b. Khung ảnh snapshot lớn
                item {
                    val snapshot = uiState.liveSnapshot
                    if (snapshot != null) {
                        LiveSnapshotContainer(
                            snapshot = snapshot,
                            isOnline = uiState.isOnline,
                            hasAnimal = uiState.currentAnalysis != null
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Không có ảnh snapshot nào được ghi nhận",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // c. Card "PHÂN TÍCH HIỆN TẠI"
                item {
                    CurrentAnalysisCard(analysis = uiState.currentAnalysis)
                }

                // d. Section LỊCH SỬ GHI NHẬN
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LỊCH SỬ GHI NHẬN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Button "Hôm nay"
                            val isTodayActive = uiState.selectedDateFilter == "today"
                            val todayBg = if (isTodayActive) primaryGreen else MaterialTheme.colorScheme.surfaceVariant
                            val todayText = if (isTodayActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(todayBg)
                                    .clickable { viewModel.setDateFilter("today") }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Hôm nay",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = todayText
                                )
                            }

                            // Button Chọn ngày (icon lịch)
                            val isCustomActive = uiState.selectedDateFilter == "custom"
                            val calendarBg = if (isCustomActive) primaryGreen else MaterialTheme.colorScheme.surfaceVariant
                            val calendarTint = if (isCustomActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(calendarBg)
                                    .clickable { showDatePicker = true }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Chọn ngày",
                                    tint = calendarTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Danh sách các HistoryItemCard
                if (uiState.historyItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không tìm thấy lịch sử ghi nhận trong ngày này",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(uiState.historyItems, key = { it.id }) { item ->
                        HistoryItemCard(item = item)
                    }
                }
            }
        }
    }

    // Dialog cập nhật tên Camera
    if (showEditNameDialog) {
        EditCameraNameDialog(
            currentName = uiState.name,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                viewModel.renameCamera(
                    newName = newName,
                    onSuccess = { showEditNameDialog = false },
                    onError = { /* Xử lý hiển thị Toast hoặc error ngoài */ }
                )
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let {
                            viewModel.setDateFilter("custom", it)
                        }
                    }
                ) {
                    Text("OK", color = primaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = primaryGreen,
                    todayContentColor = primaryGreen
                )
            )
        }
    }
}

@Composable
fun LiveSnapshotContainer(
    snapshot: SnapshotUiModel,
    isOnline: Boolean,
    hasAnimal: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        // Ảnh Snapshot
        AsyncImage(
            model = snapshot.url,
            contentDescription = "Live Snapshot",
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isOnline) Modifier.alpha(0.4f) else Modifier),
            contentScale = ContentScale.Crop
        )

        // Overlay: REC + Icon Camera (Góc trên-trái)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = recAlpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REC",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }

        // Overlay: Badge PHÁT HIỆN màu đỏ (Góc trên-phải)
        if (hasAnimal) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color(0xFFC62828), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PHÁT HIỆN",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Overlay: Thời gian chụp + Cách đây X phút (Góc dưới-trái)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 40.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = snapshot.capturedAt,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = snapshot.timeAgoText,
                color = Color.LightGray,
                fontSize = 9.sp
            )
        }

        // Overlay: Thanh metadata kỹ thuật dưới đáy
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CAM ID: ${snapshot.camId}",
                color = Color.White,
                fontSize = 9.sp
            )
            Text(
                text = "MOTION: ${if (snapshot.motionDetected) "ACTIVE" else "IDLE"}",
                color = if (snapshot.motionDetected) Color.Green else Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "GPS: ${snapshot.gpsText}",
                color = Color.White,
                fontSize = 9.sp
            )
            Text(
                text = "SD: ${snapshot.sdCardSpace}",
                color = Color.White,
                fontSize = 9.sp
            )
            Text(
                text = "ZOOM: ${snapshot.zoomText}",
                color = Color.White,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun CurrentAnalysisCard(
    analysis: AnalysisUiModel?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "PHÂN TÍCH HIỆN TẠI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (analysis != null) {
                        Text(
                            text = "${analysis.speciesName} (${analysis.speciesNameEn})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Không có động vật",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                if (analysis != null) {
                    val badgeColor = when (analysis.dangerLevel.lowercase()) {
                        "high" -> Color(0xFFFFCDD2) // đỏ nhạt
                        "medium" -> Color(0xFFFFF9C4) // vàng nhạt
                        else -> Color(0xFFC8E6C9) // xanh nhạt
                    }
                    val badgeText = when (analysis.dangerLevel.lowercase()) {
                        "high" -> "Mức độ: Cao"
                        "medium" -> "Mức độ: T.Bình"
                        else -> "Mức độ: Thấp"
                    }
                    val textColor = when (analysis.dangerLevel.lowercase()) {
                        "high" -> Color(0xFFC62828)
                        "medium" -> Color(0xFFF57F17)
                        else -> Color(0xFF2E7D32)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cột 1: Số lượng ước tính
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = Color(0xFF2C4C2C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Số lượng ước tính",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = analysis?.estimatedCount?.toString() ?: "0",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Cột 2: Độ tin cậy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Độ tin cậy",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = analysis?.let { "${it.confidencePercent}%" } ?: "--",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: DetectionHistoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail vuông bo góc trái
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (item.thumbnailUrl != null) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = "History Snapshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nội dung chính ở giữa
            Column(modifier = Modifier.weight(1f)) {
                val isKnown = item.speciesName != null
                
                Text(
                    text = item.speciesName ?: "Phát hiện chuyển động",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isKnown) Icons.Default.Pets else Icons.Default.Help,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isKnown) "Khoảng ${item.estimatedCount} cá thể" else "Không xác định loài",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isKnown && item.confidencePercent != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Tin cậy ${item.confidencePercent}%",
                            color = Color(0xFF2E7D32),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Thời gian và ngày bên phải xếp chồng
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.recordedTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.recordedDateLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun EditCameraNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val primaryGreen = Color(0xFF2C4C2C)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cập nhật tên Camera", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                Text("Tên Camera", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (it.isNotBlank()) errorText = null
                    },
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryGreen,
                        focusedLabelColor = primaryGreen
                    )
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.trim().isBlank()) {
                        errorText = "Tên camera không được để trống"
                    } else {
                        onConfirm(text.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
            ) {
                Text("Cập nhật", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = MaterialTheme.colorScheme.outline)
            }
        }
    )
}
