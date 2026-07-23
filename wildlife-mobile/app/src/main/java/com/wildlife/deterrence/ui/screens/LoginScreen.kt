package com.wildlife.deterrence.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildlife.deterrence.ui.components.AppButton
import com.wildlife.deterrence.ui.components.AppLogo
import com.wildlife.deterrence.ui.components.AppTitleText
import com.wildlife.deterrence.ui.components.ValidatedTextField
import com.wildlife.deterrence.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToMain: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onNavigateToMain()
        }
    }

    LaunchedEffect(uiState.loginError) {
        uiState.loginError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrors()
        }
    }

    val isButtonEnabled = uiState.usernameText.isNotEmpty() &&
            uiState.usernameError == null &&
            uiState.passwordText.isNotEmpty() &&
            uiState.passwordError == null &&
            !uiState.isLoading

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top: System Branding Logo
                AppLogo()

                Spacer(modifier = Modifier.height(32.dp))

                // Section Title: "Đăng nhập"
                AppTitleText(
                    text = "Đăng nhập",
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input field: Username
                ValidatedTextField(
                    value = uiState.usernameText,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = "Username",
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

                // Input field: Password
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

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Login Button
                AppButton(
                    text = if (uiState.isLoading) "Đang kết nối..." else "Đăng nhập",
                    onClick = { viewModel.onLoginClick() },
                    enabled = isButtonEnabled,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation: Register Link Button
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = "Đăng ký tài khoản",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
