package com.educards.scrollselectionviewdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.educards.scrollselectionviewdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.inflate(layoutInflater, R.layout.activity_main, null, false ) as ActivityMainBinding
    }

    private val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

    private val breakIterator = BreakIterator()

    private val sentenceHighlightSpan by lazy {
        ForegroundColorSpan(resources.getColor(R.color.purple_700))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initRecyclerView()
    }

    private fun initRecyclerView() {

        val adapter = RecyclerViewAdapter(this, DEMO_DATA)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnScrollListener(object: OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val childCount = layoutManager.childCount
                Log.d(TAG, "onScrolled [dx=$dx, dy=$dy, childCount=$childCount]")

                for (childIndex in 0 until childCount) {
                    val childView = layoutManager.getChildAt(childIndex) as TextView
                    val spannable = childView.text as Spannable

                    if (childView.y <= 0 && childView.bottom > 0) {
                        val sentenceInterval = breakIterator.getSentenceInterval(spannable, 100)
                        setSpan(spannable, sentenceInterval.first, sentenceInterval.second)
                    } else {
                        removeSpan(spannable)
                    }
                }
            }
        })
    }

    private fun setSpan(spannable: Spannable, spanStartIndex: Int, spanEndIndex: Int) {
        spannable.setSpan(sentenceHighlightSpan, spanStartIndex, spanEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun removeSpan(spannable: Spannable) {
        spannable.removeSpan(sentenceHighlightSpan)
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