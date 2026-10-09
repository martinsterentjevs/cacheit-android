package com.martinsterentjevs.cacheit.ui.note.components.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.martinsterentjevs.cacheit.ui.note.model.NoteLogicalSize
import com.martinsterentjevs.cacheit.ui.note.model.NoteSpace

@Composable
fun DrawingLayer(
    modifier: Modifier = Modifier,
    drawing: DrawingDocument,
    toolState: DrawingToolState,
    interactive: Boolean,
    alpha: Float = 1f,
    logicalSize: NoteLogicalSize = NoteLogicalSize(
        width = NoteSpace.BASE_WIDTH,
        height = NoteSpace.BASE_HEIGHT,
    ),
    onDrawingChanged: (Stroke) -> Unit,
) {
    var currentPhysicalStroke by remember {
        mutableStateOf<PhysicalStroke?>(null)
    }

    var canvasSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val coordinateSpace = remember(
        canvasSize,
        logicalSize,
    ) {
        if (
            canvasSize.width > 0 &&
            canvasSize.height > 0
        ) {
            DrawingSpaceCoordinate(
                logicalWidth = logicalSize.width,
                logicalHeight = logicalSize.height,
                physicalSize = canvasSize,
            )
        } else {
            null
        }
    }

    val inputModifier =
        if (interactive) {
            Modifier.pointerInput(
                toolState,
                coordinateSpace,
            ) {
                awaitEachGesture {
                    val down =
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Main,
                        )

                    val space =
                        coordinateSpace
                            ?: return@awaitEachGesture

                    currentPhysicalStroke =
                        PhysicalStroke(
                            tool = toolState.activeTool,
                            width = toolState.width,
                            color =
                                if (
                                    toolState.activeTool ==
                                    DrawingTool.Eraser
                                ) {
                                    null
                                } else {
                                    toolState.color
                                },
                            points = listOf(
                                down.position,
                            ),
                        )

                    while (true) {
                        val event =
                            awaitPointerEvent(
                                PointerEventPass.Main,
                            )

                        val change =
                            event.changes.firstOrNull {
                                it.id == down.id
                            }
                                ?: run {
                                    currentPhysicalStroke = null
                                    break
                                }

                        if (!change.pressed) {
                            currentPhysicalStroke?.let {
                                    physicalStroke ->

                                if (
                                    physicalStroke
                                        .points
                                        .isNotEmpty()
                                ) {
                                    val logicalPoints =
                                        physicalStroke.points.map(
                                            space::toLogical,
                                        )

                                    onDrawingChanged(
                                        Stroke(
                                            tool =
                                                physicalStroke.tool,
                                            width =
                                                physicalStroke.width,
                                            color =
                                                physicalStroke.color,
                                            points =
                                                logicalPoints,
                                        ),
                                    )
                                }
                            }

                            currentPhysicalStroke = null
                            break
                        }

                        currentPhysicalStroke =
                            currentPhysicalStroke?.copy(
                                points =
                                    currentPhysicalStroke!!
                                        .points +
                                            change.position,
                            )

                        change.consume()
                    }
                }
            }
        } else {
            Modifier
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clipToBounds()
                .graphicsLayer {
                    this.alpha = alpha
                }
                .onSizeChanged {
                    canvasSize = it
                }
                .then(inputModifier),
        ) {
            val space =
                coordinateSpace
                    ?: return@Canvas

            drawing.strokes.forEach { stroke ->
                drawPhysicalStroke(
                    points =
                        stroke.points.map(
                            space::toPhysical,
                        ),
                    tool = stroke.tool,
                    width = stroke.width,
                    color = stroke.color,
                )
            }

            currentPhysicalStroke?.let { stroke ->
                drawPhysicalStroke(
                    points = stroke.points,
                    tool = stroke.tool,
                    width = stroke.width,
                    color = stroke.color,
                )
            }
        }
    }
}

private data class PhysicalStroke(
    val tool: DrawingTool,
    val width: Int,
    val color: DrawingColor?,
    val points: List<Offset>,
)

private fun DrawScope.drawPhysicalStroke(
    points: List<Offset>,
    tool: DrawingTool,
    width: Int,
    color: DrawingColor?,
) {
    if (points.isEmpty()) {
        return
    }

    val path = Path()
    val first = points.first()

    path.moveTo(
        first.x,
        first.y,
    )

    points
        .drop(1)
        .forEach { point ->
            path.lineTo(
                point.x,
                point.y,
            )
        }

    if (tool == DrawingTool.Eraser) {
        drawPath(
            path = path,
            color = Color.Transparent,
            style = DrawStroke(
                width = width.toFloat(),
            ),
            blendMode = BlendMode.Clear,
        )

        return
    }

    val drawColor =
        color?.toComposeColor()
            ?: return

    drawPath(
        path = path,
        color = drawColor,
        style = DrawStroke(
            width = width.toFloat(),
        ),
    )
}

private fun DrawingColor.toComposeColor(): Color =
    Color(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f,
        alpha = alpha / 255f,
    )