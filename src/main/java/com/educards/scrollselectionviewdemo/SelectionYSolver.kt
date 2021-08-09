package com.educards.scrollselectionviewdemo

import kotlin.math.absoluteValue
import kotlin.math.sign

class SelectionYSolver {

    fun calculateSelectionYRatio(edgeDistanceTopPx: Int?, upwardsPerceptionRangePx: Int, edgeDistanceBottomPx: Int?, downwardsPerceptionRangePx: Int): Double {

        if (edgeDistanceBottomPx == null

            // Note, we can't use the following (and more simple to read) version here:
            // ('edgeDistance.absoluteValue >= perceptionRangePx')
            // because Int.MIN_VALUE.absoluteValue also returns MIN_VALUE due to stack overflow.
            // @see documentation of Int.absoluteValue
            || edgeDistanceBottomPx >= downwardsPerceptionRangePx
            || edgeDistanceBottomPx <= -downwardsPerceptionRangePx)
        {
            return 0.0

        } else {
            val direction = edgeDistanceBottomPx.sign
            val distanceRatio = 1 - (edgeDistanceBottomPx.absoluteValue.toDouble() / downwardsPerceptionRangePx)
            return distanceRatio * direction
        }

    }

}