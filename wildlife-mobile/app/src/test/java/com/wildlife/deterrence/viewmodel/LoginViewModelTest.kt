package com.wildlife.deterrence.viewmodel

import com.wildlife.deterrence.data.AuthApi
import com.wildlife.deterrence.data.LoginRequest
import com.wildlife.deterrence.data.LoginResponse
import com.wildlife.deterrence.data.PushTokenRequest
import com.wildlife.deterrence.data.RegisterRequest
import com.wildlife.deterrence.data.RegisterResponse
import com.wildlife.deterrence.data.TokenManager
import com.wildlife.deterrence.data.UserProfileResponse
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import okhttp3.ResponseBody

class LoginViewModelTest {

    private val tokenManager = TokenManager(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun TC_UI_VAL_USER_01() = runTest {
        val fakeAuthApi = FakeLoginAuthApi()
        val viewModel = LoginViewModel(tokenManager, fakeAuthApi)

        // Case 1: Empty username
        viewModel.onUsernameChanged("")
        assertEquals("Bắt buộc phải nhập", viewModel.uiState.value.usernameError)

        // Case 2: Too short (less than 4 chars)
        viewModel.onUsernameChanged("abc")
        assertEquals(
            "Tên đăng nhập 4–20 ký tự, gồm chữ, số và gạch dưới, không bắt đầu bằng số",
            viewModel.uiState.value.usernameError
        )

        // Case 3: Starts with a digit
        viewModel.onUsernameChanged("1username")
        assertEquals(
            "Tên đăng nhập 4–20 ký tự, gồm chữ, số và gạch dưới, không bắt đầu bằng số",
            viewModel.uiState.value.usernameError
        )

        // Case 4: Valid username
        viewModel.onUsernameChanged("valid_user")
        assertNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun TC_UI_VAL_PASS_01() = runTest {
        val fakeAuthApi = FakeLoginAuthApi()
        val viewModel = LoginViewModel(tokenManager, fakeAuthApi)

        // Case 1: Empty password
        viewModel.onPasswordChanged("")
        assertEquals("Bắt buộc phải nhập", viewModel.uiState.value.passwordError)

        // Case 2: Too short (less than 6 chars)
        viewModel.onPasswordChanged("12345")
        assertEquals("Mật khẩu 6–30 ký tự", viewModel.uiState.value.passwordError)

        // Case 3: Only whitespaces
        viewModel.onPasswordChanged("      ")
        assertEquals("Mật khẩu 6–30 ký tự", viewModel.uiState.value.passwordError)

        // Case 4: Valid password
        viewModel.onPasswordChanged("123456")
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun TC_UI_AUTH_SUCCESS() = runTest {
        val fakeAuthApi = FakeLoginAuthApi().apply {
            shouldSucceed = true
            responseToken = "valid-token"
        }
        val viewModel = LoginViewModel(tokenManager, fakeAuthApi)

        viewModel.onUsernameChanged("valid_user")
        viewModel.onPasswordChanged("valid_password")

        viewModel.onLoginClick()

        // Wait/assert outcome
        assertTrue(viewModel.uiState.value.loginSuccess)
        assertNull(viewModel.uiState.value.loginError)
        assertEquals("valid-token", tokenManager.getToken())
    }

    @Test
    fun TC_UI_AUTH_FAILURE() = runTest {
        val fakeAuthApi = FakeLoginAuthApi().apply {
            shouldSucceed = false
            errorCode = 401
        }
        val viewModel = LoginViewModel(tokenManager, fakeAuthApi)

        viewModel.onUsernameChanged("valid_user")
        viewModel.onPasswordChanged("valid_password")

        viewModel.onLoginClick()

        assertFalse(viewModel.uiState.value.loginSuccess)
        assertEquals("Sai tên đăng nhập hoặc mật khẩu", viewModel.uiState.value.loginError)
    }

    @Test
    fun testResetState() = runTest {
        val fakeAuthApi = FakeLoginAuthApi()
        val viewModel = LoginViewModel(tokenManager, fakeAuthApi)

        viewModel.onUsernameChanged("username_test")
        viewModel.onPasswordChanged("password_test")

        viewModel.resetState()

        assertEquals("", viewModel.uiState.value.usernameText)
        assertEquals("", viewModel.uiState.value.passwordText)
        assertFalse(viewModel.uiState.value.loginSuccess)
    }
}

private class FakeLoginAuthApi : AuthApi {
    var shouldSucceed: Boolean = true
    var responseToken: String = ""
    var errorCode: Int = 400

    override suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return if (shouldSucceed) {
            Response.success(LoginResponse(token = responseToken, role = "user"))
        } else {
            Response.error(errorCode, ResponseBody.create(null, ""))
        }
    }

    override suspend fun register(request: RegisterRequest): Response<RegisterResponse> {
        return Response.error(400, ResponseBody.create(null, ""))
    }

    override suspend fun registerPushToken(
        authHeader: String,
        request: PushTokenRequest
    ): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun deletePushToken(authHeader: String, fcmToken: String?): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun getUserProfile(authHeader: String): Response<UserProfileResponse> {
        return Response.error(400, ResponseBody.create(null, ""))
    }
}
