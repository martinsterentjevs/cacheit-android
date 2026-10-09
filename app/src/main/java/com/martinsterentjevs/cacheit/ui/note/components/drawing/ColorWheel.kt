package com.martinsterentjevs.cacheit.ui.note.components.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ColorWheel(
    selectedColor: DrawingColor,
    onColorSelected: (DrawingColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .size(220.dp)
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = position.x - center.x
                    val dy = position.y - center.y
                    val radius = minOf(size.width, size.height) / 2f

                    if (dx * dx + dy * dy > radius * radius) {
                        return@detectTapGestures
                    }

                    val hue = (
                            Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0
                            ) % 360.0

                    onColorSelected(hsvToColor(hue.toFloat()))
                }
            },
    ) {
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red,
                ),
            ),
        )

        val hue = colorToHue(selectedColor)
        val angle = Math.toRadians(hue.toDouble())
        val markerRadius = size.minDimension * 0.39f

        val marker = Offset(
            center.x + cos(angle).toFloat() * markerRadius,
            center.y + sin(angle).toFloat() * markerRadius,
        )

        drawCircle(Color.White, 10.dp.toPx(), marker)
        drawCircle(Color.Black, 6.dp.toPx(), marker)
    }
}

private fun hsvToColor(hue: Float): DrawingColor {
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))

    return DrawingColor(
        red = android.graphics.Color.red(rgb),
        green = android.graphics.Color.green(rgb),
        blue = android.graphics.Color.blue(rgb),
    )
}

private fun colorToHue(color: DrawingColor): Float {
    val hsv = FloatArray(3)

    android.graphics.Color.RGBToHSV(
        color.red,
        color.green,
        color.blue,
        hsv,
    )

    return hsv[0]
}
