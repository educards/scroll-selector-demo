package com.educards.scrollselectionviewdemo

import java.lang.Math.cbrt
import kotlin.math.*

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
        x: Double
    ): Pair<Double, Double>? {

        // Bezier in general doesn't have the requested properties. It's not event a
        // function by definition, Bezier is a parametric function.
        // But, with the right constraints it give the same value as the desired function.
        return curveBezierForX(width, height, curvature, x)
    }

    private fun curveBezierForP(
        width: Double,
        height: Double,
        curvature: Double,
        p: Double
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

        val x = ((1 - p).pow(3) * x0) +
                (3 * (1 - p).pow(2) * p * x1) +
                (3 * (1 - p) * p.pow(2) * x2) +
                (p.pow(3) * x3)

        val y = ((1 - p).pow(3) * y0) +
                (3 * (1 - p).pow(2) * p * y1) +
                (3 * (1 - p) * p.pow(2) * y2) +
                (p.pow(3) * y3)

        return Pair(x, y)
    }

    private fun curveBezierForX(
        width: Double,
        height: Double,
        curvature: Double,
        x: Double
    ): Pair<Double, Double>? {
        val t = findT(width, curvature, x)
        return if (t == null) {
            null
        } else {
            curveBezierForP(width, height, curvature, t)
        }
    }

    private fun findT(
        width: Double,
        curvature: Double,
        x: Double): Double? {

        val x0 = .0
        val x1 = curvature * width
        val x2 = width - curvature * width
        val x3 = width

        val roots = findRoots(x, x0, x1, x2, x3);
        if (roots?.isNotEmpty() == true) {
            for (t in roots) {
                if (t >= 0 && t < 1) return t
            }
        }
        return null
    }

    /**
     * Find the roots for a cubic polynomial with bernstein coefficients
     * {pa, pb, pc, pd}. The function will first convert those to the
     * standard polynomial coefficients, and then run through Cardano's
     * formula for finding the roots of a depressed cubic curve.
     *
     * [Source](https://stackoverflow.com/a/51883347/915756)
     * @author [Mike 'Pomax' Kamermans](https://stackoverflow.com/users/740553/mike-pomax-kamermans)
     */
    private fun findRoots(x: Double, pa: Double, pb: Double, pc: Double, pd: Double): DoubleArray? {

        val pa3 = 3 * pa
        val pb3 = 3 * pb
        val pc3 = 3 * pc
        val a = -pa + pb3 - pc3 + pd
        var b = pa3 - 2 * pb3 + pc3
        var c = -pa3 + pb3
        var d = pa - x

        // Fun fact: any Bezier curve may (accidentally or on purpose)
        // perfectly model any lower order curve, so we want to test
        // for that: lower order curves are much easier to root-find.
        if (approximately(a, 0.0)) {
            // this is not a cubic curve.
            if (approximately(b, 0.0)) {
                // in fact, this is not a quadratic curve either.
                return if (approximately(c, 0.0)) {
                    // in fact in fact, there are no solutions.
                    doubleArrayOf()
                } else doubleArrayOf(-d / c)
                // linear solution:
            }
            // quadratic solution:
            val q: Double = sqrt(c * c - 4 * b * d)
            val b2 = 2 * b
            return doubleArrayOf(
                (q - c) / b2,
                (-c - q) / b2
            )
        }

        // At this point, we know we need a cubic solution,
        // and the above a/b/c/d values were technically
        // a pre-optimized set because a might be zero and
        // that would cause the following divisions to error.
        b /= a
        c /= a
        d /= a
        val b3 = b / 3
        val p = (3 * c - b * b) / 3
        val p3 = p / 3
        val q = (2 * b * b * b - 9 * b * c + 27 * d) / 27
        val q2 = q / 2
        val discriminant = q2 * q2 + p3 * p3 * p3
        val u1: Double
        val v1: Double

        // case 1: three real roots, but finding them involves complex
        // maths. Since we don't have a complex data type, we use trig
        // instead, because complex numbers have nice geometric properties.
        return if (discriminant < 0) {
            val mp3 = -p / 3
            val r: Double = sqrt(mp3 * mp3 * mp3)
            val t = -q / (2 * r)
            val cosphi: Double = if (t < -1) -1.0 else if (t > 1) 1.0 else t
            val phi: Double = acos(cosphi)
            val crtr: Double = cbrt(r)
            val t1 = 2 * crtr
            doubleArrayOf(
                t1 * cos(phi / 3) - b3,
                t1 * cos((phi + TAU) / 3) - b3,
                t1 * cos((phi + 2 * TAU) / 3) - b3
            )
        } else if (discriminant == 0.0) {
            u1 = if (q2 < 0) cbrt(-q2) else -cbrt(q2)
            doubleArrayOf(
                2 * u1 - b3,
                -u1 - b3
            )
        } else {
            val sd: Double = sqrt(discriminant)
            u1 = cbrt(-q2 + sd)
            v1 = cbrt(q2 + sd)
            doubleArrayOf(u1 - v1 - b3)
        }
    }

    private inline fun approximately(a: Double, b: Double): Boolean = abs(a-b) < 0.000001

    companion object {
        const val TAG = "SelectionYSolver"

        private const val TAU = 2 * Math.PI
    }

}