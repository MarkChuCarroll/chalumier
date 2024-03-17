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
package org.goodmath.chalumier.design

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.goodmath.chalumier.config.ListOfDoubleParameterKind
import org.goodmath.chalumier.config.ParameterKind
import org.goodmath.chalumier.errors.ChalumierException
import org.goodmath.chalumier.errors.ConfigurationParameterException
import kotlin.math.*

/*
 * This file is based on "demakein/profile.py" from
 * the original demakein code. I've tried to reproduce the functionality
 * of the original code, but adding types to hopefully make it harder
 * to screw things up.
 *
 * Where relevant, the original comments from the Python are here, prefixed with "ph:".
 */


/**
 * A reproduction of Python's "bisect" function.
 *
 * Kotlin's binarySearch is terrific, but it's trying to
 * find a value, not an insertion point. If the value isn't found,
 * it returns -(insertion_point+1). So we just need to invert that
 * into an insertion point.
 */
fun List<Double>.bisect(target: Double): Int {
    val ind = this.binarySearch(target)
    return if (ind >= 0) {
        ind
    } else {
        (-ind) - 1
    }
}


/**
 * In addition to the diameters of its segments, a profile can
 * have a _direction" parameter. I'm interpreting that as an angle
 * that describes how the transitions between segments are shaped.
 *
 * In python, these "angle" specs are characterized by:
 * 1. an integer index which is their position in the list of angles that
 *    make up the profile.
 * 2. An interpretation string. The interpretation can be empty,
 *    or it can have the value "Mean", "Up", or "Down".
 * 3. If the interpretation string is empty, then it has a value
 *    as a floating point number.
 *
 * My interpretation of this is that parts of the profile are allowed
 * to move as you compute the shape. A value of 'up' means that you're
 * on an increasing curve, "Mean" means you should be evenly between your
 * neighbors, and a number means you should be in *this* exact position.
 *
 * The index isn't included, because it's contextual - it's not a fixed value,
 * but rather a connection to how the angle is placed into a list.
 */
class Angle(val dir: AngleDirection, val v: Double? = null) {
    enum class AngleDirection {
        Here, Mean, Up, Down
    }

    fun interpret(i: Int, a: List<Double>): Double {
        return when (dir) {
            AngleDirection.Mean -> (a[i - 1] + a[i]) * 0.5

            AngleDirection.Up -> a[i]

            AngleDirection.Down -> a[i - 1]

            AngleDirection.Here -> if (v == null) {
                throw ChalumierException("Invalid angle: value of here-angle must be non-null")
            } else {
                v * PI / 180
            }
        }
    }
}

/**
 * The solve function returns a three-tuple. Kotlin represents that
 * with a data class.
 */
data class Solution(val t1: Double, val t2: Double, val mirror: Boolean)

/**
 * A profile is basically a set of stacked cylinders (or actually
 * stacked conics - the upper and lower diameters may vary). The
 * pos of each cylinder is its elevation from the bottom; the 'low'
 * value is diameter at the lower end; the "high" is the diameter at
 * the upper end.
 *
 * @param pos a position along the tube where the diameter changes.
 * @param low the diameter at the bottom of the segment.
 * @param high the diameter at the top of the segment.
 */
@Serializable
data class Profile(val pos: ArrayList<Double>, val low: ArrayList<Double>, val high: ArrayList<Double> = low) {

    /**
     * Compute the diameter of the profile at a location.
     */
    operator fun invoke(location: Double, useHigh: Boolean = false): Double {
        if (location < pos[0]) {
            return low[0]
        } else if (location > pos.last()) {
            return high.last()
        }
        val i = pos.bisect(location)
        if (pos[i] == location) {
            if (useHigh) {
                return high[i]
            } else {
                return low[i]
            }
        }
        val t = (location - pos[i - 1]) / (pos[i] - pos[i - 1])
        return (1.0 - t) * high[i - 1] + t * low[i]
    }

    fun start(): Double = pos[0]

    fun end(): Double = pos.last()

    fun maximum(): Double = max(low.max(), high.max())

    /**
     * Combine two profiles.
     *
     * ph: Fairly dumb way to combine profiles. Won't work perfectly for min, max.
     */
    fun morph(other: Profile, op: (Double, Double) -> Double): Profile {
        val combinedPos = ArrayList((pos + other.pos).sorted())
        val combinedLow = ArrayList(combinedPos.map { p -> op(this(p, false), other(p, false)) }.toMutableList())

        val combinedHigh = ArrayList(combinedPos.map { p -> op(this(p, true), other(p, true)) }.toMutableList())
        return Profile(combinedPos, combinedLow, combinedHigh)
    }

    fun morph(other: Double, op: (Double, Double) -> Double): Profile {
        val otherProfile = Profile(arrayListOf(0.0), arrayListOf(other), arrayListOf(other))
        return morph(otherProfile, op)
    }


    fun maxWith(other: Profile): Profile = morph(other) { a, b -> max(a, b) }

    fun minWith(other: Profile): Profile = morph(other) { a, b -> min(a, b) }

    operator fun plus(other: Profile): Profile = morph(other) { a, b -> a + b }

    operator fun plus(other: Double): Profile = morph(other) { a, b -> a + b }


    operator fun minus(other: Profile): Profile = morph(other) { a, b -> a - b }

