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

class MainViewModelTest {

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
    fun testSelectTab() = runTest {
        val fakeAuthApi = FakeMainAuthApi()
        val viewModel = MainViewModel(tokenManager, fakeAuthApi)

        assertEquals(0, viewModel.selectedTab.value)
        viewModel.selectTab(2)
        assertEquals(2, viewModel.selectedTab.value)
    }

    @Test
    fun testRegisterDeviceTokenSuccess() = runTest {
        tokenManager.saveToken("valid-session-token")
        val fakeAuthApi = FakeMainAuthApi().apply {
            shouldSucceed = true
        }
        val viewModel = MainViewModel(tokenManager, fakeAuthApi)

        viewModel.registerDeviceToken("valid-fcm-token")

        assertEquals("Registered successfully", viewModel.registerStatus.value)
        assertEquals("Bearer valid-session-token", fakeAuthApi.lastAuthHeader)
        assertEquals("valid-fcm-token", fakeAuthApi.lastRequest?.fcmToken)
    }

    @Test
    fun testRegisterDeviceTokenMissingSession() = runTest {
        tokenManager.deleteToken()
        val fakeAuthApi = FakeMainAuthApi()
        val viewModel = MainViewModel(tokenManager, fakeAuthApi)

        viewModel.registerDeviceToken("valid-fcm-token")

        assertEquals("Error: Session token is null", viewModel.registerStatus.value)
        assertNull(fakeAuthApi.lastRequest)
    }

    @Test
    fun testRegisterDeviceTokenFailure() = runTest {
        tokenManager.saveToken("valid-session-token")
        val fakeAuthApi = FakeMainAuthApi().apply {
            shouldSucceed = false
            errorCode = 500
        }
        val viewModel = MainViewModel(tokenManager, fakeAuthApi)

        viewModel.registerDeviceToken("valid-fcm-token")

        assertEquals("Registration failed: 500", viewModel.registerStatus.value)
    }

    @Test
    fun testFetchUserProfileSuccess() = runTest {
        tokenManager.saveToken("valid-session-token")
        val fakeAuthApi = FakeMainAuthApi().apply {
            shouldSucceed = true
        }
        val viewModel = MainViewModel(tokenManager, fakeAuthApi)

        viewModel.fetchUserProfile()

        assertEquals("fake-user-id", viewModel.userProfile.value?.id)
        assertEquals("fake_user", viewModel.userProfile.value?.username)
        assertNull(viewModel.profileError.value)
        assertEquals(false, viewModel.isLoadingProfile.value)
        assertEquals("Bearer valid-session-token", fakeAuthApi.lastAuthHeader)
    }

    @Test
    fun testFetchUserProfileMissingSession() = runTest {
        tokenManager.deleteToken()
        val fakeAuthApi = FakeMainAuthApi()
        val viewModel = MainViewModel(tokenManager, fakeAuthApi)

        viewModel.fetchUserProfile()

        assertNull(viewModel.userProfile.value)
        assertEquals("Error: Session token is null", viewModel.profileError.value)
        assertEquals(false, viewModel.isLoadingProfile.value)
    }

    @Test
    fun testFetchUserProfileFailure() = runTest {
        tokenManager.saveToken("valid-session-token")
        val fakeAuthApi = FakeMainAuthApi().apply {
            shouldSucceed = false
            errorCode = 401
        }
        val viewModel = MainViewModel(tokenManager, fakeAuthApi)

        viewModel.fetchUserProfile()

        assertNull(viewModel.userProfile.value)
        assertEquals("Failed to load profile: 401", viewModel.profileError.value)
        assertEquals(false, viewModel.isLoadingProfile.value)
    }

    @Test
    fun testFetchUserProfileLoadsFromCacheImmediately() = runTest {
        val cachedProfile = UserProfileResponse(
            id = "cached-id",
            username = "cached_user",
            fullName = "Cached User Name",
            phoneNumber = "0901234567",
            role = "CITIZEN",
            email = "cached@example.com"
        )
        tokenManager.saveToken("valid-session-token")
        tokenManager.saveUserProfile(cachedProfile)

        val fakeAuthApi = FakeMainAuthApi().apply {
            shouldSucceed = true
        }

        // When VM initializes, it should load from cache instantly
        val viewModel = MainViewModel(tokenManager, fakeAuthApi)
        assertEquals("cached-id", viewModel.userProfile.value?.id)
        assertEquals("cached_user", viewModel.userProfile.value?.username)

        viewModel.fetchUserProfile()
        
        // After API response, it should update to API value
        assertEquals("fake-user-id", viewModel.userProfile.value?.id)
        assertEquals("fake-user-id", tokenManager.getUserProfile()?.id)
    }
}

private class FakeMainAuthApi : AuthApi {
    var shouldSucceed = true
    var errorCode = 400
    var lastAuthHeader: String? = null
    var lastRequest: PushTokenRequest? = null

    override suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return Response.error(400, ResponseBody.create(null, ""))
    }

    override suspend fun register(request: RegisterRequest): Response<RegisterResponse> {
        return Response.error(400, ResponseBody.create(null, ""))
    }

    override suspend fun registerPushToken(
        authHeader: String,
        request: PushTokenRequest
    ): Response<Unit> {
        lastAuthHeader = authHeader
        lastRequest = request
        return if (shouldSucceed) {
            Response.success(Unit)
        } else {
            Response.error(errorCode, ResponseBody.create(null, ""))
        }
    }

    override suspend fun deletePushToken(authHeader: String, fcmToken: String?): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun getUserProfile(authHeader: String): Response<UserProfileResponse> {
        lastAuthHeader = authHeader
        return if (shouldSucceed) {
            Response.success(
                UserProfileResponse(
                    id = "fake-user-id",
                    username = "fake_user",
                    fullName = "Fake User Name",
                    phoneNumber = "0901234567",
                    role = "CITIZEN",
                    email = "fake@example.com"
                )
            )
        } else {
            Response.error(errorCode, ResponseBody.create(null, ""))
        }
    }
}
