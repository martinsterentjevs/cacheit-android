package com.martinsterentjevs.cacheit.ui.auth

import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.auth.AuthRepository
import com.martinsterentjevs.cacheit.data.auth.AuthSession
import com.martinsterentjevs.cacheit.services.crypto.CryptoService
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.services.websockets.WsSessionManager
import com.martinsterentjevs.cacheit.testutil.AndroidLogRule
import com.martinsterentjevs.cacheit.testutil.MainDispatcherRule
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val androidLogRule = AndroidLogRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var cryptoService: CryptoService
    private lateinit var securityService: SecurityService
    private lateinit var popupController: PopupController
    private lateinit var wsManager: WsSessionManager
    private lateinit var viewModel: RegistrationViewModel

    private val salt = byteArrayOf(1, 2, 3)
    private val mek = byteArrayOf(4, 5, 6)
    private val authHash = byteArrayOf(7, 8, 9)
    private val muk = byteArrayOf(10, 11, 12)

    @Before
    fun setUp() {
        authRepository = mockk()
        cryptoService = mockk()
        securityService = mockk(relaxed = true)
        popupController = mockk(relaxed = true)
        wsManager = mockk(relaxed = true)
        viewModel = RegistrationViewModel(authRepository, cryptoService, securityService, popupController, wsManager)
    }

    @Test
    fun `submit shows a blank-fields snackbar when any required field is blank`() = runTest {
        viewModel.submit(name = "", username = "user", email = "user@cacheit.test", password = "pw123456", confirmPassword = "pw123456")
        advanceUntilIdle()

        verify { popupController.show(UiEvent.Snackbar(messageId = R.string.snackbar_error_blank_fields)) }
        coVerify(exactly = 0) { authRepository.register(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `submit shows a password-mismatch snackbar when the passwords differ`() = runTest {
        viewModel.submit(
            name = "Test User",
            username = "user",
            email = "user@cacheit.test",
            password = "pw123456",
            confirmPassword = "different",
        )
        advanceUntilIdle()

        verify { popupController.show(UiEvent.Snackbar(messageId = R.string.snackbar_error_password_mismatch)) }
        coVerify(exactly = 0) { authRepository.register(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `submit generates keys, registers, and stores the session on success`() = runTest {
        every { cryptoService.generateSalt() } returns salt
        every { cryptoService.generateMek() } returns mek
        every { cryptoService.hashPassword("pw123456") } returns authHash
        every { cryptoService.hashMek("pw123456") } returns muk
        every { cryptoService.wrapMek(mek, muk) } returns "wrapped-mek-envelope"
        every { securityService.getOrCreateDeviceId() } returns "device-1"
        every { securityService.getDeviceName() } returns "Pixel Test Device"
        val session = AuthSession(
            accountId = "account-1",
            deviceId = "device-1",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            wrappedMek = "wrapped-mek-envelope",
        )
        coEvery {
            authRepository.register(
                "Test User", "user", "user@cacheit.test", salt, authHash, "wrapped-mek-envelope", "device-1", "Pixel Test Device",
            )
        } returns session

        viewModel.submit(
            name = "Test User",
            username = "user",
            email = "user@cacheit.test",
            password = "pw123456",
            confirmPassword = "pw123456",
        )
        advanceUntilIdle()

        verify { securityService.setSecureMek(mek) }
        verify { securityService.setSession("account-1", "access-token", "refresh-token") }
        verify { wsManager.connectIfNeeded() }
    }
}