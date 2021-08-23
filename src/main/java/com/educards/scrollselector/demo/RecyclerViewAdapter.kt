package com.educards.scrollselector.demo

import android.content.Context
import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewAdapter(
    private val context: Context,
    private val data: List<Spannable>

) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.content)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {

        val studyMaterialTextView = LayoutInflater
            .from(viewGroup.context)
            .inflate(R.layout.item, viewGroup, false)
                as TextView

        // We want to update Spannable (add and remove spans) of the provided data
        // by textView.getText() references. To allow this we must ensure that TextView won't
        // create a copy of Spannable. Thus we need to provide our custom SpannableFactory implementation
        // (source: https://medium.com/androiddevelopers/underspanding-spans-1b91008b97e4).
        studyMaterialTextView.setSpannableFactory(object: Spannable.Factory() {
            override fun newSpannable(source: CharSequence?): Spannable {
                return source as Spannable
            }
        })

        return ViewHolder(studyMaterialTextView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val spannable = data[position]
        // We have to use BufferType.SPANNABLE here.
        // For explanation consult section named "Setting text for maximum performance"
        // in https://medium.com/androiddevelopers/underspanding-spans-1b91008b97e4.
        viewHolder.textView.setText(spannable, TextView.BufferType.SPANNABLE)
    }

    override fun getItemCount() = data.size

    companion object {
        private const val TAG = "RecyclerViewAdapter"
    }

}
