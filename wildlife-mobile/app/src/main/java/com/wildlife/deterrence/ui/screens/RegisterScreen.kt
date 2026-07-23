package com.wildlife.deterrence.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildlife.deterrence.ui.components.AppButton
import com.wildlife.deterrence.ui.components.AppSubTitleText
import com.wildlife.deterrence.ui.components.AppTitleText
import com.wildlife.deterrence.ui.components.ValidatedTextField
import com.wildlife.deterrence.viewmodel.PasswordStrength
import com.wildlife.deterrence.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.registerSuccessUsername) {
        uiState.registerSuccessUsername?.let { username ->
            onRegisterSuccess(username)
        }
    }

    LaunchedEffect(uiState.registerError) {
        uiState.registerError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrors()
        }
    }


    val isButtonEnabled = uiState.usernameText.isNotEmpty() &&
            uiState.usernameError == null &&
            uiState.fullNameText.isNotEmpty() &&
            uiState.fullNameError == null &&
            uiState.phoneNumberText.isNotEmpty() &&
            uiState.phoneNumberError == null &&
            uiState.emailError == null &&
            uiState.passwordText.isNotEmpty() &&
            uiState.passwordError == null &&
            uiState.confirmPasswordText.isNotEmpty() &&
            uiState.confirmPasswordError == null &&
            !uiState.isLoading

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AppTitleText(text = "Đăng ký tài khoản")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppSubTitleText(
                    text = "Tạo tài khoản để quản lý hệ thống cảnh báo",
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Input: Tên đăng nhập (Username)
                ValidatedTextField(
                    value = uiState.usernameText,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = "Tên đăng nhập",
                    placeholder = "Nhập tên đăng nhập",
                    leadingIcon = Icons.Default.Person,
                    errorText = uiState.usernameError,
                    onFocusChanged = { isFocused ->
                        if (!isFocused && uiState.usernameText.isNotEmpty()) {
                            viewModel.onUsernameChanged(uiState.usernameText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input: Họ và tên (Full Name)
                ValidatedTextField(
                    value = uiState.fullNameText,
                    onValueChange = { viewModel.onFullNameChanged(it) },
                    label = "Họ và tên",
                    placeholder = "Nhập họ và tên",
                    leadingIcon = Icons.Default.Person,
                    errorText = uiState.fullNameError,
                    onFocusChanged = { isFocused ->
                        if (!isFocused && uiState.fullNameText.isNotEmpty()) {
                            viewModel.onFullNameChanged(uiState.fullNameText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input: Số điện thoại (Phone Number)
                ValidatedTextField(
                    value = uiState.phoneNumberText,
                    onValueChange = { viewModel.onPhoneNumberChanged(it) },
                    label = "Số điện thoại",
                    placeholder = "Ví dụ: 0901234567",
                    leadingIcon = Icons.Default.Phone,
                    errorText = uiState.phoneNumberError,
                    onFocusChanged = { isFocused ->
                        if (!isFocused && uiState.phoneNumberText.isNotEmpty()) {
                            viewModel.onPhoneNumberChanged(uiState.phoneNumberText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )


                // Input: Email (Tùy chọn)
                ValidatedTextField(
                    value = uiState.emailText,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = "Email (Tùy chọn)",
                    placeholder = "Nhập email",
                    leadingIcon = Icons.Default.Email,
                    errorText = uiState.emailError,
                    onFocusChanged = { isFocused ->
                        if (!isFocused && uiState.emailText.isNotEmpty()) {
                            viewModel.onEmailChanged(uiState.emailText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input: Mật khẩu (Password)
                ValidatedTextField(
                    value = uiState.passwordText,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = "Mật khẩu",
                    placeholder = "Nhập mật khẩu",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    errorText = uiState.passwordError,
                    onFocusChanged = { isFocused ->
                        if (!isFocused && uiState.passwordText.isNotEmpty()) {
                            viewModel.onPasswordChanged(uiState.passwordText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Password strength bars
                PasswordStrengthIndicator(strength = uiState.passwordStrength)

                Spacer(modifier = Modifier.height(16.dp))

                // Input: Xác nhận mật khẩu (Confirm Password)
                ValidatedTextField(
                    value = uiState.confirmPasswordText,
                    onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                    label = "Xác nhận mật khẩu",
                    placeholder = "Nhập lại mật khẩu",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    errorText = uiState.confirmPasswordError,
                    onFocusChanged = { isFocused ->
                        if (!isFocused && uiState.confirmPasswordText.isNotEmpty()) {
                            viewModel.onConfirmPasswordChanged(uiState.confirmPasswordText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Action: Register button
                AppButton(
                    text = if (uiState.isLoading) "Đang đăng ký..." else "Đăng ký",
                    onClick = { viewModel.onRegisterClick() },
                    enabled = isButtonEnabled,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation: Link to Login screen
                TextButton(onClick = onNavigateBack) {
                    Text(
                        text = "Đã có tài khoản? Đăng nhập",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val (label, color) = when (strength) {
        PasswordStrength.WEAK -> "Yếu" to Color(0xFFBA1A1A)
        PasswordStrength.MEDIUM -> "Trung bình" to Color(0xFFE67E22)
        PasswordStrength.STRONG -> "Mạnh" to Color(0xFF0D631B)
    }

    val segments = 3
    val filledCount = when (strength) {
        PasswordStrength.WEAK -> 1
        PasswordStrength.MEDIUM -> 2
        PasswordStrength.STRONG -> 3
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until segments) {
                val isFilled = i < filledCount
                val segmentColor = if (isFilled) color else Color(0xFFE1E3E4)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(segmentColor)
                )
            }
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.End
            )
        }
    }
}
