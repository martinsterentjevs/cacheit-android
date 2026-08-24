package com.martinsterentjevs.cacheit.ui.note

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme
import com.martinsterentjevs.cacheit.ui.theme.TypeBody


/**
 * Notes list — the post-login home screen.
 *
 * Contents (not yet implemented):
 * - Note cards, synced via delta sync
 * - Empty state copy below is final; the CTA button that should pair with it is not
 * - Inline sync-status indicator (there is no separate sync screen by design)
 */
@OptIn(ExperimentalMaterial3Api::class)// Temporary workaround
@Composable
fun NotesListScreen(
    viewModel: NoteListViewModel = hiltViewModel(),
    onEditNote: (noteId: String) -> Unit = {},
    onCreateNote: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onHistory: (noteId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.note_list_title)) },
                actions = {
                    IconButton(onClick = onOpenAccount) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote) {
                Icon(Icons.Filled.Add, contentDescription = "New note")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {

            when (val state = uiState) {
                is NoteListUiState.Loading -> CircularProgressIndicator()
                is NoteListUiState.Empty -> Text(stringResource(R.string.note_list_empty_notice), style = TypeBody)
                is NoteListUiState.Error -> Text(state.message, style = TypeBody)
                is NoteListUiState.Content -> LazyColumn {
                    items(state.notes, key = { it.noteId ?: it.title }) { note ->
                        NoteCard(note.toCardUiState(), onClick = { onEditNote(note.noteId!!)  }, onHistory = ({ onHistory(note.noteId!!) }))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotesListScreenPreview() {
    CacheItTheme {
        NotesListScreen()
    }
}