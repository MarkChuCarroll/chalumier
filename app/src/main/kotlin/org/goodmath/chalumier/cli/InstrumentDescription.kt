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
package org.goodmath.chalumier.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.goodmath.chalumier.design.Fingering
import org.goodmath.chalumier.design.wavelength
import org.goodmath.chalumier.make.JoinType
import org.goodmath.chalumier.util.repeat
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText


@Serializable
class InstrumentDescription(
    val name: String,
    val instrumentType: String,
    val rootNote: String,
    val numberOfHoles: Int,
    val fingerings: List<Fingering>,
    val innerDiameters: List<Pair<Double, Double>>? = null,
    val outerDiameters: List<Pair<Double, Double>>? = null,
    val length: Double = wavelength(rootNote)/2.0,
    val maxLength: Double? = null,
    val closedTop: Boolean = false,
    val initialLength: Double? = null,
    val transpose: Int = 0,
    val tweakEmissions: Double? = 0.0,
    val minHoleDiameters: List<Double>? = null,
    val maxHoleDiameters: List<Double>? = null,
    val outerAdd: Boolean = false,
    val topClearanceFraction: Double = 0.0,
    val bottomClearanceFraction: Double = 0.0,
    val scale: Double = 1.0,
    val join: JoinType = JoinType.StraightJoin,
    val minHoleSpacing: List<Double?>? = null,
    val maxHoleSpacing: List<Double?>? = null,
    val balance: List<Double?>? = null,
    val holeAngles: List<Double> = listOf(0.0).repeat(numberOfHoles),
    val holeHorizAngles: List<Double> = listOf(0.0).repeat(numberOfHoles),
    val booleanOptions: Map<String, Boolean> = hashMapOf(),
    val listOptions: Map<String, List<Double>> = hashMapOf(),
    val doubleOptions: Map<String, Double> = hashMapOf(),
    val intOptions: Map<String, Int> = hashMapOf(),
    val divisions: List<List<Pair<Int, Double>>> =  listOf(
        listOf(Pair(5, 0.0)),
        listOf(Pair(2, 0.0), Pair(5, 0.333)))
        ) {

    fun getDoubleOption(key: String, default: Double=0.0): Double {
        return doubleOptions[key] ?: default
    }

    fun getIntOption(key: String, default: Int = 0): Int {
        return intOptions[key] ?: default
    }

    fun getListOption(key: String,
                      default: (inst: InstrumentDescription) -> List<Double> = { _ -> emptyList() }): List<Double> {
        return listOptions[key] ?: default(this)
    }

    fun getBooleanOption(key: String, default: Boolean = false): Boolean {
        return booleanOptions[key] ?: default
    }
     override fun toString(): String =
        Json.encodeToString(this)

    companion object {
        fun readFromFile(specFile: Path): InstrumentDescription {
            if (specFile.notExists()) {
                throw IOException("File ${specFile} does not exist")
            }
            val spec = specFile.readText()
            return Json.decodeFromString<InstrumentDescription>(spec)
        }
    }
}

