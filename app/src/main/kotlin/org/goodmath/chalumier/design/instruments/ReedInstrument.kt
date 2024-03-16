package org.goodmath.chalumier.design.instruments

import kotlinx.serialization.Serializable
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.Profile


@Serializable
open class ReedInstrument(
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
    override var trueLength: Double = length,
    override var emissionDivide: Double = 1.0,
    override var scale: Double = 1.0
) : SimpleInstrument() {

    override var steppedInner: Profile = inner.asStepped(coneStep)

    override fun dup(): Instrument {
        return ReedInstrument(
            name = name,
            length = length,
            inner = inner,
            outer = outer,
            innerKinks = innerKinks.dup(),
            outerKinks = outerKinks.dup(),
            numberOfHoles = numberOfHoles,
            holeAngles = holeAngles.dup(),
            innerHolePositions = innerHolePositions.dup(),
            holePositions = holePositions.dup(),
            holeLengths = holeLengths.dup(),
            holeDiameters = holeDiameters.dup(),
            closedTop = closedTop,
            coneStep = coneStep,
            emissionDivide = emissionDivide,
            trueLength = trueLength,
            scale = scale
        )
    }


    companion object {
        val builder = object : InstrumentBuilder<ReedInstrument>() {
            override fun create(
                designer: InstrumentDesigner<ReedInstrument>,
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
            ): ReedInstrument {
                return ReedInstrument(
                    name = name,
                    length = length,
                    inner = inner,
                    outer = outer,
                    innerKinks = innerKinks.dup(),
                    outerKinks = outerKinks.dup(),
                    numberOfHoles = numberOfHoles,
                    holeAngles = holeAngles.dup(),
                    innerHolePositions = innerHolePositions.dup(),
                    holePositions = holePositions.dup(),
                    holeLengths = holeLengths.dup(),
                    holeDiameters = holeDiameters.dup(),
                    closedTop = closedTop,
                    coneStep = coneStep,
                    scale = designer.scale
                )
            }

        }
    }
}
