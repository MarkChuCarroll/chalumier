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

import org.goodmath.chalumier.util.Point
import kotlin.math.*


fun circle(diameter: Double = 1.0, n: Int = QUALITY): Loop {
    val radius = diameter * 0.5
    return Loop((0 until n).map { i ->
        val a = (i + 0.5) * PI * 2.0 / (n.toDouble())
        Point(cos(a) * radius, sin(a) * radius)
    })
}

fun chordedCirle(amount: Double = 0.5): Loop {
    // ph: semi-circle and the like
    val a1 = PI * (0.5 + amount)
    val a2 = PI * (2.5 - amount)
    return Loop((0 until QUALITY).map { i ->
        val a = a1 + (i.toDouble() + 0.5) * (a2 - a1) / QUALITY.toDouble()
        Point(cos(a), sin(a))
    })
}

fun square(size: Double): Loop {
    return Loop(
        listOf(
            Point(size, size), Point(-size, size), Point(-size, -size), Point(size, -size)
        )
    )
}

fun squaredCircle(xPad: Double, yPad: Double, diameter: Double = 1.0): Loop {
    // ph: Squared circle with same area as circle of specified diameter
    var result = (0 until QUALITY).map { i ->
        val a = (i.toDouble() + 0.5) * PI * 2.0 / (QUALITY.toDouble())
        var x = cos(a)
        if (x < 0) {
            x -= xPad * 0.5
        } else {
            x += xPad * 0.5
        }
        var y = sin(a)
        if (y < 0) {
            y -= yPad * 0.5
        } else {
            y += yPad * 0.5
        }
        Pair(x, y)
    }
    val area = PI + xPad * yPad + xPad * 2 + yPad * 2
    val want = PI * (diameter * 0.5).pow(2)
    val scale = sqrt(want / area)
    return Loop(result.map { (x, y) -> Point(x * scale, y * scale) })
}

fun rectangle(p0: Point, p1: Point): Loop {
    return Loop(
        listOf(
            p0, Point(p1.x, p0.y), p1, Point(p0.x, p1.y)
        )
    )
}

fun roundedRectangle(p0: Point, p1: Point, diameter: Double): Loop {
    val radius = min(diameter, min(p1.x - p0.x, p1.y - p0.y)) * 0.5
    val result = (0 until QUALITY).map { i ->
        val a = (i.toDouble() + 0.5) * PI * 2.0 / QUALITY.toDouble()
        var x = cos(a) * radius
        if (x < 0.0) {
            x += (p0.x + radius)
        } else {
            x += (p1.x - radius)
        }
        var y = sin(a) * radius
        if (y < 0) {
            y += p0.y + radius
        } else {
            y += p1.y - radius
        }
        Point(x, y)
    }
    return Loop(result)
}

fun halfRoundedRectangle(p0: Point, p1: Point): Loop {
    val radius = p1.x - p0.x
    val result = (0 until QUALITY).map { i -> i.toDouble() }.map { i ->
        val a = ((i + 0.5) / QUALITY - 0.5) * PI
        val x = cos(a) * radius + p0.x
        var y = sin(a) * radius
        if (y < 0) {
            y += (p0.y + radius)
        } else {
            y += (p1.y - radius)
        }
        Point(x, y)
    }.toMutableList()
    result.add(Point(p0.x, p1.y))
    result.add(p0)
    return Loop(result)
}

fun lens(amount: Double, circumference: Double = PI): Loop {
    val turn = asin(amount)
    val turn2 = PI - turn * 2
    val shift = sin(turn)
    val result = (0 until QUALITY / 2).map { i -> i.toDouble() }.map { i ->
        val a = (i + 0.5) / QUALITY.toDouble() * 2 * turn2 + turn
        Point(cos(a), sin(a) - shift)
    }.toMutableList()
    return Loop(result + result.map { (x, y) -> Point(-x, -y) }).withCircumference(circumference)
}

fun lens2(amount: Double, circumference: Double = PI): Loop {
    val turn = PI * 0.5 * amount
    val turn2 = PI - turn * 2
    val shift = sin(turn)
    val result = (0 until QUALITY / 2).map { i -> i.toDouble() }.map { i ->
        val a = (i + 0.5) / QUALITY.toDouble() * 2 * turn2 + turn
        Point(cos(a), sin(a) - shift)
    }.toMutableList()
    return Loop(result + result.map { (x, y) -> Point(-x, -y) }).withCircumference(circumference)
}

/* ph:
#def loop_merge(loop0, loop1):
#
#def extrusion(zs, shapes, name=None):
#    centroids = [ item.centroid for item in shapes ]
#
#    faces = [ ]
#    verts = [ ]
#    vert_index = { }
#    def vert(point):
#        if point not in vert_index:
#            vert_index[point] = len(verts)
#            verts.append(point)
#        return vert_index[point]
#
#    shape_vert = [ [ vert(item+(z,)) for item in shape ] for shape,z in zip(shapes,zs) ]
#
#    end0 = vert(shapes[0].centroid+(zs[0],))
#    end1 = vert(shapes[-1].centroid+(zs[-1],))
#
#    for i in xrange(len(zs)):
#        ...
#
#    for i in xrange(len(shapes[0])):
#        j = (i+1)%len(shapes[0])
#        faces.append( (end0,shape_vert[0][i],shape_vert[0][j]) )
#    for i in xrange(len(shapes[-1])):
#        j = (i+1)%len(shapes[-1])
#        faces.append( (end1,shape_vert[-1][i],shape_vert[-1][j]) )
#
#    return create(verts, faces, name)
 */
