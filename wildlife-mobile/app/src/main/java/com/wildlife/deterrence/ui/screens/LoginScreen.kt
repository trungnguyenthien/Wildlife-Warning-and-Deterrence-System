package com.wildlife.deterrence.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wildlife.deterrence.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
  viewModel: LoginViewModel,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text("Login Screen Placeholder (To Be Implemented)")
  }
}
