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
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.Viewport
import com.github.ajalt.mordant.widgets.progress.*
import com.github.ajalt.mordant.widgets.progressLayout
import java.awt.Color.magenta

import kotlin.concurrent.thread

class Make: ChalumierCommand(name = "make", help="Generate an STL model of an instrument") {
    val workDir by option("--workdir", help="The directory to use  for saving files").path(mustExist = false).required()
    val desc by option("--description-file", help="The path to the description file used by the generator to produce the model").path(mustExist = true).required()
    val spec by option("--model-file", help="The path to a file containing a generated model from the designer").path(mustExist = true).required()
    override fun run() {
        val (description, designer) = builder.getDesigner(desc, workDir)
        val maker =  designer.getInstrumentMaker(spec, description)
        val term = Terminal()
        val height = term.info.height
        term.cursor.move {
            setPosition(0, height - 6)
            startOfLine()
            clearScreenAfterCursor()
        }
        val progress = progressBarLayout {
            marquee(term.theme.warning("Making instrument"), width = 15)
            percentage()
            progressBar()
            completed(style = term.theme.success)
        }.animateOnThread(term)
        var text = ""
        val transcript = term.animation<String> {
            Viewport(Text(text), width = term.info.width -4, height = 3)
        }

        val fut = progress.execute()
        progress.update { total = maker.totalSteps() }

        maker.statusUpdater = { s ->
            term.cursor.move {
                setPosition(0, height-5)
            }
            text = text + s
            transcript.update(text)
        }
        val th = thread {
            maker.run()
        }

        var count = 0
        while (!progress.finished) {
            val steps = maker.progress
            progress.advance(steps - count)
            count = steps
            Thread.sleep(100)
        }
        th.join()
        fut.get()
        System.err.println(text)
    }
}
