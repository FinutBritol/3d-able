package com.example.dbtest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A small, read-only live compass meant to sit on top of another screen (here, the
 * campus map) as a permanent overlay. It is NOT interactive - it has no click
 * handling of any kind.
 *
 * The ring (N/E/S/W letters, ticks, circle) stays fixed on screen, with "N" on TOP
 * at rest (the conventional layout). The needle tracks [mapRotationDegrees] - the
 * map's own current on-screen rotation, as driven by the two-finger rotate gesture
 * on [SiteMapView] - so it swings live as you twist the map with your fingers. At
 * rest (map unrotated) the needle points at the fixed "E" label on the right of
 * the ring.
 */
@Composable
fun CompassOverlay(mapRotationDegrees: Float = 0f, modifier: Modifier = Modifier) {
    CompassDial(rotationDegrees = mapRotationDegrees, modifier = modifier)
}

/**
 * Draws the dial itself. Purely visual - no click/touch handling is attached anywhere
 * in this function, so it can never intercept taps meant for the map underneath it.
 *
 * The ring (circle, ticks, N/E/S/W letters) is FIXED relative to the screen - it never
 * rotates, with N on TOP (the conventional layout). Only the needle rotates, driven
 * live by rotationDegrees (the map's current on-screen rotation). At rest (map
 * unrotated) the needle points at the fixed "E" label, which sits on the RIGHT of
 * the ring; as the map is twisted, the needle turns by the same amount.
 */
@Composable
private fun CompassDial(rotationDegrees: Float, modifier: Modifier = Modifier) {
    val dialColor = Color(0xFF6200EE)
    val tickColor = Color(0xA06200EE)
    val northColor = Color(0xFFD50000)
    val labelColor = Color(0xFF282828)

    Canvas(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.92f), CircleShape)
            .border(1.dp, dialColor.copy(alpha = 0.35f), CircleShape)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) / 2f * 0.82f

        // Outer dial circle - fixed, never rotates.
        drawCircle(
            color = dialColor,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )

        // Tick marks every 30 degrees, longer ticks every 90 (cardinal directions).
        // These angles are in screen space (0 = top, 90 = right, 180 = bottom,
        // 270 = left) and are NEVER rotated - the ring is fixed on screen.
        var angle = 0f
        while (angle < 360f) {
            val isCardinal = angle % 90f == 0f
            val outer = radius
            val inner = if (isCardinal) radius * 0.80f else radius * 0.90f
            val rad = Math.toRadians(angle.toDouble())
            val sinA = sin(rad).toFloat()
            val cosA = cos(rad).toFloat()
            drawLine(
                color = tickColor,
                start = Offset(cx + inner * sinA, cy - inner * cosA),
                end = Offset(cx + outer * sinA, cy - outer * cosA),
                strokeWidth = 2f
            )
            angle += 30f
        }

        val nativeCanvas = drawContext.canvas.nativeCanvas
        fun drawCardinalLabel(label: String, angleDeg: Float, color: Color, textSizePx: Float) {
            val rad = Math.toRadians(angleDeg.toDouble())
            val labelRadius = radius * 0.60f
            val x = cx + labelRadius * sin(rad).toFloat()
            val y = cy - labelRadius * cos(rad).toFloat()
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                isFakeBoldText = true
                this.color = color.toArgbCompat()
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = textSizePx
            }
            val textOffset = (paint.descent() + paint.ascent()) / 2f
            nativeCanvas.drawText(label, x, y - textOffset, paint)
        }

        // Fixed ring labels: N on TOP (0°), then E/S/W clockwise from there.
        drawCardinalLabel("N", 0f, northColor, radius * 0.32f)
        drawCardinalLabel("E", 90f, labelColor, radius * 0.24f)
        drawCardinalLabel("S", 180f, labelColor, radius * 0.24f)
        drawCardinalLabel("W", 270f, labelColor, radius * 0.24f)

        // The needle: THIS is what rotates live as the map is twisted. At rest (map
        // unrotated) it points at the fixed "E" label on the right (90°); as the
        // map is rotated with the two-finger gesture, the needle turns by the same
        // amount - like a real compass needle staying anchored to a fixed direction
        // while the paper card underneath it spins.
        val needleRotation = 90f + rotationDegrees

        rotate(degrees = needleRotation, pivot = Offset(cx, cy)) {
            val pointerLength = radius * 0.24f
            val pointerWidth = radius * 0.12f
            val tipX = cx
            val tipY = cy - radius - 4f
            val baseY = tipY + pointerLength
            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(tipX - pointerWidth / 2f, baseY)
                lineTo(tipX + pointerWidth / 2f, baseY)
                close()
            }
            drawPath(path, color = northColor)

            // Tail of the needle, pointing south, so it reads as a single rotating needle.
            val tailY = cy + radius * 0.55f
            val tailBaseY = tailY - pointerLength * 0.7f
            val tailPath = Path().apply {
                moveTo(tipX, tailY)
                lineTo(tipX - pointerWidth / 2.4f, tailBaseY)
                lineTo(tipX + pointerWidth / 2.4f, tailBaseY)
                close()
            }
            drawPath(tailPath, color = labelColor.copy(alpha = 0.6f))
        }

        drawCircle(color = dialColor, radius = radius * 0.05f, center = Offset(cx, cy))
    }
}

private fun Color.toArgbCompat(): Int {
    return android.graphics.Color.argb(
        (alpha * 255f).roundToInt(),
        (red * 255f).roundToInt(),
        (green * 255f).roundToInt(),
        (blue * 255f).roundToInt()
    )
}