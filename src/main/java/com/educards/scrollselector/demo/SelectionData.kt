package com.educards.scrollselector.demo

data class SelectionData(

    var contentTopDist: Int? = null,

    var contentBottomDist: Int? = null,

    /**
     * The `selectionRatio` of the scrollable [View]'s viewport:
     * * 0.0 - the top edge of the [View]
     * * 0.5 - middle part of the [View]
     * * 1.0 - the bottom edge of the [View]
     */
    var selectionRatio: Double? = null,

    )