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
import org.goodmath.chalumier.design.TaperedFluteDesigner

@Serializable
class TaperedFlute(
    override val name: String,
    val innerTaper: Double,
    val outerTaper: Double,
    override var length: Double,
    override var inner: Profile,
    override var outer: Profile,
    override val innerKinks: List<Double>,
    override val outerKinks: List<Double>,
    override val numberOfHoles: Int,
    override val holePositions: List<Double>,
    override val holeAngles: List<Double>,
    override val innerHolePositions: List<Double>,
    override var holeLengths: List<Double>,
    override val holeDiameters: List<Double>,
    override val closedTop: Boolean,
    override val coneStep: Double,
    override var trueLength: Double = length,
    override var emissionDivide: Double = 1.0,
    override var scale: Double = 1.0,
    override var divisions: List<List<Pair<Int, Double>>>): SimpleInstrument() {

    override var steppedInner: Profile = inner.asStepped(coneStep)

    override fun dup(): Instrument {
        return TaperedFlute(name, innerTaper, outerTaper,
            length, inner, outer, innerKinks, outerKinks,
            numberOfHoles, holePositions, holeAngles,
            innerHolePositions, holeLengths,
            holeDiameters, closedTop, coneStep, trueLength,
            emissionDivide, scale, divisions)
    }

    companion object {
        val builder = object: InstrumentFactory<TaperedFlute>() {
            override fun create(
                designer: InstrumentDesigner<TaperedFlute>,
                parameters: DesignParameters,
                name: String,
                length: Double,
                closedTop: Boolean,
                coneStep: Double,
                holeAngles: List<Double>,
                holeDiameters: List<Double>,
                holeLengths: List<Double>,
                holePositions: List<Double>,
                inner: Profile,
                outer: Profile,
                innerHolePositions: List<Double>,
                numberOfHoles: Int,
                innerKinks: List<Double>,
                outerKinks: List<Double>,
                divisions: List<List<Pair<Int, Double>>>
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
                    outerTaper = designer.outerTaper,
                    divisions = listOf(
                        listOf(Pair(5, 0.0)),
                        listOf(Pair(2, 0.0), Pair(5, 0.333)),
                        listOf(Pair(-1, 0.9), Pair(2, 0.0), Pair(5, 0.333)),
                        listOf(Pair(-1, 0.9), Pair(2, 0.0), Pair(5, 0.0), Pair(5, 0.7))))
            }

        }
    }
}


