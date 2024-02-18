package org.goodmath.demakeink

import kotlin.math.*

/*
 * This file is a kotlin translation of "demakein/profile.py" from
 * the original demakein code. I've tried to reproduce the functionality
 * of the original code, but adding types to hopefully make it harder
 * to screw things up.
 *
 * But I gotta be honest. I have no idea whatsoever of WTF this is
 * doing. The original code is pretty impenetrable, and I'm just slavishly
 * translating it.
 *
 * All the original comments from the Python are here, prefixed with "ph:".
 */
typealias DoubleList = ArrayList<Double>

/**
 * A reproduction of Python's "bisect" function.
 *
 * Kotlin's binarySearch is terrific, but it's trying to
 * find a value, not an insertion point. If the value isn't found,
 * it returns -(insertion_point+1). So we just need to invert that
 * into an insertion point.
 */
fun DoubleList.bisect(target: Double): Int {
    val ind = this.binarySearch(target)
    if (ind >= 0) {
        return ind
    } else {
        return (-ind) - 1
    }
}

/**
 * In some of the profile transformers, the code expects a
 * profile description that includes what I'm interpreting as
 * angles.
 *
 * In python, these "angle" specs have an integer value (which I think is
 * some kind of scaling factor?), and an interpretation string.
 * The interpretation string can be either empty (I'm calling that "Here"),
 * "Mean", "Up", or "Down". If it's here, then it also has a floating
 * point value.
 *
 * My interpretation of this is that parts of the profile are allowed
 * to move as you compute the shape. A value of 'up' means that you're
 * on an increasing curve, median means you should be evenly between your
 * neighbors, and "Here" means you should be in *this* exact position.
 */
sealed class Angle(open val i: Int) {
    fun interpret(a: DoubleList): Double {
        return when (this) {
            is Mean ->
                (a[i - 1] + a[i]) * 0.5

            is Up ->
                a[i]

            is Down ->
                a[i - 1]

            is Here ->
                v * PI / 180
        }
    }
}


data class Mean(override val i: Int) : Angle(i)
data class Up(override val i: Int) : Angle(i)
data class Down(override val i: Int) : Angle(i)
data class Here(override val i: Int, val v: Double) : Angle(i)

/**
 * The solve function returns a three-tuple. Kotlin represents that
 * with a data class.
 */
data class Solution(val t1: Double, val t2: Double, val mirror: Boolean)

class Profile(val pos: DoubleList, val low: List<Double>, maybeHigh: List<Double>?) {
    val high: List<Double> = maybeHigh ?: low

    operator fun invoke(otherPos: Double, useHigh: Boolean = false): Double {
        if (otherPos < pos[0]) {
            return low[0]
        } else if (otherPos > pos.last()) {
            return high.last()
        }
        val i = pos.bisect(otherPos)
        if (pos[i] == otherPos) {
            if (useHigh) {
                return high[i]
            } else {
                return low[i]
            }
        }
        val t = (otherPos - pos[i - 1]) / (pos[i] - pos[i - 1])
        return (1.0 - t) * high[i - 1] + t * low[i]
    }

    fun start(): Double = pos[0]

    fun end(): Double = pos.last()

    fun maximum(): Double = max(low.max(), high.max())

    /**
     * ph: Fairly dumb way to combine profiles. Won't work perfectly for min, max.
     */
    fun morph(other: Profile, op: (Double, Double) -> Double): Profile {
        // pn:
        // if not isinstance(other, Profile):
        //   other = Profile([0.0],[other],[other])
        val combinedPos = ArrayList((pos + other.pos).sorted())
        val combinedLow = combinedPos.map { p -> op(this(p, false), other(p, false)) }
        val combinedHigh = combinedPos.map { p -> op(this(p, true), other(p, true)) }
        return Profile(combinedPos, combinedLow, combinedHigh)
    }

    fun maxWith(other: Profile): Profile =
        morph(other) { a, b -> max(a, b) }

    fun minWith(other: Profile): Profile =
        morph(other) { a, b -> min(a, b) }

