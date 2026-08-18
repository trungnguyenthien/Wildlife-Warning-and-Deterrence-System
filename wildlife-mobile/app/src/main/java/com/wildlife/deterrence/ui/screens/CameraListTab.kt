package com.wildlife.deterrence.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.runtime.remember
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import com.wildlife.deterrence.viewmodel.CameraListUiState
import com.wildlife.deterrence.viewmodel.CameraListViewModel
import com.wildlife.deterrence.viewmodel.StationUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraListTab(
    viewModel: CameraListViewModel,
    onCameraClick: (String) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Auto-polling cập nhật danh sách camera mỗi 5 giây khi Tab ở Foreground
    DisposableEffect(Unit) {
        viewModel.loadCameras()
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. emergency_banner_container (Hiển thị sticky trên cùng khi có loài nguy hiểm chưa xem)
            val activeEmergencyStation = uiState.stations.firstOrNull { it.hasUnreadAlert }
            if (activeEmergencyStation != null) {
                EmergencyBanner(
                    station = activeEmergencyStation,
                    onClick = { onCameraClick(activeEmergencyStation.id) }
                )
            }

            // 2. PullToRefresh & Grid List
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshCameras() },
                state = pullToRefreshState,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isLoading && uiState.stations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error != null && uiState.stations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error ?: "Lỗi tải dữ liệu",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.stations, key = { it.id }) { station ->
                            StationCard(
                                station = station,
                                onClick = { onCameraClick(station.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyBanner(
    station: StationUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_bg")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_alpha"
    )

    val baseColor = Color(0xFFC62828) // Màu đỏ cảnh báo nguy hiểm

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(baseColor.copy(alpha = alpha))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Cảnh báo khẩn cấp",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CẢNH BÁO NGUY HIỂM XUẤT HIỆN!",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Phát hiện ${station.alertSpecies ?: "động vật hoang dã"} tại ${station.name}",
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StationCard(
    station: StationUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "warning_blink")
    val animAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_alpha"
    )

    // Tạo màu nền nhấp nháy nếu có cảnh báo chưa xem
    val cardBgColor = if (station.hasUnreadAlert) {
        Color(0xFFFFCDD2).copy(alpha = animAlpha)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderStrokeColor = if (station.hasUnreadAlert) Color.Red else Color.Transparent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = if (station.hasUnreadAlert) 1.5.dp else 0.dp,
                color = borderStrokeColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Box chứa Thumbnail Image và overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Gray.copy(alpha = 0.1f))
            ) {
                if (station.thumbnailUrl != null) {
                    AsyncImage(
                        model = station.thumbnailUrl,
                        contentDescription = "Snapshot",
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (!station.isOnline) Modifier.alpha(0.4f) else Modifier
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không có ảnh",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // offline_placeholder_state: hiển thị icon camera gạch chéo ở giữa khi offline
                if (!station.isOnline) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "Ngoại tuyến",
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                                .padding(8.dp)
                        )
                    }
                }

                // warning_badge_overlay (nhấp nháy đỏ trên ảnh khi có loài nguy hiểm chưa xem)
                if (station.hasUnreadAlert) {
                    val alertText = buildString {
                        append("⚠ PHÁT HIỆN ")
                        append(station.alertSpecies?.uppercase() ?: "ĐỘNG VẬT")
                        station.alertConfidence?.let {
                            append(" · $it%")
                        } ?: append(" · 90%")
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color.Red, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alertText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // station_status_badge (🟢 Online / 🔴 Offline) đè lên góc trên-phải ảnh thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = if (station.isOnline) Color(0xFFE8F5E9).copy(alpha = 0.9f) else Color(0xFFFFEBEE).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (station.isOnline) "🟢 Online" else "🔴 Offline",
                        color = if (station.isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Phần nội dung chữ bên dưới card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Tên trạm
                    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                    val onSurface = MaterialTheme.colorScheme.onSurface
                    val cameraDisplayName = remember(station.name, station.id, onSurfaceVariant, onSurface) {
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            ) {
                                append("[${station.id.uppercase()}] ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = onSurface
                                )
                            ) {
                                append(station.name)
                            }
                        }
                    }
                    Text(
                        text = cameraDisplayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // station_timestamp_text (🕐 Thời gian bên dưới tên trạm)
                    val timeText = if (station.isOnline) {
                        if (station.timestampText.isNotEmpty()) "🕐 ${station.timestampText}" else "🕐 Không có dữ liệu"
                    } else {
                        station.offlineDurationText ?: "Mất kết nối"
                    }
                    Text(
                        text = timeText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Địa chỉ (màu xám nhạt, cỡ nhỏ hơn tên trạm)
                    Text(
                        text = station.address,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // station_action_button (Nút hành động tròn ở góc phải card, chỉ hiển thị khi isOnline = true)
                if (station.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .clickable { /* Hành động báo động */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Hành động báo động",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
