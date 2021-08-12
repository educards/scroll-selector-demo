package com.educards.scrollselectionviewdemo

/**
 * Input params for [SelectionYSolver].
 */
data class SelectionYParams(

    var contentTopPerceptionRangePx: Int = 2500,

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
