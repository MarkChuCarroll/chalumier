package org.goodmath.demakeink

import java.lang.Math.log
import kotlin.math.*

interface InstrumentGenerator<T: Instrument> {
    fun create(): T
}
private fun dAssert(v: Boolean, msg: String) {
    if (!v) {
        throw AssertionException(msg)
    }
}

fun<T> Int.repeat(f: (i: Int) -> T): List<T> {
    return (0 until this).map { f(it) }
}
open class DemakeinException(msg: String, cause: Throwable? = null): Exception(msg, cause)

class RequiredParameterException(name: String, msg: String = "is a required parameter"): DemakeinException("${name} ${msg}")

class AssertionException(msg: String): DemakeinException(msg)

sealed class Fingering(open val fingers: ArrayList<Double>, open val nth: Double?) {
    abstract fun wavelength(transpose: Int): Double
}
data class NamedFingering(
    val noteName: String,
    override val fingers: ArrayList<Double>,
    override val nth: Double? = null
): Fingering(fingers, nth) {
    override fun wavelength(transpose: Int): Double {
        val w = SPEED_OF_SOUND / frequency(noteName)
        return w/(2.0.pow(transpose.toDouble()/12.0))
    }
}

data class WavelengthFingering(val wavelength: Double,
                               override val fingers: ArrayList<Double>,
                               override val nth: Double? = null): Fingering(fingers, nth) {
    override fun wavelength(transpose: Int): Double {
        return wavelength/(2.0.pow(transpose.toDouble()/12.0))
    }
}