    operator fun minus(other: Double): Profile = morph(other) { a, b -> a - b }

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
        val stepPositions = ArrayList<Double>()
        for (i in 0 until pos.size - 1) {
            stepPositions.add(pos[i])
            val numberOfSteps = ((low[i+1] - high[i]).absoluteValue / maxStep).toInt() + 1
            if (numberOfSteps != 0) {
                stepPositions.addAll((1 until numberOfSteps).map { j -> (pos[i+1] - pos[i]) * j / numberOfSteps + pos[i] })
            }
        }
        stepPositions.add(pos.last())
        val diams = (0 until (stepPositions.size - 1)).map { i ->
            this(0.5 * (stepPositions[i] + stepPositions[i + 1]))
        }
        val newLow = ArrayList(listOf(diams[0]) + diams)
        val newHigh = ArrayList(diams + listOf(diams.last()))
        return Profile(stepPositions, newLow, newHigh)
    }

    companion object {

        /**
         * Solve. As usual, ph didn't write down what he was solving.
         * It's some segment of the cornu/clothoid curve as a transition
         * between profile segments, but beyond that? No clue.
         */
        fun solve(a1: Double, a2: Double): Solution {
            fun score(t1: Double, t2: Double, mirror: Boolean): Double {
                if (abs(t1 - t2) < 1e-6 || max(abs(t1), abs(t2)) > PI * 10.0) {
                    return 1e30
                }
                val (y1, x1) = cornuYx(t1, mirror)
                val (y2, x2) = cornuYx(t2, mirror)
                val chordA = atan2(y2 - y1, x2 - x1)
                val chordL = distance(y2 - y1, x2 - x1)
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
                    Pair<Double, Double>(t1 + step, t2 + step),
                    Pair<Double, Double>(t1 - step, t2 - step),
                    Pair<Double, Double>(t1 - step, t2 + step),
                    Pair<Double, Double>(t1 + step, t2 - step)
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
            pos: List<Double>,
            low: List<Double>,
            high: List<Double>,
            lowAngle: ArrayList<Angle?>,
            highAngle: ArrayList<Angle?>,
            quality: Int = 512
        ): Profile {
            val a = ArrayList<Double>()
            val n = pos.size
            for (i in 0 until n - 1) {
                val x1 = pos[i]
                val y1 = high[i] * 0.5
                val x2 = pos[i + 1]
                val y2 = low[i + 1] * 0.5
                a.add((atan2(y2 - y1, x2 - x1) + PI) % (PI * 2) - PI)
            }
            val interpretedLowAngle = lowAngle.mapIndexed { i, angle ->
                angle?.interpret(i, a)
            }
            val interpretedHighAngle = highAngle.mapIndexed { i, angle -> angle?.interpret(i, a) }

            val ppos = ArrayList<Double>()
            val plow = ArrayList<Double>()
            val phigh = ArrayList<Double>()

            for (i in 0 until n - 1) {
                ppos.add(pos[i])
                plow.add(low[i])
                phigh.add(high[i])
                val x1 = pos[i]
                val y1 = high[i]
                val x2 = pos[i + 1]
                val y2 = low[i + 1]
                val l = distance(x2 - x1, y2 - y1)
                val ang = atan2(y2 - y1, x2 - x1)
                val a1 = interpretedHighAngle[i]?.let { ha -> ha - ang } ?: 0.0
                val a2 = interpretedLowAngle[i + 1]?.let { la -> la - ang } ?: 0.0
                if ((a1 - a2).absoluteValue < PI * quality) {
                    continue
                }
                val (t1, t2, mirror) = solve(a1, a2)
                val (cy1, cx1) = cornuYx(t1, mirror)
                val (cy2, cx2) = cornuYx(t2, mirror)
                val cl = distance(cy2 - cy1, cx2 - cx1).absoluteValue
                if (cl < 1e-10) {
                    continue
                }
                val ca = atan2(cy2 - cy1, cx2 - cx1)
                val steps = ((t2 - t1).absoluteValue / PI * quality).toInt()
                repeat((1 until steps).count()) {
                    val t = t1 + i * (t2 - t1) / steps
                    val (yy, xx) = cornuYx(t, mirror)
                    val aa = atan2(yy - cy1, xx - cx1)
                    val ll = distance(yy - cy1, xx - cx1)
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
            val pos = ArrayList<Double>()
            val low = ArrayList<Double>()
            val high = ArrayList<Double>()
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

object ProfileParameterKind: ParameterKind<Profile> {
    override val name: String = "Profile"

    override fun checkValue(v: Any?): Boolean {
        return v != null && v is Profile
    }

    override fun load(p: JsonElement): Profile? {
        if (p == JsonNull) { return null }
        if (p !is JsonObject) {
            throw ConfigurationParameterException("Parameter of type ${name} Expected a json object,  found '$p'")
        }
        val posStr = p["pos"] ?:  throw ConfigurationParameterException("Parameter of type ${name} expected a pos field,  found null")
        val pos = ListOfDoubleParameterKind.load(posStr)
        val low = ListOfDoubleParameterKind.load(p["low"] ?:  throw ConfigurationParameterException("Parameter of type ${name} expected a low field,  found null"))
        val high = ListOfDoubleParameterKind.load(p["high"] ?: throw ConfigurationParameterException("Parameter of type ${name} expected a high field,  found null"))
        return Profile(pos!!, low!!, high!!)
    }

    override fun dump(t: Profile?): JsonElement {
        if (t == null) { return JsonNull }
        return buildJsonObject {
            put("pos", ListOfDoubleParameterKind.dump(t.pos))
            put("low", ListOfDoubleParameterKind.dump(t.low))
            put("high", ListOfDoubleParameterKind.dump(t.high))
        }
    }
}
