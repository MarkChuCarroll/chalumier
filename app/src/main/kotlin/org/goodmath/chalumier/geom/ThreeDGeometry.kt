package org.goodmath.chalumier.geom

import org.goodmath.chalumier.design.Profile
import java.lang.Math
import java.nio.file.Path

data class Bounds3D(val min: ThreeDPoint, val max: ThreeDPoint) {
    val center: ThreeDPoint = (min + max) * 0.5
    val bounds: ThreeDPoint = ThreeDPoint(
        Math.abs(max.x - min.x), Math.abs(max.y - min.y),
        Math.abs(max.z - min.z)
    )

    operator fun times(d: Double): Bounds3D {
        return Bounds3D(min * d, max * d)
    }

    fun offset(x: Double, y: Double, z: Double): Bounds3D {
        return Bounds3D(min + ThreeDPoint(x, y, z), max + ThreeDPoint(x, y, z))
    }

}

interface ThreeDGeometry<Body: ThreeDBody<Body>, Shape: TwoDShape<Shape>> {
    val lowerGeometry: TwoDGeometry<Shape>
    fun extrudeShape(shape: (List<Double>) -> Shape,
                     profiles: List<Profile>): Body

    fun extrudeShapes(zValues: List<Double>,
                      shapes: List<Shape>): Body


    fun cylinder(radius: Double, height: Double): Body

    fun cube(width: Double, height: Double, depth: Double): Body

    fun block(p1: ThreeDPoint, p2: ThreeDPoint, ramp: Double = 0.0): Body


}

interface ThreeDBody<T: ThreeDBody<T>> {
    fun copy(): T
    fun label(label: String)
    fun save(dir: Path, filename: String)
    fun toText(): String
    fun scale(factor: Double)
    fun rotate(x: Double, y: Double, z: Double)
    fun translate(x: Double, y: Double, z: Double)
    fun union(other: T): T
    fun difference(other: T): T
    fun intersect(other: T): T
    fun bounds(): Bounds3D
    fun positionNicely(): T
}



