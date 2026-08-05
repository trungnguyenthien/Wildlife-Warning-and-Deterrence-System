package com.wildlife.deterrence.viewmodel

import com.wildlife.deterrence.data.SmsApi
import com.wildlife.deterrence.data.SmsRecipientRequest
import com.wildlife.deterrence.data.SmsRecipientResponse
import com.wildlife.deterrence.data.TokenManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
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

class SmsSetupViewModelTest {

    private val tokenManager = TokenManager(null).apply {
        saveToken("test-jwt-token-sms")
    }

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
    fun TC_SMS_LOAD_SUCCESS() = runTest {
        val fakeRecipients = listOf(
            SmsRecipientResponse("id-1", "user-1", "Nguyen Van A", "+84905111222", "family", "2026-07-31T00:00:00Z"),
            SmsRecipientResponse("id-2", "user-1", "Tran Thi B", "+84905222333", "family", "2026-07-31T00:00:00Z")
        )

        val fakeSmsApi = object : FakeSmsApi() {
            override suspend fun getSmsRecipients(token: String): List<SmsRecipientResponse> {
                return fakeRecipients
            }
        }

        val viewModel = SmsSetupViewModel(tokenManager, fakeSmsApi)
        viewModel.loadRecipients()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.recipients.size)
        assertEquals("Nguyen Van A", state.recipients[0].fullName)
        assertEquals("+84905111222", state.recipients[0].phoneNumber)
    }

    @Test
    fun TC_SMS_ADD_SUCCESS() = runTest {
        var apiCalled = false
        val fakeSmsApi = object : FakeSmsApi() {
            override suspend fun getSmsRecipients(token: String): List<SmsRecipientResponse> {
                return emptyList()
            }
            override suspend fun addSmsRecipient(token: String, request: SmsRecipientRequest): SmsRecipientResponse {
                apiCalled = true
                assertEquals("Le Van C", request.fullName)
                assertEquals("+84905333444", request.phoneNumber)
                return SmsRecipientResponse("id-3", "user-1", request.fullName, request.phoneNumber, request.relation, "2026-07-31")
            }
        }

        val viewModel = SmsSetupViewModel(tokenManager, fakeSmsApi)
        var successTriggered = false
        viewModel.addRecipient("Le Van C", "+84905333444") {
            successTriggered = true
        }

        assertTrue(apiCalled)
        assertTrue(successTriggered)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun TC_SMS_ADD_VALIDATION_FAILURE() = runTest {
        val fakeSmsApi = object : FakeSmsApi() {
            override suspend fun getSmsRecipients(token: String): List<SmsRecipientResponse> = emptyList()
        }

        val viewModel = SmsSetupViewModel(tokenManager, fakeSmsApi)

        // Test 1: Empty Name
        viewModel.addRecipient("", "+84905333444") {}
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.isNotEmpty())

        // Test 2: Invalid Phone Format (no plus sign)
        viewModel.addRecipient("Le Van C", "0905333444") {}
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.isNotEmpty())

        // Test 3: Invalid Phone Format (contains letters)
        viewModel.addRecipient("Le Van C", "+84a") {}
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun TC_SMS_DELETE_SUCCESS() = runTest {
        var deleteCalled = false
        val fakeSmsApi = object : FakeSmsApi() {
            override suspend fun getSmsRecipients(token: String): List<SmsRecipientResponse> = emptyList()
            override suspend fun deleteSmsRecipient(token: String, recipientId: String): Response<Unit> {
                deleteCalled = true
                assertEquals("id-to-delete", recipientId)
                return Response.success(Unit)
            }
        }

        val viewModel = SmsSetupViewModel(tokenManager, fakeSmsApi)
        var successTriggered = false
        viewModel.deleteRecipient("id-to-delete") {
            successTriggered = true
        }

        assertTrue(deleteCalled)
        assertTrue(successTriggered)
    }

    @Test
    fun TC_SMS_UPDATE_SUCCESS() = runTest {
        var deleteCalled = false
        var addCalled = false
        val fakeSmsApi = object : FakeSmsApi() {
            override suspend fun getSmsRecipients(token: String): List<SmsRecipientResponse> = emptyList()
            override suspend fun deleteSmsRecipient(token: String, recipientId: String): Response<Unit> {
                deleteCalled = true
                assertEquals("old-id", recipientId)
                return Response.success(Unit)
            }
            override suspend fun addSmsRecipient(token: String, request: SmsRecipientRequest): SmsRecipientResponse {
                addCalled = true
                assertEquals("Ten Moi", request.fullName)
                assertEquals("+84905999999", request.phoneNumber)
                return SmsRecipientResponse("new-id", "user-1", request.fullName, request.phoneNumber, request.relation, "2026-07-31")
            }
        }

        val viewModel = SmsSetupViewModel(tokenManager, fakeSmsApi)
        var successTriggered = false
        viewModel.updateRecipient("old-id", "Ten Moi", "+84905999999") {
            successTriggered = true
        }

        assertTrue(deleteCalled)
        assertTrue(addCalled)
        assertTrue(successTriggered)
    }
}

open class FakeSmsApi : SmsApi {
    override suspend fun getSmsRecipients(token: String): List<SmsRecipientResponse> {
        throw NotImplementedError()
    }
    override suspend fun addSmsRecipient(token: String, request: SmsRecipientRequest): SmsRecipientResponse {
        throw NotImplementedError()
    }
    override suspend fun deleteSmsRecipient(token: String, recipientId: String): Response<Unit> {
        throw NotImplementedError()
    }
}
