package org.goodmath.chalumier.geom

import org.goodmath.chalumier.design.curves.squared
import org.goodmath.chalumier.util.Point
import kotlin.math.PI
import kotlin.math.sqrt

object Scad2D: TwoDGeometry<Scad2DShape> {
    override fun circle(diameter: Double, origin: Point): Scad2DShape = ScadEllipse(origin, diameter, diameter)
    override fun ellipse(width: Double, height: Double, origin: Point): Scad2DShape = ScadEllipse(origin, width, height)

    override fun square(side: Double, origin: Point): Scad2DShape = ScadRectangle(origin, side, side)

    override fun rectangle(length: Double, width: Double, origin: Point): Scad2DShape = ScadRectangle(origin, length, width)

    override fun roundedRectangle(
        width: Double,
        height: Double,
        diameter: Double,
        origin:Point
    ): Scad2DShape = ScadRoundedRectangle(origin, width, height, diameter)

    override fun squaredCircle(
        diameter: Double,
        origin:Point
    ): Scad2DShape {
        val a = PI * diameter*diameter
        val width = sqrt(a)
        return square(width, origin)
    }

    override fun halfRoundedRectangle(
        width: Double,
        height: Double,
        origin:Point
    ): Scad2DShape = ScadHalfRoundedRectangle(origin, width, height)

    override fun polygon(points: List<Point>, origin: Point): Scad2DShape = ScadPolygon(points, origin)
}
abstract class Scad2DShape: TwoDShape<Scad2DShape> {
    abstract fun render(): String


    fun trans(origin: Point, r: String): String {
        return r
//        return if (origin != Point(0.0, 0.0)) {
//            "translate([-${origin.x}, -${origin.y}, 0]) { $r }"
//        } else {
//            r
//        }
    }

    override fun withArea(targetArea: Double): Scad2DShape {
        return scale(targetArea / area())
    }

    override fun withCircumference(target: Double): Scad2DShape {
        return scale(target / circumference())
    }
}

open class ScadRectangle(val origin: Point, val width: Double, val height: Double): Scad2DShape() {
    override fun scale(factor: Double): Scad2DShape {
        return ScadRectangle(origin, width * factor, height * factor)
    }

    override fun scale2(xFactor: Double, yFactor: Double): Scad2DShape {
        return ScadRectangle(origin, width * xFactor, height * yFactor)
    }

    fun corners(): List<Point> {
        return listOf(
            Point(origin.x + width / 2, origin.y + height / 2),
            Point(origin.x + width / 2, origin.y - height / 2),
            Point(origin.x - width / 2, origin.y - height / 2),
            Point(origin.x - width / 2, origin.y - height / 2)
        )
    }

    override fun extent(): Bounds2D {
        val cs = corners()
        return Bounds2D(cs.minOf { it.x }, cs.maxOf { it.x }, cs.minOf { it.y }, cs.maxOf { it.x })
    }

    override fun area(): Double {
        return width * height
    }

    override fun circumference(): Double {
        return width*2 + height*2
    }

    override fun offset(x: Double, y: Double): Scad2DShape {
        return ScadRectangle(Point(origin.x + x, origin.y + y), width, height)
    }

    override fun render(): String {
        return trans(origin, "square([$width, $height], center=true)")
    }
}

class ScadRoundedRectangle(origin: Point, width: Double, height: Double, val diameter: Double):
    ScadRectangle(origin, width, height) {
    override fun render(): String {
        //return trans(origin, "round2d(${diameter}) { square([$width, $height], center=true); }")
        return trans(origin, "square([$width, $height], center=true)")
    }

    override fun offset(x: Double, y: Double): Scad2DShape {
        return ScadRoundedRectangle(Point(origin.x + x, origin.y + y), width, height, diameter)
    }

}

class ScadHalfRoundedRectangle(origin: Point, width: Double, height: Double):
    ScadRectangle(origin, width, height) {
    override fun render(): String {
        val circ = "circle(${2.0*width})"
        val scaled = "scaled([1.0, ${height/width}, 1.0]) { $circ }"

        return trans(origin, "right_half(planar=true) { top_half(planar=true) { $scaled } }")
    }

    override fun offset(x: Double, y: Double): Scad2DShape {
        return ScadHalfRoundedRectangle(Point(origin.x + x, origin.y + y), width, height)
    }

}

class ScadEllipse(val origin: Point, val width: Double, val height: Double): Scad2DShape() {
    override fun scale(factor: Double): Scad2DShape {
        return ScadEllipse(origin, width * factor, height * factor)
    }

    override fun scale2(xFactor: Double, yFactor: Double): Scad2DShape {
        return ScadEllipse(origin, width * xFactor, height * yFactor)
    }

    override fun extent(): Bounds2D {
        return Bounds2D(
            origin.x - width / 2.0,
            origin.x + width / 2.0,
            origin.y - height / 2.0,
            origin.y + height / 2.0
        )
    }

    override fun area(): Double {
        return PI * (width / 2.0) * (height / 2.0)
    }

    override fun circumference(): Double {
        val a = width / 2.0
        val b = height / 2.0
        val num = 3.0 * ((a - b).squared())
        val rootTerm = sqrt(-3.0 * (a - b).squared() / (a + b).squared() + 4.0)
        val denom = (a + b).squared() * (rootTerm + 10)
        return PI * (a + b) * ((num / denom) + 1)
    }

    override fun offset(x: Double, y: Double): Scad2DShape {
        return ScadEllipse(Point(origin.x + x, origin.y + y), width, height)
    }

    override fun render(): String {
        val circle = "circle(${width})"
        val ell = if (width != height) {
            "scale([1.0, ${height / width}, 1.0]) $circle"
        } else {
            "$circle"
        }
        return trans(origin, ell)
    }
}

class ScadPolygon(val points: List<Point>, val origin: Point): Scad2DShape() {
    override fun render(): String {
        return "polygon(points=[${points.map { "[${it.x}, ${it.y}]" }.joinToString(", ")}])"
    }

    override fun scale(factor: Double): Scad2DShape {
        return ScadPolygon(points.map { it * factor }, origin )
    }

    override fun scale2(xFactor: Double, yFactor: Double): Scad2DShape {
        return ScadPolygon(points.map { (x, y) -> Point(x*xFactor, y*yFactor)}, origin)
    }

    override fun extent(): Bounds2D {
        return Bounds2D(points.minOf { it.x}, points.maxOf { it.x }, points.minOf{it.y}, points.maxOf { it.y })
    }

    override fun area(): Double {
        var total = 0.0
        var last = points.last()
        for (point in points) {
            total += (last.x * point.y - last.y*point.x)
            last = point
        }
        return 0.5*total
    }

    override fun circumference(): Double {
        var total = 0.0
        var last = points.last()
        var dx: Double
        var dy: Double
        for (point in points) {
            dx = last.x - point.x
            dy = last.y - point.y
            total += sqrt(dx * dx + dy * dy)
            last = point
        }
        return total
    }

    override fun offset(x: Double, y: Double): Scad2DShape {
        return ScadPolygon(points.map { Point(it.x + x, it.y + y)}, origin)
    }

}