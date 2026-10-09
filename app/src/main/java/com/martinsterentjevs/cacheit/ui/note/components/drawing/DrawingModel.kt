package com.martinsterentjevs.cacheit.ui.note.components.drawing

import kotlinx.serialization.Serializable

@Serializable
data class DrawingDocument(
    val schemaVersion: Int = 1,
    val strokes: List<Stroke> = emptyList(),
)

@Serializable
data class Stroke(
    val tool: DrawingTool,
    val width: Int,
    val color: DrawingColor?,
    val points: List<Point>,
)

@Serializable
data class Point(
    val x: Int,
    val y: Int,
    val pressure: Float = 1f,
)

@Serializable
data class DrawingColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255,
) {
    init {
        require(red in 0..255)
        require(green in 0..255)
        require(blue in 0..255)
        require(alpha in 0..255)
    }
}

@Serializable
enum class DrawingTool {
    Pen,
    Eraser,
}