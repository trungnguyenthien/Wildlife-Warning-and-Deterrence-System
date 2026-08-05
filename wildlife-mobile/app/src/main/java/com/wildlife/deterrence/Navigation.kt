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
import com.wildlife.deterrence.ui.screens.CameraDetailScreen
import com.wildlife.deterrence.ui.screens.AlertDetailScreen
import com.wildlife.deterrence.ui.screens.AllDetectionsScreen
import com.wildlife.deterrence.viewmodel.LoginViewModel
import com.wildlife.deterrence.viewmodel.MainViewModel
import com.wildlife.deterrence.viewmodel.RegisterViewModel
import com.wildlife.deterrence.viewmodel.SplashViewModel
import com.wildlife.deterrence.viewmodel.CameraDetailViewModel
import com.wildlife.deterrence.viewmodel.AlertDetailViewModel
import com.wildlife.deterrence.viewmodel.AllDetectionsViewModel
import com.wildlife.deterrence.ui.screens.SmsSetupScreen
import com.wildlife.deterrence.viewmodel.SmsSetupViewModel
import com.wildlife.deterrence.ui.screens.BehaviorSpeciesListScreen
import com.wildlife.deterrence.ui.screens.BehaviorConfigScreen
import com.wildlife.deterrence.viewmodel.BehaviorViewModel

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

          val pendingDeepLink = DeepLinkHandler.pendingDestination
          LaunchedEffect(pendingDeepLink) {
            if (pendingDeepLink != null) {
              backStack.add(pendingDeepLink)
              DeepLinkHandler.pendingDestination = null
            }
          }

          MainScreen(
            viewModel = mainViewModel,
            tokenManager = tokenManager,
            onLogout = {
              tokenManager.deleteToken()
              backStack.clear()
              backStack.add(Login)
            },
            onNavigateToCameraDetail = { cameraId ->
              backStack.add(CameraDetail(cameraId))
            },
            onNavigateToAlertDetail = { alertId, speciesName ->
              backStack.add(AlertDetail(alertId, speciesName))
            },
            onNavigateToAllDetections = { timeRange, speciesId, cameraId ->
              backStack.add(AllDetections(timeRange, speciesId, cameraId))
            },
            onNavigateToSmsSetup = {
              backStack.add(SmsSetup)
            },
            onNavigateToBehaviorSpeciesList = {
              backStack.add(BehaviorSpeciesList)
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }

        entry<SmsSetup> {
          val smsSetupViewModel: SmsSetupViewModel = viewModel {
            SmsSetupViewModel(tokenManager)
          }
          SmsSetupScreen(
            viewModel = smsSetupViewModel,
            onBackClick = {
              backStack.removeLastOrNull()
            }
          )
        }

        entry<BehaviorSpeciesList> {
          val behaviorViewModel: BehaviorViewModel = viewModel {
            BehaviorViewModel(tokenManager)
          }
          BehaviorSpeciesListScreen(
            viewModel = behaviorViewModel,
            onBackClick = {
              backStack.removeLastOrNull()
            },
            onSpeciesClick = { speciesId, speciesName ->
              backStack.add(BehaviorConfig(speciesId, speciesName))
            }
          )
        }

        entry<BehaviorConfig> { key ->
          val behaviorViewModel: BehaviorViewModel = viewModel {
            BehaviorViewModel(tokenManager)
          }
          BehaviorConfigScreen(
            speciesId = key.speciesId,
            speciesName = key.speciesName,
            viewModel = behaviorViewModel,
            onBackClick = {
              backStack.removeLastOrNull()
            }
          )
        }

        entry<CameraDetail> { key ->
          val cameraDetailViewModel: CameraDetailViewModel = viewModel {
            CameraDetailViewModel(key.cameraId, tokenManager)
          }
          CameraDetailScreen(
            viewModel = cameraDetailViewModel,
            onBackClick = {
              backStack.removeLastOrNull()
            }
          )
        }

        entry<AlertDetail> { key ->
          val alertDetailViewModel: AlertDetailViewModel = viewModel {
            AlertDetailViewModel(key.alertId, key.speciesName, tokenManager)
          }
          AlertDetailScreen(
            viewModel = alertDetailViewModel,
            onBackClick = {
              backStack.removeLastOrNull()
            }
          )
        }

        entry<AllDetections> { key ->
          val allDetectionsViewModel: AllDetectionsViewModel = viewModel {
            AllDetectionsViewModel(key.timeRange, key.speciesId, key.cameraId, tokenManager)
          }
          AllDetectionsScreen(
            viewModel = allDetectionsViewModel,
            onBackClick = {
              backStack.removeLastOrNull()
            },
            onAlertClick = { alertId, speciesName ->
              backStack.add(AlertDetail(alertId, speciesName))
            }
          )
        }
      },
  )
}