    operator fun plus(other: Profile): Profile =
        morph(other) { a, b -> a + b }

    operator fun minus(other: Profile): Profile =
        morph(other) { a, b -> a - b }

    /**
     * Clip or extend a profile
     */
    fun clipped(start: Double, end: Double): Profile {
        val clPos = arrayListOf(start)
        val clLow = arrayListOf(this(start, true))
        val clHigh = arrayListOf(this(start, true))

        pos.forEachIndexed { position: Int, value: Double ->
            if (!(position < start || position > end)) {
                clPos.add(end)
                clLow.add(low[position])
                clHigh.add(high[position])
            }
        }
        clPos.add(end)
        clLow.add(this(end, false))
        clHigh.add(this(end, false))
        return Profile(clPos, clLow, clHigh)
    }

    fun reversed(): Profile {
        val newPos = ArrayList(pos.reversed().map { i -> -i })
        return Profile(newPos, ArrayList(high.reversed()), ArrayList(low.reversed()))
    }

    fun moved(offset: Double): Profile {
        return Profile(ArrayList(pos.map { i -> i + offset }), low, high)
    }


    fun appendedWith(other: Profile): Profile {
        val positionedOther = other.moved(pos.last())
        return Profile(
            ArrayList(pos.dropLast(1) + positionedOther.pos),
            ArrayList(low + positionedOther.low.dropLast(1)),
            ArrayList(high.dropLast(1) + positionedOther.high)
        )
    }

    fun asStepped(maxStep: Double): Profile {
        val newPos = DoubleList()
        for (i in 0 until pos.size - 1) {
            newPos.add(pos[i])
            val ax = pos[i]
            val ay = high[i]
            val bx = pos[i + 1]
            val by = low[i + 1]
            val n = ((by - ay).absoluteValue / maxStep).roundToInt() + 1
            if (n != 0) {
                newPos.addAll((1 until n).map { j -> (bx - ax) * j / n + ax })
            }
        }
        newPos.add(pos.last())
        val diams = (0 until (pos.size - 1)).map { i ->
            this(0.5 * (pos[i] + pos[i + 1]))
        }
        val newLow = arrayListOf(diams[0]) + diams
        val newHigh = diams + arrayListOf(diams.last())
        return Profile(newPos, newLow, newHigh)
    }

