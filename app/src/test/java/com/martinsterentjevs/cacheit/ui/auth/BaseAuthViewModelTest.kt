package com.martinsterentjevs.cacheit.ui.auth

import app.cash.turbine.test
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.auth.AuthFlowException
import com.martinsterentjevs.cacheit.services.websockets.WsSessionManager
import com.martinsterentjevs.cacheit.testutil.AndroidLogRule
import com.martinsterentjevs.cacheit.testutil.MainDispatcherRule
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

private class TestAuthViewModel(
    popupController: PopupController,
    wsManager: WsSessionManager,
) : BaseAuthViewModel(popupController, wsManager) {
    fun run(block: suspend () -> Unit) = launchAuthFlow(block)
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseAuthViewModelTest {

    // Unconfined, not the default Standard - see file header. launchAuthFlow's guard needs the
    // first call's coroutine to have actually reached `_uiState.value = Loading` before the
    // second call's synchronous guard check can see it.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule
    val androidLogRule = AndroidLogRule()

    private lateinit var popupController: PopupController
    private lateinit var wsManager: WsSessionManager
    private lateinit var viewModel: TestAuthViewModel

    @Before
    fun setUp() {
        popupController = mockk(relaxed = true)
        wsManager = mockk(relaxed = true)
        viewModel = TestAuthViewModel(popupController, wsManager)
    }

    @Test
    fun `a successful flow sends Success, connects the websocket, and returns to Idle`() = runTest {
        viewModel.events.test {
            viewModel.run { /* succeeds immediately */ }
            assertEquals(AuthEvent.Success, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        verify { wsManager.connectIfNeeded() }
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `an AuthFlowException shows its own user-facing message and does not connect the websocket`() = runTest {
        viewModel.run { throw AuthFlowException("Incorrect email or password") }
        advanceUntilIdle()

        verify {
            popupController.show(
                UiEvent.Snackbar(
                    messageId = R.string.snackbar_error,
                    formatArgs = listOf("Incorrect email or password"),
                ),
            )
        }
        verify(exactly = 0) { wsManager.connectIfNeeded() }
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `an unexpected exception logs it and shows the generic error snackbar`() = runTest {
        viewModel.run { throw RuntimeException("something unrelated broke") }
        advanceUntilIdle()

        verify {
            popupController.show(UiEvent.Snackbar(messageId = R.string.snackbar_error_general))
        }
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `a second submit while one is already in flight is a no-op`() = runTest {
        val invocations = AtomicInteger(0)

        viewModel.run {
            invocations.incrementAndGet()
            delay(100.milliseconds)
        }
        viewModel.run { invocations.incrementAndGet() } // fired while the first is still Loading

        advanceUntilIdle()

        assertEquals(1, invocations.get())
    }
}