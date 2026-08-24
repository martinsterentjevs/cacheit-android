package com.martinsterentjevs.cacheit.ui.note

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
    data class Content(val notes:List<FaceNote>, val isFromCache:Boolean): NoteListUiState
    data class Error( val message:String) : NoteListUiState
}
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val popupController: PopupController,
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<NoteListUiState>(NoteListUiState.Loading)
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    private var isLoadInFlight = false

    fun load() {
        if (isLoadInFlight) return
        isLoadInFlight = true
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
                    NoteListUiState.Content(result.notes, result.isFromCache)
                }
            } catch (e: NoteFlowException) {
                _uiState.value = NoteListUiState.Error(e.userMessage)
            } finally {
                isLoadInFlight = false
            }
        }
    }
}