package com.wildlife.deterrence

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wildlife.deterrence.data.TokenManager
import com.wildlife.deterrence.ui.main.MainScreen
import com.wildlife.deterrence.ui.screens.LoginScreen
import com.wildlife.deterrence.ui.screens.RegisterScreen
import com.wildlife.deterrence.ui.screens.SplashScreen
import com.wildlife.deterrence.viewmodel.LoginViewModel
import com.wildlife.deterrence.viewmodel.MainViewModel
import com.wildlife.deterrence.viewmodel.RegisterViewModel
import com.wildlife.deterrence.viewmodel.SplashViewModel

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val tokenManager = remember { TokenManager(context) }
  val backStack = rememberNavBackStack(Splash)
  var registeredUsername by remember { mutableStateOf<String?>(null) }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Splash> {
          val splashViewModel: SplashViewModel = viewModel {
            SplashViewModel(tokenManager)
          }
          SplashScreen(
            viewModel = splashViewModel,
            onNavigateToLogin = {
              backStack.removeLastOrNull()
              backStack.add(Login)
            },
            onNavigateToMain = {
              backStack.removeLastOrNull()
              backStack.add(Main)
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }

        entry<Login> {
          val loginViewModel: LoginViewModel = viewModel {
            LoginViewModel(tokenManager)
          }
          LaunchedEffect(registeredUsername) {
            registeredUsername?.let {
              loginViewModel.onUsernameChanged(it)
              registeredUsername = null // Reset after autofill
            }
          }
          LoginScreen(
            viewModel = loginViewModel,
            onNavigateToMain = {
              backStack.removeLastOrNull()
              backStack.add(Main)
            },
            onNavigateToRegister = {
              backStack.add(Register)
            },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }

        entry<Register> {
          val registerViewModel: RegisterViewModel = viewModel()
          RegisterScreen(
            viewModel = registerViewModel,
            onNavigateBack = {
              backStack.removeLastOrNull()
            },
            onRegisterSuccess = { username ->
              registeredUsername = username
              backStack.removeLastOrNull()
            },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }

        entry<Main> {
          val mainViewModel: MainViewModel = viewModel {
            MainViewModel(tokenManager)
          }
          MainScreen(
            viewModel = mainViewModel,
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
