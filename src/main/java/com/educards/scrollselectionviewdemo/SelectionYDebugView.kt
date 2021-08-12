package com.educards.scrollselectionviewdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.absoluteValue

/**
 * Draws the trajectory and current state of `selectionY`.
 */
class SelectionYDebugView: View {

    lateinit var selectionYSolver: SelectionYSolver
    lateinit var selectionYData: SelectionYData
    lateinit var selectionYParams: SelectionYParams

    private val paintYRatio = Paint().apply {
        isAntiAlias = true
        color = Color.BLUE
        strokeWidth = 10f
        style = Paint.Style.STROKE
    }

    private val paintCurve = Paint().apply {
        isAntiAlias = true
        color = Color.BLUE
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val paintCurveComposed = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        strokeWidth = 10f
        style = Paint.Style.STROKE
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
    ) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        plotSelectionY(canvas)
        plotCurves(canvas)
    }

    private fun plotCurves(canvas: Canvas?) {

        // Associations to 'val' just to prevent null checks
        // and nullable ? types operators in rest of the code.
        val contentTopDistPx = selectionYData?.contentTopDistPx
        val contentBottomDistPx = selectionYData?.contentBottomDistPx

        if (contentTopDistPx != null && SelectionYData.isContentTopDetected(contentTopDistPx)
           && contentBottomDistPx != null && SelectionYData.isContentBottomDetected(contentBottomDistPx)) {

            val contentHeightPx = (contentTopDistPx.absoluteValue + contentBottomDistPx.absoluteValue).toFloat()
            val content2PlotCoef = width.toFloat() / contentHeightPx

            for (i in 0..width) {

                val topCurveHeightScaled = selectionYParams.selectionYMid * height
                val bottomCurveHeightScaled = (1 - selectionYParams.selectionYMid) * height

                val topPerceptionRangeScaled = selectionYParams.contentTopPerceptionRangePx.toDouble() * content2PlotCoef
                val pTop = if (i > topPerceptionRangeScaled) Pair(i.toDouble(), topCurveHeightScaled) else {
                    selectionYSolver.curve(
                        topPerceptionRangeScaled,
                        topCurveHeightScaled,
                        1 - selectionYParams.stiffness,
                        i / topPerceptionRangeScaled
                    )
                }

                val bottomPerceptionRangeScaled = selectionYParams.contentBottomPerceptionRangePx.toDouble() * content2PlotCoef
                val bottomShift = width - bottomPerceptionRangeScaled
                val pBottom = if (i < bottomShift) Pair(i.toDouble(), 0.0) else {

                    val p = selectionYSolver.curve(
                        bottomPerceptionRangeScaled,
                        bottomCurveHeightScaled,
                        1 - selectionYParams.stiffness,
                        (i - bottomShift) / bottomPerceptionRangeScaled
                    )

                    Pair(
                        bottomShift + p.first,
                        p.second
                    )
                }

                val pComposed = Pair(
                    i.toDouble(),
                    pTop.second + pBottom.second
                )

                canvas?.drawPoint(i.toFloat(), pTop.second.toFloat(), paintCurve)
                canvas?.drawPoint(i.toFloat(), pBottom.second.toFloat() + topCurveHeightScaled.toFloat(), paintCurve)
                canvas?.drawPoint(i.toFloat(), pComposed.second.toFloat(), paintCurveComposed)
            }
        }
    }

    private fun plotSelectionY(canvas: Canvas?) {
        selectionYData?.selectionY?.let { selectionYRatio ->
            val selectionYPx = (height * selectionYRatio).toFloat()
            canvas?.drawLine(0f, selectionYPx, width.toFloat(), selectionYPx, paintYRatio)
        }
    }

    companion object {
        const val TAG = "SelectionYDebugView"
    }

}