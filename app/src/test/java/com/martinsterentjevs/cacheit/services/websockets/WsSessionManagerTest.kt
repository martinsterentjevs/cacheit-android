package com.martinsterentjevs.cacheit.services.websockets

import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NotesChangeSignal
import com.martinsterentjevs.cacheit.network.websockets.CacheItWebSocketClient
import io.mockk.mockk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers only the pure decision logic (backoff timing, auth-failure detection) - NOT the
 * actual reconnect loop, which needs coroutine-timing control and a fake CacheItWebSocketClient
 * that can simulate connect()/disconnect()/failure sequences. That's tracked separately under
 * the Android ViewModel/repository test backfill item, not duplicated here.
 */
class WsSessionManagerTest {

    private lateinit var manager: WsSessionManager

    @Before
    fun setUp() {
        manager = WsSessionManager(
            wsClient = mockk<CacheItWebSocketClient>(relaxed = true),
            noteRepository = mockk<NoteRepository>(relaxed = true),
            notesChangeSignal = mockk<NotesChangeSignal>(relaxed = true),
            appScope = CoroutineScope(SupervisorJob())
        )
    }

    @Test
    fun `backoff starts at 1 second on the first attempt`() {
        assertEquals(1_000L, manager.backoffDelay(1))
    }

    @Test
    fun `backoff doubles each attempt`() {
        assertEquals(1_000L, manager.backoffDelay(1))
        assertEquals(2_000L, manager.backoffDelay(2))
        assertEquals(4_000L, manager.backoffDelay(3))
        assertEquals(8_000L, manager.backoffDelay(4))
    }

    @Test
    fun `backoff never exceeds 60 seconds even at high attempt counts`() {
        assertEquals(60_000L, manager.backoffDelay(10))
        assertEquals(60_000L, manager.backoffDelay(100))
    }

    @Test
    fun `backoff does not overflow or go negative at extreme attempt counts`() {
        val delay = manager.backoffDelay(Int.MAX_VALUE)
        assertTrue(delay in 1_000L..60_000L)
    }

    @Test
    fun `401 in the error message is recognised as an auth failure`() {
        val error = IllegalStateException(
            "Handshake exception, expected status code 101 but was 401"
        )
        assertTrue(manager.isAuthFailure(error))
    }

    @Test
    fun `a network error without 401 is not treated as an auth failure`() {
        val error = java.net.ConnectException("Connection refused")
        assertFalse(manager.isAuthFailure(error))
    }

    @Test
    fun `an error with a null message is not treated as an auth failure`() {
        val error = object : Throwable() {
            override val message: String? = null
        }
        assertFalse(manager.isAuthFailure(error))
    }
}