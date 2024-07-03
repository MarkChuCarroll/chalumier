package org.goodmath.chalumier.geom

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.jcsg.Polyhedron
import eu.mihosoft.vvecmath.Transform
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.geom.ThreeDPoint
import org.goodmath.chalumier.shape.Loop
import org.goodmath.chalumier.shape.extrudeProfile
import org.goodmath.chalumier.shape.extrusion
import org.goodmath.chalumier.util.Point
import java.lang.Math
import java.nio.file.Path
import java.util.ArrayList
import kotlin.collections.map
import kotlin.collections.toTypedArray
import kotlin.io.path.div
import kotlin.io.path.writeText

data class Bounds(val min: ThreeDPoint, val max: ThreeDPoint) {
    val center: ThreeDPoint = (min + max) * 0.5
    val bounds: ThreeDPoint = ThreeDPoint(
        Math.abs(max.x - min.x), Math.abs(max.y - min.y),
        Math.abs(max.z - min.z)
    )

    operator fun times(d: Double): Bounds {
        return Bounds(min * d, max * d)
    }
}

interface ThreeDGeometry<Body: ThreeDBody<Body>, Shape: TwoDShape<Shape>> {
    val lowerGeometry: TwoDGeometry<Shape>
    fun extrudeShape(shape: (List<Double>) -> Shape,
                     profiles: List<Profile>): Body
    
    fun extrudeShapes(zs: List<Double>,
                      shapes: List<Shape>): Body


    fun cylinder(radius: Double, height: Double): Body

    fun cube(length: Double, width: Double, height: Double): Body

    fun block(p1: ThreeDPoint, p2: ThreeDPoint, ramp: Double = 0.0): Body


}

interface ThreeDBody<T: ThreeDBody<T>> {
    fun save(dir: Path, filename: String)
    fun toText(): String
    fun scale(factor: Double): T
    fun rotate(x: Double, y: Double, z: Double): T
    fun translate(x: Double, y: Double, z: Double): T
    fun union(other: T): T
    fun difference(other: T): T
    fun intersect(other: T): T
    fun bounds(): Bounds
    fun positionNicely(): T
}



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
    fun withCircumference(target: Double): T
    fun offset(x: Double, y: Double): T
}

interface TwoDGeometry<Shape: TwoDShape<Shape>> {
    fun circle(diameter: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun ellipse(width: Double, height: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun square(side: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun rectangle(length: Double, width: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun roundedRectangle(length: Double, width: Double, diameter: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun squaredCircle(diameter: Double = 1.0, origin: Point = Point(0.0, 0.0)): Shape
    fun halfRoundedRectangle(width: Double, height: Double, origin: Point = Point(0.0, 0.0)): Shape
    fun polygon(points: List<Point>, origin: Point= Point(0.0, 0.0)): Shape
}

