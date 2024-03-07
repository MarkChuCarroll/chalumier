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

import org.goodmath.chalumier.config.*
import org.goodmath.chalumier.errors.RequiredParameterException
import org.goodmath.chalumier.util.repeat
import org.kotlinmath.Complex
import org.kotlinmath.R
import kotlin.math.*

/*
 * This file is part of the kotlin translation of "demakein/design.py" from
 * the original demakein code. I've tried to reproduce the functionality
 * of the original code, but adding types to hopefully make it harder
 * to screw things up.
 *
 * All the original comments from the Python are here, prefixed with "ph:".
 */

fun <T> ArrayList<T>.fromEnd(i: Int): T = this[this.size - i]


fun length(x: Double, y: Double): Double = sqrt(x * x + y * y)


const val FourPi = PI * 4

/**
 * ph: frequency response of a tree of connected pipes depends on area.
 */
fun circleArea(diameter: Double): Double {
    val radius = diameter / 2
    return PI * radius * radius
}

const val SPEED_OF_SOUND = 346100.0

/*
 * in ph's original code, there were about a dozen different
 * possible arguments to an action function, with many different
 * names, and parameters passed by position(!). For example,
 * the first parameter could be "reply", "reply_end", or "phase".
 * It *looks* like some of the functions at some point took/returned
 * complex values, but that appears to be an anachronism.
 *
 * Actions also get called with varying numbers of parameters.
 *
 * I'm cleaning that mess up.
 */

typealias ActionFunction = (reply: Complex, wavelength: Double, fingers: List<Double>, emission: ArrayList<Double>?) -> Complex
typealias PhaseActionFunction = (phase: Double, wavelength: Double, fingers: List<Double>) -> Double

data class Event(
    val position: Double, val descriptor: String, val index: Int = 0
)



abstract class Instrument<T: Instrument<T>>(override val name: String): Configurable<Instrument<T>>(name) {
    abstract val gen: () -> Instrument<T>

    fun copy(): Instrument<T> {
        val newInst = gen()
        val dumped = toJson()
        newInst.fromJson(dumped)
        return newInst
    }

    var designParameters: DesignParameters? = null

    //  ph: length
    open var length: Double by DoubleParameter { 0.0 }

    open var trueLength: Double by DoubleParameter {
        length
    }

    // inner profile, diameters
    open var inner: Profile by ConfigParameter(ProfileParameterKind) { throw RequiredParameterException("inner profile") }

    // ph: outer profile, diameters
    open var outer: Profile by ConfigParameter(ProfileParameterKind) { throw RequiredParameterException("outer profile") }

    open var numberOfHoles: Int by IntParameter { throw RequiredParameterException("number of holes") }

    // ph: hole positions on outside
    open var holePositions by ListOfDoubleParameter { throw RequiredParameterException("holePositions") }

    // ph: angle of hole in degrees, up positive, down negative
    open var holeAngles by ListOfDoubleParameter {
        ArrayList(it.numberOfHoles.repeat { 0.0 })
    }


    // ph: hole positions in bore
    // markcc: In ph's code, if these weren't set, then they were auto-initialized to None.
    // But if you then called prepare, shit would explode.
    open var innerHolePositions by ListOfDoubleParameter {
        // ph: roughly ArrayList(it.numberOfHoles.repeat { null })
        throw RequiredParameterException("innerHolePositions")
    }

    // ph: length of hole through instrument wall (perhaps plus an embouchure correction)
    open var holeLengths by ListOfDoubleParameter {
        ArrayList(it.numberOfHoles.repeat { 0.0 })
    }

    // ph: hole diameters
    open var holeDiameters by ListOfDoubleParameter { throw RequiredParameterException("hole diameters") }

    // ph: is the mouthpiece closed (eg reed) or open (eg ney)
    open var closedTop: Boolean by ConfigParameter(BooleanParameterKind) { false }

    // ph: inner profile step size for conical segments of inner profile
    open var coneStep: Double by DoubleParameter { 0.125 }

    open var innerKinks by ListOfDoubleParameter {
        throw RequiredParameterException("innerKinks")
    }

    open var outerKinks by ListOfDoubleParameter {
        throw RequiredParameterException("outerKinks")
    }

