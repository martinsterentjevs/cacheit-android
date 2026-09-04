package com.martinsterentjevs.cacheit.ui.note

import android.util.Log.e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.note.FaceNote
import com.martinsterentjevs.cacheit.data.note.NoteFlowException
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NoteListUiState{
    data object Loading: NoteListUiState
    data object Empty: NoteListUiState
    data class Content(val notes:List<FaceNote>, val isFromCache:Boolean, val isRefreshing:Boolean): NoteListUiState
    data class Error( val message:String) : NoteListUiState
}
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val popupController: PopupController,
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<NoteListUiState>(NoteListUiState.Loading)
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    private var isSyncInFlight = false
    private var isDeleteInFlight = false
    fun load() {
        if (isSyncInFlight) return
        isSyncInFlight = true
        viewModelScope.launch {
            _uiState.value = NoteListUiState.Loading
            try {
                val result = noteRepository.getNotes()
                if (result.failedCount > 0) {
                    popupController.show(
                        UiEvent.Snackbar(
                            messageId = R.string.note_decrypt_failiure,
                            formatArgs = listOf(result.failedCount)
                        )
                    )
                }
                _uiState.value = if (result.notes.isEmpty()) {
                    NoteListUiState.Empty
                } else {
                    NoteListUiState.Content(result.notes, result.isFromCache,isRefreshing = false)
                }
            } catch (e: NoteFlowException) {
                _uiState.value = NoteListUiState.Error(e.userMessage)
            } finally {
                isSyncInFlight = false
            }
        }
    }
    fun refresh() {
        val current = _uiState.value as? NoteListUiState.Content ?: return

        if (isSyncInFlight) return
        isSyncInFlight = true

        _uiState.value = current.copy(isRefreshing = true)

        viewModelScope.launch {
            try {
                val result = noteRepository.getNotes()

                _uiState.value = if (result.notes.isEmpty()) {
                    NoteListUiState.Empty
                } else {
                    NoteListUiState.Content(
                        notes = result.notes,
                        isFromCache = result.isFromCache,
                        isRefreshing = false
                    )
                }
            } catch (ex: NoteFlowException) {
                // Keep the existing notes if refresh fails.
                _uiState.value = current.copy(isRefreshing = false)

                popupController.show(
                    UiEvent.Snackbar(
                        messageId = R.string.note_list_refresh_error
                    )
                )
                e("Error", ex.userMessage)
            } finally {
                isSyncInFlight = false
            }
        }
    }
    fun delete(noteId: String) {
        val current = _uiState.value as? NoteListUiState.Content ?: return

        if (isDeleteInFlight) return
        isDeleteInFlight = true

        viewModelScope.launch {
            try {
                noteRepository.deleteNote(noteId)

                val remainingNotes = current.notes.filterNot { it.noteId == noteId }

                _uiState.value = if (remainingNotes.isEmpty()) {
                    NoteListUiState.Empty
                } else {
                    current.copy(
                        notes = remainingNotes
                    )
                }

            } catch (e: NoteFlowException) {
                popupController.show(
                    UiEvent.Snackbar(
                        messageId = R.string.note_list_delete_error
                    )
                )
            } finally {
                isDeleteInFlight = false
            }
        }
    }
}