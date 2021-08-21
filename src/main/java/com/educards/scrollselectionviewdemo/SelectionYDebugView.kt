package com.educards.scrollselectionviewdemo

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

/**
 * Draws the trajectory and current state of `selectionY`.
 */
class SelectionYDebugView: View {

    lateinit var selectionYSolver: SelectionYSolver
    lateinit var selectionYData: SelectionYData
    lateinit var selectionYParams: SelectionYParams

    private val paintSelectionRatio = Paint().apply {
        color = Color.BLUE
        strokeWidth = 60f
        alpha = 150
        style = Paint.Style.STROKE
    }

    private val paintSelectionText = Paint().apply {
        color = Color.WHITE
        textSize = 64f
    }

    private val paintScrollPxText = Paint().apply {
        color = Color.BLACK
        textSize = 64f
    }

    private val paintCurvePrimary = Paint().apply {
        color = Color.BLUE
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val paintCurveSecondary = Paint().apply {
        color = Color.BLUE
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val paintPlotMid = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val paintUnknownArea = Paint().apply {
        color = Color.GRAY
        alpha = 2
        style = Paint.Style.FILL
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
    ) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        plotCurves(canvas)
        plotSelectionAndDistTexts(canvas)
    }

    private fun plotCurves(canvas: Canvas?) {

        // Naming convention:
        //   p* - plot domain
        //   r* - real domain

        val rTopDist = selectionYData.contentTopDistPx?.absoluteValue
        val rTopPerceptRange = selectionYParams.contentTopPerceptionRangePx
        val rBottomDist = selectionYData.contentBottomDistPx
        val rBottomPerceptRange = selectionYParams.contentBottomPerceptionRangePx
        val rTotalPerceptRange = rTopPerceptRange + rBottomPerceptRange
        val r2pCoef = width.toFloat() / rTotalPerceptRange.toFloat()

        val pTopPerceptRange = rTopPerceptRange * r2pCoef
        canvas?.drawLine(pTopPerceptRange, 0f, pTopPerceptRange, height.toFloat(), paintPlotMid)

        for (i in 0..width) {

            val rX = (i.toDouble() / r2pCoef).roundToInt()

            if (rTopDist != null && rBottomDist != null) {

                val rTotalDist = (rTopDist + rBottomDist).toDouble()
                val rTopBottomPerceptRatio = rTopPerceptRange.toDouble() / rTotalPerceptRange.toDouble()
                val rTopStart = rTopPerceptRange - (rTotalDist * rTopBottomPerceptRatio)
                val rTopY = when {
                    rX < rTopStart -> {
                        0.0
                    }
                    rX > rTopStart + rTopPerceptRange -> {
                        selectionYParams.selectionYMid
                    }
                    else -> {
                        val rTopX = rX - rTopStart
                        selectionYSolver.curve(
                            rTopPerceptRange.toDouble(),
                            selectionYParams.selectionYMid,
                            1.0 - selectionYParams.stiffness,
                            rTopX)?.second
                    }
                }
                if (rTopY != null) canvas?.drawPoint(i.toFloat(), rTopY.toFloat() * height, paintCurveSecondary)

                val rBottomStart = rTopPerceptRange - rBottomPerceptRange + rTotalDist * (1.0 - rTopBottomPerceptRatio)
                val rBottomY = when {
                    rX < rBottomStart -> {
                        0.0
                    }
                    rX > rBottomStart + rBottomPerceptRange -> {
                        1.0 - selectionYParams.selectionYMid
                    }
                    else -> {
                        val rBottomX = rX - rBottomStart
                        selectionYSolver.curve(
                            rBottomPerceptRange.toDouble(),
                            1.0 - selectionYParams.selectionYMid,
                            1.0 - selectionYParams.stiffness,
                            rBottomX)?.second
                    }
                }
                if (rBottomY != null) {
                    val rBottomYPlotted = rBottomY + selectionYParams.selectionYMid
                    canvas?.drawPoint(i.toFloat(), rBottomYPlotted.toFloat() * height, paintCurveSecondary)
                }

                val rComposedY = if (rTopY != null && rBottomY != null) {
                    val rWeightFrom = max(rTopStart, rBottomStart)
                    val rWeightTo = min (rTopStart + rTopPerceptRange, rBottomStart + rBottomPerceptRange)
                    val rWeightDist = rWeightTo - rWeightFrom
                    var topWeight = if (rX < rWeightFrom) 1.0 else if (rX > rWeightTo) 0.0 else 1 - ((rX - rWeightFrom) / rWeightDist)
                    topWeight = sqrt(topWeight)
                    var bottomWeight = if (rX < rWeightFrom) 0.0 else if (rX > rWeightTo) 1.0 else (rX - rWeightFrom) / rWeightDist
                    bottomWeight = sqrt(bottomWeight)
                    val rTopYCentered = rTopY - selectionYParams.selectionYMid
                    (rTopYCentered * topWeight + rBottomY * bottomWeight) + selectionYParams.selectionYMid
                } else null
                if (rComposedY != null) canvas?.drawPoint(i.toFloat(), rComposedY.toFloat() * height, paintCurvePrimary)

                canvas?.drawRect(0f, 0f, (rTopStart * r2pCoef).toFloat(), bottom.toFloat() ,paintUnknownArea)
                canvas?.drawRect(((rTopStart + rTotalDist) * r2pCoef).toFloat(), 0f, width.toFloat(), bottom.toFloat() ,paintUnknownArea)

            } else if (rTopDist != null) {

                val rTopStart = 0
                val rTopY = when {
                    rX < rTopStart -> {
                        0.0
                    }
                    rX > rTopStart + rTopPerceptRange -> {
                        selectionYParams.selectionYMid
                    }
                    else -> {
                        val rTopX = (rX - rTopStart).toDouble()
                        selectionYSolver.curve(
                            rTopPerceptRange.toDouble(),
                            selectionYParams.selectionYMid,
                            1 - selectionYParams.stiffness,
                            rTopX)?.second
                    }
                }
                canvas?.drawRect(rTopPerceptRange * r2pCoef, 0f, width.toFloat(), bottom.toFloat() ,paintUnknownArea)
                if (rTopY != null) canvas?.drawPoint(i.toFloat(), rTopY.toFloat() * height, paintCurvePrimary)

            } else if (rBottomDist != null) {

                val rBottomStart = rTopPerceptRange
                val rBottomY = when {
                    rX < rBottomStart -> {
                        selectionYParams.selectionYMid
                    }
                    rX > rBottomStart + rBottomPerceptRange -> {
                        1.0
                    }
                    else -> {
                        val rBottomX = (rX - rBottomStart).toDouble()
                        val rBottomY = selectionYSolver.curve(
                            rBottomPerceptRange.toDouble(),
                            1.0 - selectionYParams.selectionYMid,
                            1 - selectionYParams.stiffness,
                            rBottomX)?.second
                        rBottomY?.plus(selectionYParams.selectionYMid)
                    }
                }
                canvas?.drawRect(0f, 0f, rBottomStart * r2pCoef, bottom.toFloat() ,paintUnknownArea)
                if (rBottomY != null) canvas?.drawPoint(i.toFloat(), rBottomY.toFloat() * height, paintCurvePrimary)

            } else {

                val rMidY = selectionYParams.selectionYMid
                canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintUnknownArea)
                if (rMidY != null) canvas?.drawPoint(i.toFloat(), rMidY.toFloat() * height, paintCurvePrimary)
            }
        }
    }

    private fun plotSelectionAndDistTexts(canvas: Canvas?) {

        selectionYData?.let { data ->
            data.selectionY?.let { selection ->
                val selectionYPx = (height * selection).toFloat()

                data.contentTopDistPx.let { topDist ->
                    val distText = if (topDist == null) "beyond perception" else "$topDist px"
                    canvas?.drawText("top distance: $distText", 72f, selectionYPx - 48f, paintScrollPxText)
                }

                canvas?.drawLine(0f, selectionYPx, width.toFloat(), selectionYPx, paintSelectionRatio)
                canvas?.drawText("selection: ${data.selectionY}", 72f, selectionYPx + 24f, paintSelectionText)

                data.contentBottomDistPx.let { bottomDist ->
                    val distText = if (bottomDist == null) "beyond perception" else "$bottomDist px"
                    canvas?.drawText("bottom distance: $distText", 72f, selectionYPx + 92f, paintScrollPxText)
                }
            }
        }
    }

    companion object {
        const val TAG = "SelectionYDebugView"
    }

}