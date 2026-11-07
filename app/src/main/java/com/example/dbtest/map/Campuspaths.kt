package com.example.dbtest.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF

object Pathway {

    val COLOR: Int = Color.argb(255, 236, 231, 192)
    val BORDER_COLOR: Int = Color.argb(255, 200, 193, 150)

    private const val CORNER_RADIUS = 10f

    val OUTLINE: List<PointF> = pts(
        1236,117, 1139,117, 1134,105, 1124,105, 1120,117, 1072,117, 1071,175, 928,175,
        927,152, 914,152, 913,228, 887,228, 886,221, 875,228, 769,227, 770,193,
        852,193, 852,171, 680,171, 679,148, 642,148, 638,171, 586,171, 585,183,
        522,183, 517,167, 505,166, 506,135, 614,135, 614,125, 473,128, 465,122,
        465,135, 488,137, 487,167, 454,167, 455,135, 443,137, 439,167, 371,167,
        370,175, 250,175, 249,152, 235,152, 234,163, 149,160, 141,186, 146,369,
        91,370, 91,325, 78,321, 69,329, 69,369, 17,374, 17,388, 25,396,
        430,399, 431,411, 461,411, 462,399, 653,404, 650,469, 638,470, 638,487,
        668,477, 669,399, 751,399, 752,410, 893,404, 1263,411, 1263,385, 1206,384,
        1206,228, 1236,227
    )

    val HOLES: List<List<PointF>> = listOf(
        pts(
            746,240, 927,247, 928,255, 942,255, 943,247, 991,247, 992,273, 1010,273,
            1012,247, 1182,248, 1187,358, 1154,362, 1146,385, 1101,382, 1095,301, 1019,299,
            1024,317, 1082,326, 1079,382, 1012,382, 1010,339, 992,339, 991,382, 767,377,
            767,281, 913,293, 913,274, 857,278, 832,264, 820,274, 745,271
        ),
        pts(
            167,186, 370,189, 371,373, 168,370
        ),
        pts(
            390,189, 423,189, 424,177, 473,178, 477,374, 386,373
        ),
        pts(
            535,199, 564,201, 578,189, 637,190, 633,377, 578,377, 577,370, 523,376,
            525,335, 577,335, 577,228, 535,230
        ),
        pts(
            684,348, 735,354, 735,328, 713,328, 712,317, 748,319, 746,377, 685,377
        ),
        pts(
            927,194, 991,197, 992,232, 928,228
        ),
        pts(
            1010,198, 1071,197, 1072,232, 1012,233
        )
    )

    fun buildPath(): Path {
        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        addSubPath(path, OUTLINE)
        for (hole in HOLES) {
            addSubPath(path, hole)
        }
        return path
    }

    private fun addSubPath(path: Path, points: List<PointF>) {
        if (points.isEmpty()) return
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        path.close()
    }

    fun draw(canvas: Canvas, paint: Paint = defaultFillPaint(), borderPaint: Paint = defaultBorderPaint()) {
        val path = buildPath()
        canvas.drawPath(path, paint)
        canvas.drawPath(path, borderPaint)
    }

    private fun defaultFillPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(CORNER_RADIUS)
    }

    private fun defaultBorderPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BORDER_COLOR
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(CORNER_RADIUS)
    }
}

private fun pts(vararg v: Int): List<PointF> {
    val list = mutableListOf<PointF>()
    var i = 0
    while (i < v.size) {
        list.add(PointF(v[i].toFloat(), v[i + 1].toFloat()))
        i += 2
    }
    return list
}