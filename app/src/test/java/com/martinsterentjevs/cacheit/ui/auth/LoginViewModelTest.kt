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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val androidLogRule = AndroidLogRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var cryptoService: CryptoService
    private lateinit var securityService: SecurityService
    private lateinit var popupController: PopupController
    private lateinit var wsManager: WsSessionManager
    private lateinit var viewModel: LoginViewModel

    private val salt = byteArrayOf(1, 2, 3)
    private val authHash = byteArrayOf(4, 5, 6)
    private val muk = byteArrayOf(7, 8, 9)
    private val mek = byteArrayOf(10, 11, 12)

    @Before
    fun setUp() {
        authRepository = mockk()
        cryptoService = mockk()
        securityService = mockk(relaxed = true)
        popupController = mockk(relaxed = true)
        wsManager = mockk(relaxed = true)
        viewModel = LoginViewModel(authRepository, cryptoService, securityService, popupController, wsManager)
    }

    @Test
    fun `submit shows a missing-credentials snackbar and calls nothing else when the identifier is blank`() = runTest {
        viewModel.submit(identifier = "", password = "password123")
        advanceUntilIdle()

        verify { popupController.show(UiEvent.Snackbar(messageId = R.string.snackbar_error_missing_credentials)) }
        coVerify(exactly = 0) { authRepository.fetchSalt(any()) }
    }

    @Test
    fun `submit shows a missing-credentials snackbar and calls nothing else when the password is blank`() = runTest {
        viewModel.submit(identifier = "user@cacheit.test", password = "")
        advanceUntilIdle()

        verify { popupController.show(UiEvent.Snackbar(messageId = R.string.snackbar_error_missing_credentials)) }
        coVerify(exactly = 0) { authRepository.fetchSalt(any()) }
    }

    @Test
    fun `submit derives keys, authenticates, and stores the unwrapped session on success`() = runTest {
        coEvery { authRepository.fetchSalt("user@cacheit.test") } returns salt
        every { cryptoService.hashPassword("password123") } returns authHash
        every { cryptoService.hashMek("password123") } returns muk
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
            authRepository.login("user@cacheit.test", authHash, "device-1", "Pixel Test Device")
        } returns session
        every { cryptoService.unwrapMek("wrapped-mek-envelope", muk) } returns mek

        viewModel.submit(identifier = "user@cacheit.test", password = "password123")
        advanceUntilIdle()

        verify { securityService.setSalt(salt) }
        verify { securityService.setSecureMek(mek) }
        verify { securityService.setSession("account-1", "access-token", "refresh-token") }
        verify { wsManager.connectIfNeeded() } // via BaseAuthViewModel's shared success path
    }
}