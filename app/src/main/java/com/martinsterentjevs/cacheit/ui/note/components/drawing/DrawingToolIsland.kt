package com.martinsterentjevs.cacheit.ui.note.components.drawing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Only Pen and Eraser are real, behaviorally distinct tools — Eraser is the
 * only tool DrawingLayer/DrawingSerializer branch on anywhere. This island
 * previously also offered Pencil and Marker buttons that rendered strokes
 * identically to Pen; selecting one implied a distinction that didn't
 * exist. Removed rather than differentiated for now — reintroduce only
 * alongside actual differentiated rendering (width/opacity/blend
 * differences per tool).
 */
@Composable
fun DrawingToolIsland(
    toolState: DrawingToolState,
    history: DrawingHistory,
    onToolSelected: (DrawingTool) -> Unit,
    onColorPickerClicked: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
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
        IconButton(onClick = onColorPickerClicked) {
            Icon(
                Icons.Default.Palette,
                contentDescription = "Color",
                tint = toolState.color.toComposeColor(),
            )
        }

        ToolButton(toolState.activeTool == DrawingTool.Pen, {
            onToolSelected(DrawingTool.Pen)
        }) {
            Icon(Icons.Default.Create, contentDescription = "Pen")
        }

        ToolButton(toolState.activeTool == DrawingTool.Eraser, {
            onToolSelected(DrawingTool.Eraser)
        }) {
            Icon(Icons.Default.Delete, contentDescription = "Eraser")
        }

        IconButton(
            onClick = onUndo,
            enabled = history.canUndo,
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
        }

        IconButton(
            onClick = onRedo,
            enabled = history.canRedo,
        ) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
        }
    }
}

@Composable
private fun ToolButton(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = if (selected) {
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        } else {
            Modifier
        },
    ) {
        val tint = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            LocalContentColor.current
        }
        CompositionLocalProvider(LocalContentColor provides tint) {
            content()
        }
    }
}

private fun DrawingColor.toComposeColor(): Color =
    Color(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f,
        alpha = alpha / 255f,
    )