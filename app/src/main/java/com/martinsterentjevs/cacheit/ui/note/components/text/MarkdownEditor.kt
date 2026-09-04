package com.martinsterentjevs.cacheit.ui.note.components.text

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Markdown editing surface.
 *
 * Markdown is deliberately edited as plain text. Formatting is represented
 * by Markdown itself and the same value is passed to MarkdownText for view
 * rendering.
 *
 * Takes TextFieldValue rather than a plain String so MarkdownToolIsland can
 * wrap the current selection when applying formatting (bold, heading, list,
 * etc.) instead of only ever appending at the end. Selection state is
 * transient editor state, not part of the persisted note (same
 * document-vs-editor-state split ADR 0005 §11 already makes for drawing) —
 * it's held by NoteCanvas, not the ViewModel.
 *
 * Uses BasicTextField, not OutlinedTextField: this must render with no
 * border, no background, and no internal content padding of its own.
 * DrawingLayer and MarkdownText, its sibling layers in NoteContentSurface,
 * are edge-to-edge within the bounds they're given — OutlinedTextField
 * reserves its own outline, focus ring, and ~16dp internal content padding
 * regardless of what modifier is passed to it, which puts editable text at
 * different physical bounds than the drawing canvas beneath it.
 *
 * The modifier is used as-is, not appended with fillMaxSize()/fillMaxWidth():
 * NoteTextLayer already applies fillMaxWidth() + heightIn(min = pageHeight)
 * (no max) before this is called. Since this now sits inside a vertically
 * scrollable container, the incoming max height is unbounded — fillMaxSize()
 * here would try to resolve an infinite height and crash at runtime.
 */
@Composable
fun MarkdownEditor(
    value: TextFieldValue,
    enabled: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
    )
}