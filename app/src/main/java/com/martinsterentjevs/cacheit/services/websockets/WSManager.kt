package com.martinsterentjevs.cacheit.services.websockets

import android.util.Log
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NotesChangeSignal
import com.martinsterentjevs.cacheit.network.websockets.CacheItWebSocketClient
import com.martinsterentjevs.cacheit.network.websockets.WsConnectionState
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class WsSessionManager @Inject constructor(
    private val wsClient: CacheItWebSocketClient,
    private val noteRepository: NoteRepository,
    private val notesChangeSignal: NotesChangeSignal,
    private val appScope: CoroutineScope
) {
    private var connectionJob: Job? = null

    fun connectIfNeeded() {
        if (connectionJob?.isActive == true) return

        connectionJob = appScope.launch {
            launch { syncOnEveryReady() }

            var attempt = 0
            while (isActive) {
                wsClient.connect()

                if (!isActive) break

                val state = wsClient.connectionState.value
                if (state is WsConnectionState.Failed && isAuthFailure(state.error)) {
                    Log.w("WS", "Auth failure — stopping reconnect loop until next login")
                    break
                }

                attempt++
                val delayMs = backoffDelay(attempt)
                Log.d("WS", "Disconnected, retrying in ${delayMs}ms (attempt $attempt)")
                delay(delayMs.milliseconds)
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        appScope.launch { wsClient.disconnect() }
    }

    private suspend fun syncOnEveryReady() {
        wsClient.connectionState.collect { state ->
            if (state is WsConnectionState.Ready) {
                try {
                    noteRepository.getNotes()
                    notesChangeSignal.notify()
                } catch (e: Exception) {
                    Log.e("WS", "Post-connect sync failed", e)
                }
            }
        }
    }

    // internal, not private - needs to be visible to WsSessionManagerTest
    internal fun backoffDelay(attempt: Int): Long {
        val base = 1_000L
        val max = 60_000L
        val exp = base * (1L shl (attempt - 1).coerceAtMost(6))
        return exp.coerceAtMost(max)
    }

    internal fun isAuthFailure(error: Throwable): Boolean =
        error.message?.contains("401") == true
}