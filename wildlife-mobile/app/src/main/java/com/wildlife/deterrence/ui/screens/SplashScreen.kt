package com.wildlife.deterrence.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wildlife.deterrence.ui.components.AppLogo
import com.wildlife.deterrence.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
  viewModel: SplashViewModel,
  onNavigateToLogin: () -> Unit,
  onNavigateToMain: () -> Unit,
  modifier: Modifier = Modifier
) {
  val target by viewModel.navigationTarget.collectAsState()

  LaunchedEffect(target) {
    when (target) {
      "login" -> onNavigateToLogin()
      "main" -> onNavigateToMain()
    }
  }

  Box(
    modifier = modifier.fillMaxSize().padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    AppLogo()
  }
}
