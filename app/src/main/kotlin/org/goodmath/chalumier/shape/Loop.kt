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
package org.goodmath.chalumier.shape

import io.github.xn32.json5k.Json5
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.goodmath.chalumier.config.Configurable
import org.goodmath.chalumier.config.DoubleParameter
import org.goodmath.chalumier.util.Point
import org.goodmath.chalumier.util.fromEnd
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
val pretty = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

class Loop(valueList: List<Point>): Configurable<Loop>("loop") {

    override fun toString(): String {

        val j = buildJsonObject {
            putJsonArray("values") {
                for (p in loopValues) {
                    addJsonObject {
                        if (p.x.isNaN()) {
                            put("x", JsonPrimitive("NaN"))
                        } else if (p.x.isInfinite()) {
                            put("x", JsonPrimitive("INF"))
                        } else {
                            put("x", JsonPrimitive(p.x))
                        }
                        if (p.y.isNaN()) {
                            put("y", JsonPrimitive("NaN"))
                        } else if (p.y.isInfinite()) {
                            put("y", "INF")
                        } else {
                            put("y", JsonPrimitive(p.y))
                        }
                    }
                }
            }
        }
        return pretty.encodeToString(j)
    }

    val loopValues = ArrayList(valueList)

    fun len(): Int = loopValues.size

    operator fun get(i: Int): Point = loopValues[i]

    operator fun set(i: Int, value: Point) {
        loopValues[i] = value
    }

    fun fromEnd(i: Int): Point = loopValues[loopValues.size - i - 1]


    val circumference by DoubleParameter {
        var total = 0.0
        var last = loopValues.fromEnd(1)
        var dx = 0.0
        var dy = 0.0
        for (point in loopValues) {
            dx = last.x - point.x
            dy = last.y - point.y
            total += sqrt(dx * dx + dy * dy)
            last = point
        }
        total
    }

    val area by DoubleParameter {
        var total = 0.0
        var last = loopValues.fromEnd(1)
        for (point in loopValues) {
            total += (last.x * point.y - last.y * point.x)
            last = point
        }
        0.5 * total
    }

    val centroid: Point by lazy {
        var sumX = 0.0
        var sumY = 0.0
        var div = 0.0
        var last = loopValues.fromEnd(1)
        for (point in loopValues) {
            val value = last.x * point.y - point.x * last.y
            div += value
            sumX += (last.x + point.x) * value
            sumY += (last.y + point.y) * value
            last = point
        }
        div *= 3.0
        if (div == 0.0) {
            // ph: # Probably a point, possibly a line, do something vaguely sensible
            div = loopValues.size.toDouble()
            sumX = loopValues.sumOf { point -> point.x }
            sumY = loopValues.sumOf { point -> point.y }
        }
        Point(sumX / div, sumY / div)
    }

    fun extent(): Limits2 {
        val xs = loopValues.map { it.x }
        val ys = loopValues.map { it.y }
        return Limits2(min(xs), max(xs), min(ys), max(ys))
    }

    fun scale(factor: Double): Loop {
        return Loop(loopValues.map { Point(it.x * factor, it.y * factor) })
    }

    fun scale2(x: Double, y: Double): Loop {
        return Loop(loopValues.map { Point(it.x * x, it.y * y) })
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
        return Loop(loopValues.map { Point(it.x + dx, it.y + dy) })
    }

    fun flipX(): Loop {
        return Loop(loopValues.reversed().map { Point(-it.x, it.y) })
    }

    fun flipY(): Loop {
        return Loop(loopValues.reversed().map { Point(it.x, -it.y) })
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
