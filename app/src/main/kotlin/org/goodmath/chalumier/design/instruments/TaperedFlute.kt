package org.goodmath.chalumier.design.instruments

import kotlinx.serialization.Serializable
import org.goodmath.chalumier.design.*

@Serializable
class TaperedFlute(
    override val name: String,
    val innerTaper: Double,
    val outerTaper: Double,
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
    override var trueLength: Double = length,
    override var emissionDivide: Double = 1.0,
    override var scale: Double = 1.0): SimpleInstrument() {

    override var steppedInner: Profile = inner.asStepped(coneStep)

    override fun dup(): Instrument {
        return TaperedFlute(name, innerTaper, outerTaper,
            length, inner, outer, innerKinks, outerKinks,
            numberOfHoles, holePositions, holeAngles,
            innerHolePositions, holeLengths,
            holeDiameters, closedTop, coneStep, trueLength,
            emissionDivide, scale)
    }

    companion object {
        val builder = object: InstrumentBuilder<TaperedFlute>() {
            override fun create(
                designer: InstrumentDesigner<TaperedFlute>,
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
            ): TaperedFlute {
                designer as TaperedFluteDesigner
                return TaperedFlute(
                    name=name,
                    length=length,
                    closedTop=closedTop,
                    coneStep=coneStep,
                    holeAngles=holeAngles.dup(),
                    holeDiameters=holeDiameters.dup(),
                    holeLengths=holeLengths.dup(),
                    holePositions=holePositions.dup(),
                    inner=inner,
                    outer=outer,
                    innerHolePositions=innerHolePositions.dup(),
                    numberOfHoles=numberOfHoles,
                    innerKinks=innerKinks.dup(),
                    outerKinks=outerKinks.dup(),
                    innerTaper = designer.innerTaper,
                    outerTaper = designer.outerTaper
                )
            }

        }
    }
}


