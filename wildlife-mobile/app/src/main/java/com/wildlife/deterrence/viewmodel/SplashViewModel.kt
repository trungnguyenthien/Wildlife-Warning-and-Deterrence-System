package com.wildlife.deterrence.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildlife.deterrence.data.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(private val tokenManager: TokenManager) : ViewModel() {
  private val _navigationTarget = MutableStateFlow<String?>(null)
  val navigationTarget: StateFlow<String?> = _navigationTarget.asStateFlow()

  init {
    checkTokenAndNavigate()
  }

  private fun checkTokenAndNavigate() {
    viewModelScope.launch {
      delay(2000)
      val token = tokenManager.getToken()
      if (token.isNullOrEmpty()) {
        _navigationTarget.value = "login"
      } else {
        _navigationTarget.value = "main"
      }
    }
  }
}