    // ph: Then call:
    //   .prepare() to prepare  for queries
    //   .evaluate(wavelength)

    open var steppedInner by ConfigParameter(ProfileParameterKind) {
        inner.asStepped(coneStep)
    }

    private val actions = ArrayList<ActionFunction>()
    private val actionsPhase = ArrayList<PhaseActionFunction>()
    private val initialEmission = ArrayList<Double>()
    open var emissionDivide by DoubleParameter { 1.0 }
    open var scale by DoubleParameter { 1.0  }

    fun prepare() {
        steppedInner = inner.asStepped(coneStep)
        val events = arrayListOf(
            Event(length, "end")
        )
        steppedInner.pos.forEachIndexed { i, p ->
            if (0.0 < p && p < length) {
                events.add(Event(p, "step", i))
            }
        }
        innerHolePositions.forEachIndexed { i, p ->
            events.add(Event(p, "hole", i))
        }
        events.sortBy { it.position }

        var position = -endFlangeLengthCorrection(
            outer(0.0, true), steppedInner(0.0, true)
        )
        var diameter = steppedInner(0.0, true)
        initialEmission.add(circleArea(diameter))

        events.forEach { event ->
            val updatedLength = event.position - position
            val eFunc: ActionFunction = { reply, wavelength, _, _ ->
                pipeReply(reply, updatedLength / wavelength)
            }
            actions.add(eFunc)
            position = event.position
            if (event.descriptor == "step") {
                assert(diameter == steppedInner.low[event.index])
                val lowArea = circleArea(diameter)
                diameter = steppedInner.high[event.index]
                val highArea = circleArea(diameter)
                val act: ActionFunction = { reply, _, _, emission ->
                    val (newReply, mag1) = junction2Reply(highArea, lowArea, reply)
                    if (emission != null) {
                        for (i in 0 until emission.size) {
                            emission[i] *= mag1
                        }
                    }
                    newReply
                }
                actions.add(act)
            } else if (event.descriptor == "hole") {
                val area = circleArea(diameter)
                val holeDiameter = holeDiameters[event.index]
                val holeArea = circleArea(holeDiameter)

                // ph: true_length = (self.outer(position) - diameter) * 0.5
                // ph: true_length += self.hole_extra_heights[index]
                // ph: true_length += self.hole_extra_height_by_diameter[index] * hole_diameter
                val trueLength = holeLengths[event.index]
                val openLength = trueLength + holeLengthCorrection(holeDiameter, diameter, false)
                val closedLength = trueLength + holeLengthCorrection(holeDiameter, diameter, true)
                val holeFunc: ActionFunction = { reply, wavelength, fingers, emission ->
                    val index = event.index

                    // TODO: again, with the complex. Not sure what I'm supposedto do with this yet...
                    val holeReply: Complex = if (fingers[index] != 0.0) {
                        pipeReply(1.0.R, closedLength / wavelength)
                    } else {
                        pipeReply((-1.0).R, openLength / wavelength)
                    }
                    val (newReply, mag1, mag2) = junction3Reply(area, area, holeArea, reply, holeReply)

                    if (emission != null) {
                        for (i in 0 until emission.size) {
                            emission[i] *= mag1
                        }
                        if (fingers[index] != 0.0) {
                            emission.add(holeArea * mag2)
                        }
                    }
                    newReply
                }
                actions.add(holeFunc)
            }
        }
        emissionDivide = circleArea(diameter)
    }

    fun resonanceScore(wavelength: Double, fingers: List<Double>, calcEmission: Boolean = false): Pair<Double, ArrayList<Double>?> {
        // ph: A score -1 <= score <= 1, zero if wavelength w resonates
        var reply = (-1.0).R  // ph: open end
        val emission = if (calcEmission) {
            ArrayList<Double>()
        } else {
            null
        }
        if (!calcEmission) {
            for (action in actions) {
                reply = action(reply, wavelength, fingers, null)
            }
        } else {
            emission!!.addAll(initialEmission)
            for (action in actions) {
                reply = action(reply, wavelength, fingers, emission)
            }
            // ph: Scale by top area
            for (i in 0 until emission.size) {
                emission[i] /= emissionDivide
            }
        }
        if (!closedTop) {
            reply *= -1.0
        }
        val angle = atan2(reply.im, reply.re)


        val angle1 = angle % (PI * 2.0)
        val angle2 = angle1 - PI * 2.0
        val result = if (angle1 < -angle2) {
            angle1 / PI
        } else {
            angle2 / PI
        }
        return Pair(result, emission)
    }


