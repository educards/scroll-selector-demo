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
class SelectionDebugView: View {

    // To make things clearer and emphasize the domain of variables used in calculations the
    // following variables naming convention is used:
    //   p* - plot domain
    //   r* - real domain

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

    private fun plotCurves(canvas: Canvas?) {

        val rTopDist = selectionYData.contentTopDistPx
        val rTopPerceptRange = selectionYParams.contentTopPerceptionRangePx
        val rBottomDist = selectionYData.contentBottomDistPx
        val rBottomPerceptRange = selectionYParams.contentBottomPerceptionRangePx
        val rTotalPerceptRange = rTopPerceptRange + rBottomPerceptRange
        val r2pCoef = width.toFloat() / rTotalPerceptRange.toFloat()
        val rMidY = selectionYParams.selectionYMid

        val pMidY = rMidY.toFloat() * height
        canvas?.drawLine(0f, pMidY, width.toFloat(), pMidY, paintPlotMid)

        if (rTopDist != null && rBottomDist != null) {
            plotTopBottom(rTopDist, rBottomDist, rTopPerceptRange, rTotalPerceptRange, rBottomPerceptRange, canvas, r2pCoef)
        } else if (rTopDist != null) {
            plotTop(canvas, rTopPerceptRange, rTopDist, r2pCoef)
        } else if (rBottomDist != null) {
            plotBottom(rTopPerceptRange, canvas, r2pCoef, rBottomPerceptRange, rBottomDist)
        } else {
            plotNone(canvas, rTopPerceptRange, pMidY, r2pCoef)
        }
    }

    private fun plotNone(canvas: Canvas?, rTopPerceptRange: Int, pMidY: Float, r2pCoef: Float) {
        val pDistLineX = rTopPerceptRange * r2pCoef
        canvas?.drawLine(pDistLineX, 0f, pDistLineX, height.toFloat(), paintPlotMid)
        canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintUnknownArea)
        canvas?.drawLine(0f, pMidY, width.toFloat(), pMidY, paintCurvePrimary)
    }

    private fun plotBottom(
        rTopPerceptRange: Int,
        canvas: Canvas?,
        r2pCoef: Float,
        rBottomPerceptRange: Int,
        rBottomDist: Int
    ) {
        val rBottomStart = rTopPerceptRange

        val pDistLineX = (rTopPerceptRange + selectionYParams.contentBottomPerceptionRangePx - rBottomDist) * r2pCoef
        canvas?.drawLine(pDistLineX, 0f, pDistLineX, height.toFloat(), paintPlotMid)

        canvas?.drawRect(0f, 0f, rBottomStart * r2pCoef, bottom.toFloat(), paintUnknownArea)

        for (i in 0..width) {
            val rX = getRX(i, r2pCoef)
            val rBottomY = calculateBottomY(rX, rBottomStart, rBottomPerceptRange, selectionYParams.selectionYMid)
            if (rBottomY != null) canvas?.drawPoint(i.toFloat(), rBottomY.toFloat() * height, paintCurvePrimary)
        }
    }

    private fun plotTop(canvas: Canvas?, rTopPerceptRange: Int, rTopDist: Int, r2pCoef: Float) {

        val rTopStart = 0

        val pDistLineX = rTopDist * r2pCoef
        canvas?.drawLine(pDistLineX, 0f, pDistLineX, height.toFloat(), paintPlotMid)

        canvas?.drawRect(rTopPerceptRange * r2pCoef, 0f, width.toFloat(), bottom.toFloat(), paintUnknownArea)

        for (i in 0..width) {
            val rX = getRX(i, r2pCoef)
            val rTopY = calculateTopY(rX, rTopStart, rTopPerceptRange)
            if (rTopY != null) canvas?.drawPoint(i.toFloat(), rTopY.toFloat() * height, paintCurvePrimary)
        }
    }

    private fun plotTopBottom(
        rTopDist: Int,
        rBottomDist: Int,
        rTopPerceptRange: Int,
        rTotalPerceptRange: Int,
        rBottomPerceptRange: Int,
        canvas: Canvas?,
        r2pCoef: Float
    ) {
        val rTotalDist = (rTopDist + rBottomDist).toDouble()
        val rTopBottomPerceptRatio = rTopPerceptRange.toDouble() / rTotalPerceptRange.toDouble()
        val rTopStart = (rTopPerceptRange - (rTotalDist * rTopBottomPerceptRatio)).toInt()
        val rBottomStart = (rTopPerceptRange - rBottomPerceptRange + rTotalDist * (1.0 - rTopBottomPerceptRatio)).toInt()

        val pDistLineX = (rTopStart + rTopDist) * r2pCoef
        canvas?.drawLine(pDistLineX, 0f, pDistLineX, height.toFloat(), paintPlotMid)

        canvas?.drawRect(0f, 0f, rTopStart * r2pCoef, bottom.toFloat(), paintUnknownArea)
        canvas?.drawRect(((rTopStart + rTotalDist) * r2pCoef).toFloat(), 0f, width.toFloat(), bottom.toFloat(), paintUnknownArea)

        for (i in 0..width) {
            val rX = getRX(i, r2pCoef)

            val rTopY = calculateTopY(rX, rTopStart, rTopPerceptRange)
            if (rTopY != null) canvas?.drawPoint(i.toFloat(), rTopY.toFloat() * height, paintCurveSecondary)

            val rBottomY = calculateBottomY(rX, rBottomStart, rBottomPerceptRange, 0.0)
            if (rBottomY != null) {
                val rBottomYPlotted = rBottomY + selectionYParams.selectionYMid
                canvas?.drawPoint(i.toFloat(), rBottomYPlotted.toFloat() * height, paintCurveSecondary)
            }

            val rComposedY = when {
                rX < max(rTopStart, rBottomStart) -> {
                    rTopY
                }
                rX > min(rTopStart + selectionYParams.contentTopPerceptionRangePx, rBottomStart + selectionYParams.contentBottomPerceptionRangePx) -> {
                    rBottomY?.plus(selectionYParams.selectionYMid)
                }
                else -> {
                    val edgeDistanceTopPx = rX - rTopStart
                    val edgeDistanceBottomPx = selectionYParams.contentBottomPerceptionRangePx - (rX - rBottomStart)
                    selectionYSolver.curveTopBottom(selectionYParams, edgeDistanceTopPx, edgeDistanceBottomPx)
                }
            }
            if (rComposedY != null) canvas?.drawPoint(i.toFloat(), rComposedY.toFloat() * height, paintCurvePrimary)
        }
    }

    private fun calculateTopY(rX: Int, rTopStart: Int, rTopPerceptRange: Int) = when {
        rX < rTopStart -> {
            0.0
        }
        rX > rTopStart + rTopPerceptRange -> {
            selectionYParams.selectionYMid
        }
        else -> {
            val rEdgeDistanceTopPx = (rX - rTopStart).toDouble()
            selectionYSolver.curveTop(selectionYParams, rEdgeDistanceTopPx)
        }
    }

    private fun calculateBottomY(rX: Int, rBottomStart: Int, rBottomPerceptRange: Int, yShift: Double): Double? {
        val y = when {
            rX < rBottomStart -> {
                0.0
            }
            rX > rBottomStart + rBottomPerceptRange -> {
                1.0 - selectionYParams.selectionYMid
            }
            else -> {
                val x = (rX - rBottomStart).toDouble()
                selectionYSolver.curveBottom(selectionYParams, x)
            }
        }
        return y?.plus(yShift)
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
        const val TAG = "SelectionDebugView"
    }

}