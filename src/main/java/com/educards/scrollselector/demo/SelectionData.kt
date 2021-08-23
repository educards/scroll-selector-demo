package com.educards.scrollselector.demo

data class SelectionData(

    var contentTopDistPx: Int? = null,

    var contentBottomDistPx: Int? = null,

    /**
     * * Value `(0, 1)` if the ratio has been calculated.
     *     * `0` - The line located at the top edge is selected
     *     * `1` - The line located at the bottom edge is selected
     * * `null` if the selection is not defined.
     */
    var selectionY: Double? = null,

    ) {

    companion object {

        fun isContentTopDetected(contentTopDistPx: Int?): Boolean {
            return contentTopDistPx != null && contentTopDistPx != Int.MIN_VALUE
        }

        fun isContentBottomDetected(contentBottomDistPx: Int?): Boolean {
            return contentBottomDistPx != null && contentBottomDistPx != Int.MAX_VALUE
        }

    }

}
