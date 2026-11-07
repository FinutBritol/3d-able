package com.example.dbtest

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Shows a live compass. The dial is rotated so North sits on the right at rest,
 * instead of the conventional top.
 */
class CompassActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    // Exposed to Compose; updated from the sensor listener.
    private var onAzimuthChanged: ((Float) -> Unit)? = null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    private var smoothedAzimuth = 0f
    private var hasSmoothedValue = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, gravity.size)
                    hasGravity = true
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
                    hasGeomagnetic = true
                }
            }

            if (hasGravity && hasGeomagnetic) {
                val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                if (success) {
                    val (outX, outY) = remapAxesForRotation()
                    SensorManager.remapCoordinateSystem(rotationMatrix, outX, outY, rotationMatrix)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)

                    var azimuthDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                    azimuthDeg = (azimuthDeg + 360f) % 360f

                    updateSmoothedAzimuth(azimuthDeg)
                    onAzimuthChanged?.invoke(smoothedAzimuth)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No-op: accuracy changes don't need special handling for this simple compass.
        }
    }

    // Keeps the heading correct if the device is rotated (landscape etc.).
    private fun remapAxesForRotation(): Pair<Int, Int> {
        val rotation = windowManager.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
    }

    private fun updateSmoothedAzimuth(newAzimuth: Float) {
        if (!hasSmoothedValue) {
            smoothedAzimuth = newAzimuth
            hasSmoothedValue = true
            return
        }
        // Shortest angular distance, so it doesn't spin the long way around at the 0/360 wrap.
        var delta = newAzimuth - smoothedAzimuth
        delta = (delta + 540f) % 360f - 180f
        val alpha = 0.15f
        smoothedAzimuth = (smoothedAzimuth + alpha * delta + 360f) % 360f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val sensorsAvailable = accelerometer != null && magnetometer != null

        setContent {
            var azimuth by remember { mutableFloatStateOf(0f) }

            DisposableEffect(Unit) {
                onAzimuthChanged = { azimuth = it }
                accelerometer?.let {
                    sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
                }
                magnetometer?.let {
                    sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
                }
                onDispose {
                    sensorManager.unregisterListener(sensorListener)
                    onAzimuthChanged = null
                }
            }

            MaterialTheme {
                M3Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    CompassScreen(azimuthDegrees = azimuth, sensorsAvailable = sensorsAvailable)
                }
            }
        }
    }
}

@Composable
private fun CompassScreen(azimuthDegrees: Float, sensorsAvailable: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Compass",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "${azimuthDegrees.roundToInt()}°",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6200EE)
        )

        Text(
            text = cardinalNameFor(azimuthDegrees),
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        CompassDial(
            azimuthDegrees = azimuthDegrees,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(bottom = 16.dp)
        )

        if (!sensorsAvailable) {
            Text(
                text = "Compass sensors are not available on this device.",
                color = Color(0xFFD32F2F),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The compass dial. Unlike a typical compass (N at the top, dial rotates so N lines
 * up with true north), this dial is rotated 90° so **N sits on the right** at rest.
 * The fixed pointer on the right always represents the direction the top of the
 * device is currently facing; the dial rotates underneath it exactly like a real
 * compass card, just offset by 90°.
 */
@Composable
private fun CompassDial(azimuthDegrees: Float, modifier: Modifier = Modifier) {
    val dialColor = Color(0xFF6200EE)
    val tickColor = Color(0xA06200EE)
    val northColor = Color(0xFFD50000)
    val labelColor = Color(0xFF282828)

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) / 2f * 0.82f

        // Outer dial circle.
        drawCircle(color = dialColor, radius = radius, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

        // Dial rotation: -azimuth is the classic "rotate the card to keep N aligned
        // with true north" behavior; the extra +90 shifts the whole card so N's rest
        // position is on the right instead of the top.
        val dialRotation = 90f - azimuthDegrees

        rotate(degrees = dialRotation, pivot = Offset(cx, cy)) {
            // Tick marks every 30 degrees, longer ticks every 90 (cardinal directions).
            var angle = 0f
            while (angle < 360f) {
                val isCardinal = angle % 90f == 0f
                val outer = radius
                val inner = if (isCardinal) radius * 0.82f else radius * 0.90f
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
                val labelRadius = radius * 0.68f
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

            drawCardinalLabel("N", 0f, northColor, radius * 0.18f)
            drawCardinalLabel("E", 90f, labelColor, radius * 0.16f)
            drawCardinalLabel("S", 180f, labelColor, radius * 0.16f)
            drawCardinalLabel("W", 270f, labelColor, radius * 0.16f)
        }

        // Fixed pointer: always on the right, independent of dial rotation.
        // Represents the current facing direction of the device.
        rotate(degrees = 90f, pivot = Offset(cx, cy)) {
            val pointerLength = radius * 0.22f
            val pointerWidth = radius * 0.10f
            val tipX = cx
            val tipY = cy - radius - 4f
            val baseY = tipY + pointerLength
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(tipX, tipY)
                lineTo(tipX - pointerWidth / 2f, baseY)
                lineTo(tipX + pointerWidth / 2f, baseY)
                close()
            }
            drawPath(path, color = northColor)
        }

        drawCircle(color = dialColor, radius = radius * 0.035f, center = Offset(cx, cy))
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

private val cardinalNames = arrayOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
)

private fun cardinalNameFor(azimuthDeg: Float): String {
    val index = (((azimuthDeg / 22.5f) + 0.5f).toInt()) % 16
    return cardinalNames[index]
}