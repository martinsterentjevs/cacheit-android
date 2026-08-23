package com.martinsterentjevs.cacheit.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.data.note.FaceNoteVersion
import com.martinsterentjevs.cacheit.data.note.NoteFlowException
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NoteWriteResult
import com.martinsterentjevs.cacheit.network.note.NoteVersionMeta
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NoteVersionUiState {
    data object Loading : NoteVersionUiState
    data object Empty : NoteVersionUiState
    data class Content(
        val versions: List<NoteVersionMeta>,
        val previewedVersion: FaceNoteVersion? = null,
        val previewedVersionId: String? = null,
        val isRestoring: Boolean = false,
    ) : NoteVersionUiState
    data class Error(val message: String) : NoteVersionUiState
}

sealed interface NoteVersionEvent {
    /** Caller pops all the way back to the notes list on this - see NoteVersionScreen. */
    data object RestoreSuccess : NoteVersionEvent
}

@HiltViewModel
class NoteVersionViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val popupController: PopupController,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteVersionUiState>(NoteVersionUiState.Loading)
    val uiState: StateFlow<NoteVersionUiState> = _uiState.asStateFlow()

    private val _events = Channel<NoteVersionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var isLoadInFlight = false
    private var noteId: String = ""

    fun load(noteId: String) {
        this.noteId = noteId
        if (isLoadInFlight) return
        isLoadInFlight = true

        viewModelScope.launch {
            try {
                val versions = noteRepository.getVersionHistory(noteId)
                _uiState.value = if (versions.isEmpty()) {
                    NoteVersionUiState.Empty
                } else {
                    NoteVersionUiState.Content(versions)
                }
            } catch (e: NoteFlowException) {
                _uiState.value = NoteVersionUiState.Error(e.userMessage)
            } finally {
                isLoadInFlight = false
            }
        }
    }

    fun onVersionTapped(versionId: String) {
        viewModelScope.launch {
            try {
                val preview = noteRepository.getVersion(noteId, versionId)
                _uiState.update {
                    (it as? NoteVersionUiState.Content)?.copy(
                        previewedVersion = preview,
                        previewedVersionId = versionId,
                    ) ?: it
                }
            } catch (e: NoteFlowException) {
                popupController.show(UiEvent.Snackbar(e.userMessage))
            }
        }
    }

    fun dismissPreview() {
        _uiState.update {
            (it as? NoteVersionUiState.Content)?.copy(previewedVersion = null, previewedVersionId = null) ?: it
        }
    }

    fun restore(versionId: String) {
        val current = _uiState.value as? NoteVersionUiState.Content ?: return
        _uiState.update { (it as? NoteVersionUiState.Content)?.copy(isRestoring = true) ?: it }

        viewModelScope.launch {
            try {
                when (noteRepository.restoreVersion(noteId, versionId)) {
                    is NoteWriteResult.Verified -> _events.send(NoteVersionEvent.RestoreSuccess)
                    NoteWriteResult.Unverified -> {
                        // No local FaceNote to revert to here (this screen never held the note's
                        // live content, only version metadata/previews) - same as ADR 0002's
                        // write-path spirit otherwise: don't navigate on unconfirmed data, let
                        // the next sync/local-cache read reconcile whatever the server holds.
                        popupController.show(UiEvent.Snackbar("Couldn't confirm the restore - check shortly"))
                    }
                }
            } catch (e: NoteFlowException) {
                popupController.show(UiEvent.Snackbar(e.userMessage))
            } finally {
                _uiState.update { (it as? NoteVersionUiState.Content)?.copy(isRestoring = false) ?: it }
            }
        }
    }
}