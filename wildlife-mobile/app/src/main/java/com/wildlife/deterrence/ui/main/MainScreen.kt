package com.wildlife.deterrence.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wildlife.deterrence.data.TokenManager
import com.wildlife.deterrence.data.NetworkClient
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.wildlife.deterrence.ui.screens.CameraListTab
import com.wildlife.deterrence.ui.screens.StatisticsTab
import com.wildlife.deterrence.viewmodel.CameraListViewModel
import com.wildlife.deterrence.viewmodel.StatisticsViewModel
import com.wildlife.deterrence.ui.components.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wildlife.deterrence.data.ThemeSettings
import com.wildlife.deterrence.viewmodel.MainViewModel
import java.util.UUID

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    onNavigateToCameraDetail: (String) -> Unit,
    onNavigateToAlertDetail: (String, String?) -> Unit,
    onNavigateToAllDetections: (String, String?, String?) -> Unit,
    onNavigateToSmsSetup: () -> Unit,
    onNavigateToBehaviorSpeciesList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val context = LocalContext.current

    // Request POST_NOTIFICATIONS permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("NotifPermission", "Granted: $isGranted")
        retrieveAndRegisterToken(context, viewModel)
    }

    LaunchedEffect(Unit) {
        viewModel.fetchUnreadCount()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                retrieveAndRegisterToken(context, viewModel)
            }
        } else {
            retrieveAndRegisterToken(context, viewModel)
        }
    }

    val unreadCount by com.wildlife.deterrence.NotificationState.unreadCount.collectAsState()

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) {
            viewModel.fetchUserProfile()
        }
        if (selectedTab == 2) {
            com.wildlife.deterrence.NotificationState.clear()
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple("Camera", Icons.Default.Videocam, 0),
                    Triple("Thống kê", Icons.Default.BarChart, 1),
                    Triple("Cảnh báo", Icons.Default.Notifications, 2),
                    Triple("Cài đặt", Icons.Default.Settings, 3)
                )

                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        icon = {
                            if (index == 2 && unreadCount > 0) {
                                BadgedBox(
                                    badge = { Badge { Text(unreadCount.toString()) } }
                                ) {
                                    Icon(icon, contentDescription = label)
                                }
                            } else {
                                Icon(icon, contentDescription = label)
                            }
                        },
                        label = { Text(label) },
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> {
                    val cameraListViewModel: CameraListViewModel = viewModel {
                        CameraListViewModel(tokenManager)
                    }
                    CameraListTab(
                        viewModel = cameraListViewModel,
                        onCameraClick = { cameraId ->
                            onNavigateToCameraDetail(cameraId)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    val statisticsViewModel: StatisticsViewModel = viewModel {
                        StatisticsViewModel(tokenManager)
                    }
                    StatisticsTab(
                        viewModel = statisticsViewModel,
                        onBackClick = { viewModel.selectTab(0) },
                        onAlertClick = onNavigateToAlertDetail,
                        onViewAllClick = {
                            val uiState = statisticsViewModel.uiState.value
                            onNavigateToAllDetections(
                                uiState.selectedTimeRange,
                                uiState.selectedSpeciesId,
                                uiState.selectedCameraId
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BlankTabScreen(title = "Nhật Ký Cảnh Báo", subtitle = "Thông tin phát hiện động vật thời gian thực")
                }
                3 -> SettingsTabContent(
                    viewModel = viewModel,
                    onLogout = onLogout,
                    onNavigateToSmsSetup = onNavigateToSmsSetup,
                    onNavigateToBehaviorSpeciesList = onNavigateToBehaviorSpeciesList
                )
                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BlankTabScreen(title = "Không tìm thấy", subtitle = "Tab không hợp lệ")
                }
            }
        }
    }
}

