package com.wildlife.deterrence.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wildlife.deterrence.viewmodel.MainViewModel
import java.util.UUID

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val context = LocalContext.current

    // Request POST_NOTIFICATIONS permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Retrieve and register token (FCM or Mock)
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
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> BlankTabScreen(title = "Trạm Camera", subtitle = "Danh sách camera giám sát động vật hoang dã")
                1 -> BlankTabScreen(title = "Thống Kê", subtitle = "Biểu đồ tần suất xuất hiện và cảnh báo")
                2 -> BlankTabScreen(title = "Nhật Ký Cảnh Báo", subtitle = "Thông tin phát hiện động vật thời gian thực")
                3 -> BlankTabScreen(title = "Cấu Hình Hệ Thống", subtitle = "Thiết lập cảnh báo và cấu hình phòng vệ")
                else -> BlankTabScreen(title = "Không tìm thấy", subtitle = "Tab không hợp lệ")
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
