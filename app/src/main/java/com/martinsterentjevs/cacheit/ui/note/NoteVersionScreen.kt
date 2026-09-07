package com.martinsterentjevs.cacheit.ui.note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martinsterentjevs.cacheit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteVersionScreen(
    noteId: String,
    onBack: () -> Unit,
    onRestored: () -> Unit, // caller pops all the way back to the notes list
    viewModel: NoteVersionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                NoteVersionEvent.RestoreSuccess -> onRestored()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Version history") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(
                            R.string.navigation_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                NoteVersionUiState.Loading -> CircularProgressIndicator()
                NoteVersionUiState.Empty -> Text(stringResource(R.string.note_versions_none_found))
                is NoteVersionUiState.Error -> Text(state.message)
                is NoteVersionUiState.Content -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(state.versions, key = { it.versionId }) { version ->
                            ListItem(
                                headlineContent = { Text(version.createdAt) },
                                supportingContent = { if (version.isCurrent) Text(stringResource(R.string.note_version_current)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onVersionTapped(version.versionId)
                                    },
                            )
                            // Tap target - ListItem itself doesn't take onClick directly in
                            // this Material3 version; wrap in a clickable Row if needed, or
                            // swap to a plain Row + Text if ListItem's click API differs here.
                        }
                    }

                    state.previewedVersion?.let { preview ->
                        AlertDialog(
                            onDismissRequest = viewModel::dismissPreview,
                            title = { Text(preview.title) },
                            text = {
                                Column {
                                    preview.body?.let { Text(it) }
                                    if (preview.drawing != null) Text(stringResource(R.string.note_version_has_drawing))
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = { state.previewedVersionId?.let(viewModel::restore) },
                                    enabled = !state.isRestoring,
                                ) {
                                    Text(if (state.isRestoring) stringResource(R.string.note_version_restore_in_progress) else stringResource(R.string.note_version_restore_this))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = viewModel::dismissPreview) { Text(
                                    stringResource(R.string.cancel)
                                ) }
                            },
                        )
                    }
                }
            }
        }
    }
}