@Composable
private fun BlankTabScreen(title: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Tính năng đang được phát triển...",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun SettingsTabContent(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    onNavigateToSmsSetup: () -> Unit,
    onNavigateToBehaviorSpeciesList: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoadingProfile by viewModel.isLoadingProfile.collectAsState()
    val profileError by viewModel.profileError.collectAsState()
    val themeMode by ThemeSettings.themeMode.collectAsState()
    val context = LocalContext.current

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showPermissionBanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showPermissionBanner = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val titleColor = if (isDark) Color(0xFFF4D03F) else Color(0xFF2C4C2C)
        Text(
            text = "Cài đặt",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        if (showPermissionBanner) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quyền thông báo chưa được cấp! Bạn có thể bỏ lỡ các cảnh báo nguy khẩn về thú rừng.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Bật trong Cài đặt", fontSize = 12.sp)
                    }
                }
            }
        }

        // Card 1: User Profile Display (Compact horizontal Row layout with adaptive colors)
        val profileCardBg = if (isDark) Color(0xFF6E5906) else Color(0xFF2C4C2C)
        val avatarBg = if (isDark) Color(0xFFFEF9E7) else Color(0xFFEFF7EF)
        val avatarTint = if (isDark) Color(0xFF6E5906) else Color(0xFF2C4C2C)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = profileCardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = avatarTint,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoadingProfile) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else if (profileError != null) {
                        Text(
                            text = profileError ?: "Lỗi tải thông tin",
                            color = Color(0xFFFFCDD2),
                            fontSize = 14.sp
                        )
                    } else if (userProfile != null) {
                        Text(
                            text = userProfile?.fullName ?: "",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${userProfile?.username} | Vai trò: ${userProfile?.role}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.material3.Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ID: ${userProfile?.id}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Không có dữ liệu profile.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Card 2: Settings Group Card (Contains theme settings and navigation buttons)
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppSectionTitleText(
                        text = "Giao diện ứng dụng",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val themeLabels = listOf("Hệ thống", "Sáng", "Tối")
                    val selectedLabel = when (themeMode) {
                        "light" -> "Sáng"
                        "dark" -> "Tối"
                        else -> "Hệ thống"
                    }

                    H1ChoiceButtonGroup(
                        options = themeLabels,
                        selectedOption = selectedLabel,
                        onOptionSelected = { label ->
                            val mode = when (label) {
                                "Sáng" -> "light"
                                "Tối" -> "dark"
                                else -> "system"
                            }
                            ThemeSettings.setThemeMode(mode)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSmsSetup() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "SMS Config",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AppBodyText(
                        text = "Quản lý SĐT nhận cảnh báo",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToBehaviorSpeciesList() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Defense Config",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AppBodyText(
                        text = "Thiết lập hành vi ứng phó",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card 3: Server Configuration
        val tokenManager = remember { TokenManager(context) }
        var serverUrlInput by remember { mutableStateOf(tokenManager.getServerUrl()) }

        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AppSectionTitleText(
                    text = "Cấu hình Máy chủ (Server URL)",
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    label = { Text("Địa chỉ máy chủ API") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val trimmed = serverUrlInput.trim()
                        if (trimmed.isNotEmpty()) {
                            val formatted = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
                            tokenManager.saveServerUrl(formatted)
                            NetworkClient.customServerUrl = formatted
                            android.widget.Toast.makeText(context, "Đã lưu địa chỉ máy chủ: $formatted", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Vui lòng nhập địa chỉ máy chủ hợp lệ", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Lưu địa chỉ", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Red Logout Button: Styled matching the light red background design
        val logoutBgColor = if (isDark) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        } else {
            Color(0xFFFFEBEE)
        }
        val logoutTextColor = if (isDark) {
            Color(0xFFEF9A9A)
        } else {
            Color(0xFFC62828)
        }

        Button(
            onClick = { showLogoutConfirm = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = logoutBgColor,
                contentColor = logoutTextColor
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(48.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Đăng xuất", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(text = "Xác nhận đăng xuất?") },
            text = { Text(text = "Bệ Hạ có chắc chắn muốn đăng xuất khỏi hệ thống không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    }
                ) {
                    Text(text = "Đăng xuất", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(text = "Hủy")
                }
            }
        )
    }
}

private fun retrieveAndRegisterToken(context: Context, viewModel: MainViewModel) {
    try {
        val hasFirebase = try {
            com.google.firebase.FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }

        if (hasFirebase) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        android.util.Log.d("FCM_Token", "FCM Token thực tế: ${task.result}")
                        viewModel.registerDeviceToken(task.result)
                    } else {
                        val mockToken = "mock-token-fail-${UUID.randomUUID()}"
                        android.util.Log.w("FCM_Token", "Không lấy được FCM token, dùng mock: $mockToken")
                        viewModel.registerDeviceToken(mockToken)
                    }
                }
        } else {
            val mockToken = "mock-token-noapp-${UUID.randomUUID()}"
            android.util.Log.w("FCM_Token", "Không có FirebaseApp, dùng mock: $mockToken")
            viewModel.registerDeviceToken(mockToken)
        }
    } catch (e: Throwable) {
        val mockToken = "mock-token-err-${UUID.randomUUID()}"
        android.util.Log.e("FCM_Token", "Lỗi retrieveAndRegisterToken, dùng mock: $mockToken", e)
        viewModel.registerDeviceToken(mockToken)
    }
}
