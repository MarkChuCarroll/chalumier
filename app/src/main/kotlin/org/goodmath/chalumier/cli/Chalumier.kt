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
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import org.goodmath.chalumier.design.*
import java.nio.file.Path


class Design: CliktCommand() {
    private val instrument by option().choice("folkflute", "pflute", "folkwhistle").required()
    private val outputDir: Path by option("--output-dir").path().required()
    private val reportingInterval: Int by option("--report-interval").int().default(5000)

    fun makeDesigner(name: String): InstrumentDesigner {
        return when (instrument) {
            "folkflute" -> folkFluteDesigner(outputDir)
            "pflute" -> pFluteDesigner(outputDir)
            "folkwhistle" -> folkWhistleDesigner(outputDir)
            else -> throw CliktError()
        }
    }
    override fun run() {
        val des = makeDesigner(instrument)
        val i = des.run(outputDir, reportingInterval)
        echo("Designed: " + i)
    }
}

fun main(args: Array<String>) = Design().main(args)

