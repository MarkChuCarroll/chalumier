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
package org.goodmath.chalumier.cli.ui

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal

interface ProgressState {
    fun columns(): List<ColumnWidth>

    fun header(): List<Any>

    fun body(): List<Any>

    val height: Int
}

class ProgressMonitor<State : ProgressState>(val instrumentName: String) {
    lateinit var state: State
    private val term = Terminal()
    private val maxLines = (term.info.height - 8)
    private val maxColumns = term.info.width - 4
    private val transcript = BoundedTranscript(maxLines)

    fun print(a: Any) {
        transcript.print(a)
    }

    fun updateState(newState: State) {
        state = newState
        display()
    }

    fun display() {
        term.cursor.move {
            clearScreen()
            setPosition(0, 0)
            startOfLine()
        }
        term.print(
            table {
                borderType = BorderType.SQUARE
                borderStyle = TextColors.rgb("#4b25b9")
                align = TextAlign.CENTER
                tableBorders = Borders.ALL
                state.columns().mapIndexed { i, c ->
                    column(i) {
                        width = c
                    }
                }
                header {
                    row {
                        state.header().forEach {
                            cell(it)
                        }
                    }
                }
                body {
                    row { state.body().forEach { cell(it) } }
                }
                footer {
                    cellBorders = Borders.NONE
                    val lines = transcript.transcript()
                    lines.map {
                        row {
                            cell(it) {
                                columnSpan = state.columns().size
                                align = TextAlign.LEFT
                                style = TextColors.brightGreen
                            }
                        }
                    }
                }
            },
        )
    }
}
