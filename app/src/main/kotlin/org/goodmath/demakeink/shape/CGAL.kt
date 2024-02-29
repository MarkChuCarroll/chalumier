package org.goodmath.demakeink.shape

import org.goodmath.demakeink.geom.XYZ
import org.goodmath.demakeink.util.Point
import kotlin.math.pow


data class Extent(
        val xMin: Double, val xMax: Double,
        val yMin: Double, val yMax: Double,
        val zMin: Double, val zMax: Double)


// Currently a placeholder, while I figure out what I'm going to
// do about CGAL.
interface Shape {
    fun clip(other: Shape)

    fun extent(): Extent
    fun copy(): Shape

    fun size(): XYZ

    fun rotate(x: Int, y: Int, z: Int, degrees: Double)

    fun move(x: Double, y: Double, z: Double)

    fun polygonMask(): Shape2
}

// placeholder
fun create(verts: List<XYZ>, tris: List<Triple<Int, Int, Int>>, name: String? = null, accuracy: Int =   2.0.pow(16.0).toInt()): Shape {
    throw Exception("Not implemented yet")
}

interface Shape2 {
    fun loops(holes: Boolean = true): List<Loop>
    fun loop(holes: Boolean = true): Loop {
        return loops()[0]
    }

    fun extent(): Extent

    fun remove(other: Shape2)

    fun add(other: Shape2)

    fun clip(other: Shape2)

    fun invert()

    fun orientation(): Int

    fun isEmpty(): Boolean

    fun move(x: Double, y: Double, accuracy: Double = 2.0.pow(16))

    fun intersects(other: Shape2): Boolean

    fun minkowskiSum(other: Shape2): Shape2

    fun erosion(other: Shape2): Shape2

    fun offsetCurve(amount: Double, quality: Double? = null): Shape2

    fun to3(): Shape
}

fun emptyShape2(): Shape2 {
    throw NotImplementedError()
}

fun createPolygon2(verts: List<Point>, accuracy: Double = 2.0.pow(16)): Shape2 {
    throw NotImplementedError()
}

