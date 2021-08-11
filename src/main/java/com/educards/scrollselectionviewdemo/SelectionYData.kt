package com.educards.scrollselectionviewdemo

data class SelectionYData(

    var contentTopDistPx: Int? = null,

    var contentBottomDistPx: Int? = null,

    /**
     * * Value `(0, 1)` if the ratio has been calculated.
     *     * `0` - The line located at the top edge is selected
     *     * `1` - The line located at the bottom edge is selected
     * * `null` if the selection is not defined.
     */
    var selectionY: Double? = null,

    var selectionYDefault: Double? = SELECTION_Y_MID,

    ) {

    companion object {
        const val CONTENT_TOP_PERCEPTION_RANGE_PX = 2500
        const val CONTENT_BOTTOM_PERCEPTION_RANGE_PX = 2500
        const val SELECTION_Y_MID = .1

        /**
         * * 0 - TODO define
         * * 1 - Straight line
         */
        const val STIFFNESS = 0.5

        fun isContentTopDetected(contentTopDistPx: Int?): Boolean {
            return contentTopDistPx != null && contentTopDistPx != Int.MIN_VALUE
        }

        fun isContentBottomDetected(contentBottomDistPx: Int?): Boolean {
            return contentBottomDistPx != null && contentBottomDistPx != Int.MAX_VALUE
        }
    }

}
