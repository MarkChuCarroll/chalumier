package org.goodmath.demakeink.shape

import org.goodmath.demakeink.util.Point
import org.goodmath.demakeink.util.fromEnd
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


/*
 * This file is part of the kotlin translation of "demakein/mask.py" from
 * the original demakein code. I've tried to reproduce the functionality
 * of the original code, but adding types to hopefully make it harder
 * to screw things up.
 *
 * All the original comments from the Python are here, prefixed with "ph:".
 */


data class Bounds(val x: Int, val y: Int, val width: Int, val height: Int) {

    fun intersection(other: Bounds): Bounds {
        return if (width == 0 || height == 0) {
            other
        } else if (other.width == 0 || other.height == 0) {
            this
        } else {
            val x = max(x, other.x)
            val y = max(y, other.y)
            val width = max(0, min(x + width, other.x + other.width))
            val height = max(0, min(y + height, other.y + other.height))
            Bounds(x, y, width, height)
        }
    }

    fun union(other: Bounds): Bounds {
        return if (width == 0 || height == 0) {
            other
        } else if (other.width == 0 || other.height == 0) {
            this
        } else {
            val x = min(x, other.x)
            val y = min(y, other.y)
            val width = max(x + width, other.width + other.width) - x
            val height = max(y + height, other.y + other.height) - y
            Bounds(x, y, width, height)
        }
    }
}

class Grid<T>(val width: Int, val height: Int, val init: (x: Int, y: Int) -> T) {
    val backing: MutableList<T> = (0 until width).flatMap {
        x -> (0 until height).map { y -> init(x, y) }
    }.toMutableList()

    operator fun get(x: Int, y: Int): T {
        return backing[width*y+x]
    }

    operator fun set(x: Int, y: Int, v: T) {
        backing[width*y+x] = v
    }

    fun<U> map(op: (T) -> U): Grid<U> {
        return Grid<U>(width, height) { x, y -> op(this[x, y])}
    }

    fun<U> zip(other: Grid<U>): Grid<Pair<T, U>> {
        val minWidth = min(width, other.width)
        val minHeight = min(height, other.height)
        return Grid<Pair<T, U>>(minWidth, minHeight) { x, y -> Pair(this[x,y], other[x,y])}
    }
}

fun lineParam(p1: Point, p2: Point): Point {
    if (p2.x == p1.x) {
        return Point(0.0, p1.y)
    }
    val m = (p2.y-p1.y)/(p2.x-p1.x)
    val c = p1.y-m*p1.x
    return Point(m,c)
}

data class Line(val p1: Point, val p2: Point)

fun makeMask(lines: List<Line>): Mask {
    val baseX = floor(lines.map { l -> min(l.p1.x, l.p2.x) }.min()).toInt()
    val baseY = floor(lines.map { l -> min(l.p1.y, l.p2.y)}.min()).toInt()
    val width = ceil(lines.map { l -> max(l.p1.x, l.p2.x)}.max()).toInt()
    val height = ceil(lines.map { l -> max(l.p1.y, l.p2.y)}.max()).toInt()
    val lineCount = HashMap<Line, Int>()
    for( (p1, p2) in lines) {
        val (n, key) = if (p1.x < p2.x) {
            Pair(1, Line(p1, p2))
        } else {
            Pair(-1, Line(p2, p1))
        }
        lineCount[key] = (lineCount[key]?:0) + n
    }
    val count = Grid<Int>(height, width) { _, _ -> 0}
    for ((l, nn) in lineCount.entries) {
        if (nn != 0) {
            val ix1 = ceil(l.p1.x).toInt()
            val ix2 = ceil(l.p2.x).toInt()
            if (ix1 != ix2) {
                val (m, c) = lineParam(l.p1, l.p2)
                for (x in ix1 until ix2) {
                    val y = ceil(m*x+c).toInt()
                    count[x-baseX, y-baseY] += nn
                }
            }
        }
    }
    for (y in 1 until height) {
        for (x in 0 until width) {
            count[x, y] += count[x, y - 1]
        }
    }
    return Mask(Bounds(baseX, baseY, count.width, count.height)) { x, y ->
        count[x, y] > 0
    }
}

/**
 * ph originally called this type "BigMatrix", and made it look as if it
 * was going to be used for multiple types of values. But in practice,
 * it's really just used as a mask with booleans.
 */
class Mask(val bounds: Bounds, val init: (x: Int, y: Int) -> Boolean) {
    val data = Grid(bounds.width, bounds.height, init)

    fun copy(): Mask {
        return Mask(bounds) { x, y -> data[x,y] }
    }
    operator fun get(x: Int, y: Int): Boolean {
        return data[x - bounds.x, y - bounds.y]
    }

