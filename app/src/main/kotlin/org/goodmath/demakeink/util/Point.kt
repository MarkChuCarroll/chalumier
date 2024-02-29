package org.goodmath.demakeink.util

import kotlinx.serialization.Serializable
import org.goodmath.demakeink.geom.XYZ

@Serializable
data class Point(val x: Double, val y: Double) {
    fun at(z: Double): XYZ = XYZ(x, y, z)

    operator fun compareTo(other: Point): Int {
        if (x < other.x) {
            return -1
        } else if (x == other.x) {
            if (y < other.y) {
                return -1
            } else if (y == other.y) {
                return 0
            }
        }
        return 1
    }

    operator fun plus(other: Point): Point =
            Point(x+other.x, y+other.y)

    operator fun minus(other: Point): Point =
            Point(x-other.x, y-other.y)

}

fun List<Point>.max(): Point {
    var m = this[0]
    for (p in this) {
        if (p > m) {
            m = p
        }
    }
    return m
}

fun List<Point>.min(): Point {
    var m = this[0]
    for (p in this) {
        if (p < m) {
            m = p
        }
    }
    return m
}
