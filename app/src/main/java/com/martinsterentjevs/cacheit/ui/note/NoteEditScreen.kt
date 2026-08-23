package com.martinsterentjevs.cacheit.ui.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martinsterentjevs.cacheit.ui.navigation.Route
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.TypeCaption

/**
 * ASSUMPTION: NoteEditViewModel exposes `events: Flow<NoteEditEvent>` with a `Success` case,
 * mirroring BaseAuthViewModel's pattern - adjust the LaunchedEffect below if the real shape
 * you added differs (channel name, event type name, etc).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: String?,
    onBack: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                NoteEditUiEvent.Success -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null || noteId == Route.NoteEdit.NEW_NOTE_ID) "New note" else "Edit note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                NoteEditUiState.Loading -> CircularProgressIndicator()
                NoteEditUiState.NotFound -> Text("This note couldn't be found.")
                is NoteEditUiState.Ready -> NoteEditContent(state, viewModel)
            }
        }
    }
}

@Composable
private fun NoteEditContent(state: NoteEditUiState.Ready, viewModel: NoteEditViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(CacheItSpacing.lg),
    ) {
        OutlinedTextField(
            value = state.note.title,
            onValueChange = viewModel::onTitleChanged,
            label = { Text("Title") },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.note.body ?: "",
            onValueChange = viewModel::onBodyChanged,
            label = { Text("Body") },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.sm),
        )

        // Drawing entry point only - the actual canvas UI is Issue #6's scope, not this one.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Brush, contentDescription = null)
            TextButton(
                onClick = {
                    if (state.isDrawingLocked) viewModel.releaseDrawingLock() else viewModel.acquireDrawingLock()
                },
            ) {
                Text(if (state.isDrawingLocked) "Editing drawing" else "Add drawing")
            }

            state.lockTtlRemaining?.let { remaining ->
                Text(
                    text = "expires in ${remaining.toMinutes()}m",
                    style = TypeCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = CacheItSpacing.xl),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = viewModel::save, enabled = !state.isSaving) {
                Text(if (state.isSaving) "Saving..." else "Save")
            }
        }
    }
}