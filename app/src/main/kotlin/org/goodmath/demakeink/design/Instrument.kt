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
package org.goodmath.demakeink.design

import kotlinx.serialization.Serializable
import org.goodmath.demakeink.errors.DemakeinException
import org.goodmath.demakeink.errors.RequiredParameterException
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

fun<T> ArrayList<T>.fromEnd(i: Int): T =
    this[this.size - i]

interface Copyable<T> {
    fun copy(): T
}

fun length(x: Double, y: Double): Double =
    sqrt(x * x + y * y)


const val FourPi = PI * 4

/**
 * ph: frequency response of a tree of connected pipes depends on area.
 */
fun circleArea(diameter: Double): Double {
    val radius = diameter / 2
    return PI * radius * radius
}

const val SPEED_OF_SOUND = 346100.0

data class ActionParameterException(val typeName: String, val name: String):
    DemakeinException("Action expected a $typeName parameter named $name")

@Serializable
class ActionParams(val doubleParams: Map<String, Double>,
    val doubleListParams: Map<String, List<Double>>,
    val intParams: Map<String, Int>) {
    fun double(name: String): Double =
        doubleParams[name] ?: throw ActionParameterException("Double", name)

    fun optDouble(name: String): Double? =
        doubleParams[name]
    fun doubleList(name: String): List<Double> =
        doubleListParams[name] ?: throw ActionParameterException("List<Double>", name)

    fun optDoubleList(name: String): List<Double>? =
        doubleListParams[name]

    fun int(name: String): Int =
        intParams[name] ?: throw ActionParameterException("Int", name)

    fun optInt(name: String): Int? =
        intParams[name]
}

typealias Action = (ActionParams) -> Double
data class Event(
    val position: Double,
    val descriptor: String,
    val index: Int = 0
)

object EventComparator: Comparator<Event> {
    override fun compare(o1: Event?, o2: Event?): Int {
        return when {
            o1 == null && o2 == null -> 0
            o1 == null && o2 != null -> -1
            o1 != null && o2 == null -> 1
            o1!!.position == o2!!.position -> 0
            o1.position > o2.position -> 1
            else -> -1
        }
    }
}



/**
 * Comments on the constructor parameters are all ph:
 *
 * ph: Fill in:
 */
abstract class Instrument<T: Instrument<T>>() {

    abstract fun copy(): T

    //  ph: length
    open var length: Double by ConfigurationParameter("") { 0.0 }

    open var trueLength: Double by ConfigurationParameter("trueLength") {
        length
    }


    // inner profile, diameters
    open var inner: Profile by ConfigurationParameter("") { throw RequiredParameterException("inner profile") }

    // ph: outer profile, diameters
    open var outer: Profile by ConfigurationParameter("") { throw RequiredParameterException("outer profile") }

    open var numberOfHoles: Int by ConfigurationParameter("") { throw RequiredParameterException("number of holes") }

    // ph: hole positions on outside
    open var holePositions: DoubleList by ConfigurationParameter("") { throw RequiredParameterException("holePositions") }

    // ph: angle of hole in degrees, up positive, down negative
    open var holeAngles: List<Double> by ConfigurationParameter<Instrument<T>, List<Double>>( "") {
        ArrayList(it.numberOfHoles.repeat { 0.0 })
    }


    // ph: hole positions in bore
    // In ph's code, if these weren't set, then they were auto-initialized to None.
    // But if you then called prepare, shit would explode.
    open var innerHolePositions: ArrayList<Double> by ConfigurationParameter("") {
        // ph: roughly ArrayList(it.numberOfHoles.repeat { null })
        throw RequiredParameterException("innerHolePositions")
    }

    // ph: length of hole through instrument wall (perhaps plus an embouchure correction)
    open var holeLengths: ArrayList<Double> by ConfigurationParameter("") {
        ArrayList(it.numberOfHoles.repeat { 0.0 })
    }

    // ph: hole diameters
    open var holeDiameters: DoubleList by ConfigurationParameter("") { throw RequiredParameterException("hole diameters") }

    // ph: is the mouthpiece closed (eg reed) or open (eg ney)
    open var closedTop: Boolean by ConfigurationParameter("") { false }

    // ph: inner profile step size for conical segments of inner profile
    open var coneStep: Double by ConfigurationParameter("") { 0.125 }

    open var innerKinks: ArrayList<Double> by ConfigurationParameter<Instrument<T>, ArrayList<Double>>("") {
        throw RequiredParameterException("innerKinks")
    }

    open var outerKinks: ArrayList<Double> by ConfigurationParameter<Instrument<T>, ArrayList<Double>>("") {
        throw RequiredParameterException("outerKinks")
    }

    // ph: Then call:
    //   .prepare() to prepare  for queries
    //   .evaluate(wavelength)

