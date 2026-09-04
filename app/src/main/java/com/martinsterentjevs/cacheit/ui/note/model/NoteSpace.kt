package com.martinsterentjevs.cacheit.ui.note.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

/**
 * Unified logical coordinate system for a note.
 *
 * One logical unit corresponds to one dp at the base rendering scale.
 *
 * The base note is 360 x 840 logical units, giving a 9:21 aspect ratio.
 *
 * Text uses a 24-unit line advance, matching CacheIt's TypeBody line height.
 */
object NoteSpace {

    const val BASE_WIDTH = 360f
    const val BASE_HEIGHT = 840f

    const val TEXT_LINE_HEIGHT = 24f

    const val ASPECT_WIDTH = 9f
    const val ASPECT_HEIGHT = 21f

    fun heightForLines(lineCount: Int): Float {
        return max(
            BASE_HEIGHT,
            lineCount.coerceAtLeast(1) * TEXT_LINE_HEIGHT,
        )
    }

    fun logicalSizeFor(document: NoteDocument): NoteLogicalSize {
        val lineCount =
            document.elements.keys.maxOrNull()?.plus(1)?:0


        return NoteLogicalSize(
            width = BASE_WIDTH,
            height = heightForLines(lineCount),
        )
    }

    fun logicalToPhysical(
        point: Offset,
        logicalSize: NoteLogicalSize,
        physicalSize: IntSize,
    ): Offset {
        if (
            logicalSize.width <= 0f ||
            logicalSize.height <= 0f ||
            physicalSize.width <= 0 ||
            physicalSize.height <= 0
        ) {
            return Offset.Zero
        }

        return Offset(
            x = point.x / logicalSize.width * physicalSize.width,
            y = point.y / logicalSize.height * physicalSize.height,
        )
    }

    fun physicalToLogical(
        point: Offset,
        logicalSize: NoteLogicalSize,
        physicalSize: IntSize,
    ): Offset {
        if (
            logicalSize.width <= 0f ||
            logicalSize.height <= 0f ||
            physicalSize.width <= 0 ||
            physicalSize.height <= 0
        ) {
            return Offset.Zero
        }

        return Offset(
            x = point.x / physicalSize.width * logicalSize.width,
            y = point.y / physicalSize.height * logicalSize.height,
        )
    }
}

data class NoteLogicalSize(
    val width: Float,
    val height: Float,
) {
    init {
        require(width > 0f)
        require(height > 0f)
    }
}