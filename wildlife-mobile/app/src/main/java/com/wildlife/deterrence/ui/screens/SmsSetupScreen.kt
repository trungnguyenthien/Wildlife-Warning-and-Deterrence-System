package com.wildlife.deterrence.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildlife.deterrence.viewmodel.SmsRecipientUiModel
import com.wildlife.deterrence.viewmodel.SmsSetupUiState
import com.wildlife.deterrence.viewmodel.SmsSetupViewModel
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSetupScreen(
    viewModel: SmsSetupViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val primaryGreen = Color(0xFF2C4C2C)

    var showEditModal by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedRecipientForEdit by remember { mutableStateOf<SmsRecipientUiModel?>(null) }

    var showDeleteConfirmModal by remember { mutableStateOf(false) }
    var selectedRecipientForDelete by remember { mutableStateOf<SmsRecipientUiModel?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý SĐT",
                        color = primaryGreen,
                        fontSize = 18.sp,
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.recipients.isEmpty()) {
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner thông tin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = primaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tối đa 3 SĐT. SĐT của bạn đã nhận mặc định — danh sách này là các SĐT bổ sung.",
                        color = Color.DarkGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Danh sách tối đa 3 dòng
                val recipients = uiState.recipients
                for (i in 0 until 3) {
                    if (i < recipients.size) {
                        val recipient = recipients[i]
                        // Dòng đã có SĐT
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRecipientForEdit = recipient
                                    isEditMode = true
                                    showEditModal = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone",
                                        tint = primaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recipient.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = recipient.phoneNumber,
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedRecipientForDelete = recipient
                                        showDeleteConfirmModal = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa",
                                        tint = Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    } else {
                        // Dòng trống
                        if (i == 2) {
                            // Dòng cuối nét đứt
                            DashedBorderBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clickable {
                                        selectedRecipientForEdit = null
                                        isEditMode = false
                                        showEditModal = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone",
                                        tint = Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Còn 1 chỗ trống",
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            // Dòng trống thường
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRecipientForEdit = null
                                        isEditMode = false
                                        showEditModal = true
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEEEEEE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Phone",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Còn chỗ trống",
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PHONE_EDIT_MODAL
    if (showEditModal) {
        PhoneEditModal(
            isEditMode = isEditMode,
            recipient = selectedRecipientForEdit,
            onDismiss = { showEditModal = false },
            onConfirm = { name, phone ->
                showEditModal = false
                if (isEditMode && selectedRecipientForEdit != null) {
                    viewModel.updateRecipient(
                        recipientId = selectedRecipientForEdit!!.id,
                        fullName = name,
                        phoneNumber = phone,
                        onSuccess = {}
                    )
                } else {
                    viewModel.addRecipient(
                        fullName = name,
                        phoneNumber = phone,
                        onSuccess = {}
                    )
                }
            },
            primaryGreen = primaryGreen
        )
    }

    // DELETE_CONFIRM_MODAL
    if (showDeleteConfirmModal && selectedRecipientForDelete != null) {
        DeleteConfirmModal(
            recipient = selectedRecipientForDelete!!,
            onDismiss = { showDeleteConfirmModal = false },
            onConfirm = {
                showDeleteConfirmModal = false
                viewModel.deleteRecipient(
                    recipientId = selectedRecipientForDelete!!.id,
                    onSuccess = {}
                )
            }
        )
    }
}

@Composable
fun PhoneEditModal(
    isEditMode: Boolean,
    recipient: SmsRecipientUiModel?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    primaryGreen: Color
) {
    var name by remember { mutableStateOf(recipient?.fullName ?: "") }
    var phone by remember { mutableStateOf(recipient?.phoneNumber ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Chỉnh sửa số điện thoại" else "Thêm số điện thoại mới",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Input Số điện thoại
                Column {
                    Text(
                        text = "Số điện thoại (định dạng E.164, VD: +84908888888)",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = null
                        },
                        placeholder = { Text("Nhập số điện thoại...") },
                        isError = phoneError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            focusedLabelColor = primaryGreen
                        )
                    )
                    phoneError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Input Tên người nhận
                Column {
                    Text(
                        text = "Tên người nhận",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        placeholder = { Text("Nhập tên người nhận...") },
                        isError = nameError != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            focusedLabelColor = primaryGreen
                        )
                    )
                    nameError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (name.trim().isEmpty()) {
                        nameError = "Tên không được để trống"
                        hasError = true
                    }
                    val e164Regex = Regex("^\\+[1-9]\\d{1,14}$")
                    if (!e164Regex.matches(phone.trim())) {
                        phoneError = "Định dạng SĐT không đúng (cần bắt đầu bằng +)"
                        hasError = true
                    }

                    if (!hasError) {
                        onConfirm(name.trim(), phone.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
            ) {
                Text(
                    text = if (isEditMode) "Lưu" else "Thêm mới",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Hủy",
                    color = primaryGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
fun DeleteConfirmModal(
    recipient: SmsRecipientUiModel?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Xóa số điện thoại này?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Số ${recipient?.phoneNumber ?: ""} (${recipient?.fullName ?: ""}) sẽ không còn nhận cảnh báo SMS. Bạn có chắc chắn muốn xóa?",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                color = Color.DarkGray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("Xóa", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
            ) {
                Text("Hủy", color = Color.DarkGray)
            }
        }
    )
}

@Composable
fun DashedBorderBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val stroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.5f),
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}
