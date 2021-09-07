package com.educards.scrollselector.demo

import android.content.Context
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.SimpleItemAnimator
import com.educards.scrollselector.InputParams
import com.educards.scrollselector.RecyclerViewSelector

/**
 * [RecyclerViewSelector] which selects a specific sentence of [RecyclerView] child items.
 */
open class RecyclerViewSentenceSelector(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val adapter: Adapter<*>,
    private val linearLayoutManager: LinearLayoutManager,
    inputParams: InputParams
) : RecyclerViewSelector(
    recyclerView,
    adapter,
    linearLayoutManager,
    inputParams
){

    init {

        // Disable animations.
        // The items of recyclerView are frequently updated to render the updated
        // highlight span. However, having animations turned on while scrolling
        // the recyclerView and also rendering the updated item in the same time
        // causes the UI to flicker.
        // Source: https://stackoverflow.com/a/42379756/915756
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

    private val breakIterator = BreakIterator()

    private val backgroundHighlightSpan by lazy {
        BackgroundColorSpan(context.resources.getColor(R.color.selectedSentenceBackground))
    }
    private val foregroundHighlightSpan by lazy {
        ForegroundColorSpan(context.resources.getColor(R.color.selectedSentenceForeground))
    }

    private var currentHighlightItemPos = -1
    private var currentHighlightOffsets: Pair<Int, Int>? = null

    override fun onUpdateSelection(selectionRatio: Double?, scrollDeltaY: Int, topDistance: Int?, bottomDistance: Int?) {

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onSelectionRatioChanged [selectionRatio=$selectionRatio, topDistance=$topDistance, bottomDistance=$bottomDistance]")
        }

        if (selectionRatio == null) {
            if (currentHighlightItemPos > -1) {
                val textView = findChildByPosition(linearLayoutManager, currentHighlightItemPos)
                if (textView != null) {
                    removeSpan(textView, adapter)
                }
            }

        } else {
            val selectionPx = (recyclerView.height * selectionRatio).toInt()

            val firstChildPos = linearLayoutManager.findFirstVisibleItemPosition()
            val lastChildPos = linearLayoutManager.findLastVisibleItemPosition()
            for (childPos in firstChildPos..lastChildPos) {
                val childView = findChildByPosition(linearLayoutManager, childPos)

                if (childView != null && childView.y <= selectionPx && selectionPx < childView.bottom) {

                    val lineOffsets = getLineOffsets(childView, selectionPx)
                    if (lineOffsets != null) {

                        // If the span was previously added to another spannable (view)
                        // then we have to explicitly remove the span from this view.
                        if (currentHighlightItemPos >= 0 && currentHighlightItemPos != childPos) {
                            val textView = findChildByPosition(linearLayoutManager, currentHighlightItemPos)
                            if (textView != null) {
                                removeSpan(textView, adapter)
                            }
                        }

                        if (currentHighlightOffsets?.equals(lineOffsets) != true) {
                            setSpan(childView, childPos, lineOffsets, adapter)
                        }
                    }
                }
            }
        }
    }

    private fun setSpan(
        textView: TextView,
        childPos: Int,
        lineOffsets: Pair<Int, Int>,
        adapter: Adapter<*>
    ) {
        val spannable = getSpannable(textView)
        val sentenceDetectionOffset = lineOffsets.first + (lineOffsets.second - lineOffsets.first) / 2
        val sentenceInterval = breakIterator.getSentenceInterval(spannable, sentenceDetectionOffset)

        spannable.setSpan(backgroundHighlightSpan, sentenceInterval.first, sentenceInterval.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(foregroundHighlightSpan, sentenceInterval.first, sentenceInterval.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        currentHighlightItemPos = childPos
        currentHighlightOffsets = lineOffsets

        // notify UI changed
        recyclerView.post { adapter.notifyItemChanged(childPos) }
    }

    private fun <T : RecyclerView.ViewHolder> removeSpan(
        textView: TextView,
        adapter: Adapter<T>
    ) {
        val spannable = getSpannable(textView)
        spannable.removeSpan(backgroundHighlightSpan)
        spannable.removeSpan(foregroundHighlightSpan)

        currentHighlightItemPos = -1
        currentHighlightOffsets = null

        // notify UI changed
        recyclerView.post { adapter.notifyItemChanged(currentHighlightItemPos) }
    }

    private fun findChildByPosition(layoutManager: LinearLayoutManager, childPos: Int): TextView? {
        val childView = layoutManager.findViewByPosition(childPos)
        return if (childView == null) null else childView as TextView
    }

    private fun getSpannable(textView: TextView): Spannable = textView?.text as Spannable

    /**
     * @param y Y coordinate relative to `RecyclerView` containing the `textView`
     * @return Corresponding start and end text offsets of the line located at the provided
     * `y` position.
     */
    private fun getLineOffsets(textView: TextView, y: Int): Pair<Int, Int>? {
        return if (textView.y <= y && y < textView.bottom) {
            val lineY = (y - textView.y).toInt()
            val line = textView.layout.getLineForVertical(lineY)
            Pair(
                textView.layout.getLineStart(line),
                textView.layout.getLineEnd(line)
            )
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "RecyclerViewSntSelect"
    }

}