    /**
     * ph: phase version
     */
    fun preparePhase() {
        val events = arrayListOf(Event(length, "end"))
        steppedInner.pos.forEachIndexed { i: Int, pos: Double ->
            if (0.0 < pos && pos < length) {
                events.add(Event(pos, "step", i))
            }
        }
        innerHolePositions.forEachIndexed { i, pos ->
            events.add(Event(pos, "hole", i))
        }
        events.sortBy { it.position }
        var position = -endFlangeLengthCorrection(
            outer(0.0, true), steppedInner(0.0, true)
        )
        var diameter = steppedInner(0.0, true)
        events.forEach { ev ->
            val pos = ev.position
            val action = ev.descriptor
            val index = ev.index

            val length = pos - position
            val eFunc: PhaseActionFunction = { phaseEnd, wavelength, _ ->
                pipeReplyPhase(phaseEnd, length / wavelength)
            }
            actionsPhase.add(eFunc)

            position = pos
            if (action == "step") {
                assert(diameter == steppedInner.low[index])
                val area1 = circleArea(diameter)
                diameter = steppedInner.high[index]
                val area = circleArea(diameter)

                val stepFunc: PhaseActionFunction = { phase, _, _ ->
                    junction2ReplyPhase(area, area1, phase)
                }
                actionsPhase.add(stepFunc)
            } else if (action == "hole") {
                val area = circleArea(diameter)
                val holeDiameter = holeDiameters[ev.index]
                val holeArea = circleArea(holeDiameter)
                // ph: true_length = (self.outer(position) - diameter) * 0.5
                // ph: true_length += self.hole_extra_heights[index]
                // ph true_length += self.hole_extra_height_by_diameter[index] * hole_diameter
                val trueLength = holeLengths[ev.index]
                val openLength = trueLength + holeLengthCorrection(holeDiameter, diameter, false)
                val closedLength = trueLength + holeLengthCorrection(holeDiameter, diameter, true)
                val holeFunc: PhaseActionFunction = { phase, wavelength, fingers ->
                    val holePhase = if (fingers[index] != 0.0) {
                        pipeReplyPhase(0.0, closedLength /wavelength)
                    } else {
                        pipeReplyPhase((-0.5), openLength / wavelength)
                    }
                    junction3ReplyPhase(area, area, holeArea, phase, holePhase)
                }
                actionsPhase.add(holeFunc)
            }
        }
    }

    /*
     * ph: score % 1 == 0 if wavelength w resonates
     */
    fun resonancePhase(wavelength: Double, fingers: List<Double>): Double {
        var phase = 0.5 // ph: open end
        for (action in actionsPhase) {
            phase = action(phase, wavelength, fingers)
        }
        if (!closedTop) {
            phase += 0.5
        }
        return phase
    }

    fun trueWavelengthNear(
        wavelength: Double,
        fingers: List<Double>,
        stepCents: Double = 1.0,
        stepIncrease: Double = 1.05,
        maxSteps: Int = 100
    ): Double {

        fun scorer(probe: Double): Double = ((resonancePhase(probe, fingers) + 0.5) % 1.0) - 0.5

        var step = 2.0.pow(stepCents / 1200.0)
        val halfStep = sqrt(step)
        val probes = arrayListOf(wavelength / halfStep, wavelength * halfStep)
        val scores = ArrayList(probes.map { scorer(it) })

        fun evaluate(i: Int): Double {
            val y1 = scores[i]
            val x1 = probes[i]
            val y2 = scores[i + 1]
            val x2 = probes[i + 1]
            val m = (y2 - y1) / (x2 - x1)
            val c = y1 - m * x1
            val intercept = -c / m
            /*ph:
             grad = -m*intercept
             if grad > max_grad: return None
             assert x1 <= intercept <= x2, '%f %f %f' % (x1,intercept,x2)
            */
            return intercept
        }

        for (iteration in 0 until maxSteps) {
            if (scores.fromEnd(2) >= 0 && scores.fromEnd(1) < 0) {
                return evaluate(scores.size - 2)
            }
            probes.add(0, probes[0] / step)
            scores.add(0, scorer(probes[0]))

            if (scores[0] >= 0 && scores[1] < 0) {
                return evaluate(0)
            }
            probes.add(probes.fromEnd(1) * step)
            scores.add(scorer(probes.fromEnd(1)))
            step = step.pow(stepIncrease)
        }
        return if (abs(scores.fromEnd(1)) < abs(scores[0])) {
            probes.fromEnd(1)
        } else {
            probes[0]
        }
    }

