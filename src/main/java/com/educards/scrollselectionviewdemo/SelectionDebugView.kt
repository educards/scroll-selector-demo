package com.educards.scrollselectionviewdemo

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

/**
 * Draws the trajectory and the current state of a `selection`.
 */
class SelectionYDebugView: View {

    lateinit var selectionYSolver: SelectionYSolver
    lateinit var selectionYData: SelectionYData
    lateinit var selectionYParams: SelectionYParams

    private val paintSelectionLine = Paint(ANTI_ALIAS_FLAG).apply {
        strokeWidth = 60f
        alpha = 150
        style = Paint.Style.STROKE
    }

    private val paintSelectionText = Paint(ANTI_ALIAS_FLAG).apply {
        textSize = 64f
    }

    private val paintDistanceText = Paint(ANTI_ALIAS_FLAG).apply {
        textSize = 64f
    }

    private val paintCurvePrimary = Paint(ANTI_ALIAS_FLAG).apply {
        strokeWidth = 9f
        style = Paint.Style.STROKE
    }

    private val paintCurveSecondary = Paint(ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val paintPlotMid = Paint(ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val paintUnknownArea = Paint().apply {
        style = Paint.Style.FILL
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
    ) : super(context, attrs, defStyleAttr) {

        val a = context.obtainStyledAttributes(attrs, R.styleable.SelectionDebugView)
        paintCurvePrimary.color = a.getColor(R.styleable.SelectionDebugView_primaryCurveColor, -1)
        paintCurveSecondary.color = a.getColor(R.styleable.SelectionDebugView_secondaryCurveColor, -1)
        paintDistanceText.color = a.getColor(R.styleable.SelectionDebugView_distanceTextColor, -1)
        paintSelectionText.color = a.getColor(R.styleable.SelectionDebugView_selectionTextColor, -1)
        paintSelectionLine.color = a.getColor(R.styleable.SelectionDebugView_selectionLineColor, -1)
        paintUnknownArea.color = a.getColor(R.styleable.SelectionDebugView_unknownAreaColor, -1)
        paintPlotMid.color = paintCurveSecondary.color
        a.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        plotCurves(canvas)
        plotSelectionAndDistTexts(canvas)
    }

    private fun calculateTopY(rX: Int, rTopStart: Double, rTopPerceptRange: Int) = when {
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
                rTopX
            )?.second
        }
    }

    private fun calculateBottomY(rX: Int, rBottomStart: Double, rBottomPerceptRange: Int, yShift: Double): Double? {
        val y = when {
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
                    rBottomX
                )?.second
            }
        }
        return y?.plus(yShift)
    }

    private fun plotCurves(canvas: Canvas?) {

        val rTopDist = selectionYData.contentTopDistPx?.absoluteValue
        val rTopPerceptRange = selectionYParams.contentTopPerceptionRangePx
        val rBottomDist = selectionYData.contentBottomDistPx
        val rBottomPerceptRange = selectionYParams.contentBottomPerceptionRangePx
        val rTotalPerceptRange = rTopPerceptRange + rBottomPerceptRange
        val r2pCoef = width.toFloat() / rTotalPerceptRange.toFloat()

        val pTopPerceptRange = rTopPerceptRange * r2pCoef
        canvas?.drawLine(pTopPerceptRange, 0f, pTopPerceptRange, height.toFloat(), paintPlotMid)

        if (rTopDist != null && rBottomDist != null) {

            val rTotalDist = (rTopDist + rBottomDist).toDouble()
            val rTopBottomPerceptRatio = rTopPerceptRange.toDouble() / rTotalPerceptRange.toDouble()
            val rTopStart = rTopPerceptRange - (rTotalDist * rTopBottomPerceptRatio)
            val rBottomStart = rTopPerceptRange - rBottomPerceptRange + rTotalDist * (1.0 - rTopBottomPerceptRatio)

            canvas?.drawRect(0f, 0f, (rTopStart * r2pCoef).toFloat(), bottom.toFloat() ,paintUnknownArea)
            canvas?.drawRect(((rTopStart + rTotalDist) * r2pCoef).toFloat(), 0f, width.toFloat(), bottom.toFloat() ,paintUnknownArea)

            for (i in 0..width) {
                val rX = getRX(i, r2pCoef)

                val rTopY = calculateTopY(rX, rTopStart, rTopPerceptRange)
                if (rTopY != null) canvas?.drawPoint(i.toFloat(), rTopY.toFloat() * height, paintCurveSecondary)

                val rBottomY = calculateBottomY(rX, rBottomStart, rBottomPerceptRange, 0.0)
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
            }

        } else if (rTopDist != null) {

            val rTopStart = 0.0

            canvas?.drawRect(rTopPerceptRange * r2pCoef, 0f, width.toFloat(), bottom.toFloat(), paintUnknownArea)

            for (i in 0..width) {
                val rX = getRX(i, r2pCoef)
                val rTopY = calculateTopY(rX, rTopStart, rTopPerceptRange)
                if (rTopY != null) canvas?.drawPoint(i.toFloat(), rTopY.toFloat() * height, paintCurvePrimary)
            }

        } else if (rBottomDist != null) {

            val rBottomStart = rTopPerceptRange.toDouble()

            canvas?.drawRect(0f, 0f, (rBottomStart * r2pCoef).toFloat(), bottom.toFloat() ,paintUnknownArea)

            for (i in 0..width) {
                val rX = getRX(i, r2pCoef)
                val rBottomY = calculateBottomY(rX, rBottomStart, rBottomPerceptRange, selectionYParams.selectionYMid)
                if (rBottomY != null) canvas?.drawPoint(i.toFloat(), rBottomY.toFloat() * height, paintCurvePrimary)
            }

        } else {

            val rMidY = selectionYParams.selectionYMid
            canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintUnknownArea)
            for (i in 0..width) {
                if (rMidY != null) canvas?.drawPoint(i.toFloat(), rMidY.toFloat() * height, paintCurvePrimary)
            }
        }
    }

    private inline fun getRX(i: Int, r2pCoef: Float) = (i.toDouble() / r2pCoef).roundToInt()

    private fun plotSelectionAndDistTexts(canvas: Canvas?) {

        selectionYData?.let { data ->
            data.selectionY?.let { selection ->
                val selectionYPx = (height * selection).toFloat()

                data.contentTopDistPx.let { topDist ->
                    val distText = if (topDist == null) "beyond perception" else "$topDist px"
                    canvas?.drawText("top distance: $distText", 72f, selectionYPx - 48f, paintDistanceText)
                }

                canvas?.drawLine(0f, selectionYPx, width.toFloat(), selectionYPx, paintSelectionLine)
                canvas?.drawText("selection: ${data.selectionY}", 72f, selectionYPx + 24f, paintSelectionText)

                data.contentBottomDistPx.let { bottomDist ->
                    val distText = if (bottomDist == null) "beyond perception" else "$bottomDist px"
                    canvas?.drawText("bottom distance: $distText", 72f, selectionYPx + 92f, paintDistanceText)
                }
            }
        }
    }

    companion object {
        const val TAG = "SelectionYDebugView"
    }

}