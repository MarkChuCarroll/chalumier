/*
 * Copyright 2024 Mark C. Chu-Carroll and Paul Francis Harrison
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

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import org.goodmath.chalumier.make.InstrumentMakerProgressUpdater

class Make : ChalumierCommand(name = "make", help = "Generate an STL model of an instrument") {
    private val workDir by option("--workdir", help = "The directory to use  for saving files").path(mustExist = false).required()
    private val desc by option(
        "--description-file",
        help = "The path to the description file used by the generator to produce the model",
    ).path(mustExist = true).required()
    private val spec by option(
        "--model-file",
        help = "The path to a file containing a generated model from the designer",
    ).path(mustExist = true).required()

    override fun run() {
        val designer = builder.getDesigner(desc, workDir)

        val progress = ProgressMonitor<MakerState>(designer.name)
        val maker = designer.getInstrumentMaker(spec)

        maker.reporter =
            object : InstrumentMakerProgressUpdater {
                override fun update(
                    name: String,
                    stage: String,
                    total: Int,
                    current: Int,
                ) {
                    progress.updateState(MakerState(name, stage, total, current, 1))
                }

                override fun print(a: Any) {
                    progress.print(a)
                }
            }
        val instrumentParts = maker.run()
        print("Done making instrument. Generated ${instrumentParts.size} parts in the workdir.")

    }
}
