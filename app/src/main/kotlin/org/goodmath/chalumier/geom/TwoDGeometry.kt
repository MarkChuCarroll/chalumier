package org.goodmath.chalumier.geom

import org.goodmath.chalumier.util.Point
import kotlin.math.PI

data class Bounds2D(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double) {
    operator fun plus(b: Bounds2D): Bounds2D {
        return Bounds2D(
            minX + b.minX, maxX + b.maxX,
            minY + b.minY, maxY + b.maxY
        )
    }

    operator fun times(d: Double): Bounds2D {
        return Bounds2D(minX * d, maxX * d, minY * d, maxY * d)
    }
}

interface TwoDShape<T: TwoDShape<T>> {
    fun scale(factor: Double): T
    fun scale2(xFactor: Double, yFactor: Double): T
    fun extent(): Bounds2D
    fun area(): Double
    fun circumference(): Double
    fun withArea(targetArea: Double): T
    fun withEffectiveDiameter(diameter: Double): T = withArea(PI * 0.25 * diameter * diameter)
    fun withCircumference(target: Double): T
    fun offset(x: Double, y: Double): T
}

// Trying to figure out howto match PH's original code.
//
// His rectangles and polygons are all done with absolute
// positions.
//
// His circles are defined by diameter, and always centered on (0.0).
// His squares are centered on (0,0), and give their side as
// the distance from (0.0) to a size - so they're effectively
// based on radius, not diameter.
// But his squaredcircle squares are based on diameters.

interface TwoDGeometry<Shape: TwoDShape<Shape>> {
    fun circle(diameter: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun ellipse(width: Double, height: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun square(side: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun phSquare(size: Double):Shape = square(2*size)


    fun rectangle(length: Double, width: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun phRect(x0: Double, x1: Double, y0: Double, y1: Double): Shape =
        rectangle(x1-x0, y1-y0, Point((x1-x0)/2.0, (y1-y0)/2.0))

    fun roundedRectangle(length: Double, width: Double, diameter: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun phRoundedRectangle(x0: Double, x1: Double, y0: Double, y1: Double, diameter: Double): Shape =
        roundedRectangle(x1-x0, y1-y0, diameter,  Point((x1-x0)/2.0, (y1-y0)/2.0))
    fun squaredCircle(xPad: Double, yPad:Double, diameter: Double = 1.0, origin: Point = Point(0.0, 0.0)): Shape
    fun halfRoundedRectangle(width: Double, height: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun polygon(points: List<Point>, origin: Point = Point(0.0, 0.0)): Shape
}
