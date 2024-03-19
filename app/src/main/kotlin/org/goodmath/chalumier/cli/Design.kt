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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import kotlin.io.path.div

abstract class ChalumierCommand(name: String, help: String): CliktCommand(name, help) {
    val templates = mapOf(
        "folkFlute" to { n: String, dir: Path ->  org.goodmath.chalumier.design.folkFluteDesigner(n, dir) },
        "pFlute" to { n: String, dir: Path -> org.goodmath.chalumier.design.pFluteDesigner(n, dir)},
        "folkShawm" to { n: String, dir: Path -> org.goodmath.chalumier.design.FolkShawmDesigner(n, dir) },
        "folkWhistle" to { n: String, dir: Path -> org.goodmath.chalumier.design.folkWhistleDesigner(n, dir) },
        "recorder" to { n: String, dir: Path -> org.goodmath.chalumier.design.RecorderDesigner(n, dir) }
    )
    val builder = InstrumentDesignerFactory(templates)

}

class Design: ChalumierCommand(name = "design", help="Compute an instrument design from a specification") {

    private val specFile: Path by argument("instrument-spec.json5").path(mustExist = true)
    private val outputDir: Path by option("--output-dir").path().required()
    private val reportingInterval: Int by option("--report-interval").int().default(5000)


    override fun run() {
        val des = builder.getDesigner(specFile, outputDir)
        val i = des.run(::echo, reportingInterval)
        echo("Execution complete! Designed ${i.name}; diagram in ${outputDir / "diagram.svg"}")
    }
}
