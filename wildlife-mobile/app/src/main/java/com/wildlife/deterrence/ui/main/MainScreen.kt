package com.wildlife.deterrence.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val context = LocalContext.current

    // Request POST_NOTIFICATIONS permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        retrieveAndRegisterToken(context, viewModel)
    }

    LaunchedEffect(Unit) {
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

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) {
            viewModel.fetchUserProfile()
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
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) }
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
                0 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BlankTabScreen(title = "Trạm Camera", subtitle = "Danh sách camera giám sát động vật hoang dã")
                }
                1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BlankTabScreen(title = "Thống Kê", subtitle = "Biểu đồ tần suất xuất hiện và cảnh báo")
                }
                2 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BlankTabScreen(title = "Nhật Ký Cảnh Báo", subtitle = "Thông tin phát hiện động vật thời gian thực")
                }
                3 -> SettingsTabContent(viewModel = viewModel, onLogout = onLogout)
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
    onLogout: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoadingProfile by viewModel.isLoadingProfile.collectAsState()
    val profileError by viewModel.profileError.collectAsState()
    val themeMode by ThemeSettings.themeMode.collectAsState()

    var showLogoutConfirm by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val titleColor = if (isDark) Color(0xFF81C784) else Color(0xFF2C4C2C)
        Text(
            text = "Cài đặt",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        // Card 1: User Profile Display (Dark green filled card matching mockup)
        val profileCardBg = Color(0xFF2C4C2C)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = profileCardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF7EF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color(0xFF2C4C2C),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingProfile) {
                    CircularProgressIndicator(color = Color.White)
                } else if (profileError != null) {
                    Text(
                        text = profileError ?: "Lỗi tải thông tin",
                        color = Color(0xFFFFCDD2),
                        fontSize = 14.sp
                    )
                } else if (userProfile != null) {
                    Text(
                        text = "MÃ NGƯỜI DÙNG",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ID: ${userProfile?.id}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${userProfile?.fullName} (${userProfile?.username})",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Vai trò: ${userProfile?.role}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Không có dữ liệu profile.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
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
                        .clickable { /* Điều hướng sang SMS Config */ }
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
                        .clickable { /* Điều hướng sang Species Config */ }
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
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        viewModel.registerDeviceToken(task.result)
                    } else {
                        viewModel.registerDeviceToken("mock-token-fail-${UUID.randomUUID()}")
                    }
                }
        } else {
            viewModel.registerDeviceToken("mock-token-noapp-${UUID.randomUUID()}")
        }
    } catch (e: Throwable) {
        viewModel.registerDeviceToken("mock-token-err-${UUID.randomUUID()}")
    }
}
