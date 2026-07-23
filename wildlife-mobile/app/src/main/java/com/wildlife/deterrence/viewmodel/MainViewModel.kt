package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.AuthApi
import com.wildlife.deterrence.data.NetworkClient
import com.wildlife.deterrence.data.PushTokenRequest
import com.wildlife.deterrence.data.TokenManager
import com.wildlife.deterrence.data.UserProfileResponse
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

    private val _userProfile = MutableStateFlow<UserProfileResponse?>(null)
    val userProfile: StateFlow<UserProfileResponse?> = _userProfile.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError.asStateFlow()

    init {
        val cached = tokenManager.getUserProfile()
        if (cached != null) {
            _userProfile.value = cached
        }
    }

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

    fun fetchUserProfile() {
        val token = tokenManager.getToken()
        if (token == null) {
            _profileError.value = "Error: Session token is null"
            return
        }

        val authHeader = "Bearer $token"
        _profileError.value = null

        // Load cached profile first if in-memory is null
        val cached = tokenManager.getUserProfile()
        if (cached != null && _userProfile.value == null) {
            _userProfile.value = cached
        }

        // Show spinner only when there is no data at all (neither cache nor in-memory)
        if (_userProfile.value == null) {
            _isLoadingProfile.value = true
        }

        viewModelScope.launch {
            try {
                val response = authApi.getUserProfile(authHeader)
                _isLoadingProfile.value = false
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    _userProfile.value = profile
                    tokenManager.saveUserProfile(profile) // Save to local cache
                } else {
                    _profileError.value = "Failed to load profile: ${response.code()}"
                }
            } catch (e: Exception) {
                _isLoadingProfile.value = false
                _profileError.value = "Error: ${e.message}"
            }
        }
    }
}
