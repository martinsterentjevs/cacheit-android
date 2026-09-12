package com.martinsterentjevs.cacheit.ui.account

import app.cash.turbine.test
import com.martinsterentjevs.cacheit.data.auth.AuthFlowException
import com.martinsterentjevs.cacheit.data.auth.AuthRepository
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.services.websockets.WsSessionManager
import com.martinsterentjevs.cacheit.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountOverviewViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var securityService: SecurityService
    private lateinit var wsManager: WsSessionManager
    private lateinit var viewModel: AccountOverviewViewModel

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        securityService = mockk(relaxed = true)
        wsManager = mockk(relaxed = true)
        viewModel = AccountOverviewViewModel(authRepository, securityService, wsManager)
    }

    @Test
    fun `onLogoutTapped clears the local session and emits LoggedOut when the server call succeeds`() = runTest {
        viewModel.events.test {
            viewModel.onLogoutTapped()
            advanceUntilIdle()
            assertEquals(AccountOverviewEvent.LoggedOut, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { authRepository.logout() }
        verify { wsManager.disconnect() }
        verify { securityService.clearSession() }
        verify { securityService.clearSecureMek() }
    }

    @Test
    fun `onLogoutTapped still clears the local session and emits LoggedOut when the server call fails`() = runTest {
        coEvery { authRepository.logout() } throws AuthFlowException("Couldn't log out - try again")

        viewModel.events.test {
            viewModel.onLogoutTapped()
            advanceUntilIdle()
            assertEquals(AccountOverviewEvent.LoggedOut, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Local teardown is unconditional - a best-effort server revoke failing must never
        // leave the user stuck unable to log out on-device.
        verify { wsManager.disconnect() }
        verify { securityService.clearSession() }
        verify { securityService.clearSecureMek() }
    }
}