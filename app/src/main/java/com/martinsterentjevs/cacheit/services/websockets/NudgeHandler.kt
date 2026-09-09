package com.martinsterentjevs.cacheit.services.websockets

import android.util.Log
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NotesChangeSignal
import com.martinsterentjevs.cacheit.network.websockets.CacheItWebSocketClient
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class NudgeHandler @Inject constructor(
    private val wsClient: CacheItWebSocketClient,
    private val noteRepository: NoteRepository,
    private val notesChangeSignal: NotesChangeSignal,
    private val appScope: CoroutineScope
) {
    fun start() {
        appScope.launch {
            wsClient.nudges.collect { nudge ->
                try {
                    noteRepository.getNotes()
                    notesChangeSignal.notify()
                } catch (e: Exception) {
                    Log.e("WS", "Failed to refresh notes after nudge (${nudge.type})", e)
                }
            }
        }
    }
}