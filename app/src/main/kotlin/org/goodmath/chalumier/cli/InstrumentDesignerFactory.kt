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

import kotlinx.serialization.ExperimentalSerializationApi
import org.goodmath.chalumier.config.InstrumentDescription
import org.goodmath.chalumier.config.DescriptionParser
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.errors.ChalumierException
import java.nio.file.Path
import kotlin.io.path.reader

@OptIn(ExperimentalSerializationApi::class)
class InstrumentDesignerFactory(val templates: Map<String, (name: String, outputDir: Path) -> InstrumentDesigner<*>>) {

    fun getDesigner(descriptionFile: Path, outputDir: Path): InstrumentDesigner<*> {
        val desc = DescriptionParser(descriptionFile.reader()).parseConfig()
        val designer = templates[desc.name]!!(desc.name, outputDir)
        designer.updateFromConfig(desc)
        return designer
    }

    fun getDesigner(spec: InstrumentDescription, dir: Path): InstrumentDesigner<*> {
        val baseTemplate = templates[spec.name] ?: throw ChalumierException("Unknown instrument type ${spec.name}")
        val designer = baseTemplate(spec.name, dir)
        designer.updateFromConfig(spec)
        return designer
    }

}
