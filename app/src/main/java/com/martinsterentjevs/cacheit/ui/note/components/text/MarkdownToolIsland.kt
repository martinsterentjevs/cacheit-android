package com.martinsterentjevs.cacheit.ui.note.components.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Formatting toolbar for Markdown editing — parity with DrawingToolIsland
 * (same pill shape, same right-of-canvas placement) for Create/TextEdit,
 * the way DrawingToolIsland covers DrawingEdit.
 *
 * Every button here wraps or prefixes the current selection rather than
 * only appending at the end, which is why MarkdownEditor takes a
 * TextFieldValue (carries selection) rather than a plain String — a
 * formatting toolbar that can't see where the cursor is can't do anything
 * more useful than appending to the end of the note.
 *
 * Deliberately narrow set: only operations markdown can express
 * unambiguously with a single wrap/prefix (bold, italic, inline code,
 * heading, bullet list). No table/link/image buttons — those need a
 * dialog, not a one-tap toggle, and would be a different kind of control.
 */
@Composable
fun MarkdownToolIsland(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = { onValueChange(value.wrapSelection("**")) }) {
            Icon(Icons.Default.FormatBold, contentDescription = "Bold")
        }

        IconButton(onClick = { onValueChange(value.wrapSelection("*")) }) {
            Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
        }

        IconButton(onClick = { onValueChange(value.wrapSelection("`")) }) {
            Icon(Icons.Default.Code, contentDescription = "Inline code")
        }

        IconButton(onClick = { onValueChange(value.toggleLinePrefix("# ")) }) {
            Icon(Icons.Default.Title, contentDescription = "Heading")
        }

        IconButton(onClick = { onValueChange(value.toggleLinePrefix("- ")) }) {
            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullet list")
        }
    }
}

/**
 * Wraps the current selection in [marker] on both sides. With a collapsed
 * selection (just a cursor), inserts an empty marker pair and places the
 * cursor between them, matching the usual "type here" affordance of a
 * formatting toolbar. With a real selection, wraps it and collapses the
 * selection to just after the closing marker.
 */
private fun TextFieldValue.wrapSelection(marker: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + marker + selected + marker + text.substring(end)

    val newCursor = if (start == end) {
        start + marker.length
    } else {
        end + marker.length * 2
    }

    return copy(text = newText, selection = TextRange(newCursor))
}

/**
 * Toggles [prefix] at the start of the line the cursor/selection currently
 * sits on. Adds it if absent, removes it if already present — a second tap
 * undoes the first, which is the behavior a toolbar toggle button implies.
 */
private fun TextFieldValue.toggleLinePrefix(prefix: String): TextFieldValue {
    val cursor = selection.min
    val searchFrom = (cursor - 1).coerceAtLeast(0)
    val newlineIndex = text.lastIndexOf('\n', searchFrom)
    val lineStart = if (newlineIndex == -1) 0 else newlineIndex + 1

    return if (text.startsWith(prefix, lineStart)) {
        val newText = text.removeRange(lineStart, lineStart + prefix.length)
        val newCursor = (cursor - prefix.length).coerceAtLeast(lineStart)
        copy(text = newText, selection = TextRange(newCursor))
    } else {
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val newCursor = cursor + prefix.length
        copy(text = newText, selection = TextRange(newCursor))
    }
}