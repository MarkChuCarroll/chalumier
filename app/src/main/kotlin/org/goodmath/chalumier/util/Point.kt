/*
 * Copyright 2024 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.goodmath.chalumier.util

import eu.mihosoft.vvecmath.Vector3d
import kotlinx.serialization.Serializable
import org.goodmath.chalumier.geom.XYZ

@Serializable
data class Point(val x: Double, val y: Double) {
    fun at(z: Double): Vector3d = Vector3d.xyz(x, y, z)

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

    operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)

    operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)

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
