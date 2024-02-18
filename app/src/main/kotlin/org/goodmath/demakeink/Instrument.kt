package org.goodmath.demakeink

import org.kotlinmath.Complex
import org.kotlinmath.R
import kotlin.math.*


fun<T> ArrayList<T>.fromEnd(i: Int): T =
    this[this.size - i]


// The actions are a *^&*^# mess. Ph just used functions with different
// signatures will-nilly, like nothing matters. But we can't get away
// with that, unfortunately.
typealias Action = (complexValues: Map<String, Complex>,
                    doubleValues: Map<String, Double>,
                    doubleListValues: Map<String, DoubleList>,
                    intValues: Map<String, Int>) -> Complex
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
class Instrument(
    val length: Double, //  length
    val inner: Profile, // inner profile, diameters
    val outer: Profile, // outer profile, diameters
    val holePositions: DoubleList, // hole positions on outside
    val holeAngles: DoubleList ,// angle of hole in degrees, up positive, down negative
    val innerHolePositions: DoubleList, // hole positions in bore
    val holeLengths: DoubleList, // length of hole through instrument wall (perhaps plus an embouchure correction)
    val holeDiameters: DoubleList, // hole diameters
    val closedTop: Boolean, // is the mouthpiece closed (eg reed) or open (eg ney)
    val coneStep: Double // - inner profile step size for conical segments of inner profile
    // ph: Then call:
    //   .prepare() to prepare  for queries
    //   .evaluate(wavelength)
) {
    var steppedInner = inner.asStepped(coneStep)
    val actions = ArrayList<Action>()
    val actionsPhase = ArrayList<Action>()
    val initialEmission = ArrayList<Double>()
    var emissionDivide: Double = 1.0
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
            val efunc: Action =
                { complexValues: Map<String, Complex>, doubleValues: Map<String, Double>, doubleListValues: Map<String, DoubleList>,
                  intValues: Map<String, Int> ->
                    val replyParam = complexValues["reply"]!!
                    val lengthParam = doubleValues["length"] ?: updatedLength
                    val wavelengthParam = doubleValues["wavelength"]!!
                    pipeReply(replyParam, lengthParam / wavelengthParam)

                }
            actions.add(efunc)
            position = event.position
            if (event.descriptor == "step") {
                assert(diameter == steppedInner.low[event.index])
                val area1 = circleArea(diameter)
                diameter = steppedInner.high[event.index]
                val area = circleArea(diameter)
                val act: Action = { complexValues: Map<String, Complex>,
                                    doubleValues: Map<String, Double>,
                                    doubleListValues: Map<String, DoubleList>,
                                    intValues: Map<String, Int> ->
                    val replyParam = complexValues["reply"]!!
                    val emissionParam = doubleListValues["emission"]
                    val areaParam = doubleValues["area"] ?: area
                    val area1Param = doubleValues["area1"]
                    val a = areaParam ?: area
                    val a1 = area1Param ?: area1
                    val (newReply, mag1) = junction2Reply(a, a1, replyParam)
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
                val afunc: Action = { complexValues: Map<String, Complex>,
                                      doubleValues: Map<String, Double>,
                                      doubleListValues: Map<String, DoubleList>,
                                      intValues: Map<String, Int> ->
                    val replyParam = complexValues["reply"]!!
                    val wavelengthParam = doubleValues["wavelength"]!!
                    val fingersParam = doubleListValues["fingers"]!!
                    val emissionParam = doubleListValues["emission"]
                    val areaParam = doubleValues["area"] ?: area
                    val holeAreaParam = doubleValues["holeArea"] ?: holeArea
                    val openLengthParam = doubleValues["openLength"] ?: openLength
                    val closedLengthParam = doubleValues["closedLength"] ?: closedLength

                    val indexParam = intValues["index"] ?: event.index

                    // Not sure about this condition...
                    val holeReply: Complex = if (fingersParam[event.index] != 0.0) {
                        pipeReply(1.0.R, closedLengthParam / wavelengthParam)
                    } else {
                        pipeReply((-1.0).R, openLengthParam / wavelengthParam)
                    }
                    val (newReply, mag1, mag2) = junction3Reply(area, area, holeAreaParam, replyParam, holeReply)
                    if (emissionParam != null) {
                        for (i in 0 until emissionParam.size) {
                            emissionParam[i] *= mag1
                        }
                        if (fingersParam[event.index] != 0.0) {
                            emissionParam.add(holeArea * mag2)
                        }
                    }
                    newReply

                }
                actions.add(afunc)
            }
        }
        emissionDivide = circleArea(diameter)

    }

    fun resonanceScore(w: Double, fingers: DoubleList, calcEmission: Boolean = false): Pair<Double, DoubleList?> {
        // ph: A score -1 <= score <= 1, zero if wavelength w resonantes
        var reply = (-1.0).R  // ph: open end
        val emission = if (calcEmission) {
            ArrayList<Double>()
        } else {
            null
        }
        if (!calcEmission) {
            for (action in actions) {
                val complexParams = mapOf("reply" to reply)
                val doubleParams = mapOf("wavelength" to w)
                val doubleListParams = mapOf("fingers" to fingers)
                reply = action(complexParams, doubleParams, doubleListParams, emptyMap())
            }
        } else {
            emission!!.addAll(initialEmission)
            for (action in actions) {
                val complexParams = mapOf("reply" to reply)
                val doubleParams = mapOf("wavelength" to w)
                val doubleListParams = mapOf("fingers" to fingers, "emission" to emission)

                reply = action(complexParams, doubleParams, doubleListParams, emptyMap())
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
            val efunc: Action = { complexValues, doubleValues, doubleListValues, intValues ->
                val phase_end = complexValues["phase"]!!
                val wavelength = doubleValues["wavelength"]!!
                val evLength = doubleValues["length"] ?: length
                pipeReplyPhase(phase_end, evLength / wavelength)
            }
            actionsPhase.add(efunc)

            position = pos
            if (action == "step") {
                assert(diameter == steppedInner.low[index])
                val area1 = circleArea(diameter)
                diameter = steppedInner.high[index]
                val area = circleArea(diameter)

                val efunc: Action = { complexValues, doubleValues, _, _ ->
                    val vPhase = complexValues["phase"]!!
                    val vArea = doubleValues["area"] ?: area
                    val vArea1 = doubleValues["area1"] ?: area1
                    junction2ReplyPhase(vArea, vArea1, vPhase)
                }
                actionsPhase.add(efunc)
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
                val eFunc: Action = { complexValues, doubleValues, doubleListValues, intValues ->
                    val vPhase = complexValues["phase"]!!
                    val vWavelength = doubleValues["wavelength"]!!
                    val vFingers = doubleListValues["fingers"]!! // I don't want to add yet another type
                    // to action, so if fingers[i]== 0 then it's interpreted as false, otherwise true
                    val vArea = doubleValues["area"] ?: area
                    val vHoleArea = doubleValues["holeArea"] ?: holeArea
                    val vOpenLength = doubleValues["openLength"] ?: openLength
                    val vClosedLength = doubleValues["closedLength"] ?: closedLength
                    val vIndex = intValues["index"] ?: index
                    val holePhase = if (vFingers[index] != 0.0) {
                        pipeReplyPhase(0.0.R, vClosedLength / vWavelength)
                    } else {
                        pipeReplyPhase((-0.5).R, vOpenLength / vWavelength)
                    }
                    junction3ReplyPhase(area, area, holeArea, vPhase, holePhase)
                }
                actionsPhase.add(eFunc)
            }
        }
    }

    /**
     * ph: score % 1 == 0 if wavelength w resonantes
     *
     */
    fun resonancePhase(wavelength: Double, fingers: DoubleList): Complex {
        var phase = 0.5.R // ph: open end
        for (action in actionsPhase) {
            val complexValues = mapOf("phase" to phase)
            val doubleListValues = mapOf("fingers" to fingers)
            val doubleValues = mapOf("wavelength" to wavelength)
            phase = action(complexValues, doubleValues, doubleListValues, emptyMap())
        }
        if (!closedTop) {
            phase += 0.5
        }
        return phase
    }

    fun trueWavelengthNear(wavelength: Double,
                           fingers: DoubleList,
                           stepCents: Double = 1.0,
                           stepIncrease: Double = 1.05,
                           maxSteps: Int = 100): Double {
        fun scorer(probe: Double): Double =
            ((resonancePhase(probe, fingers) + 0.5) % 1.0.R).im - 0.5
        var step = 2.0.pow(stepCents/1200.0)
        val halfStep = sqrt(step)
        val probes = arrayListOf(wavelength/halfStep, wavelength*halfStep)
        val scores = ArrayList(probes.map{scorer(it)})

        fun evaluate(i: Int): Double {
            val y1 = scores[i]
            val x1 = probes[i]
            val y2 = scores[i+1]
            val x2 = probes[i+1]
            val m = (y2-y1)/(x2-x1)
            val c = y1-m*x1
            val intercept = -c/m
            /*ph:
             grad = -m*intercept
             if grad > max_grad: return None
             assert x1 <= intercept <= x2, '%f %f %f' % (x1,intercept,x2)
             */
            return intercept
        }

        for (iteration in 0 until maxSteps) {
            if (scores.fromEnd(2) >= 0 && scores.fromEnd(1) < 0) {
                return evaluate(scores.size-2)
            }
            probes.addFirst(probes[0]/step)
            scores.addFirst(scorer(probes[0]))

            if (scores[0] >= 0 && scores[1] < 0) {
                return evaluate(0)
            }
            probes.add(probes.fromEnd(1)*step)
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
                              fingers: DoubleList,
                              n: Double,
                              stepCents: Double = 1.0,
                              stepIncrease: Double = 1.5,
                              maxSteps: Int = 20): Double {
        fun scorer(probe: Double): Double =
            ((resonancePhase(probe, fingers) + 0.5) % 1.0.R).im - 0.5

        var step = 2.0.pow(stepCents / 1200.0)
        var halfStep = sqrt(step)
        val probes = arrayListOf(wavelength / halfStep, wavelength * halfStep)
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
                probes.addFirst(probes[0] / step)
                scores.addFirst(scorer(probes[0]))
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
