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
import kotlinx.serialization.Transient
import org.goodmath.chalumier.design.DesignParameters
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.Profile

fun<T> ArrayList<T>.dup(): ArrayList<T> {
    val result = ArrayList<T>()
    result.addAll(this)
    return result
}

fun<T> List<T>.dup(): ArrayList<T> {
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
    override var trueLength: Double,
    override var emissionDivide: Double = 1.0,
    override var scale: Double = 1.0,
    override var divisions: List<List<Pair<Int, Double>>>,
    ) : SimpleInstrument() {

    @Transient
    override val actions = ArrayList<ActionFunction>()

    @Transient
    override val actionsPhase = ArrayList<PhaseActionFunction>()

    override val initialEmission = ArrayList<Double>()
    override var steppedInner = inner.asStepped(coneStep)
    override fun dup(): Instrument {
        return Whistle(name,
            length=length,
            inner=inner.dup(),
            outer=outer.dup(),
            innerKinks=innerKinks.dup(),
            outerKinks=outerKinks.dup(),
            numberOfHoles=numberOfHoles,
            holePositions=holePositions.dup(),
            holeAngles=holeAngles.dup(),
            innerHolePositions=innerHolePositions.dup(),
            holeLengths=holeLengths.dup(),
            holeDiameters=holeDiameters.dup(),
            closedTop=closedTop,
            coneStep=coneStep,
            trueLength=trueLength,
            emissionDivide=emissionDivide,
            scale=scale,
            divisions=divisions)
    }

    companion object {
        val builder = object : InstrumentFactory<Whistle>() {
            override fun create(
                designer: InstrumentDesigner<Whistle>,
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
            ): Whistle {
                return Whistle(
                    name = name,
                    length = length,
                    closedTop = closedTop,
                    coneStep = coneStep,
                    holeAngles = holeAngles.dup(),
                    holeDiameters = holeDiameters.dup(),
                    holeLengths = holeLengths.dup(),
                    holePositions = holePositions.dup(),
                    inner = inner.dup(),
                    outer = outer.dup(),
                    innerHolePositions = innerHolePositions.dup(),
                    numberOfHoles = numberOfHoles,
                    innerKinks = innerKinks.dup(),
                    outerKinks = outerKinks.dup(),
                    trueLength = length,
                    divisions = divisions
                )
            }
        }
    }
}
