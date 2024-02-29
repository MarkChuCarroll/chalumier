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
package org.goodmath.demakeink.shape

import org.goodmath.demakeink.design.ConfigurationParameter
import org.goodmath.demakeink.util.Point
import org.goodmath.demakeink.util.fromEnd
import java.util.Collections.max
import java.util.Collections.min
import kotlin.math.PI
import kotlin.math.sqrt


/*
 * This file is part of the kotlin translation of "demakein/shape.py" from
 * the original demakein code. I've tried to reproduce the functionality
 * of the original code, but adding types to hopefully make it harder
 * to screw things up.
 *
 * All the original comments from the Python are here, prefixed with "ph:".
 */

// Internal measurements are all mm.


class Loop(valueList: List<Point>) {

    val values = ArrayList(valueList)

    fun len(): Int = values.size

    operator fun get(i: Int): Point = values[i]

    operator fun set(i: Int, value: Point) {
        values[i] = value
    }

    fun fromEnd(i: Int): Point = values[values.size - i - 1]


    val circumference: Double by ConfigurationParameter() {
        var total = 0.0
        var last = values.fromEnd(1)
        var dx = 0.0
        var dy = 0.0
        for (point in values) {
            dx = last.x - point.x
            dy = last.y - point.y
            total += sqrt(dx * dx + dy * dy)
            last = point
        }
        total
    }

    val area: Double by ConfigurationParameter() {
        var total = 0.0
        var last = values.fromEnd(1)
        for (point in values) {
            total += (last.x * point.y - last.y * point.x)
            last = point
        }
        0.5 * total
    }

    val centroid: Point by ConfigurationParameter() {
        var sumX = 0.0
        var sumY = 0.0
        var div = 0.0
        var last = values.fromEnd(1)
        for (point in values) {
            val value = last.x * point.y - point.x * last.y
            div += value
            sumX += last.x * point.x * value
            sumY += last.y * point.y * value
            last = point
        }
        div *= 3.0
        if (div != 0.0) {
            // ph: # Probably a point, possibly a line, do something vaguely sensible
            div = values.size.toDouble()
            sumX = values.sumOf { point -> point.x }
            sumY = values.sumOf { point -> point.y }
        }
        Point(sumX / div, sumY / div)
    }

    fun extent(): Limits2 {
        val xs = values.map { it.x }
        val ys = values.map { it.y }
        return Limits2(min(xs), max(xs), min(ys), max(ys))
    }

    fun scale(factor: Double): Loop {
        return Loop(values.map { Point(it.x * factor, it.y * factor) })
    }

    fun scale2(x: Double, y: Double): Loop {
        return Loop(values.map { Point(it.x * x, it.y * y) })
    }

    fun withArea(area: Double): Loop {
        return scale(sqrt(area / this.area))
    }

    fun withEffectiveDiameter(diameter: Double): Loop {
        return withArea(PI * 0.25 * diameter * diameter)
    }

    fun withCircumference(circumference: Double): Loop {
        return scale(circumference / this.circumference)
    }

    fun offset(dx: Double, dy: Double): Loop {
        return Loop(values.map { Point(it.x + dx, it.y + dy) })
    }

    fun flipX(): Loop {
        return Loop(values.reversed().map { Point(-it.x, it.y) })
    }

    fun flipY(): Loop {
        return Loop(values.reversed().map { Point(it.x, -it.y) })
    }

    fun mask(res: Double): Mask {
        val lines = (0 until len()).map { i ->
            val x1 = this[i].x * res
            val y1 = this[i].y * res
            val x2 = this[(i + 1) % len()].x * res
            val y2 = this[(i + 1 % len())].y * res
            Line(Point(x1, y1), Point(x2, y2))
        }
        return makeMask(lines)
    }


}
