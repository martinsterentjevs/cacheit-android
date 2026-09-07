package com.martinsterentjevs.cacheit.ui.note.components.text

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.martinsterentjevs.cacheit.ui.note.NoteEditMode
import com.martinsterentjevs.cacheit.ui.note.NoteEditUiState
import com.martinsterentjevs.cacheit.ui.note.model.NoteDocument
import com.martinsterentjevs.cacheit.ui.note.model.NoteSpace
import kotlin.math.max

/**
 * Text layer for the unified note surface.
 *
 * TextEdit/Create:
 *     The complete Markdown source is exposed through MarkdownEditor.
 *
 * View/DrawingEdit:
 *     Each logical document element is rendered independently.
 *     The map key is the element's logical start line.
 *
 *     The requested position is:
 *
 *         startLine * NoteSpace.TEXT_LINE_HEIGHT
 *
 *     If a previous Markdown element renders taller than the space available
 *     before the next element, the next element is pushed down to prevent
 *     visual overlap.
 *
 *     Markdown wrapping therefore affects rendered geometry, but never
 *     changes the document's authored start line.
 */
@Composable
fun NoteTextLayer(
    state: NoteEditUiState.Ready,
    document: NoteDocument,
    textFieldValue: TextFieldValue,
    modifier: Modifier = Modifier,
    onTextFieldValueChanged: (TextFieldValue) -> Unit,
) {
    val editing =
        state.mode == NoteEditMode.Create ||
                state.mode == NoteEditMode.TextEdit

    if (editing) {
        MarkdownEditor(
            value = textFieldValue,
            enabled = !state.isSaving,
            onValueChange = onTextFieldValueChanged,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        MarkdownDocumentRenderer(
            document = document,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MarkdownDocumentRenderer(
    document: NoteDocument,
    modifier: Modifier = Modifier,
) {
    /*
     * Empty elements are deliberately not rendered.
     *
     * Their existence is already represented by their logical line key.
     * Non-empty elements are paired with that key so the layout never loses
     * the element's authored position.
     */
    val renderableElements =
        document.elements
            .toSortedMap()
            .filterValues { it.content.isNotEmpty() }
            .toList()

    Layout(
        modifier = modifier,
        content = {
            renderableElements.forEach { (_, element) ->
                MarkdownText(
                    markdown = element.content,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { measurables, constraints ->

        val placements = mutableListOf<Placement>()

        var previousBottom = 0

        renderableElements.forEachIndexed { index, (startLine, _) ->
            val measurable = measurables[index]

            val placeable =
                measurable.measure(constraints)

            val requestedTop =
                (
                        startLine *
                                NoteSpace.TEXT_LINE_HEIGHT
                        ).dp.roundToPx()

            val top =
                max(
                    requestedTop,
                    previousBottom,
                )

            placements += Placement(
                placeable = placeable,
                top = top,
            )

            previousBottom =
                top + placeable.height
        }

        val contentHeight =
            max(
                constraints.minHeight,
                previousBottom,
            )

        layout(
            width = constraints.maxWidth,
            height = contentHeight,
        ) {
            placements.forEach { (placeable, top) ->
                placeable.placeRelative(
                    x = 0,
                    y = top,
                )
            }
        }
    }
}

private data class Placement(
    val placeable: Placeable,
    val top: Int,
)