package com.martinsterentjevs.cacheit.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.ui.note.components.NoteCanvas
import kotlinx.coroutines.flow.collectLatest

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
        viewModel.events.collectLatest { event ->
            when (event) {
                NoteEditUiEvent.Dismiss -> onBack()
            }
        }
    }

    val interceptBack =
        (uiState as? NoteEditUiState.Ready)?.mode != NoteEditMode.View &&
                uiState is NoteEditUiState.Ready

    BackHandler(enabled = interceptBack) {
        viewModel.onBackPressed()
    }

    Scaffold(
        topBar = {
            NoteEditTopBar(
                state = uiState,
                viewModel = viewModel,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                NoteEditUiState.Loading -> Unit

                NoteEditUiState.NotFound -> {
                    Text(stringResource(R.string.note_edit_note_not_found))
                }

                is NoteEditUiState.Ready -> {
                    NoteCanvas(
                        state = state,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditTopBar(
    state: NoteEditUiState,
    viewModel: NoteEditViewModel,
    onBack: () -> Unit,
) {
    val ready = state as? NoteEditUiState.Ready
    val borderColor =
        when (ready?.mode) {
            NoteEditMode.View -> colorScheme.outline
            NoteEditMode.Create,
            NoteEditMode.TextEdit,
            NoteEditMode.DrawingEdit -> colorScheme.primary
            null -> colorScheme.outline
        }

    Column {
        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (
                            ready != null &&
                            ready.mode != NoteEditMode.View
                        ) {
                            viewModel.onBackPressed()
                        } else {
                            onBack()
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription =
                            stringResource(R.string.navigation_back),
                    )
                }
            },
            title = {
                when (ready?.mode) {
                    NoteEditMode.Create,
                    NoteEditMode.TextEdit -> {
                        OutlinedTextField(
                            value = ready.note.title,
                            onValueChange = viewModel::onTitleChanged,
                            enabled = !ready.isSaving,
                            singleLine = true,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.note_edit_note_title,
                                    ),
                                )
                            },
                        )
                    }

                    NoteEditMode.View,
                    NoteEditMode.DrawingEdit -> {
                        Text(
                            ready.note.title.ifBlank {
                                stringResource(
                                    R.string.note_edit_title,
                                )
                            },
                        )
                    }

                    null -> {
                        Text(
                            stringResource(
                                R.string.note_edit_title,
                            ),
                        )
                    }
                }
            },
            actions = {
                when (ready?.mode) {
                    NoteEditMode.View -> {
                        IconButton(
                            onClick = viewModel::enterTextEdit,
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit note",
                            )
                        }
                    }

                    NoteEditMode.Create,
                    NoteEditMode.TextEdit -> {
                        IconButton(
                            onClick = viewModel::enterDrawingEdit,
                        ) {
                            Icon(
                                Icons.Default.Draw,
                                contentDescription =
                                    "Switch to drawing",
                            )
                        }

                        IconButton(
                            onClick = {
                                if (
                                    ready.mode ==
                                    NoteEditMode.Create
                                ) {
                                    viewModel.exitCreate()
                                } else {
                                    viewModel.exitTextEdit()
                                }
                            },
                            enabled = !ready.isSaving,
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription =
                                    stringResource(R.string.save),
                            )
                        }
                    }

                    NoteEditMode.DrawingEdit -> {
                        IconButton(
                            onClick = viewModel::enterTextEdit,
                        ) {
                            Icon(
                                Icons.Default.TextFields,
                                contentDescription = "Switch to text",
                            )
                        }

                        IconButton(
                            onClick = viewModel::exitDrawingEdit,
                            enabled = !ready.isSaving,
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription =
                                    stringResource(R.string.save),
                            )
                        }
                    }

                    null -> Unit
                }
            },
        )

        HorizontalDivider(
            color = borderColor,
        )
    }
}