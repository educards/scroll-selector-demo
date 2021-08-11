package com.educards.scrollselectionviewdemo

import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

/**
 * @see curve
 */
class SelectionYSolver {

    fun calculateSelectionYRatio(
        edgeDistanceTopPx: Int?,
        upwardsPerceptionRangePx: Int,
        edgeDistanceBottomPx: Int?,
        downwardsPerceptionRangePx: Int
    ): Double {

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

    /**
     * Computes a value of a function `f` with the following properties:
     * * `f` is continuous
     * * Is constant for `f(0) = 0`
     * * Is constant for `f(width) = height`
     * * `f` is monotonically decreasing (`x <= y | f(x) <= f(y)`)
     *
     * These properties ensure, that by proper combining of multiple curves the
     * resulting curve will also have the same properties.
     *
     * Bezier curve is one possible implementation.
     */
    fun curve(
        width: Double,
        height: Double,
        curvature: Double,
        t: Double
    ): Pair<Double, Double> {
        return curveBezier(width, height, curvature, t)
    }

    private fun curveBezier(
        width: Double,
        height: Double,
        curvature: Double,
        t: Double
    ): Pair<Double, Double> {

        // B(t) = (1 - t)3P0 + 3(1-t)2tP1 + 3(1-t)t2P2 + t3P3

        val x0 = .0
        val y0 = .0

        val x1 = curvature * width
        val y1 = .0

        val x2 = width - curvature * width
        val y2 = height

        val x3 = width
        val y3 = height

        val x = ((1 - t).pow(3) * x0) +
                (3 * (1 - t).pow(2) * t * x1) +
                (3 * (1 - t) * t.pow(2) * x2) +
                (t.pow(3) * x3)

        val y = ((1 - t).pow(3) * y0) +
                (3 * (1 - t).pow(2) * t * y1) +
                (3 * (1 - t) * t.pow(2) * y2) +
                (t.pow(3) * y3)

        return Pair(x, y)
    }

}