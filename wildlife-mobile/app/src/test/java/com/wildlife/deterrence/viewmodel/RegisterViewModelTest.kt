package com.wildlife.deterrence.viewmodel

import com.wildlife.deterrence.data.AuthApi
import com.wildlife.deterrence.data.LoginRequest
import com.wildlife.deterrence.data.LoginResponse
import com.wildlife.deterrence.data.RegisterRequest
import com.wildlife.deterrence.data.RegisterResponse
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

class RegisterViewModelTest {

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
    fun TC_UI_REG_VAL_USER_01() = runTest {
        val fakeAuthApi = FakeRegisterAuthApi()
        val viewModel = RegisterViewModel(fakeAuthApi)

        viewModel.onUsernameChanged("")
        assertEquals("Bắt buộc phải nhập", viewModel.uiState.value.usernameError)

        viewModel.onUsernameChanged("abc")
        assertEquals(
            "Tên đăng nhập 4–20 ký tự, gồm chữ, số và gạch dưới, không bắt đầu bằng số",
            viewModel.uiState.value.usernameError
        )

        viewModel.onUsernameChanged("valid_user")
        assertNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun TC_UI_REG_VAL_PHONE_01() = runTest {
        val fakeAuthApi = FakeRegisterAuthApi()
        val viewModel = RegisterViewModel(fakeAuthApi)

        viewModel.onPhoneNumberChanged("")
        assertEquals("Bắt buộc phải nhập", viewModel.uiState.value.phoneNumberError)

        viewModel.onPhoneNumberChanged("123456789")
        assertEquals("Số điện thoại phải gồm 10 chữ số bắt đầu bằng 0", viewModel.uiState.value.phoneNumberError)

        viewModel.onPhoneNumberChanged("0901234567")
        assertNull(viewModel.uiState.value.phoneNumberError)
    }

    @Test
    fun TC_UI_REG_VAL_PASS_01() = runTest {
        val fakeAuthApi = FakeRegisterAuthApi()
        val viewModel = RegisterViewModel(fakeAuthApi)

        viewModel.onPasswordChanged("")
        assertEquals("Bắt buộc phải nhập", viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("1234567")
        assertEquals("Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số", viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("onlyletters")
        assertEquals("Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số", viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("valid12345")
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun TC_UI_REG_VAL_CONFIRM_01() = runTest {
        val fakeAuthApi = FakeRegisterAuthApi()
        val viewModel = RegisterViewModel(fakeAuthApi)

        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("different123")
        assertEquals("Mật khẩu xác nhận không khớp", viewModel.uiState.value.confirmPasswordError)

        viewModel.onConfirmPasswordChanged("password123")
        assertNull(viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun TC_UI_REG_STRENGTH_01() = runTest {
        val fakeAuthApi = FakeRegisterAuthApi()
        val viewModel = RegisterViewModel(fakeAuthApi)

        viewModel.onPasswordChanged("123")
        assertEquals(PasswordStrength.WEAK, viewModel.uiState.value.passwordStrength)

        viewModel.onPasswordChanged("password123")
        assertEquals(PasswordStrength.MEDIUM, viewModel.uiState.value.passwordStrength)

        viewModel.onPasswordChanged("verystrongpassword123")
        assertEquals(PasswordStrength.STRONG, viewModel.uiState.value.passwordStrength)
    }

    @Test
    fun TC_UI_REG_SUCCESS() = runTest {
        val fakeAuthApi = FakeRegisterAuthApi().apply {
            shouldSucceed = true
        }
        val viewModel = RegisterViewModel(fakeAuthApi)

        viewModel.onUsernameChanged("valid_user")
        viewModel.onFullNameChanged("Valid Full Name")
        viewModel.onPhoneNumberChanged("0901234567")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")

        viewModel.onRegisterClick()

        assertNull(viewModel.uiState.value.registerError)
        assertEquals("valid_user", viewModel.uiState.value.registerSuccessUsername)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun TC_UI_REG_FAILURE_CONFLICT() = runTest {
        val fakeAuthApi = FakeRegisterAuthApi().apply {
            shouldSucceed = false
            errorCode = 409
        }
        val viewModel = RegisterViewModel(fakeAuthApi)

        viewModel.onUsernameChanged("valid_user")
        viewModel.onFullNameChanged("Valid Full Name")
        viewModel.onPhoneNumberChanged("0901234567")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")

        viewModel.onRegisterClick()

        assertEquals("Username hoặc số điện thoại đã tồn tại trong hệ thống.", viewModel.uiState.value.registerError)
        assertNull(viewModel.uiState.value.registerSuccessUsername)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

private class FakeRegisterAuthApi : AuthApi {
    var shouldSucceed: Boolean = true
    var errorCode: Int = 400

    override suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return Response.error(400, ResponseBody.create(null, ""))
    }

    override suspend fun register(request: RegisterRequest): Response<RegisterResponse> {
        assertEquals("CITIZEN", request.role)
        return if (shouldSucceed) {
            Response.success(
                RegisterResponse(
                    id = "fake-user-id",
                    username = request.username,
                    fullName = request.fullName,
                    phoneNumber = request.phoneNumber,
                    role = request.role,
                    createdAt = "2026-07-23T11:00:00Z"
                )
            )
        } else {
            Response.error(errorCode, ResponseBody.create(null, ""))
        }
    }
}
