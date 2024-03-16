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

import kotlinx.serialization.Serializable
import org.goodmath.chalumier.util.Randomizer
import org.goodmath.chalumier.util.StandardRandomizer
import kotlin.math.sqrt

/**
 * The design parameters represents the current state of the mutable parameters
 * that can be varied by the design optimizer. Each of these values
 * is, in some form, normalized relative to the size of the instrument.
 * For example, the holePositions are stored in the state as fractions
 * of the instrument's body length.
 */
@Serializable
data class DesignParameters private constructor(
    var length: Double,
    var holePositions: ArrayList<Double>,
    var holeAreas: ArrayList<Double>,
    var innerKinks: ArrayList<Double>,
    var outerKinks: ArrayList<Double>) {

    val size: Int
        get() = (1 + holePositions.size + holeAreas.size + innerKinks.size + outerKinks.size)

    // Get and set methods are provided to make it easier to actually
    // perform the mutations during optimization. In order to make it
    // easier to implement those, it's useful to have these guidepoints
    // for the beginning of each block.
    val holePositionsStart = 1
    val holeAreasStart = holePositionsStart + holePositions.size
    val innerKinksStart = holeAreasStart + holeAreas.size
    val outerKinksStart = innerKinksStart + innerKinks.size

    operator fun get(idx: Int): Double {
        return when(idx) {
            0 -> length
            in (holePositionsStart until holePositions.size + holePositionsStart) ->
                holePositions[idx - holePositionsStart]
            in (holeAreasStart until holeAreas.size + holeAreasStart) ->
                holeAreas[idx - holeAreasStart]
            in (innerKinksStart until innerKinks.size + innerKinksStart) ->
                innerKinks[idx - innerKinksStart]
            in (outerKinksStart until outerKinks.size + outerKinksStart) ->
                outerKinks[idx - outerKinksStart]

            else ->
                throw Exception("Should be impossible")

        }
    }

    operator fun set(idx: Int, value: Double) {
        when(idx) {
            0 -> length = value
            in (holePositionsStart until holePositions.size + holePositionsStart) ->
                holePositions[idx - holePositionsStart] = value
            in (holeAreasStart until holeAreas.size + holeAreasStart) ->
                holeAreas[idx - holeAreasStart] =  value
            in (innerKinksStart until innerKinks.size + innerKinksStart) ->
                innerKinks[idx - innerKinksStart] = value
            in (outerKinksStart until outerKinks.size + outerKinksStart) ->
                outerKinks[idx - outerKinksStart] = value

            else ->
                throw Exception("Should be impossible")

        }
    }

    fun copy(): DesignParameters =
        make(length, holePositions, holeAreas, innerKinks, outerKinks)

    companion object {

        fun make(length: Double,
                 holePositions: ArrayList<Double>,
                 holeAreas: ArrayList<Double>,
                 innerKinks: ArrayList<Double>,
                 outerKinks: ArrayList<Double>): DesignParameters {
            val hpCopy = ArrayList<Double>()
            hpCopy.addAll(holePositions)
            val haCopy = ArrayList<Double>()
            haCopy.addAll(holeAreas)
            val ikCopy = ArrayList<Double>()
            ikCopy.addAll(innerKinks)
            val okCopy = ArrayList<Double>()
            okCopy.addAll(outerKinks)
            return DesignParameters(length, hpCopy, haCopy, ikCopy, okCopy)
        }

        fun generateNewDesignParameters(candidates: List<DesignParameters>, initialAccuracy: Double, doNoiseOpt: Boolean, r: Randomizer = StandardRandomizer): DesignParameters {
            val doNoise = doNoiseOpt || r.nextDouble() < 0.2
            val numberOfCandidates = candidates.size

            // Calculate the weights that we'll use for the different input states
            // to update our instrument.  We're going to try for a gaussian distribution of
            // weights, so we'll start by setting a threshold for the stddev.
            val weightStdDev = (1.0 + 2.0 * r.nextDouble()) / sqrt(numberOfCandidates.toDouble())
            val randomWeights = (0 until numberOfCandidates).map { r.nextGaussian(0.0, weightStdDev) }.toList()
            // We're looking for the weights to be distributed around zero, so we compute the
            // mean, and then subtract it from each weight.
            val offset = (0.0 - randomWeights.sum()) / numberOfCandidates
            val weights = ArrayList(randomWeights.map { weight -> weight + offset })
            // Randomly pick one parameter set and increase it's weight.
            weights[ r.nextInt(numberOfCandidates)] += 1.0
            // Maybe inject extra noise.
            val noise = if (doNoise) { r.nextDouble() * initialAccuracy} else { 0.0 }

            val newDesignParameters = candidates[0].copy()
            for (i in (0 until newDesignParameters.size)) {
                newDesignParameters[i] = candidates.indices.sumOf { candidateIdx ->
                    candidates[candidateIdx][i] * weights[candidateIdx]
                }
            }
            if (noise != 0.0) {
                for (i in 0 until newDesignParameters.size) {
                    val n = r.nextGaussian(0.0, noise)
                    newDesignParameters[i] +=  n
                }
            }
            return newDesignParameters
        }

    }
}