open class InstrumentDesignerConfigurator<T: Instrument>(
    val gen: InstrumentGenerator<T>
) {

    private val fingeringList: ArrayList<Fingering> = ArrayList()

    fun addFingering(f: Fingering) {
        fingeringList.add(f)
    }

    private fun checkFingeringHoles(fingerings: List<Fingering>): Int {
        val count = fingerings[0].fingers.size
        if (!fingerings.all { it.fingers.size != count }) {
            throw DemakeinException("All fingerings must have the same number of holes")
        }
        return count
    }


    val fingerings: ArrayList<Fingering>
        get() {
            checkFingeringHoles(fingeringList)
            return fingeringList
        }

    var length: Double by AssignableParameter {
        it.initialLength * it.scale
    }

    var closedTop: Boolean by AssignableParameter { false }

    val initialLength: Double by AssignableParameter<InstrumentDesignerConfigurator<T>, Double> { throw RequiredParameterException("initialLength") }


    // ph; diameters of inner bore: first is diam at bottom, last is diam at top
    val innerDiameters: List<Pair<Double, Double>> by AssignableParameter<InstrumentDesignerConfigurator<T>, List<Pair<Double, Double>>>() {
        throw RequiredParameterException("innerDiameters")
    }

    var outerDiameters: List<Pair<Double, Double>> by AssignableParameter<InstrumentDesignerConfigurator<T>, List<Pair<Double, Double>>> {
        throw RequiredParameterException("outerDiameters", "must have at least two elements")
    }

    var transpose: Int by AssignableParameter { 0 }

    /**
     * Experimental. Add a term to the optimization
     *  to try to make instrument louder, possibly sacrificing
     *  being-in-tune-ness.
     */
    var tweakEmissions: Double by AssignableParameter { 0.0 }

    var innerAngles: List<Pair<Angle, Angle>?> by AssignableParameter { c -> c.innerDiameters.map { null } }

    var outerAngles: List<Pair<Angle, Angle>?> by AssignableParameter { c -> c.outerDiameters.map { null } }

    var numberOfHoles: Int by AssignableParameter { c -> c.maxHoleDiameters.size }

    var minHoleDiameters: List<Double> by AssignableParameter { c -> c.numberOfHoles.repeat { 0.5 } }

    var maxHoleDiameters: List<Double> by AssignableParameter { throw RequiredParameterException("maxHoleDiameters") }

    var outerAdd: Boolean by AssignableParameter { false }

    var topClearanceFraction: Double by AssignableParameter { 0.0 }

    var bottomClearanceFraction: Double by AssignableParameter { 0.0 }

    var scale: Double by AssignableParameter { 1.0 }

    val minHoleSpacing: List<Double?> by AssignableParameter { it.numberOfHoles.repeat { 0.0 } }

    val maxHoleSpacing: List<Double?> by AssignableParameter { c -> c.numberOfHoles.repeat { c.initialLength } }

    val balance: List<Double?> by AssignableParameter { c -> (c.numberOfHoles - 2).repeat { null } }
    val holeAngles: List<Double> by AssignableParameter { it.numberOfHoles.repeat { 0.0 } }

    val initialInnerFractions: List<Double?> by AssignableParameter { c ->
        (c.innerDiameters.size - 2).repeat { (it + 1.0) / (c.innerDiameters.size - 1) }
    }

    val minInnerFractionSep: List<Double> by AssignableParameter {
        (it.innerDiameters.size - 1).repeat { 0.0 }
    }
    val maxInnerFractionSep: List<Double> by AssignableParameter {  (it.innerDiameters.size - 1).repeat { 1.0 } }

    val minInnerSep: List<Double?> by AssignableParameter { (it.innerDiameters.size - 1).repeat { null } }

    val maxInnerSep: List<Double?> by AssignableParameter { (it.innerDiameters.size - 1).repeat { null }}
    val initialOuterFractions: List<Double?> by AssignableParameter { c ->
        (c.outerDiameters.size-2).repeat { (it + 1.0)/(c.outerDiameters.size-1) }
    }

    val minOuterFractionSep: List<Double> by AssignableParameter { c ->
        (c.outerDiameters.size-1).repeat {  0.0 }
    }

    val maxOuterFractionSep: List<Double> by AssignableParameter { (it.outerDiameters.size-2).repeat { 1.0 } }

    val initialHoleFractions: List<Double?> by AssignableParameter { c ->
        c.numberOfHoles.repeat {(it+3.0)/(c.numberOfHoles+2) * 0.5 }
    }
    val initialHoleDiameterFractions: List<Double?> by AssignableParameter {  it.numberOfHoles.repeat { 0.75 } }

    val coneStep: Double by AssignableParameter { 0.125 }

    val maxLength: Double? by AssignableParameter { null }

    /* equivalent code in python commented out by ph
    val holeExtraHeightByDiameter: List<Double?>
        get() = getDoubleList("holeExtraHeightByDiameter") ?: numberOfHoles.repeat { 0.0 }


    state_vars = [
        'length',
        'holeFractions',
        'outerFractions',
        'innerFractions',
        'innerDiameters',
        'outerDiameters',
        ]
    ...
    */

    /**
     * All of the validations scattered in ph's code are moved here, and called
     * before anything gets filled in in the instrument designer.
     */
    fun validate() {
        dAssert(initialHoleFractions.size == numberOfHoles, "initialHoleFractions has wrong length")
        dAssert(initialHoleDiameterFractions.size == numberOfHoles, "initialHoleDiameterFractions has wrong length")
        dAssert(initialInnerFractions.size == innerDiameters.size - 2, "initialInnerFractions has wrong length")
        dAssert(initialOuterFractions.size == outerDiameters.size - 2, "initialOuterFractions has wrong length")
        checkFingeringHoles(fingerings)
    }
}


/**
 * MC: I've refactored this quite a bit, by moving all of the
 * configurables up to be constructor parameters, instead of
 * things randomly initialized into class variables.
 *

 *
 */
