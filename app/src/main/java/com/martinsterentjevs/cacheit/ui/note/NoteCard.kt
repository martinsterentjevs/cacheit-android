package com.martinsterentjevs.cacheit.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.martinsterentjevs.cacheit.R.string
import com.martinsterentjevs.cacheit.data.note.FaceNote
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.TypeBody
import com.martinsterentjevs.cacheit.ui.theme.TypeCaption
import com.martinsterentjevs.cacheit.ui.theme.TypeHeading

data class NoteCardUiState(
    val noteId: String,
    val title: String,
    val bodyPreview: String?,  // null -> title-only layout, no truncation logic needed here
    val hasDrawing: Boolean,   // top-right icon
    val hasHistory: Boolean,   // bottom-right cut corner
    val lastModifiedAt: String,
    val isLocked: Boolean,
)

fun FaceNote.toCardUiState() = NoteCardUiState(
    noteId = requireNotNull(noteId),
    title = title,
    bodyPreview = body?.takeIf { it.isNotBlank() },
    hasDrawing = !drawing.isNullOrBlank(),
    hasHistory = hasHistory,
    lastModifiedAt = lastModifiedAt,
    isLocked = lockedByDeviceId != null,
)

/**
 * hasHistory is signaled by a bigger cut on the bottom-right corner and an iconButton.
 */
@Composable
fun NoteCard(state: NoteCardUiState, onClick: () -> Unit, onHistory: () -> Unit) {
    val cardShape = CutCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 12.dp,
        bottomEnd = if (state.hasHistory) 16.dp else 12.dp,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, cardShape)
            .clickable(onClick = onClick)
            .padding(CacheItSpacing.md, CacheItSpacing.xl),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row - fixed height per design system's card header row spec.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.title,
                    style = TypeHeading,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (state.isLocked) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = stringResource(string.note_drawing_locked),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = CacheItSpacing.xs),
                    )
                }

                if (state.hasDrawing) {
                    Icon(
                        Icons.Filled.Brush,
                        contentDescription = stringResource(string.note_info_drawing_true),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = CacheItSpacing.xs),
                    )
                }

                Text(
                    text = state.lastModifiedAt,
                    style = TypeCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Content row - title-only layout when there's no body, no empty space reserved.
            state.bodyPreview?.let { preview ->
                Text(
                    text = preview,
                    style = TypeBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Clip, // spec: clips, no ellipsis
                    modifier = Modifier.padding(top = CacheItSpacing.sm),
                )
            }

            // Footer row - hasHistory badge kept here too (in addition to the corner cut)
            // until it's confirmed the corner alone reads clearly enough on-device.
            if (state.hasHistory) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CacheItSpacing.sm),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onHistory,
                        modifier = Modifier.size(CacheItSpacing.xl),
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(string.note_view_history),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            }
        }
    }
