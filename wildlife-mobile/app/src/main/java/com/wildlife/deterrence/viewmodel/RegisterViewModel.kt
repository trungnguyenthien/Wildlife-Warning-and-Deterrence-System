package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.AuthApi
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG
}

data class RegisterUiState(
    val usernameText: String = "",
    val usernameError: String? = null,
    val fullNameText: String = "",
    val fullNameError: String? = null,
    val phoneNumberText: String = "",
    val phoneNumberError: String? = null,
    val emailText: String = "",
    val emailError: String? = null,
    val passwordText: String = "",
    val passwordError: String? = null,
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK,
    val confirmPasswordText: String = "",
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val registerError: String? = null,
    val registerSuccessUsername: String? = null
)

class RegisterViewModel(
    private val authApi: AuthApi = NetworkClient.authApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val usernameRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]{3,19}$")
    private val phoneRegex = Regex("^0[0-9]{9}$")
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

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
            registerError = null
        )
    }

    fun onFullNameChanged(text: String) {
        val error = if (text.trim().isEmpty()) {
            "Họ và tên không được để trống"
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(
            fullNameText = text,
            fullNameError = error,
            registerError = null
        )
    }

    fun onPhoneNumberChanged(text: String) {
        val error = if (text.isEmpty()) {
            "Bắt buộc phải nhập"
        } else if (!phoneRegex.matches(text)) {
            "Số điện thoại phải gồm 10 chữ số bắt đầu bằng 0"
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(
            phoneNumberText = text,
            phoneNumberError = error,
            registerError = null
        )
    }

    fun onEmailChanged(text: String) {
        val error = if (text.isNotEmpty() && !emailRegex.matches(text)) {
            "Email không đúng định dạng"
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(
            emailText = text,
            emailError = error,
            registerError = null
        )
    }

    fun onPasswordChanged(text: String) {
        // Calculate strength
        val strength = when {
            text.isEmpty() || text.length < 8 || text.all { it.isLetter() } || text.all { it.isDigit() } -> PasswordStrength.WEAK
            text.length >= 12 && text.any { it.isLetter() } && text.any { it.isDigit() } -> PasswordStrength.STRONG
            else -> PasswordStrength.MEDIUM
        }

        // Validate error
        val error = if (text.isEmpty()) {
            "Bắt buộc phải nhập"
        } else if (text.length !in 8..30 || !text.any { it.isLetter() } || !text.any { it.isDigit() } || text.all { it.isWhitespace() }) {
            "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số"
        } else {
            null
        }

        _uiState.value = _uiState.value.copy(
            passwordText = text,
            passwordError = error,
            passwordStrength = strength,
            registerError = null
        )

        // Validate confirm password too if it was already filled
        if (_uiState.value.confirmPasswordText.isNotEmpty()) {
            onConfirmPasswordChanged(_uiState.value.confirmPasswordText)
        }
    }

    fun onConfirmPasswordChanged(text: String) {
        val error = if (text.isEmpty()) {
            "Bắt buộc phải nhập"
        } else if (text != _uiState.value.passwordText) {
            "Mật khẩu xác nhận không khớp"
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(
            confirmPasswordText = text,
            confirmPasswordError = error,
            registerError = null
        )
    }

    fun onRegisterClick() {
        val state = _uiState.value
        // Trigger validations for empty fields
        onUsernameChanged(state.usernameText)
        onFullNameChanged(state.fullNameText)
        onPhoneNumberChanged(state.phoneNumberText)
        onEmailChanged(state.emailText)
        onPasswordChanged(state.passwordText)
        onConfirmPasswordChanged(state.confirmPasswordText)

        val updatedState = _uiState.value
        if (updatedState.usernameText.isEmpty() || updatedState.usernameError != null ||
            updatedState.fullNameText.trim().isEmpty() || updatedState.fullNameError != null ||
            updatedState.phoneNumberText.isEmpty() || updatedState.phoneNumberError != null ||
            updatedState.emailError != null ||
            updatedState.passwordText.isEmpty() || updatedState.passwordError != null ||
            updatedState.confirmPasswordText.isEmpty() || updatedState.confirmPasswordError != null
        ) {
            _uiState.value = updatedState.copy(registerError = "Vui lòng kiểm tra các ô bị lỗi")
            return
        }

        _uiState.value = updatedState.copy(isLoading = true, registerError = null)

        viewModelScope.launch {
            try {
                // Convert 0xxxxxxxxx to E.164 +84xxxxxxxxx
                val rawPhone = updatedState.phoneNumberText
                val formattedPhone = if (rawPhone.startsWith("0")) {
                    "+84" + rawPhone.substring(1)
                } else {
                    rawPhone
                }

                val response = authApi.register(
                    RegisterRequest(
                        username = updatedState.usernameText,
                        fullName = updatedState.fullNameText,
                        phoneNumber = formattedPhone,
                        email = if (updatedState.emailText.isEmpty()) null else updatedState.emailText,
                        role = "CITIZEN",
                        password = updatedState.passwordText,
                        confirmPassword = updatedState.confirmPasswordText
                    )
                )

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        registerSuccessUsername = updatedState.usernameText
                    )
                } else {
                    val errorMsg = when (response.code()) {
                        409 -> "Username hoặc số điện thoại đã tồn tại trong hệ thống."
                        else -> "Đăng ký thất bại (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        registerError = errorMsg
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registerError = "Lỗi kết nối mạng: ${e.localizedMessage ?: "Vui lòng thử lại"}"
                )
            }
        }
    }

    fun clearErrors() {
        _uiState.value = _uiState.value.copy(registerError = null)
    }
}
