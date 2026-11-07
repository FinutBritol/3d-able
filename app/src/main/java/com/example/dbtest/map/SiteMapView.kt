package com.example.dbtest.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Region
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewConfiguration
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.hypot


class SiteMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    var designWidth: Float = 700f
    var designHeight: Float = 1000f

    var buildings: List<Building> = emptyList()
        set(value) {
            field = value
            rebuildPaths()
            invalidate()
        }

    var backgroundBitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    var pathwayOutline: List<PointF> = emptyList()
        set(value) {
            field = value
            rebuildPathwayPath()
            invalidate()
        }

    var pathwayHoles: List<List<PointF>> = emptyList()
        set(value) {
            field = value
            rebuildPathwayPath()
            invalidate()
        }

    var pathwayColor: Int = Color.argb(255, 236, 231, 192)
        set(value) {
            field = value
            pathwayFillPaint.color = value
            invalidate()
        }

    var activeFloor: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    var otherFloorAlphaFactor: Float = 0.3f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }


    var onBuildingClick: ((Building) -> Unit)? = null

    var onBuildingSelected: ((Building?) -> Unit)? = null

    var onBuildingLongClick: ((Building) -> Unit)? = null

    var minZoom = 0.75f
    var maxZoom = 6f

    var panEnabled = true

    var zoomEnabled = true

    var rotateEnabled = true

    // Fired with the map's cumulative rotation (0-360°) any time the two-finger
    // rotate gesture spins it, so other UI (e.g. the compass overlay) can track
    // the map's current on-screen orientation.
    var onMapRotationChanged: ((Float) -> Unit)? = null

    private var currentMapRotationDegrees = 0f


    private var defaultFillColor = Color.argb(90, 187, 134, 252) // subtle purple_200 tint, matches app theme

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = defaultFillColor
    }
    private var defaultPressedFillColor = Color.argb(160, 98, 0, 238) // purple_500 highlight while pressed/selected

    private val pressedFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = defaultPressedFillColor
    }

    private var strokeColor = Color.argb(200, 0, 0, 0)

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = strokeColor
    }
    private val selectedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(98, 0, 238)
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val pathwayFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = pathwayColor
    }

    private var pathwayPath: Path? = null

    private fun rebuildPathwayPath() {
        if (pathwayOutline.isEmpty()) {
            pathwayPath = null
            return
        }
        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        addPolygonTo(path, pathwayOutline)
        pathwayHoles.forEach { addPolygonTo(path, it) }
        pathwayPath = path
    }

    private fun addPolygonTo(path: Path, points: List<PointF>) {
        points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
    }


    fun setBuildingColors(
        fill: Int? = null,
        pressedFill: Int? = null,
        stroke: Int? = null,
        selectedStroke: Int? = null
    ) {
        fill?.let { defaultFillColor = it }
        pressedFill?.let { defaultPressedFillColor = it }
        stroke?.let { strokeColor = it }
        selectedStroke?.let { selectedStrokePaint.color = it }
        invalidate()
    }


    private data class Entry(
        val building: Building,
        val path: Path,
        val region: Region,
        val bounds: RectF,
        val points: List<PointF>
    )

    // Make every footprint wind the same way (building_3a / building_3b were reversed),
    // otherwise their walls would be culled/shaded as if they faced the wrong way in 3D.
    private fun normalizeWinding(p: List<PointF>): List<PointF> {
        var s = 0f
        for (i in p.indices) {
            val a = p[i]
            val b = p[(i + 1) % p.size]
            s += a.x * b.y - b.x * a.y
        }
        return if (s < 0f) p.reversed() else p
    }

    private var entries: List<Entry> = emptyList()
    private var pressedId: String? = null
    private var selectedId: String? = null

    private val designClipRect = Rect()

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Site map"
    }

    private fun rebuildPaths() {
        designClipRect.set(0, 0, designWidth.toInt(), designHeight.toInt())
        pickDirty = true
        entries = buildings.map { b ->
            val path = Path()
            b.points.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()

            val region = Region()
            region.setPath(path, Region(designClipRect))

            val bounds = RectF()
            path.computeBounds(bounds, true)

            Entry(b, path, region, bounds, normalizeWinding(b.points))
        }
    }


    private val baseMatrix = Matrix()
    private val mapMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private var baseFitScale = 1f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val scaleX = w / designWidth
        val scaleY = h / designHeight

        baseFitScale = minOf(scaleX, scaleY)
        val offsetX = (w - designWidth * baseFitScale) / 2f
        val offsetY = (h - designHeight * baseFitScale) / 2f

        baseMatrix.reset()
        baseMatrix.postScale(baseFitScale, baseFitScale)
        baseMatrix.postTranslate(offsetX, offsetY)


        if (mapMatrix.isIdentity) {
            mapMatrix.set(baseMatrix)
        }
        updateInverse()

        // Claim the whole view (including its left/right edges) as a system-gesture
        // exclusion zone. Without this, panning the map near the screen edges gets
        // grabbed by Android's edge-swipe "back" gesture instead of the map, which
        // backgrounds/closes the app on every edge swipe.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && w > 0 && h > 0) {
            systemGestureExclusionRects = listOf(Rect(0, 0, w, h))
        }
    }

    private fun updateInverse() {
        mapMatrix.invert(inverseMatrix)
    }


    fun resetView() {
        mapMatrix.set(baseMatrix)
        updateInverse()
        invalidate()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = (widthSize * (designHeight / designWidth)).toInt()
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val finalHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }
        setMeasuredDimension(widthSize, finalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(mapMatrix)

        if (mode3D) {
            pickDirty = true
            drawScene3D(canvas, pick = false)
            canvas.restore()
            return
        }

        backgroundBitmap?.let { bmp ->
            val src = Rect(0, 0, bmp.width, bmp.height)
            val dst = RectF(0f, 0f, designWidth, designHeight)
            canvas.drawBitmap(bmp, src, dst, bitmapPaint)
        }

        pathwayPath?.let { canvas.drawPath(it, pathwayFillPaint) }

        for (e in entries) {
            val isActiveFloor = e.building.floor == activeFloor

            if (!isActiveFloor && !e.building.showOnOtherFloor) continue

            val isPressed = e.building.id == pressedId
            val isSelected = e.building.id == selectedId

            if (isPressed || isSelected) {

                pressedFillPaint.color = e.building.fillColor?.let { darkenColor(it) } ?: defaultPressedFillColor
                canvas.drawPath(e.path, pressedFillPaint)
                canvas.drawPath(e.path, selectedStrokePaint)
            } else {
                val baseFill = e.building.fillColor ?: defaultFillColor
                fillPaint.color = if (isActiveFloor) baseFill else fadeColor(baseFill, otherFloorAlphaFactor)
                canvas.drawPath(e.path, fillPaint)

                strokePaint.color = if (isActiveFloor) strokeColor else fadeColor(strokeColor, otherFloorAlphaFactor)
                canvas.drawPath(e.path, strokePaint)
            }
        }

        canvas.restore()
    }

    private fun darkenColor(color: Int, factor: Float = 0.55f): Int {
        val a = (Color.alpha(color) * 1.3f).toInt().coerceIn(0, 255)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }


    private fun fadeColor(color: Int, factor: Float): Int {
        val a = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun screenToDesign(x: Float, y: Float): PointF {
        val pts = floatArrayOf(x, y)
        inverseMatrix.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }

    private fun findBuildingAt(viewX: Float, viewY: Float): Entry? {
        if (mode3D) {
            val e = pick3D(viewX, viewY) ?: return null
            return if (e.building.clickable) e else null
        }
        val p = screenToDesign(viewX, viewY)
        for (e in entries.asReversed()) {
            if (e.building.floor != activeFloor) continue
            if (!e.building.clickable) continue
            if (e.bounds.contains(p.x, p.y) && e.region.contains(p.x.toInt(), p.y.toInt())) {
                return e
            }
        }
        return null
    }


    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val scaleGestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!zoomEnabled) return true
                var factor = detector.scaleFactor
                val currentScale = currentMapScale()
                val newScale = currentScale * factor
                // Clamp so pinching past the min/max zoom doesn't keep scaling.
                val clamped = newScale.coerceIn(baseFitScale * minZoom, baseFitScale * maxZoom)
                if (currentScale != 0f) factor = clamped / currentScale
                mapMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                updateInverse()
                invalidate()
                return true
            }
        })


    private fun currentMapScale(): Float {
        val values = FloatArray(9)
        mapMatrix.getValues(values)
        return hypot(values[Matrix.MSCALE_X].toDouble(), values[Matrix.MSKEW_Y].toDouble()).toFloat()
    }

    // Minimum amount of the map (in px) that must stay on screen at all times.
    // Prevents a fast/hard swipe from translating the whole map off-screen in a
    // single large ACTION_MOVE jump (which made the view appear to "go invisible").
    private val minVisiblePan = 80f
    private val reusablePanTestMatrix = Matrix()
    private val reusablePanRect = RectF()

    private fun clampPan(dx: Float, dy: Float): PointF {
        reusablePanTestMatrix.set(mapMatrix)
        reusablePanTestMatrix.postTranslate(dx, dy)

        reusablePanRect.set(0f, 0f, designWidth, designHeight)
        reusablePanTestMatrix.mapRect(reusablePanRect)

        var clampedDx = dx
        var clampedDy = dy

        if (reusablePanRect.right < minVisiblePan) {
            clampedDx += (minVisiblePan - reusablePanRect.right)
        }
        if (reusablePanRect.left > width - minVisiblePan) {
            clampedDx -= (reusablePanRect.left - (width - minVisiblePan))
        }
        if (reusablePanRect.bottom < minVisiblePan) {
            clampedDy += (minVisiblePan - reusablePanRect.bottom)
        }
        if (reusablePanRect.top > height - minVisiblePan) {
            clampedDy -= (reusablePanRect.top - (height - minVisiblePan))
        }

        return PointF(clampedDx, clampedDy)
    }


    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false


    private var rotatePointerId1 = -1
    private var rotatePointerId2 = -1
    private var previousAngle = 0f

    private fun angleBetween(e: MotionEvent, id1: Int, id2: Int): Float {
        val i1 = e.findPointerIndex(id1)
        val i2 = e.findPointerIndex(id2)
        val dx = e.getX(i1) - e.getX(i2)
        val dy = e.getY(i1) - e.getY(i2)
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    private val longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = findBuildingAt(event.x, event.y)
                pressedId = hit?.building?.id
                if (hit != null) {
                    selectedId = hit.building.id
                    onBuildingSelected?.invoke(hit.building)
                    invalidate()

                    longPressRunnable = Runnable {
                        onBuildingLongClick?.invoke(hit.building)
                        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    }
                    longPressHandler.postDelayed(
                        longPressRunnable!!,
                        android.view.ViewConfiguration.getLongPressTimeout().toLong()
                    )
                }
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {

                if (event.pointerCount == 2) {
                    rotatePointerId1 = event.getPointerId(0)
                    rotatePointerId2 = event.getPointerId(1)
                    previousAngle = angleBetween(event, rotatePointerId1, rotatePointerId2)
                    longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                    if (pressedId != null) {
                        pressedId = null
                        invalidate()
                    }
                    isDragging = true
                }
            }

            MotionEvent.ACTION_MOVE -> {

                if (rotateEnabled && rotatePointerId1 != -1 && rotatePointerId2 != -1 &&
                    event.findPointerIndex(rotatePointerId1) != -1 &&
                    event.findPointerIndex(rotatePointerId2) != -1
                ) {
                    val newAngle = angleBetween(event, rotatePointerId1, rotatePointerId2)
                    val delta = newAngle - previousAngle
                    val i1 = event.findPointerIndex(rotatePointerId1)
                    val i2 = event.findPointerIndex(rotatePointerId2)
                    val pivotX = (event.getX(i1) + event.getX(i2)) / 2f
                    val pivotY = (event.getY(i1) + event.getY(i2)) / 2f
                    if (mode3D) yawDeg += delta else mapMatrix.postRotate(delta, pivotX, pivotY)
                    previousAngle = newAngle
                    currentMapRotationDegrees = (currentMapRotationDegrees + delta + 360f) % 360f
                    onMapRotationChanged?.invoke(currentMapRotationDegrees)
                    updateInverse()
                    invalidate()
                } else if (panEnabled && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        isDragging = true
                        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                        if (pressedId != null) {
                            pressedId = null
                            invalidate()
                        }
                    }
                    if (isDragging) {
                        val clamped = clampPan(dx, dy)
                        mapMatrix.postTranslate(clamped.x, clamped.y)
                        updateInverse()
                        invalidate()
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                } else if (pressedId != null) {

                    val stillInside = findBuildingAt(event.x, event.y)?.building?.id == pressedId
                    if (!stillInside) {
                        pressedId = null
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                rotatePointerId1 = -1
                rotatePointerId2 = -1
            }

            MotionEvent.ACTION_UP -> {
                longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                if (!isDragging) {
                    val hit = findBuildingAt(event.x, event.y)
                    if (hit != null && hit.building.id == pressedId) {
                        performClick()
                        onBuildingClick?.invoke(hit.building)
                        announceForAccessibility("${hit.building.label} selected")
                    }
                }
                pressedId = null
                isDragging = false
                rotatePointerId1 = -1
                rotatePointerId2 = -1
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                pressedId = null
                isDragging = false
                rotatePointerId1 = -1
                rotatePointerId2 = -1
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }



    // ===================== 3D (extruded) mode =====================

    var mode3D = false
        private set

    // Rotation about the vertical axis (what the two-finger twist controls in 3D).
    var yawDeg = 0f
        set(v) {
            field = ((v % 360f) + 360f) % 360f
            updateCamera()
            invalidate()
        }

    // 90 = straight top-down (identical to the 2D map); lower = more tilted.
    var pitchDeg = 55f
        set(v) {
            field = v.coerceIn(25f, 90f)
            updateCamera()
            invalidate()
        }

    private var cosY = 1f
    private var sinY = 0f
    private var cosP = 0f
    private var sinP = 1f

    init { updateCamera() } // must stay below the four fields above

    private fun updateCamera() {
        val y = Math.toRadians(yawDeg.toDouble())
        val p = Math.toRadians(pitchDeg.toDouble())
        cosY = cos(y).toFloat(); sinY = sin(y).toFloat()
        cosP = cos(p).toFloat(); sinP = sin(p).toFloat()
    }

    // design-space (x, y, height) -> flat design-space point as it appears on screen
    private fun project(x: Float, y: Float, z: Float, out: FloatArray) {
        val cx = designWidth / 2f
        val cy = designHeight / 2f
        val dx = x - cx
        val dy = y - cy
        out[0] = cx + dx * cosY - dy * sinY
        out[1] = cy + (dx * sinY + dy * cosY) * sinP - z * cosP
    }

    fun set3D(enabled: Boolean) {
        mode3D = enabled
        yawDeg = 0f
        currentMapRotationDegrees = 0f
        onMapRotationChanged?.invoke(0f)
        resetView()
    }

    private val tmp = FloatArray(2)
    private val facePath = Path()
    private val groundPath = Path()

    private fun opaque(c: Int) = Color.rgb(Color.red(c), Color.green(c), Color.blue(c))

    private fun shade(c: Int, f: Float) = Color.rgb(
        (Color.red(c) * f).toInt().coerceIn(0, 255),
        (Color.green(c) * f).toInt().coerceIn(0, 255),
        (Color.blue(c) * f).toInt().coerceIn(0, 255)
    )

    // Fixed world-space light so walls keep their shade as the camera yaws.
    private fun wallShade(ex: Float, ey: Float): Float {
        val len = hypot(ex, ey)
        if (len == 0f) return 1f
        val d = (ey / len) * -0.5f + (-ex / len) * 0.85f
        return 0.5f + 0.5f * d.coerceAtLeast(0f)
    }

    // pick = true paints every building a unique flat colour (red channel = index + 1)
    // into an off-screen bitmap used for hit testing.
    private fun drawScene3D(canvas: Canvas, pick: Boolean) {
        fillPaint.isAntiAlias = !pick
        strokePaint.isAntiAlias = !pick

        if (!pick && pathwayOutline.isNotEmpty()) {
            groundPath.reset()
            groundPath.fillType = Path.FillType.EVEN_ODD
            for (poly in listOf(pathwayOutline) + pathwayHoles) {
                poly.forEachIndexed { i, p ->
                    project(p.x, p.y, 0f, tmp)
                    if (i == 0) groundPath.moveTo(tmp[0], tmp[1]) else groundPath.lineTo(tmp[0], tmp[1])
                }
                groundPath.close()
            }
            canvas.drawPath(groundPath, pathwayFillPaint)
        }

        // Painter's algorithm: flat things first, then far -> near, lower storey before upper.
        val order = entries.indices.sortedWith(
            compareBy<Int>(
                { entries[it].building.topZ > 0f },
                {
                    val b = entries[it].bounds
                    (b.centerX() - designWidth / 2f) * sinY + (b.centerY() - designHeight / 2f) * cosY
                },
                { entries[it].building.baseZ }
            )
        )

        for (idx in order) {
            val e = entries[idx]
            val b = e.building
            val pts = e.points
            val z0 = b.baseZ
            val z1 = b.topZ
            val n = pts.size
            val base = if (pick) Color.rgb(idx + 1, 0, 0) else opaque(b.fillColor ?: defaultFillColor)

            if (z1 > z0) {
                for (i in 0 until n) {
                    val a = pts[i]
                    val c = pts[(i + 1) % n]
                    val ex = c.x - a.x
                    val ey = c.y - a.y
                    if (ex * cosY - ey * sinY >= 0f) continue // back-facing wall
                    facePath.reset()
                    project(a.x, a.y, z0, tmp); facePath.moveTo(tmp[0], tmp[1])
                    project(c.x, c.y, z0, tmp); facePath.lineTo(tmp[0], tmp[1])
                    project(c.x, c.y, z1, tmp); facePath.lineTo(tmp[0], tmp[1])
                    project(a.x, a.y, z1, tmp); facePath.lineTo(tmp[0], tmp[1])
                    facePath.close()
                    fillPaint.color = if (pick) base else shade(base, wallShade(ex, ey))
                    canvas.drawPath(facePath, fillPaint)
                    if (!pick) {
                        strokePaint.color = strokeColor
                        canvas.drawPath(facePath, strokePaint)
                    }
                }
            }

            facePath.reset()
            pts.forEachIndexed { i, p ->
                project(p.x, p.y, z1, tmp)
                if (i == 0) facePath.moveTo(tmp[0], tmp[1]) else facePath.lineTo(tmp[0], tmp[1])
            }
            facePath.close()
            val selected = b.id == selectedId
            fillPaint.color = if (pick || !selected) base else darkenColor(base)
            canvas.drawPath(facePath, fillPaint)
            if (!pick) {
                strokePaint.color = strokeColor
                canvas.drawPath(facePath, strokePaint)
                if (selected) canvas.drawPath(facePath, selectedStrokePaint)
            }
        }

        fillPaint.isAntiAlias = true
        strokePaint.isAntiAlias = true
    }

    private var pickBitmap: Bitmap? = null
    private var pickDirty = true

    private fun pick3D(vx: Float, vy: Float): Entry? {
        if (width == 0 || height == 0) return null
        var bmp = pickBitmap
        if (bmp == null || bmp.width != width || bmp.height != height) {
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            pickBitmap = bmp
            pickDirty = true
        }
        if (pickDirty) {
            val c = Canvas(bmp!!)
            c.drawColor(Color.BLACK)
            c.concat(mapMatrix)
            drawScene3D(c, pick = true)
            pickDirty = false
        }
        val x = vx.toInt()
        val y = vy.toInt()
        if (x !in 0 until width || y !in 0 until height) return null
        val id = Color.red(bmp!!.getPixel(x, y))
        return if (id in 1..entries.size) entries[id - 1] else null
    }

    fun selectBuilding(id: String?) {
        selectedId = id
        onBuildingSelected?.invoke(entries.find { it.building.id == id }?.building)
        invalidate()
    }


    fun getBuildingCenterOnScreen(id: String): PointF? {
        val e = entries.find { it.building.id == id } ?: return null
        val centerX = (e.bounds.left + e.bounds.right) / 2f
        val centerY = (e.bounds.top + e.bounds.bottom) / 2f
        val pts = floatArrayOf(centerX, centerY)
        if (mode3D) project(centerX, centerY, e.building.topZ, pts)
        mapMatrix.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }
}