    fun trueNthWavelengthNear(
        wavelength: Double,
        fingers: List<Double>,
        stepCents: Double = 1.0,
        stepIncrease: Double = 1.5,
        maxSteps: Int = 20
    ): Double {
        fun scorer(probe: Double): Double {
            return ((resonancePhase(probe, fingers) + 0.5) % 1.0) - 0.5
        }

        var step = 2.0.pow(stepCents / 1200.0)
        val halfStep = sqrt(step)
        val probes: ArrayList<Double> = ArrayList(listOf(wavelength / halfStep, wavelength * halfStep))
        val scores = ArrayList(probes.map { scorer(it) })
        fun evaluate(i: Int): Double {
            val y1 = scores[i]
            val x1 = probes[i]
            val y2 = scores[i + 1]
            val x2 = probes[i + 1]
            val m = (y2 - y1) / (x2 - x1)
            val c = y1 - m * x1
            return -c / m
        }

        for (iteration in 0 until maxSteps) {
            if (scores.fromEnd(2) >= 0 && scores.fromEnd(1) < 0) {
                return evaluate(scores.size - 2)
            }
            if (scores[0] <= 0) {
                probes.add(0, probes[0] / step)
                scores.add(0, scorer(probes[0]))
            }
            if (scores[0] >= 0 && scores[1] < 0) {
                return evaluate(0)
            }
            if (scores.fromEnd(1) >= 0) {
                probes.add(probes.fromEnd(1) * step)
                scores.add(scorer(probes.fromEnd(1)))
            }
            step = step.pow(stepIncrease)
        }
        return if (abs(scores.fromEnd(1)) < abs(scores[0])) {
            probes.fromEnd(1)
        } else {
            probes[0]
        }
    }


    fun sqrtScaler(values: List<Double?>): List<Double?> {
        val scaleFactor = scale.pow(0.5)
        return values.map {
            if (it != null) {
                it * scaleFactor
            } else {
                null
            }
        }
    }

    fun scaler(values: List<Double?>): List<Double?> = values.map {
        if (it != null) {
            it * scale
        } else {
            null
        }
    }


    fun powerScaler(power: Double, values: List<Double?>): List<Double?> {
        val scaleFactor = scale.pow(power)
        return values.map {
            if (it != null) {
                it * scaleFactor
            } else {
                null
            }
        }
    }
}

/**
 * the original ph code took a list of what could be pairs of doubles,
 * lists of doubles of length 2, or single doubles. I'm... not going
 * to reproduce that. This takes a list of pairs, and returns
a * a pair of lists. The config parameters now must be pairs.
 */
fun <T> lowHigh(vec: List<Pair<T, T>>): Pair<ArrayList<T>, ArrayList<T>> {
    val low = ArrayList<T>()
    val high = ArrayList<T>()
    vec.forEach { item ->
        low.add(item.first)
        high.add(item.second)
    }
    return Pair(low, high)
}

fun <T> lowHighOpt(vec: List<Pair<T, T>?>): Pair<ArrayList<T?>, ArrayList<T?>> {
    val low = ArrayList<T?>()
    val high = ArrayList<T?>()
    vec.forEach { item ->
        low.add(item?.first)
        high.add(item?.second)
    }
    return Pair(low, high)
}

/**
 * As with lowHigh, I'm saying it's just a pair.
 */
fun describeLowHigh(item: Pair<Double, Double>): String {
    return if (item.first == item.second) {
        "%.1f".format(item.first)
    } else {
        "%.1f->%.1f".format(item.first, item.second)
    }
}

