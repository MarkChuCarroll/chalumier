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

import io.github.xn32.json5k.Json5
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.goodmath.chalumier.design.Fingering
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.frequency
import org.goodmath.chalumier.design.wavelength
import org.goodmath.chalumier.errors.ChalumierException
import java.nio.file.Path
import kotlin.io.path.readText

@OptIn(ExperimentalSerializationApi::class)
class DesignerBuilder(val templates: Map<String, (name: String, outputDir: Path) -> InstrumentDesigner<*>>) {

    fun getDesigner(specFile: Path, outputDir: Path): InstrumentDesigner<*> {
        val spec = Json5.decodeFromString<InstrumentSpec>(specFile.readText())
        return getDesigner(spec, outputDir)
    }

    fun getDesigner(spec: InstrumentSpec, dir: Path): InstrumentDesigner<*> {
        val baseTemplate = templates[spec.instrumentType] ?: throw ChalumierException("Unknown instrument type ${spec.instrumentType}")
        val designer = baseTemplate(spec.name, dir)
        designer.initialLength = wavelength(spec.rootNote) * 0.5
        designer.numberOfHoles = spec.numberOfHoles
        designer.fingerings = ArrayList(spec.fingerings)
        spec.innerDiameters?.let { designer.innerDiameters = it }
        spec.outerDiameters?.let { designer.outerDiameters = it }
        spec.maxLength?.let { designer.maxLength = it}
        designer.closedTop = spec.closedTop
        spec.initialLength?.let { designer.initialLength = it }
        designer.transpose = spec.transpose
        spec.tweakEmissions?.let { designer.tweakEmissions = it }
        spec.minHoleDiameters?.let {
            designer.minHoleDiameters = ArrayList(it) }
        spec.maxHoleDiameters?.let { designer.maxHoleDiameters = ArrayList(it) }
        designer.outerAdd = spec.outerAdd
        designer.topClearanceFraction = spec.topClearanceFraction
        designer.bottomClearanceFraction = spec.bottomClearanceFraction
        designer.scale = spec.scale
        spec.minHoleSpacing?.let { designer.minHoleSpacing = ArrayList(it) }
        spec.maxHoleSpacing?.let { designer.maxHoleSpacing = ArrayList(it) }
        spec.balance?.let { designer.balance = ArrayList(it) }
        spec.holeAngles?.let { designer.holeAngles = ArrayList(it) }
        spec.holeHorizAngles?.let { designer.holeHorizAngles = ArrayList(it)}
        spec.divisions?.let { designer.divisions = ArrayList(it.map { el -> ArrayList(el) })}
        return designer
    }

}
