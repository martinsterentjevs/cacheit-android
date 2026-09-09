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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds


enum class NoteEditMode {
    Create,
    View,
    TextEdit,
    DrawingEdit,
}


sealed interface NoteEditUiState {

    data object Loading : NoteEditUiState

    data class NotFound(val couldNotConfirm: Boolean = false) : NoteEditUiState

    data class Ready(
        val mode: NoteEditMode,
        val note: FaceNote,
        val preEditNote: FaceNote?,

        val isSaving: Boolean = false,

        val isDrawingLocked: Boolean = false,
        val lockTtlRemaining: Duration? = null,
    ) : NoteEditUiState
}


sealed interface NoteEditUiEvent {

    /**
     * Used when the screen itself should be dismissed.
     *
     * This currently applies to:
     * - blank Create -> Back
     * - successful/unverified Create save if the screen chooses
     *   to treat creation as complete rather than entering View.
     */
    data object Dismiss : NoteEditUiEvent
}


@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val popupController: PopupController,
    private val securityService: SecurityService,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<NoteEditUiState>(NoteEditUiState.Loading)

    val uiState: StateFlow<NoteEditUiState> =
        _uiState.asStateFlow()

    private val _events =
        Channel<NoteEditUiEvent>(Channel.BUFFERED)

    val events =
        _events.receiveAsFlow()

    private var isLoadInFlight = false

    private var ttlJob: Job? = null

    private var isNewNote = false
    private val notifiedThresholds =
        mutableSetOf<Duration>()


    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    /**
     * null / NEW_NOTE_ID -> Create mode
     *
     * Existing note -> View mode
     */
    fun load(noteId: String? = null) {
        if (isLoadInFlight) return

        isLoadInFlight = true

        val realNoteId =
            noteId.takeIf { it != Route.NoteEdit.NEW_NOTE_ID }

        viewModelScope.launch {
            try {
                if (realNoteId == null) {
                    loadCreate()
                } else {
                    loadExisting(realNoteId)
                }
            } catch (ex: Exception) {
                popupController.show(
                    UiEvent.Snackbar(
                        R.string.snackbar_alert_accountId_unavailable
                    )
                )

                e(
                    TAG,
                    "load: Failed loading operation ${ex.message}"
                )
            } finally {
                isLoadInFlight = false
            }
        }
    }

    private fun loadCreate() {
        val accountId =
            securityService.getAccountId()
                ?: throw IllegalStateException("Account ID unavailable")

        val blank = FaceNote(
            noteId = UUID.randomUUID().toString(),
            userId = accountId,
            lastModifiedAt = Clock.System.now(),
            isDeleted = false,
            hasHistory = false,
            title = "",
            body = null,
            drawing = null,
            lockedByDeviceId = null,
            lockedAt = null,
        )
        isNewNote = true
        _uiState.value =
            NoteEditUiState.Ready(
                mode = NoteEditMode.Create,
                note = blank,
                preEditNote = null,
            )
    }

    private suspend fun loadExisting(noteId: String) {
        val cached = noteRepository.getLocalNote(noteId)

        if (cached != null) {
            isNewNote=false
            _uiState.value=NoteEditUiState.Ready(
                mode = NoteEditMode.View,
                note = cached,
                preEditNote = cached,
                isDrawingLocked = cached.lockedByDeviceId != null
            )
            return
        }
        // Not in local cache - doesn't necessarily mean it doesn't exist server-side. Confirm
        // with a refresh before declaring NotFound. Full getNotes() rather than a single-note
        // fetch - same stopgap already used in WsSessionManager/NudgeHandler today, until a
        // GET /notes/{id} endpoint exists server-side.
        isNewNote = false
        try {
            noteRepository.getNotes()
        } catch (ex: NoteFlowException) {
           // Couldn't confirm either way - don't claim the note doesn't exist.
           _uiState.value = NoteEditUiState.NotFound(couldNotConfirm = true)
           e(TAG, "loadExisting: Failed to confirm note",ex )
           return
        }

        val confirmed = noteRepository.getLocalNote(noteId)
        _uiState.value = if (confirmed != null) {
            NoteEditUiState.Ready(
                mode = NoteEditMode.View,
                note = confirmed,
                preEditNote = confirmed,
                isDrawingLocked = confirmed.lockedByDeviceId != null,
            )
        } else {
            NoteEditUiState.NotFound(couldNotConfirm = false)
        }
    }


    // -------------------------------------------------------------------------
    // Mode transitions
    // -------------------------------------------------------------------------

    /**
     * View -> TextEdit
     *
     * Create does not need this transition because Create starts with
     * text interaction enabled.
     */
    fun enterTextEdit() {
        val current = currentReady() ?: return
        when (current.mode) {
            NoteEditMode.DrawingEdit -> exitDrawingEdit(targetMode = NoteEditMode.TextEdit)
            NoteEditMode.View, NoteEditMode.Create -> updateReady { it.copy(mode = NoteEditMode.TextEdit) }
            NoteEditMode.TextEdit -> Unit
        }
    }

    /**
     * View/Create/TextEdit -> DrawingEdit.
     *
     * Existing notes acquire the server-side drawing lock.
     * Create has no server-side existence yet, therefore no lock is acquired.
     */
    fun enterDrawingEdit() {
        val current =
            currentReady() ?: return
        if (current.mode == NoteEditMode.DrawingEdit) {
            return
        }

        if (current.preEditNote == null || isNewNote) {
            // Create mode: purely local drawing editing.
            _uiState.update {
                (it as? NoteEditUiState.Ready)?.copy(
                    mode = NoteEditMode.DrawingEdit
                ) ?: it
            }

            return
        }

        acquireDrawingLockAndEnter()
    }

    private fun acquireDrawingLockAndEnter() {
        val current =
            currentReady() ?: return

        val noteId =
            current.note.noteId ?: return

        viewModelScope.launch {
            try {
                val locked =
                    noteRepository.acquireDrawingLock(noteId)

                _uiState.update { state ->
                    (state as? NoteEditUiState.Ready)?.copy(
                        mode = NoteEditMode.DrawingEdit,
                        isDrawingLocked = true,
                    ) ?: state
                }

                locked.lockedAt?.let {
                    startTtlCountdown(Instant.parse(it))
                }

            } catch (e: NoteFlowException) {
                popupController.show(
                    UiEvent.Snackbar(
                        R.string.snackbar_error,
                        listOf(e.userMessage)
                    )
                )
            }
        }
    }


    // -------------------------------------------------------------------------
    // Text / drawing input
    // -------------------------------------------------------------------------

    fun onTitleChanged(newTitle: String) {
        updateReady { state ->
            when (state.mode) {
                NoteEditMode.Create,
                NoteEditMode.TextEdit -> {
                    state.copy(
                        note = state.note.copy(
                            title = newTitle
                        )
                    )
                }

                NoteEditMode.View,
                NoteEditMode.DrawingEdit -> state
            }
        }
    }

    fun onBodyChanged(newBody: String) {
        updateReady { state ->
            when (state.mode) {
                NoteEditMode.Create,
                NoteEditMode.TextEdit -> {
                    state.copy(
                        note = state.note.copy(
                            body = newBody
                        )
                    )
                }

                NoteEditMode.View,
                NoteEditMode.DrawingEdit -> state
            }
        }
    }

    /**
     * Drawing input should eventually call this from the drawing editor.
     *
     * The VM intentionally treats drawing as part of the complete FaceNote,
     * rather than creating a separate save path.
     */
    fun onDrawingChanged(newDrawing: String?) {
        updateReady { state ->
            when (state.mode) {
                NoteEditMode.Create,
                NoteEditMode.DrawingEdit -> {
                    state.copy(
                        note = state.note.copy(
                            drawing = newDrawing
                        )
                    )
                }

                NoteEditMode.View,
                NoteEditMode.TextEdit -> state
            }
        }
    }


    // -------------------------------------------------------------------------
    // Back / mode exits
    // -------------------------------------------------------------------------

    /**
     * Called by NoteEditScreen's BackHandler whenever the current mode is
     * not View.
     */
    fun onBackPressed() {
        val current =
            currentReady() ?: return

        when (current.mode) {
            NoteEditMode.Create ->
                exitCreate()

            NoteEditMode.TextEdit ->
                exitTextEdit()

            NoteEditMode.DrawingEdit ->
                exitDrawingEdit()

            NoteEditMode.View ->
                _events.trySend(NoteEditUiEvent.Dismiss)
        }
    }

    /** Create:
     * blank -> dismiss immediately
     * content -> save using addNote, then View
     */
    fun exitCreate() {
        val current =
            currentReady() ?: return

        if (current.mode != NoteEditMode.Create) {
            return
        }

        if (isBlank(current.note)) {
            _events.trySend(NoteEditUiEvent.Dismiss)
        } else {
            saveAndExit(
                expectedMode = NoteEditMode.Create,
                saveAsNew = true,
                releaseDrawingLock = false,
            )
        }
    }

    /**
     * TextEdit:
     * save -> View
     * No drawing lock is released here because TextEdit does not own one.
     */
    fun exitTextEdit() {
        val current =
            currentReady() ?: return

        if (current.mode != NoteEditMode.TextEdit) {
            return
        }

        saveAndExit(
            expectedMode = NoteEditMode.TextEdit,
            saveAsNew = false,
            releaseDrawingLock = false,
        )
    }

    /**
     * DrawingEdit exit, with a configurable landing mode.
     *
     * Shared by two call sites that want different outcomes after the same
     * save-and-release-lock sequence:
     * - onBackPressed(): the user is exiting editing entirely -> View.
     * - enterTextEdit(): the user is switching to text editing, not
     *   exiting -> TextEdit.
     *
     * Previously this always landed on View regardless of caller, which
     * meant switching from DrawingEdit to TextEdit silently bounced to
     * read-only View instead - the save/lock-release side effects ran
     * correctly, but the mode transition the caller actually wanted never
     * happened.
     *
     * A genuine save failure leaves the mode and lock intact regardless of
     * [targetMode] - see handleSaveFailure.
     */
    fun exitDrawingEdit(targetMode: NoteEditMode = NoteEditMode.View) {
        val current =
            currentReady() ?: return

        if (current.mode != NoteEditMode.DrawingEdit) {
            return
        }

        saveAndExit(
            expectedMode = NoteEditMode.DrawingEdit,
            saveAsNew = isNewNote,
            releaseDrawingLock = true,
            targetMode = targetMode,
        )
    }


    // -------------------------------------------------------------------------
    // Saving
    // -------------------------------------------------------------------------

    /**
     * One whole-note write path.
     * There is deliberately no saveText() / saveDrawing().
     */
    private fun saveAndExit(
        expectedMode: NoteEditMode,
        saveAsNew: Boolean,
        releaseDrawingLock: Boolean,
        targetMode: NoteEditMode = NoteEditMode.View,
    ) {
        val current =
            currentReady() ?: return

        if (current.mode != expectedMode || current.isSaving) {
            return
        }

        _uiState.update { state ->
            (state as? NoteEditUiState.Ready)?.copy(
                isSaving = true
            ) ?: state
        }

        viewModelScope.launch {
            try {
                val result =
                    if (saveAsNew) {
                        noteRepository.addNote(current.note)
                    } else {
                        noteRepository.updateNote(
                            current.note.noteId!!,
                            current.note
                        )
                    }

                when (result) {

                    is NoteWriteResult.Verified -> {
                        handleVerifiedSave(
                            result.note,
                            releaseDrawingLock,
                            targetMode,
                        )
                    }

                    NoteWriteResult.Unverified -> {
                        handleUnverifiedSave(
                            releaseDrawingLock,
                            targetMode,
                        )
                    }
                }
                isNewNote = false
            } catch (e: NoteFlowException) {
                handleSaveFailure(e)
            }
        }
    }

    private suspend fun handleVerifiedSave(
        savedNote: FaceNote,
        releaseDrawingLock: Boolean,
        targetMode: NoteEditMode,
    ) {
        if (releaseDrawingLock) {
            releaseIfHeld()
        }

        _uiState.update { state ->
            (state as? NoteEditUiState.Ready)?.copy(
                mode = targetMode,
                note = savedNote,
                preEditNote = savedNote,
                isSaving = false,
            ) ?: state
        }
    }

    private suspend fun handleUnverifiedSave(
        releaseDrawingLock: Boolean,
        targetMode: NoteEditMode,
    ) {
        /* ADR 0002 semantics:
         * Unverified means the server almost certainly accepted the write.
         * Therefore it is safe to release the drawing lock and transition to
         * targetMode. Do NOT restore preEditNote here.
         */
        if (releaseDrawingLock) {
            releaseIfHeld()
        }

        _uiState.update { state ->
            (state as? NoteEditUiState.Ready)?.copy(
                mode = targetMode,
                isSaving = false,
            ) ?: state
        }

        popupController.show(
            UiEvent.Snackbar(
                R.string.note_version_restore_unconfirmed
            )
        )
    }

    private fun handleSaveFailure(
        exception: NoteFlowException,
    ) {
        /*
         * Genuine failure:
         *
         * - do not release DrawingEdit's lock
         * - do not enter View
         * - keep the edited note in memory
         * - allow retry
         */
        _uiState.update { state ->
            (state as? NoteEditUiState.Ready)?.copy(
                isSaving = false
            ) ?: state
        }

        popupController.show(
            UiEvent.Snackbar(
                R.string.snackbar_error,
                listOf(exception.userMessage)
            )
        )
    }


    // -------------------------------------------------------------------------
    // Drawing lock
    // -------------------------------------------------------------------------

    /**
     * Releases the lock only when the note actually existed server-side.
     *
     * Create mode never has a server-side lock.
     */
    private suspend fun releaseIfHeld() {
        val current =
            currentReady() ?: return

        if (
            current.preEditNote != null &&
            current.isDrawingLocked
        ) {
            releaseDrawingLockInternal(
                current.note.noteId ?: return
            )
        }
    }

    private suspend fun releaseDrawingLockInternal(
        noteId: String,
    ) {
        try {
            noteRepository.releaseDrawingLock(noteId)
        } catch (e: NoteFlowException) {
            popupController.show(
                UiEvent.Snackbar(
                    R.string.snackbar_error,
                    listOf(e.userMessage)
                )
            )
        } finally {
            stopTtlCountdown()

            _uiState.update { state ->
                (state as? NoteEditUiState.Ready)?.copy(
                    isDrawingLocked = false,
                    lockTtlRemaining = null,
                ) ?: state
            }
        }
    }


    // -------------------------------------------------------------------------
    // Lock TTL
    // -------------------------------------------------------------------------

    private fun startTtlCountdown(
        lockedAt: Instant,
    ) {
        ttlJob?.cancel()
        notifiedThresholds.clear()

        ttlJob =
            viewModelScope.launch {
                while (isActive) {

                    val remaining =
                        Duration.between(
                            Instant.now(),
                            lockedAt.plusSeconds(
                                DRAWING_LOCK_TTL_SECONDS
                            )
                        )

                    if (remaining.isNegative || remaining.isZero) {
                        _uiState.update { state ->
                            (state as? NoteEditUiState.Ready)?.copy(
                                isDrawingLocked = false,
                                lockTtlRemaining = null,
                            ) ?: state
                        }

                        popupController.show(
                            UiEvent.Snackbar(
                                R.string.snackbar_drawing_lock_expired
                            )
                        )

                        break
                    }

                    _uiState.update { state ->
                        (state as? NoteEditUiState.Ready)?.copy(
                            lockTtlRemaining = remaining
                        ) ?: state
                    }

                    ALERT_THRESHOLDS
                        .firstOrNull {
                            it >= remaining &&
                                    it !in notifiedThresholds
                        }
                        ?.let { threshold ->
                            notifiedThresholds += threshold

                            popupController.show(
                                UiEvent.Snackbar(
                                    R.string.snackbar_alert_drawing_lock_time,
                                    listOf(threshold.toMinutes())
                                )
                            )
                        }

                    delay(
                        (
                                if (remaining <= Duration.ofSeconds(30)) {
                                    1_000
                                } else {
                                    5_000
                                }
                                ).milliseconds
                    )
                }
            }
    }

    private fun stopTtlCountdown() {
        ttlJob?.cancel()
        ttlJob = null
        notifiedThresholds.clear()
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun currentReady(): NoteEditUiState.Ready? =
        _uiState.value as? NoteEditUiState.Ready

    private fun updateReady(
        transform: (NoteEditUiState.Ready) -> NoteEditUiState.Ready,
    ) {
        _uiState.update { state ->
            (state as? NoteEditUiState.Ready)
                ?.let(transform)
                ?: state
        }
    }

    private fun isBlank(note: FaceNote): Boolean =
        note.title.isBlank() &&
                note.body.isNullOrBlank() &&
                note.drawing.isNullOrBlank()


    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        stopTtlCountdown()
        super.onCleared()
    }


    companion object {

        // Must match the server's DRAWING_LOCK_TTL_SECONDS constant.
        private const val DRAWING_LOCK_TTL_SECONDS = 3600L

        private val ALERT_THRESHOLDS =
            listOf(
                Duration.ofMinutes(5),
                Duration.ofMinutes(2),
                Duration.ofMinutes(1),
                Duration.ofSeconds(30),
            )
    }
}