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

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import org.goodmath.chalumier.scad.FluteModel
import java.nio.file.Path
import kotlin.io.path.writeText

class Model: ChalumierCommand(name="model", help="Generate a 3d model of a design") {
    val descFile: Path by argument("--description").path(mustExist = true)
    val specFile: Path by option("--spec", help="the path of a JSON file containing a generated instrument").path(mustExist = true).required()
    val workDir: Path by option("--workdir", help="The directory to save files").path(mustExist = true).required()
    val facets: Int by option("--facets", help="Number of outer facets on the instrument").int().default(0)
    val output: Path by option("--output", help="The path to write the OpenSCAD file to").path(mustExist = false).required()

    override fun run() {
        val (inst, des) = builder.getDesigner(descFile, workDir)
        val spec = des.readInstrument(specFile)

        output.writeText(FluteModel(spec, facets).render())
    }
}
