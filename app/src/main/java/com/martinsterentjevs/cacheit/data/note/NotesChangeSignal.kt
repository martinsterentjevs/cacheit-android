package com.martinsterentjevs.cacheit.data.note

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fired when notes changed due to a BACKGROUND event (WS nudge, post-reconnect sync) — never
 * fired by a user-initiated load()/refresh(), which already has fresh data on screen by the
 * time it completes. Listened to by NoteListViewModel (and any future screen showing note
 * data) so background updates surface without the user needing to pull-to-refresh.
 */
@Singleton
class NotesChangeSignal @Inject constructor() {
    private val _changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changed: SharedFlow<Unit> = _changed.asSharedFlow()

    suspend fun notify() {
        _changed.emit(Unit)
    }
}