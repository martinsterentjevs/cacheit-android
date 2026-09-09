package com.martinsterentjevs.cacheit.ui.note.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.martinsterentjevs.cacheit.ui.note.NoteEditMode
import com.martinsterentjevs.cacheit.ui.note.NoteEditUiState
import com.martinsterentjevs.cacheit.ui.note.NoteEditViewModel
import com.martinsterentjevs.cacheit.ui.note.components.drawing.ColorWheel
import com.martinsterentjevs.cacheit.ui.note.components.drawing.DrawingDocument
import com.martinsterentjevs.cacheit.ui.note.components.drawing.DrawingHistory
import com.martinsterentjevs.cacheit.ui.note.components.drawing.DrawingLayer
import com.martinsterentjevs.cacheit.ui.note.components.drawing.DrawingSerializationException
import com.martinsterentjevs.cacheit.ui.note.components.drawing.DrawingSerializer
import com.martinsterentjevs.cacheit.ui.note.components.drawing.DrawingToolIsland
import com.martinsterentjevs.cacheit.ui.note.components.drawing.DrawingToolState
import com.martinsterentjevs.cacheit.ui.note.components.text.MarkdownToolIsland
import com.martinsterentjevs.cacheit.ui.note.components.text.NoteTextLayer
import com.martinsterentjevs.cacheit.ui.note.model.NoteDocument
import com.martinsterentjevs.cacheit.ui.note.model.NoteElement
import com.martinsterentjevs.cacheit.ui.note.model.NoteSpace

@Composable
fun NoteCanvas(
    state: NoteEditUiState.Ready,
    viewModel: NoteEditViewModel,
    modifier: Modifier = Modifier,
) {
    var toolState by remember {
        mutableStateOf(DrawingToolState())
    }

    var showColorWheel by remember {
        mutableStateOf(false)
    }

    var historyRevision by remember {
        mutableIntStateOf(0)
    }

    val history = remember(state.note.noteId) {
        val initialDrawing =
            state.note.drawing
                ?.takeIf(String::isNotBlank)
                ?.let {
                    try {
                        DrawingSerializer.deserialize(it)
                    } catch (_: DrawingSerializationException) {
                        DrawingDocument()
                    }
                }
                ?: DrawingDocument()

        DrawingHistory(initialDrawing)
    }

    var textFieldValue by remember(state.note.noteId) {
        mutableStateOf(
            TextFieldValue(
                state.note.body.orEmpty(),
            ),
        )
    }

    /*
     * Transitional adapter:
     *
     * Existing plaintext body -> unified NoteDocument.
     *
     * Persistence remains unchanged for this migration step.
     */
    val noteDocument = remember(
        state.note.noteId,
        textFieldValue.text,
    ) {
        NoteDocument(
            version = 1,
            elements = textFieldValue.text.split('\n')
                    .mapIndexed { index, line ->
                        index to NoteElement(
                            content = line,
                        )
                    }
                    .toMap()

        )
    }

    /*
     * One geometry contract for every visual layer.
     */
    val noteLogicalSize = remember(noteDocument) {
        NoteSpace.logicalSizeFor(noteDocument)
    }

    historyRevision

    LaunchedEffect(state.mode) {
        if (state.mode != NoteEditMode.DrawingEdit) {
            showColorWheel = false
        }
    }

    fun onTextChanged(
        newValue: TextFieldValue,
    ) {
        textFieldValue = newValue
        viewModel.onBodyChanged(newValue.text)
    }

    fun persistDrawingHistory() {
        viewModel.onDrawingChanged(
            DrawingSerializer.serialize(
                history.current(),
            ),
        )
    }

    val scrollState = rememberScrollState()
    Box(
        modifier = modifier.fillMaxWidth().verticalScroll(scrollState),
    ) {
        NoteContentSurface(
            document = noteDocument,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            borderColor =
                if (state.mode == NoteEditMode.View) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.primary
                },
        ) {
            when (state.mode) {
                NoteEditMode.TextEdit -> {
                    DrawingLayer(
                        drawing = history.current(),
                        toolState = toolState,
                        interactive = false,
                        alpha = 0.5f,
                        logicalSize = noteLogicalSize,
                        onDrawingChanged = {},
                        modifier = Modifier.fillMaxSize(),
                    )

                    NoteTextLayer(
                        state = state,
                        document = noteDocument,
                        textFieldValue = textFieldValue,
                        onTextFieldValueChanged = ::onTextChanged,
                    )
                }

                NoteEditMode.DrawingEdit -> {
                    NoteTextLayer(
                        state = state,
                        document = noteDocument,
                        textFieldValue = textFieldValue,
                        onTextFieldValueChanged = ::onTextChanged,
                    )

                    DrawingLayer(
                        drawing = history.current(),
                        toolState = toolState,
                        interactive = true,
                        logicalSize = noteLogicalSize,
                        onDrawingChanged = { stroke ->
                            history.add(stroke)
                            historyRevision++
                            persistDrawingHistory()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    NoteTextLayer(
                        state = state,
                        document = noteDocument,
                        textFieldValue = textFieldValue,
                        onTextFieldValueChanged = ::onTextChanged,
                    )

                    DrawingLayer(
                        drawing = history.current(),
                        toolState = toolState,
                        interactive = false,
                        logicalSize = noteLogicalSize,
                        onDrawingChanged = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (
            state.mode == NoteEditMode.Create ||
            state.mode == NoteEditMode.TextEdit
        ) {
            MarkdownToolIsland(
                value = textFieldValue,
                onValueChange = ::onTextChanged,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            )
        }

        if (state.mode == NoteEditMode.DrawingEdit) {
            DrawingToolIsland(
                toolState = toolState,
                history = history,
                onToolSelected = { tool ->
                    toolState = toolState.copy(
                        activeTool = tool,
                    )
                },
                onColorPickerClicked = {
                    showColorWheel = !showColorWheel
                },
                onUndo = {
                    history.undo()
                    historyRevision++
                    persistDrawingHistory()
                },
                onRedo = {
                    history.redo()
                    historyRevision++
                    persistDrawingHistory()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            )

            if (showColorWheel) {
                ColorWheel(
                    selectedColor = toolState.color,
                    onColorSelected = { color ->
                        toolState = toolState.copy(
                            color = color,
                        )
                        showColorWheel = false
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 72.dp),
                )
            }
        }
    }
}