open class InstrumentDesigner<T: Instrument> protected constructor(
    val cfg: InstrumentDesignerConfigurator<T>
) {

    companion object {
        fun<T: Instrument> create(cfg: InstrumentDesignerConfigurator<T>): InstrumentDesigner<T> {
            cfg.validate()
            return InstrumentDesigner<T>(cfg)
        }
    }


    val fingerings = cfg.fingerings
    val initialLength = cfg.initialLength
    val innerDiameters = cfg.innerDiameters
    val outerDiameters = cfg.outerDiameters
    val transpose = cfg.transpose
    val tweakEmissions = cfg.tweakEmissions
    val innerAngles = cfg.innerAngles
    val outerAngles = cfg.outerAngles
    val numberOfHoles = cfg.numberOfHoles
    val minHoleDiameters = cfg.minHoleDiameters
    val maxHoleDiameters = cfg.maxHoleDiameters
    val outerAdd = cfg.outerAdd
    val topClearanceFraction = cfg.topClearanceFraction
    val bottomClearanceFraction = cfg.bottomClearanceFraction
    val scale = cfg.scale
    val minHoleSpacing = cfg.minHoleSpacing
    val maxHoleSpacing = cfg.maxHoleSpacing
    val balance = cfg.balance
    val holeAngles = cfg.holeAngles
    val initialInnerFractions = cfg.initialInnerFractions
    val minInnerFractionSep = cfg.minInnerFractionSep
    val maxInnerFractionSep = cfg.maxInnerFractionSep
    val minInnerSep = cfg.minInnerSep
    val maxInnerSep = cfg.maxInnerSep
    val initialOuterFractions = cfg.initialOuterFractions
    val minOuterFractionSep = cfg.minOuterFractionSep
    val maxOuterFractionSep = cfg.maxOuterFractionSep
    val initialHoleFractions = cfg.initialHoleFractions
    val initialHoleDiameterFractions = cfg.initialHoleDiameterFractions
    val coneStep = cfg.coneStep
    val closedTop = cfg.closedTop
    val maxLength = cfg.maxLength

    val initialStateVec: List<Double?>
        get() {
            val result = ArrayList<Double?>()
            result.add(1.0)
            result.addAll(initialHoleFractions)
            result.addAll(initialHoleFractions)
            result.addAll(initialHoleDiameterFractions.map { i -> i?.let { it * it } })
            result.addAll(initialInnerFractions)
            result.addAll(initialOuterFractions)
            return result
        }

    fun unpack(stateVec: List<Double?>): T {
        val (innerLow, innerHigh) = lowHigh(innerDiameters)
        val (outerLow, outerHigh) = lowHigh(outerDiameters)
        val (innerAngleLow, innerAngleHigh) = lowHighOpt(innerAngles)
        val (outerAngleLow, outerAngleHigh) = lowHighOpt(outerAngles)

        val inst = cfg.gen.create()
        var p = 0
        inst.length = (stateVec[0] ?: 1.0) * initialLength * scale
        p += 1
        inst.holePositions = ArrayList(stateVec.slice(p until p + numberOfHoles).map { it!! * inst.length })
        p += inst.numberOfHoles
        inst.holeDiameters = ArrayList(stateVec.slice(p until p + numberOfHoles).mapIndexed { i, item ->
            minHoleDiameters[i] + signed_sqrt(item ?: 0.0) * (maxHoleDiameters[i] - minHoleDiameters[i])
        })
        p += numberOfHoles
        val innerKinks = ArrayList(stateVec.slice(p until p + innerDiameters.size - 2).map { item ->
            item!! * inst.length
        })
        p += (innerDiameters.size - 2)
        val outerKinks = ArrayList(stateVec.slice(p until p + outerDiameters.size - 2).map {
            it!! * inst.length
        })
        p += (outerDiameters.size - 2)

        dAssert(p == stateVec.size, "Invalid state vec; expected size ${p}, but found ${stateVec.size}")

        inst.inner = Profile.curvedProfile(
            ArrayList(listOf(0.0) + innerKinks + listOf(inst.length)),
            innerLow,
            innerHigh,
            innerAngleLow,
            innerAngleHigh
        )

        inst.outer = Profile.curvedProfile(
            ArrayList(listOf(0.0) + outerKinks + listOf(inst.length)),
            outerLow,
            outerHigh,
            outerAngleLow,
            outerAngleHigh
        )

        if (outerAdd) {
            inst.outer += inst.inner
        }

        inst.holeAngles = holeAngles
        inst.innerHolePositions = ArrayList(numberOfHoles.repeat { 0.0 })
        inst.holeLengths = ArrayList(numberOfHoles.repeat { 0.0 })

        (0 until numberOfHoles).forEach { i ->
            // ph: Note: approximates bore as cylindrical calculating shift, length
            val radians = inst.holeAngles[i] * PI / 180.0
            val thickness = (inst.outer(inst.holePositions[i]) - inst.inner(inst.holePositions[i])) * 0.5
            val shift = sin(radians) * thickness

            inst.innerHolePositions[i] = inst.holePositions[i] + shift
            inst.holeLengths[i] = (sqrt(thickness * thickness + shift * shift)
                    // ph: #+ self.hole_extra_height_by_diameter[i] * inst.hole_diameters[i]
                    )
            inst.innerKinks = innerKinks
            inst.outerKinks = outerKinks

            inst.coneStep = coneStep
            inst.closedTop = closedTop
        }
        return inst
    }

    fun constraintScore(inst: Instrument): Double {
        val scores = ArrayList<Double>()
        scores.add(inst.length)

        if (maxLength != null) {
            scores.add(maxLength * scale - inst.length)
        }
        val inners = ArrayList(listOf(0.0) + inst.innerKinks + listOf(inst.length))
        for (i in 0 until inners.size - 1) {
            val sep = inners[i + 1] - inners[i]
            val minCheck = sep - minInnerFractionSep[i] * inst.length
            scores.add(minCheck)
            val maxCheck = maxInnerFractionSep[i] * inst.length - sep
            scores.add(maxCheck)
            if (minInnerSep[i] != null) {
                scores.add(sep - minInnerSep[i]!! * scale)
            }
            if (maxInnerSep[i] != null) {
                scores.add(maxInnerSep[i]!! * scale - sep)
            }
        }
        val outers = ArrayList(listOf(0.0) + inst.outerKinks + listOf(inst.length))
        for (i in 0 until outers.size - 1) {
            val sep = outers[i + 1] - outers[i]
            val minCheck = sep - minOuterFractionSep[i] * inst.length
            scores.add(minCheck)
            val maxCheck = maxOuterFractionSep[i] * inst.length - sep
            scores.add(maxCheck)
        }
        val bottomCheck = inst.holePositions[0] - bottomClearanceFraction * inst.length
        scores.add(bottomCheck)
        val topCheck = (1.0 - topClearanceFraction) * inst.length - inst.holePositions.last()
        scores.add(topCheck)
        minHoleSpacing.forEachIndexed { i, value ->
            if (value != null) {
                val check = (inst.holePositions[i + 1] - inst.holePositions[i]) - value
                scores.add(check)
            }
        }
        maxHoleSpacing.forEachIndexed { i, value ->
            if (value != null) {
                val check = value - (inst.holePositions[i + 1] - inst.holePositions[i])
                scores.add(check)
            }
        }
        minHoleDiameters.forEachIndexed { i, value ->
            val check = inst.holeDiameters[i] - value
            scores.add(check)
        }
        maxHoleDiameters.forEachIndexed { i, value ->
            val check = value - inst.holeDiameters[i]
            scores.add(check)
        }
        balance.forEachIndexed { i, value ->
            if (value != null) {
                val check = value * 0.5 * (inst.holePositions[i + 2] - inst.holePositions[i]) - abs(
                    0.5 * inst.holePositions[i] + 0.5 * inst.holePositions[i + 2] - inst.holePositions[i + 1]
                )
                scores.add(check)
            }
        }
        return scores.map { x -> -x }.sum()
    }

    /**
     * ph: Hook to modify instrument before scoring.
     */
    open fun patchInstrument(inst: Instrument): Instrument {
        return inst
    }


    /**
     * ph: Hook for how to calculate emission.
     *
     *  Lets Flute_designer rate emission relative to embouchure hole.
    */
    fun calcEmission(emission: ArrayList<Double>, fingers: ArrayList<Double>): Double {
        return sqrt(emission.sumOf { e -> e * e })
    }

    fun score(i: Instrument): Double {
        val inst = patchInstrument(i)
        var score = 0.0
        var div = 0.0

        var emissionScore = 0.0
        // ph: var emission_score2 = 0.0
        var emissionDiv = 0.0
        if (tweakEmissions != 0.0) {
            inst.prepare()
        }
        inst.preparePhase()
        val s = 1200.0 / ln(2.0)
        for (item in fingerings) {
            val fingers = item.fingers
            val w1 = item.wavelength(transpose)
            val w2 = if (item.nth == null) {
                inst.trueWavelengthNear(w1, fingers)
            } else {
                inst.trueNthWavelengthNear(w1, fingers, item.nth!!)
            }
            val diff = abs(log(w1) - log(w2)) * s
            // ph: weight = w1
            val weight = 1.0
            // ph: score += weight * diff**3 / (1.0 + (diff/20.0)**2)
            score += weight * diff.pow(3)
            div += weight
            /* ph:
            score += (max(0.0,inst.resonance_score(w2/2.0, fingers)) * 1000.0)**3
            diff += max(0.0,inst.resonance_score(w2/3.0, fingers)) * 100.0
            diff += max(0.0,inst.resonance_score(w2/4.0, fingers)) * 100.0
            if inst.resonance_score(w2/3.0, fingers) < 0.0:
                diff += 100.0
            if inst.resonance_score(w2/4.0, fingers) < 0.0:
                diff += 100.0
             */
            if (tweakEmissions != 0.0) {
                val emissionWeight = 1.0 // ph: w1
                emissionDiv += emissionWeight
                val (_, emission) = inst.resonanceScore(w2, fingers, true)
                val rms = calcEmission(emission!!, fingers)
                // ph: math.sqrt(sum(item*item for item in emission))
                val x = log(rms)
                emissionScore += emissionWeight * x
                // ph: emission_score2 += emission_weight * x * x
            }
        }
        var result = (score / div).pow(1.0/3.0)
        if (tweakEmissions != 0.0) {
            val x = emissionScore / emissionDiv
            // ph: #x2 = emission_score2 / emission_div
            // ph: #var = x2-x*x
            result += (tweakEmissions * -x)  //ph: #math.sqrt(var)/x
        }
        return result
        // ph: #return ( (score/scale) ** (1.0/2) )*100.0
    }

    fun _constrainer(stateVec: ArrayList<Double>): Double {
        return constraintScore(unpack(stateVec))
    }

    fun _scorer(state_vec: ArrayList<Double>): Double {
        return score(unpack(state_vec))
    }

    fun _draw(diagram: Diagram, stateVec: ArrayList<Double>, color: String = "#000000", redColor: String = "#ff0000") {
        val instrument = unpack(stateVec)
        instrument.prepare()

        (0 until numberOfHoles).forEach { i ->
            diagram.circle(
                0.0, -instrument.innerHolePositions[i], instrument.holeDiameters[i],
                redColor
            )
            diagram.circle(0.0, -instrument.holePositions[i], instrument.holeDiameters[i], color)
            diagram.profile(instrument.outer, color)
            diagram.profile(instrument.steppedInner, color)
        }

        if (closedTop) {
            val d = instrument.steppedInner(instrument.length)
            diagram.line(listOf(Pair(-0.5 * d, -instrument.length), Pair(0.5 * d, -instrument.length)), color)
        }

        val tickX = instrument.outer.maximum() * -0.625
        for (pos in instrument.innerKinks) {
            diagram.line(listOf(Pair(tickX, -pos), Pair(tickX - 5, -pos)), color)
        }
        for (pos in instrument.inner.pos.drop(1).dropLast(1)) {
            diagram.line(listOf(Pair(tickX - 2, -pos), Pair(tickX - 3, -pos)), color)
        }
        for (pos in instrument.outerKinks) {
            diagram.line(listOf(Pair(tickX - 10, -pos), Pair(tickX - 15, -pos)), color)
        }
        for (pos in instrument.outer.pos.drop(1).dropLast(1)) {
            diagram.line(listOf(Pair(tickX - 12, -pos), Pair(tickX - 13, -pos)), color)
        }
    }

}