    var steppedInner = inner.asStepped(coneStep)
    val actions = ArrayList<Action>()
    val actionsPhase = ArrayList<Action>()
    val initialEmission = ArrayList<Double>()
    var emissionDivide: Double = 1.0
    var scale: Double = 1.0
    fun prepare() {
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
        events.sortWith(EventComparator)
        var position = -endFlangeLengthCorrection(
            outer(0.0, true),
            steppedInner(0.0, true)
        )
        var diameter = steppedInner(0.0, true)
        initialEmission.add(circleArea(diameter))

        events.forEach { event ->
            val updatedLength = event.position - position
            val eFunc: Action =
                { params: ActionParams ->
                    val replyParam = params.double("reply")
                    val lengthParam = params.double("length") ?: updatedLength
                    val wavelengthParam = params.double("wavelength")
                    // TODO(markcc): the next line is severely fudged up. The correct content
                    // drops the ".R" and ".absoluteValue()", but it seems unused?
                    pipeReply(replyParam.R, lengthParam / wavelengthParam).absoluteValue()
                }
            actions.add(eFunc)
            position = event.position
            if (event.descriptor == "step") {
                assert(diameter == steppedInner.low[event.index])
                val area1 = circleArea(diameter)
                diameter = steppedInner.high[event.index]
                val area = circleArea(diameter)
                val act: Action = { params ->
                    val replyParam = params.double("reply")
                    val emissionParam = params.optDoubleList("emission")?.toMutableList()
                    val areaParam = params.optDouble("area") ?: area
                    val area1Param = params.optDouble("area1") ?: area1
                    val (newReply, mag1) = junction2Reply(areaParam, area1Param, replyParam)
                    if (emissionParam != null) {
                        for (i in 0 until emissionParam.size) {
                            emissionParam[i] *= mag1
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
                val holeFunc: Action = { params ->
                    val replyParam = params.double("reply")
                    val wavelengthParam = params.double("wavelength")
                    val fingersParam = params.doubleList("fingers")
                    val emissionParam = params.optDoubleList("emission")?.toMutableList()
                    val areaParam = params.optDouble("area") ?: area
                    val holeAreaParam = params.optDouble("holeArea") ?: holeArea
                    val openLengthParam = params.optDouble("openLength") ?: openLength
                    val closedLengthParam = params.optDouble("closedLength") ?: closedLength

                    val indexParam = params.optInt("index") ?: event.index

                    // Not sure about this condition...
                    val holeReply: Complex = if (fingersParam[indexParam] != 0.0) {
                        pipeReply(1.0.R, closedLengthParam / wavelengthParam)
                    } else {
                        pipeReply((-1.0).R, openLengthParam / wavelengthParam)
                    }
                    val (newReply, mag1, mag2) = junction3Reply(
                        areaParam,
                        areaParam,
                        holeAreaParam,
                        replyParam,
                        holeReply.absoluteValue()
                    )
                    if (emissionParam != null) {
                        for (i in 0 until emissionParam.size) {
                            emissionParam[i] *= mag1
                        }
                        if (fingersParam[indexParam] != 0.0) {
                            emissionParam.add(holeArea * mag2)
                        }
                    }
                    newReply
                }
                actions.add(holeFunc)
            }
        }
        emissionDivide = circleArea(diameter)
    }

    fun resonanceScore(w: Double, fingers: List<Double>, calcEmission: Boolean = false): Pair<Double, DoubleList?> {
        // ph: A score -1 <= score <= 1, zero if wavelength w resonates
        var reply = (-1.0)  // ph: open end
        val emission = if (calcEmission) {
            ArrayList<Double>()
        } else {
            null
        }
        if (!calcEmission) {
            for (action in actions) {
                //val complexParams = mapOf("reply" to reply)
                val params = ActionParams(mapOf("reply" to reply, "wavelength" to w), mapOf("fingers" to fingers), emptyMap())
                reply = action(params)
            }
        } else {
            emission!!.addAll(initialEmission)
            for (action in actions) {
                reply = action(ActionParams(mapOf("reply" to reply, "wavelength" to w),
                    mapOf("fingers" to fingers, "emission" to emission),
                    emptyMap()))

            }
            // ph: Scale by top area
            for (i in 0 until emission.size) {
                emission[i] /= emissionDivide
            }
        }
        if (!closedTop) {
            reply *= -1.0
        }
        //val angle = atan2(reply.im, reply.re)
        val angle = atan(reply)

        val angle1 = angle % (PI * 2.0)
        val angle2 = angle1 - PI * 2.0
        val result = if (angle1 < -angle2) {
            angle1 / PI
        } else {
            angle2 / PI
        }
        return Pair(result, null)
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
        events.sortWith(EventComparator)
        var position = -endFlangeLengthCorrection(
            outer(0.0, true), steppedInner(0.0, true)
        )
        var diameter = steppedInner(0.0, true)
        events.forEach { ev ->
            val pos = ev.position
            val action = ev.descriptor
            val index = ev.index

            val length = pos - position
            val eFunc: Action = { params ->
                val phase_end = params.double("phase")
                val wavelength = params.double("wavelength")
                val evLength = params.optDouble("length") ?: length
                pipeReplyPhase(phase_end, evLength / wavelength)
            }
            actionsPhase.add(eFunc)

            position = pos
            if (action == "step") {
                assert(diameter == steppedInner.low[index])
                val area1 = circleArea(diameter)
                diameter = steppedInner.high[index]
                val area = circleArea(diameter)

                val stepFunc: Action = { params ->
                    val vPhase = params.double("phase")
                    val vArea = params.optDouble("area") ?: area
                    val vArea1 = params.optDouble("area1") ?: area1
                    junction2ReplyPhase(vArea, vArea1, vPhase)
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
                val holeFunc: Action = { params ->
                    val vPhase = params.double("phase")
                    val vWavelength = params.double("wavelength")
                    val vFingers = params.doubleList("fingers") // I don't want to add yet another type
                    // to action, so if fingers[i]== 0 then it's interpreted as false, otherwise true
                    val vArea = params.optDouble("area") ?: area
                    val vHoleArea = params.optDouble("holeArea") ?: holeArea
                    val vOpenLength = params.optDouble("openLength") ?: openLength
                    val vClosedLength = params.optDouble("closedLength") ?: closedLength
                    val vIndex = params.optInt("index") ?: index
                    val holePhase = if (vFingers[vIndex] != 0.0) {
                        pipeReplyPhase(0.0, vClosedLength / vWavelength)
                    } else {
                        pipeReplyPhase((-0.5), vOpenLength / vWavelength)
                    }
                    junction3ReplyPhase(vArea, vArea, vHoleArea, vPhase, holePhase)
                }
                actionsPhase.add(holeFunc)
            }
        }
    }

    /**
     * ph: score % 1 == 0 if wavelength w resonates
     *
     */
    fun resonancePhase(wavelength: Double, fingers: List<Double>): Double {
        var phase = 0.5 // ph: open end
        for (action in actionsPhase) {
            val doubleListValues = mapOf("fingers" to fingers)
            val doubleValues = mapOf("phase" to phase, "wavelength" to wavelength)
            phase = action(ActionParams(doubleValues, doubleListValues, emptyMap()))
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

        fun scorer(probe: Double): Double =
            ((resonancePhase(probe, fingers) + 0.5) % 1.0) - 0.5

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
        if (abs(scores.fromEnd(1)) < abs(scores[0])) {
            return probes.fromEnd(1)
        } else {
            return probes[0]
        }
        /* ph:
          y = mx + c

          step = pow(2.0, max_cents/((n_probes-1)*0.5*1200.0))
          low = w * pow(step, -(n_probes-1)/2.0)
          probes = [ low * pow(step,i) for i in range(n_probes) ]

          scores = [ abs(self.resonance_score(probe, fingers)) for probe in probes ]

          best = min(range(n_probes), key=lambda i: scores[i])

          if best == 0 or best == n_probes-1:
            return probes[best]

          c = scores[best]
          b = 0.5*(scores[best+1]-scores[best-1])
          a = scores[best+1]-c-b
          return low*pow(step, best-b*0.5/a)
         */
    }

    fun trueNthWavelengthNear(wavelength: Double,
                              fingers: List<Double>,
                              _n: Double,
                              stepCents: Double = 1.0,
                              stepIncrease: Double = 1.5,
                              maxSteps: Int = 20): Double {
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
        if (abs(scores.fromEnd(1)) < abs(scores[0])) {
            return probes.fromEnd(1)
        } else {
            return probes[0]
        }
    }

}

/**
 * the original ph code took a list of what could be pairs of doubles,
 * lists of doubles of length 2, or single doubles. I'm... not going
 * to reproduce that. This takes a list of pairs, and returns
 * a pair of lists. I'll fix it at the call site.
 */
fun<T> lowHigh(vec: List<Pair<T, T>>): Pair<ArrayList<T>, ArrayList<T>> {
    val low = ArrayList<T>()
    val high = ArrayList<T>()
    vec.forEach { item ->
        low.add(item.first)
        high.add(item.second)}
    return Pair(low, high)
}

fun<T> lowHighOpt(vec: List<Pair<T, T>?>): Pair<ArrayList<T?>, ArrayList<T?>> {
    val low = ArrayList<T?>()
    val high = ArrayList<T?>()
    vec.forEach { item ->
        low.add(item?.first)
        high.add(item?.second)}
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

fun<T: Instrument<T>> scaler(inst: T, values: List<Double?>): List<Double?> =
    values.map {
        if (it != null) {
            it * inst.scale
        } else {
            null
        }
    }

fun<T: Instrument<T>> sqrtScaler(values: List<Double?>): List<Double?> {
    val scaleFactor = scale.pow(0.5)
    return values.map {
        if (it != null) {
            it * scaleFactor
        } else {
            null
        }
    }
}

fun<T: Instrument<T>> powerScaler(inst: T, power: Double, values: List<Double?>) :List<Double?> {
    val scaleFactor = inst.scale.pow(power)
    return values.map {
        if (it != null) {
            it * scaleFactor
        } else {
            null
        }
    }
}
