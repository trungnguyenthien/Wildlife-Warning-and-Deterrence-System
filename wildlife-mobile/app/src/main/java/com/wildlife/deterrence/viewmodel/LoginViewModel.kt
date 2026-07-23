package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.AuthApi
import com.wildlife.deterrence.data.LoginRequest
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val usernameText: String = "",
    val usernameError: String? = null,
    val passwordText: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val loginSuccess: Boolean = false
)

class LoginViewModel(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi = NetworkClient.authApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val usernameRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]{3,19}$")

    fun onUsernameChanged(text: String) {
        val error = if (text.isEmpty()) {
            "Bắt buộc phải nhập"
        } else if (!usernameRegex.matches(text)) {
            "Tên đăng nhập 4–20 ký tự, gồm chữ, số và gạch dưới, không bắt đầu bằng số"
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(
            usernameText = text,
            usernameError = error,
            loginError = null
        )
    }

    fun onPasswordChanged(text: String) {
        val error = if (text.isEmpty()) {
            "Bắt buộc phải nhập"
        } else if (text.length !in 6..30 || text.all { it.isWhitespace() }) {
            "Mật khẩu 6–30 ký tự"
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(
            passwordText = text,
            passwordError = error,
            loginError = null
        )
    }

    fun onLoginClick() {
        val currentState = _uiState.value
        if (currentState.usernameText.isEmpty() || currentState.usernameError != null ||
            currentState.passwordText.isEmpty() || currentState.passwordError != null
        ) {
            return
        }

        _uiState.value = currentState.copy(isLoading = true, loginError = null)

        viewModelScope.launch {
            try {
                val response = authApi.login(
                    LoginRequest(
                        username = currentState.usernameText,
                        password = currentState.passwordText
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        tokenManager.saveToken(body.token)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            loginSuccess = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            loginError = "Phản hồi rỗng từ hệ thống"
                        )
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Sai tên đăng nhập hoặc mật khẩu"
                        else -> "Đăng nhập thất bại (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginError = errorMsg
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginError = "Lỗi kết nối mạng: ${e.localizedMessage ?: "Vui lòng thử lại"}"
                )
            }
        }
    }

    fun clearErrors() {
        _uiState.value = _uiState.value.copy(loginError = null)
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}
