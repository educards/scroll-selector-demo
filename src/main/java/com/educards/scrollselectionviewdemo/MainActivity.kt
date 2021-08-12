package com.educards.scrollselectionviewdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.SimpleItemAnimator
import com.educards.scrollselectionviewdemo.databinding.ActivityMainBinding
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.inflate(layoutInflater, R.layout.activity_main, null, false ) as ActivityMainBinding
    }

    private val solver = SelectionYSolver()
    private val selectionYParams = SelectionYParams()
    private val selectionYData = SelectionYData()

    private val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

    private val breakIterator = BreakIterator()

    private val sentenceHighlightSpan by lazy {
        ForegroundColorSpan(resources.getColor(R.color.purple_700))
    }

    private var currentHighlightItemPos = -1
    private var currentHighlightOffsets: Pair<Int, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initRecyclerView()
        initSelectionYDebugView()
    }

    private fun initSelectionYDebugView() {
        binding.selectionYDebugView.selectionYSolver = solver
        binding.selectionYDebugView.selectionYData = selectionYData
        binding.selectionYDebugView.selectionYParams = selectionYParams
    }

    private fun updateSelectionYDebugView() {
        binding.selectionYDebugView.invalidate()
    }

    private fun initRecyclerView() {

        val adapter = RecyclerViewAdapter(this, DEMO_DATA)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        // The items of recyclerView are frequently updated to render the updated
        // highlight span. However, having animations turned on while scrolling
        // the recyclerView and also rendering the updated item in the same time
        // causes the UI to flicker.
        // Source: https://stackoverflow.com/a/42379756/915756
        disableItemAnimations()

        binding.recyclerView.addOnScrollListener(object: OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                // interested only in vertical changes (y)
                if (dy != 0) {

                    selectionYData.contentTopDistPx = checkEdgeDistance(adapter, selectionYParams.contentTopPerceptionRangePx, false)
                    selectionYData.contentBottomDistPx = checkEdgeDistance(adapter, selectionYParams.contentBottomPerceptionRangePx, true)

                    selectionYData.selectionY = solver.calculateSelectionYRatio(
                        selectionYData.contentTopDistPx, selectionYParams.contentTopPerceptionRangePx,
                        selectionYData.contentBottomDistPx, selectionYParams.contentBottomPerceptionRangePx
                    )

                    val selectionYRatio = selectionYData?.selectionY
                    if (selectionYRatio == null) {
                        if (currentHighlightItemPos > -1) {
                            val textView = findChildByPosition(layoutManager, currentHighlightItemPos)
                            if (textView != null) {
                                removeSpan(textView, adapter)
                            }
                        }

                    } else {
                        val selectionYPx = calculateSelectionYPx(selectionYRatio)
                        if (BuildConfig.DEBUG) Log.d(TAG, "selectionY: $selectionYPx")

                        updateSelectionYDebugView()

                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstChildPos = layoutManager.findFirstVisibleItemPosition()
                        val lastChildPos = layoutManager.findLastVisibleItemPosition()
                        for (childPos in firstChildPos..lastChildPos) {
                            if (BuildConfig.DEBUG) Log.d(TAG, "childPos: $childPos")
                            val childView = findChildByPosition(layoutManager, childPos)

                            if (childView != null && childView.y <= selectionYPx && selectionYPx < childView.bottom) {

                                val lineOffsets = getLineOffsets(childView, selectionYPx)
                                if (lineOffsets != null) {

                                    // If the span was previously added to another spannable (view)
                                    // then we have to explicitly remove the span from this view.
                                    if (currentHighlightItemPos >= 0 && currentHighlightItemPos != childPos) {
                                        val textView = findChildByPosition(layoutManager, currentHighlightItemPos)
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
            }
        })
    }

    private fun disableItemAnimations() {
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

    private fun findChildByPosition(layoutManager: LinearLayoutManager, childPos: Int): TextView? {
        val childView = layoutManager.findViewByPosition(childPos)
        return if (childView == null) null else childView as TextView
    }

    private fun getSpannable(textView: TextView): Spannable = textView?.text as Spannable

    private fun checkEdgeDistance(adapter: RecyclerViewAdapter, perceptionRangePx: Int, checkBottom: Boolean): Int? {

        var positionToEvaluate = if (checkBottom) layoutManager.findLastVisibleItemPosition() else layoutManager.findFirstVisibleItemPosition()

        if (positionToEvaluate == RecyclerView.NO_POSITION) {
            return null

        } else {

            val firstChild = layoutManager.findViewByPosition(positionToEvaluate)
                ?: throw RuntimeException("Requested child view has not been laid out")

            var exploredDistance: Int
            if (checkBottom) {
                positionToEvaluate++
                exploredDistance = firstChild.y.toInt() + firstChild.height - binding.recyclerView.height
            } else {
                positionToEvaluate--
                exploredDistance = firstChild.y.toInt()
            }

            val phantomViewHolder = adapter.onCreateViewHolder(binding.recyclerView, 0)

            // Evaluate views until the watchAheadDistance is met
            // and there are children to evaluate.
            while (exploredDistance.absoluteValue < perceptionRangePx
                && 0 <= positionToEvaluate && positionToEvaluate < adapter.itemCount) {

                // Previously we evaluated the very first or the very last child view (depending on the scroll direction).
                // The next view to examine will therefore lie beyond the drawable boundary.
                // To detect the height of the next/previous child we need to measure it offscreen.
                var childView = createPhantomChild(adapter, phantomViewHolder, positionToEvaluate)

                if (checkBottom) {
                    positionToEvaluate++
                    exploredDistance += childView.measuredHeight
                } else {
                    positionToEvaluate--
                    exploredDistance -= childView.measuredHeight
                }
            }

            if (exploredDistance.absoluteValue >= perceptionRangePx) {
                return if (checkBottom) Int.MAX_VALUE else Int.MIN_VALUE
            } else {
                return exploredDistance
            }
        }
    }

    private fun createPhantomChild(adapter: RecyclerViewAdapter, phantomViewHolder: RecyclerViewAdapter.ViewHolder, position: Int): View {
        adapter.onBindViewHolder(phantomViewHolder, position)
        val childView = phantomViewHolder.textView
        layoutManager.measureChild(childView, 0, 0)
        return childView
    }

    private fun calculateSelectionYPx(selectionYRatio: Double): Int {
        return (binding.recyclerView.height * selectionYRatio).toInt()
    }

    private fun setSpan(
        textView: TextView,
        childPos: Int,
        lineOffsets: Pair<Int, Int>,
        adapter: RecyclerViewAdapter
    ) {
        val spannable = getSpannable(textView)
        val sentenceDetectionOffset = lineOffsets.first + (lineOffsets.second - lineOffsets.first) / 2
        val sentenceInterval = breakIterator.getSentenceInterval(spannable, sentenceDetectionOffset)

        spannable.setSpan(sentenceHighlightSpan, sentenceInterval.first, sentenceInterval.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        currentHighlightItemPos = childPos
        currentHighlightOffsets = lineOffsets

        // notify UI changed
        binding.recyclerView.post { adapter.notifyItemChanged(childPos) }
    }

    private fun removeSpan(
        textView: TextView,
        adapter: RecyclerViewAdapter
    ) {
        val spannable = getSpannable(textView)
        spannable.removeSpan(sentenceHighlightSpan)

        currentHighlightItemPos = -1
        currentHighlightOffsets = null

        // notify UI changed
        binding.recyclerView.post { adapter.notifyItemChanged(currentHighlightItemPos) }
    }

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

        private const val TAG = "MainActivity"

        private val DEMO_DATA: List<Spannable> = listOf(
            Html.fromHtml("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Sed felis eget velit aliquet sagittis id consectetur purus ut. Volutpat ac tincidunt vitae semper. Nec sagittis aliquam malesuada bibendum arcu vitae elementum. Volutpat maecenas volutpat blandit aliquam etiam erat velit scelerisque in. Enim ut sem viverra aliquet eget sit amet. Sodales neque sodales ut etiam sit amet nisl purus in. Egestas diam in arcu cursus. Pellentesque pulvinar pellentesque habitant morbi. Mauris pellentesque pulvinar pellentesque habitant. Faucibus ornare suspendisse sed nisi lacus sed viverra tellus in. Lectus proin nibh nisl condimentum id venenatis a. Mi in nulla posuere sollicitudin aliquam ultrices sagittis. Pharetra vel turpis nunc eget lorem dolor sed viverra. Dui vivamus arcu felis bibendum ut. Massa enim nec dui nunc mattis enim. Ut porttitor leo a diam sollicitudin.") as Spannable,
            Html.fromHtml("Pretium nibh ipsum consequat nisl vel pretium. Lacus vel facilisis volutpat est velit egestas dui. Elementum sagittis vitae et leo duis. Ultrices gravida dictum fusce ut placerat. Dignissim sodales ut eu sem integer. Elementum sagittis vitae et leo. Commodo ullamcorper a lacus vestibulum sed arcu non odio euismod. Est ullamcorper eget nulla facilisi. Integer eget aliquet nibh praesent tristique magna sit amet. Nulla pellentesque dignissim enim sit.") as Spannable,
            Html.fromHtml("Sagittis id consectetur purus ut faucibus pulvinar elementum. Non consectetur a erat nam at lectus urna duis convallis. At risus viverra adipiscing at in tellus integer feugiat scelerisque. A erat nam at lectus urna duis. Mollis aliquam ut porttitor leo a. Curabitur gravida arcu ac tortor dignissim. Ante metus dictum at tempor. Fringilla ut morbi tincidunt augue interdum velit. Sagittis orci a scelerisque purus semper. Eleifend mi in nulla posuere sollicitudin aliquam ultrices sagittis orci. Et ligula ullamcorper malesuada proin libero nunc consequat interdum varius. Volutpat commodo sed egestas egestas fringilla phasellus faucibus scelerisque eleifend. Et magnis dis parturient montes nascetur. Nullam non nisi est sit amet facilisis magna etiam tempor. Lacus viverra vitae congue eu consequat ac felis donec. Arcu cursus vitae congue mauris rhoncus aenean. Sapien pellentesque habitant morbi tristique. Aliquam sem et tortor consequat id porta nibh venenatis. Neque laoreet suspendisse interdum consectetur libero id faucibus nisl tincidunt. Dictumst vestibulum rhoncus est pellentesque elit ullamcorper.") as Spannable,
            Html.fromHtml("Sed sed risus pretium quam vulputate dignissim. Morbi blandit cursus risus at ultrices. Nisi scelerisque eu ultrices vitae auctor eu augue ut. Hac habitasse platea dictumst quisque sagittis. Ut ornare lectus sit amet. Varius duis at consectetur lorem donec massa sapien. Ante metus dictum at tempor commodo ullamcorper. Vel quam elementum pulvinar etiam. Duis at tellus at urna. Imperdiet massa tincidunt nunc pulvinar sapien et ligula ullamcorper malesuada. Quam nulla porttitor massa id neque aliquam vestibulum morbi blandit. Tellus id interdum velit laoreet id donec ultrices tincidunt. Vitae ultricies leo integer malesuada nunc. Erat velit scelerisque in dictum non consectetur a erat. Tortor aliquam nulla facilisi cras. Semper risus in hendrerit gravida. Neque convallis a cras semper auctor neque vitae. Bibendum enim facilisis gravida neque convallis. Magna ac placerat vestibulum lectus mauris ultrices eros. Gravida cum sociis natoque penatibus et magnis.") as Spannable,
            Html.fromHtml("Egestas erat imperdiet sed euismod nisi porta lorem mollis aliquam. Nunc pulvinar sapien et ligula ullamcorper malesuada. Metus vulputate eu scelerisque felis imperdiet proin. Aenean pharetra magna ac placerat vestibulum lectus mauris ultrices. Id leo in vitae turpis massa sed elementum. Justo donec enim diam vulputate. Scelerisque in dictum non consectetur. Varius quam quisque id diam. Amet nulla facilisi morbi tempus iaculis. Enim sit amet venenatis urna. Orci phasellus egestas tellus rutrum tellus pellentesque eu tincidunt tortor. Bibendum neque egestas congue quisque egestas diam. Nunc sed id semper risus in hendrerit gravida. A cras semper auctor neque vitae tempus quam pellentesque nec. Purus sit amet luctus venenatis lectus magna fringilla urna porttitor. Gravida arcu ac tortor dignissim convallis aenean et tortor. Urna condimentum mattis pellentesque id nibh tortor id aliquet lectus. Aliquam purus sit amet luctus venenatis lectus magna. Suscipit tellus mauris a diam maecenas sed enim. Est ultricies integer quis auctor elit sed vulputate.") as Spannable
        )

    }

}