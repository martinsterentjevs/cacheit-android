package com.martinsterentjevs.cacheit.ui.note.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize
import com.martinsterentjevs.cacheit.ui.note.model.NoteLogicalSize
import kotlin.math.max

/**
 * Pan-only viewport for the unified note surface.
 *
 * The viewport owns camera movement. Content layers do not maintain their own
 * scroll offsets.
 */
class NoteViewportState {
    var offset by mutableStateOf(Offset.Zero)
        private set

    private val panAnimation = Animatable(Offset.Zero, Offset.VectorConverter)
    private val decay = exponentialDecay<Offset>(frictionMultiplier = 1.8f)

    fun panBy(delta: Offset) {
        offset += delta
    }

    fun clamp(
        contentSizePx: IntSize,
        viewportSizePx: IntSize,
        scale: Float = 1f,
    ) {
        val minX = - max(
            0f, (contentSizePx.width - viewportSizePx.width).toFloat(),
        )

        val minY = - max(
            0f, (contentSizePx.height - viewportSizePx.height).toFloat(),
        )

        offset = Offset(
            x = offset.x.coerceIn(minX, 0f),
            y = offset.y.coerceIn(minY, 0f),
        )
    }

    suspend fun fling(
        velocity: Velocity,
        contentSizePx: IntSize,
        viewportSizePx: IntSize,
    ) {
        panAnimation.snapTo(offset)

        panAnimation.animateDecay(
            initialVelocity = Offset(velocity.x, velocity.y),
            animationSpec = decay,
        ) {
            offset = value
            clamp(contentSizePx, viewportSizePx)
        }
    }

    fun reset() {
        offset = Offset.Zero
    }
}
