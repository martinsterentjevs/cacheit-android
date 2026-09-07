package com.martinsterentjevs.cacheit.ui.note.components.text

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.martinsterentjevs.cacheit.ui.note.model.NoteSpace
import dev.jeziellago.compose.markdowntext.MarkdownText as ComposeMarkdownText

/**
 * Renders the note body as Markdown.
 *
 * The persisted note body remains the original Markdown source.
 * This component is the UI boundary between the note model and the
 * Markdown rendering library.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    preserveLine: Boolean = false,
) {
    val lineModifier =
        if (preserveLine) {
            modifier.heightIn(min= NoteSpace.TEXT_LINE_HEIGHT.dp)
        } else {
            modifier
        }

    ComposeMarkdownText(
        markdown = markdown,
        modifier = lineModifier,
        enableSoftBreakAddsNewLine = true
    )
}