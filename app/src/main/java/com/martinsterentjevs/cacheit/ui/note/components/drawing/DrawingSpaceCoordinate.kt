package com.martinsterentjevs.cacheit.ui.note.components.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * Maps drawing coordinates between the physical Canvas and the unified
 * logical note coordinate system.
 *
 * The drawing has no independent coordinate space.
 * It also has no knowledge of scrolling, panning, or zoom.
 */
data class DrawingSpaceCoordinate(
    val logicalWidth: Float,
    val logicalHeight: Float,
    val physicalSize: IntSize,
) {

    init {
        require(logicalWidth > 0f) {
            "logicalWidth must be greater than zero"
        }

        require(logicalHeight > 0f) {
            "logicalHeight must be greater than zero"
        }
    }

    fun toLogical(position: Offset): Point {
        if (
            physicalSize.width <= 0 ||
            physicalSize.height <= 0
        ) {
            return Point(
                x = 0,
                y = 0,
                pressure = 1f,
            )
        }

        return Point(
            x = (
                    position.x /
                            physicalSize.width *
                            logicalWidth
                    )
                .toInt()
                .coerceIn(
                    0,
                    logicalWidth.toInt(),
                ),

            y = (
                    position.y /
                            physicalSize.height *
                            logicalHeight
                    )
                .toInt()
                .coerceIn(
                    0,
                    logicalHeight.toInt(),
                ),

            pressure = 1f,
        )
    }

    fun toPhysical(point: Point): Offset {
        if (
            physicalSize.width <= 0 ||
            physicalSize.height <= 0
        ) {
            return Offset.Zero
        }

        return Offset(
            x =
                point.x.toFloat() /
                        logicalWidth *
                        physicalSize.width,

            y =
                point.y.toFloat() /
                        logicalHeight *
                        physicalSize.height,
        )
    }
}