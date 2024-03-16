package org.goodmath.chalumier.design.instruments

import kotlinx.serialization.Serializable
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.Profile

fun<T> ArrayList<T>.dup(): ArrayList<T> {
    val result = ArrayList<T>()
    result.addAll(this)
    return result
}

@Serializable
class Whistle(
    override val name: String,
    override var length: Double,
    override var inner: Profile,
    override var outer: Profile,
    override val innerKinks: ArrayList<Double>,
    override val outerKinks: ArrayList<Double>,
    override val numberOfHoles: Int,
    override val holePositions: ArrayList<Double>,
    override val holeAngles: ArrayList<Double>,
    override val innerHolePositions: ArrayList<Double>,
    override val holeLengths: ArrayList<Double>,
    override val holeDiameters: ArrayList<Double>,
    override val closedTop: Boolean,
    override val coneStep: Double,
    override var trueLength: Double,
    override var emissionDivide: Double = 1.0,
    override var scale: Double = 1.0) : SimpleInstrument() {

    override val actions = ArrayList<ActionFunction>()
    override val actionsPhase = ArrayList<PhaseActionFunction>()
    override val initialEmission = ArrayList<Double>()
    override var steppedInner = inner.asStepped(coneStep)
    override fun dup(): Instrument {
        return Whistle(name, length, inner, outer,
            innerKinks.dup(),
            outerKinks.dup(),
            numberOfHoles,
            holePositions.dup(),
            holeAngles.dup(),
            innerHolePositions.dup(),
            holeLengths.dup(),
            holeDiameters.dup(),
            closedTop,
            coneStep,
            trueLength,
            emissionDivide,
            scale)
    }

    companion object {
        val builder = object : InstrumentBuilder<Whistle>() {
            override fun create(
                designer: InstrumentDesigner<Whistle>,
                name: String,
                length: Double,
                closedTop: Boolean,
                coneStep: Double,
                holeAngles: ArrayList<Double>,
                holeDiameters: ArrayList<Double>,
                holeLengths: ArrayList<Double>,
                holePositions: ArrayList<Double>,
                inner: Profile,
                outer: Profile,
                innerHolePositions: ArrayList<Double>,
                numberOfHoles: Int,
                innerKinks: ArrayList<Double>,
                outerKinks: ArrayList<Double>
            ): Whistle {
                return Whistle(
                    name = name,
                    length = length,
                    closedTop = closedTop,
                    coneStep = coneStep,
                    holeAngles = holeAngles,
                    holeDiameters = holeDiameters,
                    holeLengths = holeLengths,
                    holePositions = holePositions,
                    inner = inner,
                    outer = outer,
                    innerHolePositions = innerHolePositions,
                    numberOfHoles = numberOfHoles,
                    innerKinks = innerKinks,
                    outerKinks = outerKinks,
                    trueLength = length
                )
            }
        }
    }
}
