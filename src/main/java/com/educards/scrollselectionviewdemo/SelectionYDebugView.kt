package com.educards.scrollselectionviewdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Draws the trajectory and current state of `selectionY`.
 */
class SelectionYDebugView: View {

    var selectionYData: SelectionYData? = null

    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.BLUE
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

        selectionYData?.selectionYRatio?.let { selectionYRatio ->
            val selectionYPx = (height * selectionYRatio).toFloat()
            canvas?.drawLine(0f, selectionYPx, width.toFloat(), selectionYPx, paint)
        }
    }

}