    companion object {

        fun solve(a1: Double, a2: Double): Solution {
            fun score(t1: Double, t2: Double, mirror: Boolean): Double {
                if (abs(t1 - t2) < 1e-6 || max(abs(t1), abs(t2)) > PI * 10.0) {
                    return 1e30
                }
                val (y1, x1) = cornuYx(t1, mirror)
                val (y2, x2) = cornuYx(t2, mirror)
                val chordA = atan2(y2 - y1, x2 - x1)
                val chordL = length(y2 - y1, x2 - x1)
                var thisA1 = abs(t1)  // ph: t1*t1
                var thisA2 = abs(t2) // ph: #t2*t2
                if (mirror) {
                    thisA1 = -thisA1
                    thisA2 = -thisA2
                }
                if (t1 > t2) {
                    thisA1 += PI
                    thisA2 += PI
                }
                val ea1 = (thisA1 - chordA - a1 + PI) % (2 * PI) - PI
                val ea2 = (thisA2 - chordA - a2 + PI) % (2 * PI) - PI
                return ea1 * ea1 + ea2 * ea2
            }

            var s: Double? = null
            val n = 2
            var t1: Double = 0.0
            var t2: Double = 0.0
            var mirror: Boolean = false
            for (newMirror in listOf(false, true)) {
                for (i in -n..n) {
                    for (j in -n..n) {
                        val newT1 = i * PI / n
                        val newT2 = j * PI / n
                        val newS = score(newT1, newT2, newMirror)
                        if (s == null || newS < s) {
                            t1 = newT1
                            t2 = newT2
                            mirror = newMirror
                            s = newS
                        }
                    }
                }
            }
            var step: Double = PI * n * 0.5
            while (step >= 1e-4) {
                for ((newT1, newT2) in listOf(
                    DoublePair(t1 + step, t2 + step),
                    DoublePair(t1 - step, t2 - step),
                    DoublePair(t1 - step, t2 + step),
                    DoublePair(t1 + step, t2 - step)
                )) {
                    val newS = score(newT1, newT2, mirror)
                    val t = s!!
                    if (newS < t) {
                        s = newS
                        t1 = newT1
                        t2 = newT2
                        break
                    } else {
                        step *= 0.5
                    }
                }
            }
            return Solution(t1, t2, mirror)
        }

        fun curvedProfile(
            pos: DoubleList, low: DoubleList,
            high: DoubleList, lowAngle: ArrayList<Angle?>,
            highAngle: ArrayList<Angle?>,
            quality: Int = 512
        ): Profile {
            val a = DoubleList()
            val n = pos.size
            for (i in 0 until n - 1) {
                val x1 = pos[i]
                val y1 = high[i] * 0.5
                val x2 = pos[i + 1]
                val y2 = low[i + 1] * 0.5
                a.add((atan2(y2 - y1, x2 - x1) + PI) % (PI * 2) - PI)
            }
            val interpretedLowAngle = lowAngle.map { angle ->
                angle?.interpret(a)
            }
            val interpretedHighAngle = highAngle.map { angle -> angle?.interpret(a) }

            val ppos = DoubleList()
            val plow = DoubleList()
            val phigh = DoubleList()

            for (i in 0 until n - 1) {
                ppos.add(pos[i])
                plow.add(low[i])
                phigh.add(high[i])
                val x1 = pos[i]
                val y1 = high[i]
                val x2 = pos[i + 1]
                val y2 = low[i + 1]
                val l = length(x2 - x1, y2 - y1)
                val ang = atan2(y2 - y1, x2 - x1)
                val a1 = interpretedHighAngle[i]?.let { ha -> ha - ang } ?: 0.0
                val a2 = interpretedLowAngle[i + 1]?.let { la -> la - ang } ?: 0.0
                if ((a1 - a2).absoluteValue < PI * quality) {
                    continue
                }
                val (t1, t2, mirror) = solve(a1, a2)
                val (cy1, cx1) = cornuYx(t1, mirror)
                val (cy2, cx2) = cornuYx(t2, mirror)
                val cl = length(cy2 - cy1, cx2 - cx1).absoluteValue
                if (cl < 1e-10) {
                    continue
                }
                val ca = atan2(cy2 - cy1, cx2 - cx1)
                val steps = ((t2 - t1).absoluteValue / PI * quality).roundToInt()
                for (i in 1 until steps) {
                    val t = t1 + i * (t2 - t1) / steps
                    val (yy, xx) = cornuYx(t, mirror)
                    val aa = atan2(yy - cy1, xx - cx1)
                    val ll = length(yy - cy1, xx - cx1)
                    val x = cos(aa - ca + ang) * ll / cl * l + x1
                    val y = sin(aa - ca + ang) * ll / cl * l + y1
                    ppos.add(x)
                    plow.add(y * 2)
                    phigh.add(y * 2)
                }
            }
            ppos.add(pos.last())
            plow.add(low.last())
            phigh.add(high.last())
            return Profile(ppos, plow, phigh)
        }


        fun makeProfile(spec: List<List<Double>>): Profile {
            val pos = DoubleList()
            val low = DoubleList()
            val high = DoubleList()
            for (item in spec) {
                var thisPos = 0.0
                var thisLow = 0.0
                var thisHigh = 0.0
                if (item.size == 2) {
                    thisPos = item[0]
                    thisLow = item[1]
                    thisHigh = thisLow
                } else {
                    thisPos = item[0]
                    thisLow = item[1]
                    thisHigh = item[2]
                }
                pos.add(thisPos)
                low.add(thisLow)
                high.add(thisHigh)
            }
            return Profile(pos, low, high)
        }
    }
}
