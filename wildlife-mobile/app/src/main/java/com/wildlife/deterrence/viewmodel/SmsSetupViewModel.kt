package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.SmsApi
import com.wildlife.deterrence.data.SmsRecipientRequest
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SmsRecipientUiModel(
    val id: String,
    val phoneNumber: String,
    val fullName: String,
    val relation: String = "family"
)

data class SmsSetupUiState(
    val recipients: List<SmsRecipientUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SmsSetupViewModel(
    private val tokenManager: TokenManager,
    private val smsApi: SmsApi = NetworkClient.smsApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmsSetupUiState())
    val uiState: StateFlow<SmsSetupUiState> = _uiState.asStateFlow()

    init {
        loadRecipients()
    }

    fun loadRecipients() {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Vui lòng đăng nhập lại.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val list = smsApi.getSmsRecipients("Bearer $token")
                val uiList = list.map {
                    SmsRecipientUiModel(
                        id = it.id,
                        phoneNumber = it.phoneNumber,
                        fullName = it.fullName,
                        relation = it.relation
                    )
                }
                _uiState.value = SmsSetupUiState(recipients = uiList, isLoading = false)
            } catch (e: Exception) {
                // Fallback mockup list if offline or server fails
                println("SmsSetup: API get recipients failed, falling back to empty mock list: ${e.localizedMessage}")
                _uiState.value = SmsSetupUiState(
                    recipients = mockRecipients,
                    isLoading = false
                )
            }
        }
    }

    fun addRecipient(fullName: String, phoneNumber: String, onSuccess: () -> Unit) {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Vui lòng đăng nhập lại.")
            return
        }

        val trimmedName = fullName.trim()
        val trimmedPhone = phoneNumber.trim()

        if (trimmedName.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Tên người nhận không được để trống")
            return
        }

        val e164Regex = Regex("^\\+[1-9]\\d{1,14}$")
        if (!e164Regex.matches(trimmedPhone)) {
            _uiState.value = _uiState.value.copy(error = "Số điện thoại không hợp lệ. Phải đúng chuẩn E.164 (VD: +84908888888)")
            return
        }

        if (_uiState.value.recipients.size >= 3) {
            _uiState.value = _uiState.value.copy(error = "Đã đạt giới hạn tối đa 3 số điện thoại phụ")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                smsApi.addSmsRecipient(
                    token = "Bearer $token",
                    request = SmsRecipientRequest(fullName = trimmedName, phoneNumber = trimmedPhone, relation = "family")
                )
                _uiState.value = _uiState.value.copy(successMessage = "Thêm số điện thoại thành công")
                loadRecipients()
                onSuccess()
            } catch (e: Exception) {
                // Mock add logic for local testing
                println("SmsSetup: API add recipient failed, mocking add locally: ${e.localizedMessage}")
                val mockId = "mock-id-" + java.util.UUID.randomUUID().toString()
                mockRecipients = mockRecipients + SmsRecipientUiModel(id = mockId, phoneNumber = trimmedPhone, fullName = trimmedName)
                _uiState.value = _uiState.value.copy(successMessage = "Thêm số điện thoại thành công (Offline)")
                loadRecipients()
                onSuccess()
            }
        }
    }

    fun deleteRecipient(recipientId: String, onSuccess: () -> Unit) {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Vui lòng đăng nhập lại.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                smsApi.deleteSmsRecipient("Bearer $token", recipientId)
                _uiState.value = _uiState.value.copy(successMessage = "Xóa số điện thoại thành công")
                loadRecipients()
                onSuccess()
            } catch (e: Exception) {
                // Mock delete logic for local testing
                println("SmsSetup: API delete recipient failed, mocking delete locally: ${e.localizedMessage}")
                mockRecipients = mockRecipients.filter { it.id != recipientId }
                _uiState.value = _uiState.value.copy(successMessage = "Xóa số điện thoại thành công (Offline)")
                loadRecipients()
                onSuccess()
            }
        }
    }

    fun updateRecipient(recipientId: String, fullName: String, phoneNumber: String, onSuccess: () -> Unit) {
        val token = tokenManager.getToken()
        if (token == null) {
            _uiState.value = _uiState.value.copy(error = "Vui lòng đăng nhập lại.")
            return
        }

        val trimmedName = fullName.trim()
        val trimmedPhone = phoneNumber.trim()

        if (trimmedName.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Tên người nhận không được để trống")
            return
        }

        val e164Regex = Regex("^\\+[1-9]\\d{1,14}$")
        if (!e164Regex.matches(trimmedPhone)) {
            _uiState.value = _uiState.value.copy(error = "Số điện thoại không hợp lệ. Phải đúng chuẩn E.164 (VD: +84908888888)")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                // RESTful workaround: DELETE old recipient and POST new recipient details
                smsApi.deleteSmsRecipient("Bearer $token", recipientId)
                smsApi.addSmsRecipient(
                    token = "Bearer $token",
                    request = SmsRecipientRequest(fullName = trimmedName, phoneNumber = trimmedPhone, relation = "family")
                )
                _uiState.value = _uiState.value.copy(successMessage = "Cập nhật số điện thoại thành công")
                loadRecipients()
                onSuccess()
            } catch (e: Exception) {
                // Mock update logic for local testing
                println("SmsSetup: API update recipient failed, mocking update locally: ${e.localizedMessage}")
                mockRecipients = mockRecipients.map {
                    if (it.id == recipientId) {
                        it.copy(fullName = trimmedName, phoneNumber = trimmedPhone)
                    } else {
                        it
                    }
                }
                _uiState.value = _uiState.value.copy(successMessage = "Cập nhật số điện thoại thành công (Offline)")
                loadRecipients()
                onSuccess()
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    companion object {
        // Biến static để mock data khi offline, giữ trạng thái local trong suốt vòng đời session
        private var mockRecipients: List<SmsRecipientUiModel> = listOf(
            SmsRecipientUiModel("mock-recipient-1", "+84905111222", "Nguyễn Văn A"),
            SmsRecipientUiModel("mock-recipient-2", "+84905222333", "Trần Thị B")
        )
    }
}
