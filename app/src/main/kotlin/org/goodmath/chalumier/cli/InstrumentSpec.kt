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
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText

@Serializable
enum class InstrumentType {
    Flute, Shawm, Whistle, Reedpipe
}


@Serializable
data class InstrumentSpec(
    val name: String,
    val instrumentType: String,
    val rootNote: String,
    val numberOfHoles: Int,
    val fingerings: List<Fingering>,
    val innerDiameters: List<Pair<Double, Double>>? = null,
    val outerDiameters: List<Pair<Double, Double>>? = null,
    val length: Double? = null,
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
    val minHoleSpacing: List<Double?>? = null,
    val maxHoleSpacing: List<Double?>? = null,
    val balance: List<Double?>? = null,
    val holeAngles: List<Double>? = null,
    val holeHorizAngles: List<Double>? = null,
    val divisions: List<List<Pair<Int, Double>>>? = null
) {

    override fun toString(): String =
        Json.encodeToString(this)

    companion object {
        fun readFromFile(specFile: Path): InstrumentSpec {
            if (specFile.notExists()) {
                throw IOException("File ${specFile} does not exist")
            }
            val spec = specFile.readText()
            return Json.decodeFromString<InstrumentSpec>(spec)
        }
    }
}

