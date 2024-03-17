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
package org.goodmath.chalumier.design.instruments

import kotlinx.serialization.Serializable
import org.goodmath.chalumier.design.DesignParameters
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.ReedInstrumentDesigner


@Serializable
open class ReedInstrument(
    override val name: String,
    override var length: Double,
    override var inner: Profile,
    override var outer: Profile,
    val bore: Double,
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
    override var scale: Double = 1.0,
    override var divisions: List<List<Pair<Int, Double>>>

) : SimpleInstrument() {

    override var steppedInner: Profile = inner.asStepped(coneStep)

    override fun dup(): Instrument {
        return ReedInstrument(
            name = name,
            length = length,
            inner = inner,
            outer = outer,
            bore = bore,
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
            scale = scale,
            divisions = divisions
        )
    }


    companion object {
        val builder = object : InstrumentFactory<ReedInstrument>() {
            override fun create(
                designer: InstrumentDesigner<ReedInstrument>,
                parameters: DesignParameters,
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
                outerKinks: ArrayList<Double>,
                divisions: List<List<Pair<Int, Double>>>
            ): ReedInstrument {
                designer as ReedInstrumentDesigner<ReedInstrument>
                return ReedInstrument(
                    name = name,
                    length = length,
                    inner = inner,
                    outer = outer,
                    bore = designer.bore,
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
                    scale = designer.scale,
                    divisions = divisions
                )
            }

        }
    }
}
