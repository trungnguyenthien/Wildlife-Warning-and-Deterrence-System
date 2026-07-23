package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.AuthApi
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.PushTokenRequest
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi = NetworkClient.authApi
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _registerStatus = MutableStateFlow<String?>(null)
    val registerStatus: StateFlow<String?> = _registerStatus.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun registerDeviceToken(fcmToken: String) {
        val token = tokenManager.getToken()
        if (token == null) {
            _registerStatus.value = "Error: Session token is null"
            return
        }

        val authHeader = "Bearer $token"
        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"

        viewModelScope.launch {
            try {
                val response = authApi.registerPushToken(
                    authHeader = authHeader,
                    request = PushTokenRequest(
                        fcmToken = fcmToken,
                        deviceModel = deviceModel,
                        osVersion = osVersion
                    )
                )
                if (response.isSuccessful) {
                    _registerStatus.value = "Registered successfully"
                } else {
                    _registerStatus.value = "Registration failed: ${response.code()}"
                }
            } catch (e: Exception) {
                _registerStatus.value = "Error: ${e.message}"
            }
        }
    }
}
