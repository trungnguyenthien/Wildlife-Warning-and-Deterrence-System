package com.wildlife.deterrence.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildlife.deterrence.viewmodel.BehaviorConfigUiModel
import com.wildlife.deterrence.viewmodel.BehaviorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorConfigScreen(
    speciesId: String,
    speciesName: String,
    viewModel: BehaviorViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryGreen = Color(0xFF2C4C2C)
    val uiState by viewModel.speciesListState.collectAsState()
    val context = LocalContext.current
    val deterrentSounds by viewModel.deterrentSounds.collectAsState()
    val citizenAlertSounds by viewModel.citizenAlertSounds.collectAsState()

    // Load initial config from view model (might be default until API loading finishes)
    var configState by remember { mutableStateOf(viewModel.getConfigForSpecies(speciesId)) }
    var loadedConfig by remember { mutableStateOf(viewModel.getConfigForSpecies(speciesId)) }

    LaunchedEffect(uiState.isLoading, speciesId) {
        if (!uiState.isLoading) {
            val realConfig = viewModel.getConfigForSpecies(speciesId)
            configState = realConfig
            loadedConfig = realConfig
        }
    }

    // Accordion expand states
    var audioExpanded by remember { mutableStateOf(true) }
    var ledExpanded by remember { mutableStateOf(false) }
    var alertExpanded by remember { mutableStateOf(false) }

    // Helper to update config and automatically switch to Custom preset if needed
    fun updateConfig(updated: BehaviorConfigUiModel) {
        configState = if (updated.presetType != "custom" && 
            (updated.audioType != loadedConfig.audioType ||
             updated.audioVolume != loadedConfig.audioVolume ||
             updated.ledFrequency != loadedConfig.ledFrequency ||
             updated.ledColor != loadedConfig.ledColor ||
             updated.ledDuration != loadedConfig.ledDuration ||
             updated.sirenSampleId != loadedConfig.sirenSampleId ||
             updated.silentAlertSms != loadedConfig.silentAlertSms ||
             updated.silentAlertPush != loadedConfig.silentAlertPush)
        ) {
            updated.copy(presetType = "custom")
        } else {
            updated
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thiết lập phòng vệ với $speciesName",
                        color = primaryGreen,
                        fontSize = 17.sp,
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
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveConfigForSpecies(speciesId, configState)
                        Toast.makeText(context, "Đã lưu cấu hình cho $speciesName", Toast.LENGTH_SHORT).show()
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Lưu cấu hình",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. CẤU HÌNH NHANH
            Text(
                text = "CẤU HÌNH NHANH",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = primaryGreen
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val presets = listOf(
                        Triple("intruder", "Người lạ đột nhập", null),
                        Triple("medium_animal", "Thú vừa", null),
                        Triple("critical", "Thú cực kỳ nguy hiểm", Icons.Default.Warning),
                        Triple("custom", "Tùy chỉnh", null)
                    )

                    presets.forEach { (type, label, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (type == "custom") {
                                        configState = configState.copy(presetType = "custom")
                                    } else {
                                        configState = viewModel.applyPreset(speciesId, type)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = configState.presetType == type,
                                onClick = {
                                    if (type == "custom") {
                                        configState = configState.copy(presetType = "custom")
                                    } else {
                                        configState = viewModel.applyPreset(speciesId, type)
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                            icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = Color(0xFFEF6C00),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. TÙY CHỈNH NÂNG CAO (ACCORDION)
            Text(
                text = "TÙY CHỈNH NÂNG CAO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = primaryGreen
            )

            // Section 2.1: ÂM THANH XUA ĐUỔI
            AccordionSection(
                title = "ÂM THANH XUA ĐUỔI",
                isExpanded = audioExpanded,
                onHeaderClick = { audioExpanded = !audioExpanded },
                primaryGreen = primaryGreen
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Dropdown Loại âm thanh
                    var audioMenuOpen by remember { mutableStateOf(false) }
                    val audioOptions = remember(deterrentSounds) {
                        if (deterrentSounds.isEmpty()) {
                            listOf("Tiếng súng", "Tiếng gầm", "Tiếng chó sủa lớn", "Tiếng nổ giả lập", "Tần số siêu âm", "Không")
                        } else {
                            deterrentSounds.map { it.name } + "Không"
                        }
                    }

                    Column {
                        Text(text = "Loại âm thanh xua đuổi", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .clickable { audioMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = configState.audioType, fontSize = 14.sp, color = Color.Black)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                            DropdownMenu(
                                expanded = audioMenuOpen,
                                onDismissRequest = { audioMenuOpen = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                audioOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option) },
                                        onClick = {
                                            audioMenuOpen = false
                                            updateConfig(configState.copy(audioType = option, presetType = "custom"))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Slider cường độ âm thanh
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Cường độ âm thanh", fontSize = 12.sp, color = Color.Gray)
                            Text(text = "${configState.audioVolume}/100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }
                        Slider(
                            value = configState.audioVolume.toFloat(),
                            onValueChange = {
                                updateConfig(configState.copy(audioVolume = it.toInt(), presetType = "custom"))
                            },
                            valueRange = 1f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryGreen,
                                activeTrackColor = primaryGreen
                            )
                        )
                    }

                    // Nút Nghe thử
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "🔊 Đang phát thử: ${configState.audioType} ở cường độ ${configState.audioVolume}%", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, primaryGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Nghe thử", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 2.2: ĐÈN LED
            AccordionSection(
                title = "ĐÈN LED",
                isExpanded = ledExpanded,
                onHeaderClick = { ledExpanded = !ledExpanded },
                primaryGreen = primaryGreen
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Dropdown Tần suất
                    var freqMenuOpen by remember { mutableStateOf(false) }
                    val freqOptions = listOf("2 lần/giây", "4 lần/giây", "Nhấp nháy ngẫu nhiên")

                    Column {
                        Text(text = "Tần suất nhấp nháy", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .clickable { freqMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = configState.ledFrequency, fontSize = 14.sp, color = Color.Black)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                            DropdownMenu(
                                expanded = freqMenuOpen,
                                onDismissRequest = { freqMenuOpen = false }
                            ) {
                                freqOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option) },
                                        onClick = {
                                            freqMenuOpen = false
                                            updateConfig(configState.copy(ledFrequency = option, presetType = "custom"))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Dropdown Màu sắc
                    var colorMenuOpen by remember { mutableStateOf(false) }
                    val colorOptions = listOf("Đỏ", "Trắng", "Đỏ xen trắng")

                    Column {
                        Text(text = "Màu sắc đèn LED", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .clickable { colorMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = configState.ledColor, fontSize = 14.sp, color = Color.Black)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                            DropdownMenu(
                                expanded = colorMenuOpen,
                                onDismissRequest = { colorMenuOpen = false }
                            ) {
                                colorOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option) },
                                        onClick = {
                                            colorMenuOpen = false
                                            updateConfig(configState.copy(ledColor = option, presetType = "custom"))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Slider Thời lượng nhấp nháy (giây)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Thời lượng nhấp nháy", fontSize = 12.sp, color = Color.Gray)
                            Text(text = "${configState.ledDuration} giây", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
                        }
                        Slider(
                            value = configState.ledDuration.toFloat(),
                            onValueChange = {
                                updateConfig(configState.copy(ledDuration = it.toInt(), presetType = "custom"))
                            },
                            valueRange = 1f..60f,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryGreen,
                                activeTrackColor = primaryGreen
                            )
                        )
                    }
                }
            }

            // Section 2.3: PHÁT CẢNH BÁO
            AccordionSection(
                title = "PHÁT CẢNH BÁO",
                isExpanded = alertExpanded,
                onHeaderClick = { alertExpanded = !alertExpanded },
                primaryGreen = primaryGreen
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Dropdown Mẫu nội dung loa
                    var sampleMenuOpen by remember { mutableStateOf(false) }
                    val sampleOptions = remember(citizenAlertSounds) {
                        // Danh sách mẫu loa lấy từ API (GET /alertSounds qua audio-samples), không hardcode
                        citizenAlertSounds.map { it.name }
                    }

                    Column {
                        Text(text = "Mẫu nội dung loa", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .clickable { sampleMenuOpen = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = configState.sirenSampleId, fontSize = 14.sp, color = Color.Black)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                            DropdownMenu(
                                expanded = sampleMenuOpen,
                                onDismissRequest = { sampleMenuOpen = false }
                            ) {
                                sampleOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option) },
                                        onClick = {
                                            sampleMenuOpen = false
                                            updateConfig(configState.copy(sirenSampleId = option, presetType = "custom"))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Toggle Gửi SMS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Gửi SMS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(text = "Gửi tin nhắn cảnh báo tới SĐT phụ", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = configState.silentAlertSms,
                            onCheckedChange = {
                                updateConfig(configState.copy(silentAlertSms = it, presetType = "custom"))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryGreen, checkedTrackColor = primaryGreen.copy(alpha = 0.4f))
                        )
                    }

                    // Toggle Gửi Push Notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Gửi Push Notification", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(text = "Gửi thông báo đẩy về ứng dụng di động", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = configState.silentAlertPush,
                            onCheckedChange = {
                                updateConfig(configState.copy(silentAlertPush = it, presetType = "custom"))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryGreen, checkedTrackColor = primaryGreen.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun AccordionSection(
    title: String,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    primaryGreen: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = primaryGreen
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = primaryGreen
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    content()
                }
            }
        }
    }
}