    operator fun set(x: Int, y: Int, v: Boolean) {
        data[x - bounds.x, y - bounds.y] = v
    }

    fun shift(newX: Int, newY: Int): Mask {
        return Mask(Bounds(bounds.x + newX, bounds.y + newY,
                bounds.width, bounds.height),  { x, y -> data[x, y] })
    }

    fun clip(clipBounds: Bounds): Mask {
        if (clipBounds.y == bounds.y && clipBounds.x == bounds.x &&
                clipBounds.width == bounds.width && clipBounds.height == bounds.height) {
            return this
        }
        val result = blanks(bounds, false)
        val x1 = max(bounds.x, clipBounds.x)
        val x2 = min(bounds.x + bounds.width, clipBounds.x + clipBounds.width)
        val y1 = max(bounds.y, clipBounds.y)
        val y2 = min(bounds.y + bounds.height, clipBounds.y + clipBounds.height)
        if (y1 < y2 && x1 < x2) {
            (y1 until y2).forEach { y ->
                (x1 until x2).forEach { x ->
                    result[x + clipBounds.x, y + clipBounds.y] = data[x + bounds.x, y + bounds.y]
                }
            }
        }
        return result
    }

    fun apply(other: Mask, bounds: Bounds, op: (Boolean, Boolean) -> Boolean): Mask {
        val a = clip(bounds)
        val b = other.clip(bounds)
        val newData = a.data.zip(b.data).map { (one, two) -> op(one, two) }
        return Mask(a.bounds) { x, y -> newData[x, y] }
    }

    fun unionApply(other: Mask, operation: (Boolean, Boolean) -> Boolean): Mask {
        val newBounds = bounds.union(other.bounds)
        return apply(other, newBounds, operation)
    }

    fun intersectionApply(other: Mask, operation: (Boolean, Boolean) -> Boolean): Mask {
        val newBounds = bounds.intersection(other.bounds)
        return apply(other, bounds, operation)
    }

    infix fun and(other: Mask): Mask {
        return intersectionApply(other) { a, b -> a and b }
    }

    infix fun or(other: Mask): Mask {
        return unionApply(other) { a, b -> a or b}
    }

    infix fun andNot(other: Mask): Mask {
        return apply(other, bounds) { a, b -> a and !b }
    }

    fun spans(): Iterator<Triple<Int, Int, Int>> {
        val matrix = this
        return object: Iterator<Triple<Int, Int, Int>> {
            var y: Int = 0
            var x:  Int = 0
            var start: Int = 0
            var next: Triple<Int, Int, Int>? = scanForNext()

            override fun hasNext(): Boolean {
                return next != null
            }

            private fun scanForNext(): Triple<Int, Int, Int>? {
                while (y < bounds.y && x < bounds.x) {
                    val p = matrix.data[x, y]
                    x++
                    if (x == matrix.bounds.width) {
                        y++
                        x = 0
                    }
                    if (!p) {
                        if (start != x) {
                            next = Triple(y+matrix.bounds.y, start+matrix.bounds.x, x+matrix.bounds.x)
                            return next
                        }
                    }
                }
                if (start != bounds.width) {
                    next = Triple(y+bounds.y, start+bounds.x, bounds.width+bounds.x)
                    return next
                }
                next = null
                return next
            }

            override fun next(): Triple<Int, Int, Int> {
                if (next == null) {
                    throw Exception("Iterator is exhausted")
                }
                val result = next!!
                scanForNext()
                return result
            }

        }
    }

    fun morph(mask: Mask, op: (Mask?, Mask) -> Mask): Mask {
        val spans: MutableList<Mask?> = mutableListOf(null, this)
        fun getSpan(n: Int): Mask {
            while (spans.size <= n) {
                spans.add(op(spans.fromEnd(1), shift(spans.size - 1, 0)))
            }
            return spans[n]!!
        }
        var result: Mask? = null
        for ((y: Int, x1: Int, x2: Int) in mask.spans()) {
            val span = getSpan(x2-x1).shift(x1,y)
            if (result == null) {
                result = span
            } else {
                result = op(result, span)
            }
        }
        return result!!
    }

    fun dilate(mask: Mask): Mask {
        return morph(mask) { a, b -> if (a == null) { b } else { a or b }}
    }

    fun erode(mask: Mask): Mask {
        return morph(mask) { a, b -> if (a == null) { b } else {a and b}}
    }

    fun open(mask: Mask): Mask {
        return erode(mask).dilate(mask)
    }

    fun close(mask: Mask): Mask {
        return dilate(mask).erode(mask)
    }

    /*
    fun trace(res: Double=1.0): BigMatrix {
        return trace(this, res)
    }

     */

    companion object {
        fun blanks(bounds: Bounds, value: Boolean): Mask =
                Mask(bounds) { x, y -> value }

    }
}

