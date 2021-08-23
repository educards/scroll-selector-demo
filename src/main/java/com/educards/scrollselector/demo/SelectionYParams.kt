package com.educards.scrollselector.demo

/**
 * Input params for [SelectionYSolver].
 */
data class SelectionYParams(

    /**
     * Positive integer which determines how far will the algorithm
     * look for finding the top edge of the content wrapped in [RecyclerView].
     * * **Top edge distance value**: The top edge distance is null (not detected withing the range)
     * or a positive integer (detected) which is always <= as this value (`contentTopDistancePx <= contentTopPerceptionRangePx`).
     * * **Impact on transition curve**: The bigger the `contentTopPerceptionRangePx` the smoother is the transition from
     * `selectionYMid` to content edge.
     * * **Impact on performance**: Big values of `contentTopPerceptionRangePx` may result in poor performance, because
     * the algorithm internally creates `View` instance to measure the dimension of hidden elements.
     */
    var contentTopPerceptionRangePx: Int = 2500,

    /**
     * Positive integer which is the equivalent of [contentTopPerceptionRangePx] for detecting the content bottom edge.
     */
    var contentBottomPerceptionRangePx: Int = 2500,

    /**
     * * 0 - Viewport top
     * * 1 - Viewport bottom
     */
    var selectionYMid: Double = .5,

    /**
     * * 0 - TODO define
     * * 1 - Straight line
     */
    var stiffness: Double = 0.5

)
