package com.educards.scrollselectionviewdemo

data class SelectionYData(

    var edgeDistanceTopPx: Int? = null,

    var edgeDistanceBottomPx: Int? = null,

    /**
     * * Value `(0, 1)` if the ratio has been calculated.
     *     * `0` - The line located at the top edge is selected
     *     * `1` - The line located at the bottom edge is selected
     * * `null` if the selection is not defined.
     */
    var selectionYRatio: Double? = null

) {

    companion object {
        const val UPWARDS_PERCEPTION_RANGE_PX = 5000
        const val DOWNWARDS_PERCEPTION_RANGE_PX = 5000
    }

}
