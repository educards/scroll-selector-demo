package com.educards.scrollselector.demo

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class BreakIterator {

    @RequiresApi(Build.VERSION_CODES.N)
    private lateinit var icuSentenceIter: android.icu.text.BreakIterator
    private lateinit var javaSentenceIter: java.text.BreakIterator

    /**
     * @return Interval (start, end) of the sentence located at the provided position.
     */
    fun getSentenceInterval(text: CharSequence, offsetPosition: Int): Pair<Int, Int> {
        // TODO When acquiring instance use version with Locale depending the
        //      locale of the text which is displayed (english, french, ...)
        //      BreakIterator.getSentenceInstance(Locale) <---
        // TODO Init by lazy?
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!::icuSentenceIter.isInitialized) {
                Log.i(TAG, "Using 'ICU' sentence break iterator")
                icuSentenceIter = android.icu.text.BreakIterator.getSentenceInstance()
            }
            getIntervalIcu(text, icuSentenceIter, offsetPosition)
        } else {
            if (!::javaSentenceIter.isInitialized) {
                Log.i(TAG, "Using 'Java text' sentence break iterator")
                javaSentenceIter = java.text.BreakIterator.getSentenceInstance()
            }
            getIntervalJavaText(text, javaSentenceIter, offsetPosition)
        }
    }

    @RequiresApi(24)
    private fun getIntervalIcu(text: CharSequence, breakIterator: android.icu.text.BreakIterator, offsetPosition: Int): Pair<Int, Int> {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API >= 29
            breakIterator.setText(text)
        } else {
            breakIterator.setText(text.toString())
        }

        val calculatedStartPos = breakIterator.preceding(offsetPosition)
        val startPos = getResultPosition(offsetPosition, calculatedStartPos, android.icu.text.BreakIterator.DONE)
        val calculatedEndPos = breakIterator.following(offsetPosition)
        val endPos = getResultPosition(offsetPosition, calculatedEndPos, android.icu.text.BreakIterator.DONE)

        return startPos to endPos
    }

    private fun getIntervalJavaText(text: CharSequence, breakIterator: java.text.BreakIterator, offsetPosition: Int): Pair<Int, Int> {

        breakIterator.setText(text.toString())

        val calculatedStartPos = breakIterator.preceding(offsetPosition)
        val startPos = getResultPosition(offsetPosition, calculatedStartPos, java.text.BreakIterator.DONE)
        val calculatedEndPos = breakIterator.following(offsetPosition)
        val endPos = getResultPosition(offsetPosition, calculatedEndPos, java.text.BreakIterator.DONE)

        return startPos to endPos
    }

    private fun getResultPosition(initialSearchPosition: Int, calculatedPosition: Int, doneConstant: Int): Int {
        return if (calculatedPosition == doneConstant) initialSearchPosition else calculatedPosition
    }

    companion object {
        private const val TAG: String = "BreakIterator"
    }

}
