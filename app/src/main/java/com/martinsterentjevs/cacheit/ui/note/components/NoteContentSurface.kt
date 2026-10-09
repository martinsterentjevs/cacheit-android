package com.martinsterentjevs.cacheit.ui.note.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.martinsterentjevs.cacheit.ui.note.model.NoteDocument
import com.martinsterentjevs.cacheit.ui.note.model.NoteSpace

/**
 * Spatial authority for the unified note.
 *
 * The note uses one logical coordinate system shared by text and drawing.
 *
 * The note width follows the available viewport width. Its height is derived
 * from the logical note dimensions, preserving the 9:21 base aspect ratio
 * while allowing the document to grow vertically with its content.
 *
 * Scrolling is intentionally not handled here. The parent NoteCanvas owns
 * viewport scrolling, while this surface reports its actual content height.
 */
@Composable
fun NoteContentSurface(
    document: NoteDocument,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val logicalSize = NoteSpace.logicalSizeFor(document)

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val noteWidth = maxWidth
        val noteHeight =
            noteWidth *
                    (logicalSize.height / logicalSize.width)

        Box(
            modifier = Modifier
                .width(noteWidth)
                .height(noteHeight),
            content = content,
        )
    }
}