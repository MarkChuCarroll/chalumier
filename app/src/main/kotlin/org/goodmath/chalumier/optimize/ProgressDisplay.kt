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
package org.goodmath.chalumier.optimize

import com.github.ajalt.mordant.rendering.*
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.ProgressBar
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/*
 * Chalumier is doing a lot of computation behind the scenes, and
 * it can spend a long time before producing a result. It can also
 * spend a lot of time chasing down a dead end that'll never get to
 * anything good. In order to make it possible to know at least a little
 * of what's going on, I put together this display code, which draws a
 * chart on the screen showing you how much it's done, where it's model
 * stands on the moment, how close it is to converging, and a rough scale
 * of the degree of intonation error still present in the model.
 */

class BoundedTranscript(val size: Int) {
    val elements: Array<String> = Array<String>(size) { "" }
    var len = 0
    var front = 0
    fun print(s: String) {
        synchronized(this) {
            if (len == size) {
                elements[front] = s
                front = (front + 1) % size
            } else {
                elements[front + len] = s
                len++
            }
        }
    }

    operator fun get(idx: Int): String {
        return elements[(front + idx) % size]
    }

    fun transcript(): List<String> {
        synchronized(this) {
            return (0 until len).map {
                this[it]
            }
        }
    }
}

class ProgressDisplay(lines: Int) {
    var initialISpan: Double = Double.NaN
    var initialPSpan: Double = Double.NaN
    var lastIntonationSpan: Double = Double.NaN
    var lastParameterSpan: Double = Double.NaN
    var intonationDiff = 0.0
    var maxIntonationDifference = Double.NaN

    private val term = Terminal()
    private val height = term.info.height
    private val width = term.info.width - 2

    val transcript = BoundedTranscript(lines)

    fun updateIntonation(d: Double) {
        intonationDiff = d
        if (maxIntonationDifference.isNaN() || maxIntonationDifference < d) {
            maxIntonationDifference = intonationDiff
        }
    }

    private val colorSequence = listOf(
        TextColors.brightRed,
        TextColors.brightYellow,
        TextColors.brightMagenta,
        TextColors.brightBlue,
        TextColors.brightGreen
    )


    private fun spanColor(current: Double, orig: Double): TextStyle {
        if (orig.isNaN()) {
            return TextColors.brightYellow
        }
        val curLog = log10(current).roundToInt()
        val origLog = log10(orig).roundToInt()
        return colorSequence[abs(origLog - curLog) % colorSequence.size]
    }

    fun fmtIterations(its: Int): String {
        return "%05d".format(its)
    }

    fun updateStats(
        iterCount: Int, numberOfCandidates: Int, bestScore: Score, maxScore: Score, iSpan: Double, pSpan: Double,
        updates: Int, satisfactory: Int
    ) {
        val iSpanFmt =
            spanColor(iSpan, initialISpan)("I%.6f".format(iSpan))
        val pSpanFmt = spanColor(pSpan, initialPSpan)("P%.6f".format(pSpan))
        val formattedSpans = "$iSpanFmt / $pSpanFmt"
        val iterCountFmt = TextColors.brightCyan("[[${fmtIterations(iterCount)}]]")
        val scoreText = if (bestScore.constraintScore > 0) {
            TextColors.brightRed(bestScore.toString())
        } else {
            TextColors.brightGreen(bestScore.toString())
        }

        term.cursor.move {
            setPosition(0, 0)
            startOfLine()
            clearScreenAfterCursor()
        }
        term.print(table {
            borderType = BorderType.SQUARE
            borderStyle = TextColors.rgb("#4b25b9")
            align = TextAlign.CENTER
            tableBorders = Borders.ALL
            column(0) {
                width = ColumnWidth.Fixed(12)
            }
            column(1) {
                width = ColumnWidth.Fixed(12)
            }
            column(2) {
                width = ColumnWidth.Fixed(12)
            }
            column(3) {
                width = ColumnWidth.Fixed(8)
            }
            column(4) {
                width = ColumnWidth.Fixed(8)
            }
            column(5) {
                width = ColumnWidth.Fixed(25)
            }

            header {
                row {
                    cell("Chalumier Instrument Designer/Optimizer") {
                        style = TextStyles.bold + TextColors.brightMagenta
                        columnSpan=6
                    }
                }
                row {
                    cellBorders = Borders.NONE
                    cell("Iterations")
                    cell("Best")
                    cell("Max")
                    cell("Updates")
                    cell("Satisfactory")
                    cell("Spans")
                    style = TextColors.brightBlue + TextStyles.bold
                }
            }
            body {

                column(0) {
                    align = TextAlign.RIGHT
                    cellBorders = Borders.ALL
                }
                column(1) {
                    cellBorders = Borders.ALL
                    align = TextAlign.RIGHT
                }
                column(2) {
                    align = TextAlign.RIGHT
                    cellBorders = Borders.ALL
                }
                column(3) {
                    align = TextAlign.RIGHT
                    cellBorders = Borders.ALL
                }
                column(4) {
                    align = TextAlign.RIGHT
                    cellBorders = Borders.ALL
                }
                column(5) {
                    align = TextAlign.CENTER
                    cellBorders = Borders.ALL
                }
                row(
                    iterCountFmt, scoreText, maxScore, updates,
                    satisfactory, formattedSpans
                )
                row {
                    cell(
                        ProgressBar(
                            total=maxIntonationDifference.roundToLong(),
                            completed=(maxIntonationDifference-intonationDiff).roundToLong(),
                            pendingChar = "*",
                            pendingStyle = TextColors.red,
                            completeChar = "#",
                            completeStyle = TextColors.brightGreen
                    )) {
                        columnSpan = 6
                    }
                }
            }
            footer {
                cellBorders = Borders.NONE
                val lines = transcript.transcript()
                lines.map {
                        row {
                            cell(it) {
                                align = TextAlign.LEFT
                                columnSpan = 6
                                style = TextColors.brightGreen
                            }
                        }
                    }
                }
        })

    }

}