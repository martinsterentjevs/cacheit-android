package com.martinsterentjevs.cacheit.ui.note

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log.e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.note.FaceNote
import com.martinsterentjevs.cacheit.data.note.NoteFlowException
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NoteWriteResult
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import com.martinsterentjevs.cacheit.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

sealed interface NoteEditUiState {
    data object Loading : NoteEditUiState   // edit mode only - reading from local cache
    data object NotFound : NoteEditUiState  // edit mode only - noteId wasn't in the cache
    data class Ready(
        val note: FaceNote,
        val preEditNote: FaceNote?,   // null in create mode
        val isSaving: Boolean = false,
        val isDrawingLocked: Boolean = false,
        val lockTtlRemaining: Duration? = null,
    ) : NoteEditUiState
}
sealed interface NoteEditUiEvent{
    data object Success: NoteEditUiEvent
}

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val popupController: PopupController, val securityService: SecurityService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteEditUiState>(NoteEditUiState.Loading)
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    val _events = Channel<NoteEditUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var isLoadInFlight = false
    private var ttlJob: Job? = null
    private val notifiedThresholds = mutableSetOf<Duration>()

    /** noteId null -> create mode. noteId present -> edit mode, loaded from local cache only. */
    fun load(noteId: String? = null) {
        if (isLoadInFlight) return
        isLoadInFlight = true

        val realNoteId = noteId.takeIf { it != Route.NoteEdit.NEW_NOTE_ID }

        viewModelScope.launch {
            try {
                if (realNoteId == null) {
                    val blank = FaceNote(
                        noteId = UUID.randomUUID().toString(),
                        userId = securityService.getAccountId() ?: throw Exception(),
                        lastModifiedAt = Instant.now().toString(),
                        isDeleted = false,
                        hasHistory = false,
                        title = "",
                        body = null,
                        drawing = null,
                        lockedByDeviceId = null,
                        lockedAt = null,
                    )
                    _uiState.value = NoteEditUiState.Ready(note = blank, preEditNote = null)
                } else {
                    val note = noteRepository.getLocalNote(realNoteId)
                    _uiState.value = if (note == null) {
                        NoteEditUiState.NotFound
                    } else {
                        NoteEditUiState.Ready(
                            note = note,
                            preEditNote = note,
                            isDrawingLocked = note.lockedByDeviceId != null,
                        )
                    }
                }
                isLoadInFlight = false
            } catch (ex: Exception) {
                popupController.show(UiEvent.Snackbar(R.string.snackbar_alert_accountId_unavailable))
                e(TAG, "load: Failed Loading operation ${ex.message}")
            } finally {
                isLoadInFlight = false
            }
        }
    }
    fun save() {
        val current = _uiState.value as? NoteEditUiState.Ready ?: return
        _uiState.update { (it as? NoteEditUiState.Ready)?.copy(isSaving = true) ?: it }

        viewModelScope.launch {
            try {
                val result = if (current.preEditNote == null) {
                    noteRepository.addNote(current.note)
                } else {
                    noteRepository.updateNote(current.note.noteId!!, current.note)
                }

                when (result) {
                    is NoteWriteResult.Verified -> {
                        _uiState.update {
                            (it as? NoteEditUiState.Ready)?.copy(note = result.note, isSaving = false) ?: it
                        }
                        _events.send(NoteEditUiEvent.Success)
                    }
                    NoteWriteResult.Unverified -> {
                        _uiState.update { state ->
                            (state as? NoteEditUiState.Ready)?.copy(
                                note = current.preEditNote ?: state.note,
                                isSaving = false,
                            ) ?: state
                        }
                        popupController.show(UiEvent.Snackbar(R.string.note_version_restore_unconfirmed))
                    }
                }
            } catch (e: NoteFlowException) {
                _uiState.update { (it as? NoteEditUiState.Ready)?.copy(isSaving = false) ?: it }
                popupController.show(UiEvent.Snackbar(R.string.snackbar_error,listOf(e.userMessage)))
            }
        }
    }

    fun acquireDrawingLock() {
        val current = _uiState.value as? NoteEditUiState.Ready ?: return
        val noteId = current.note.noteId ?: return

        viewModelScope.launch {
            try {
                val locked = noteRepository.acquireDrawingLock(noteId)
                _uiState.update { (it as? NoteEditUiState.Ready)?.copy(isDrawingLocked = true) ?: it }
                locked.lockedAt?.let { startTtlCountdown(Instant.parse(it)) }
            } catch (e: NoteFlowException) {
                popupController.show(UiEvent.Snackbar(R.string.snackbar_error,
                    listOf(e.userMessage)
                ))
            }
        }
    }

    fun releaseDrawingLock() {
        val current = _uiState.value as? NoteEditUiState.Ready ?: return
        val noteId = current.note.noteId ?: return

        viewModelScope.launch {
            try {
                noteRepository.releaseDrawingLock(noteId)
            } catch (e: NoteFlowException) {
                popupController.show(UiEvent.Snackbar(R.string.snackbar_error,listOf(e.userMessage)))
            } finally {
                stopTtlCountdown()
                _uiState.update {
                    (it as? NoteEditUiState.Ready)?.copy(isDrawingLocked = false, lockTtlRemaining = null) ?: it
                }
            }
        }
    }

    private fun startTtlCountdown(lockedAt: Instant) {
        ttlJob?.cancel()
        notifiedThresholds.clear()

        ttlJob = viewModelScope.launch {
            while (isActive) {
                val remaining = Duration.between(Instant.now(), lockedAt.plusSeconds(DRAWING_LOCK_TTL_SECONDS))

                if (remaining.isNegative || remaining.isZero) {
                    _uiState.update {
                        (it as? NoteEditUiState.Ready)?.copy(isDrawingLocked = false, lockTtlRemaining = null) ?: it
                    }
                    popupController.show(UiEvent.Snackbar(R.string.snackbar_drawing_lock_expired))
                    break
                }

                _uiState.update {
                    (it as? NoteEditUiState.Ready)?.copy(lockTtlRemaining = remaining) ?: it
                }

                // Fire at most one alert per tick, even if the app was backgrounded past
                // several thresholds at once - don't stack popups on returning to foreground.
                ALERT_THRESHOLDS.firstOrNull { it >= remaining && it !in notifiedThresholds }?.let { threshold ->
                    notifiedThresholds += threshold
                    popupController.show(
                        UiEvent.Snackbar(
                                R.string.snackbar_alert_drawing_lock_time,
                                listOf(threshold.toMinutes())
                            )
                    )
                }

                delay((if (remaining <= Duration.ofSeconds(30)) 1_000 else 5_000).milliseconds)
            }
        }
    }
    fun onTitleChanged(newTitle: String) {
        _uiState.update { (it as? NoteEditUiState.Ready)?.let { s -> s.copy(note = s.note.copy(title = newTitle)) } ?: it }
    }

    fun onBodyChanged(newBody: String) {
        _uiState.update { (it as? NoteEditUiState.Ready)?.let { s -> s.copy(note = s.note.copy(body = newBody)) } ?: it }
    }
    private fun stopTtlCountdown() {
        ttlJob?.cancel()
        ttlJob = null
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        stopTtlCountdown()
        super.onCleared()
    }

    companion object {
        // Must match the server's DRAWING_LOCK_TTL_SECONDS constant (3600L) - not shared code
        // between repos, so this has to be kept manually in sync if the server value changes.
        private const val DRAWING_LOCK_TTL_SECONDS = 3600L

        private val ALERT_THRESHOLDS = listOf(
            Duration.ofMinutes(5), Duration.ofMinutes(2), Duration.ofMinutes(1), Duration.ofSeconds(30),
        )
    }
}
