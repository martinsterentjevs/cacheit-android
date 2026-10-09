package com.martinsterentjevs.cacheit.ui.note.components.drawing

import androidx.compose.runtime.Immutable

@Immutable
data class DrawingToolState(
    val activeTool: DrawingTool = DrawingTool.Pen,
    val color: DrawingColor = DrawingColor(255, 0, 0),
    val width: Int = 4,
)

class DrawingHistory(
    private val baseline: DrawingDocument,
) {
    private val actions = mutableListOf<Stroke>()
    private var cursor = 0

    val canUndo: Boolean
        get() = cursor > 0

    val canRedo: Boolean
        get() = cursor < actions.size

    fun add(stroke: Stroke) {
        if (cursor < actions.size) {
            actions.subList(cursor, actions.size).clear()
        }

        actions += stroke
        cursor++
    }

    fun undo() {
        if (canUndo) cursor--
    }

    fun redo() {
        if (canRedo) cursor++
    }

    fun current(): DrawingDocument =
        baseline.copy(
            strokes = baseline.strokes + actions.take(cursor),
        )
}
