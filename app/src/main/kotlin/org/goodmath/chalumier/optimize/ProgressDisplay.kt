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
import org.goodmath.chalumier.cli.BoundedTranscript
import kotlin.math.*

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

interface ProgressDisplay {
    fun print(line: String)

    fun setMaximumSpans(iSpan: Double?, pSpan: Double?)
    fun updateStats(
        iterCount: Int, bestScore: Score, size: Int, maxScore: Score, iSpan: Double, pSpan: Double,
        updates: Int, satisfactory: Int
    )
}
class TerminalProgressDisplay(var instrumentName: String, maxLines: Int): ProgressDisplay {
    var maximumISpan: Double = Double.NaN
    var maximumPSpan: Double = Double.NaN
    private var maxIntonationScore: Double = Double.POSITIVE_INFINITY

    private val term = Terminal()

    private val transcript = BoundedTranscript(maxLines)

    override fun print(line: String) {
        transcript.print(line)
    }

    override fun setMaximumSpans(iSpan: Double?, pSpan: Double?) {
        if (iSpan != null && (maximumISpan.isNaN() ||
                maximumISpan < iSpan)) {
            maximumISpan = iSpan
        }
        if (pSpan != null && (maximumPSpan.isNaN() || pSpan > maximumPSpan)) {
            print("Updating pspan: max was ${maximumPSpan}, now $pSpan")
            maximumPSpan = pSpan
        }
    }

    fun updateIntonation(d: Double) {
        if (maxIntonationScore.isInfinite() || maxIntonationScore < d) {
            maxIntonationScore = d
        }
    }

    private val colorSequence = listOf(
        TextColors.brightRed,
        TextColors.brightYellow,
        TextColors.brightMagenta,
        TextColors.brightBlue,
        TextColors.brightGreen
    )

    /**
     * Set a color for the span displays. The color is based on
     * how much the span has decreased during the optimization process.
     * Each time the span reduces by a factor of 10, we switch to the
     * next color in the sequence.
     */
    private fun spanColor(current: Double, orig: Double): TextStyle {
        if (orig.isNaN()) {
            return TextColors.brightRed
        }
        val curLog = ln(current)
        val origLog = ln(orig)
        return colorSequence[min(abs(origLog - curLog).roundToInt(), colorSequence.size - 1)]
    }

    fun fmtIterations(its: Int): String {
        return "%05d".format(its)
    }

    override fun updateStats(
        iterCount: Int, bestScore: Score, size: Int, maxScore: Score, iSpan: Double, pSpan: Double,
        updates: Int, satisfactory: Int
    ) {
        if (maximumISpan.isFinite() && maximumPSpan.isFinite() ) {
            val curILog = ln(iSpan)
            val origILog = ln(maximumISpan)
            val iSpanLogDiff = abs(origILog - curILog).roundToInt()
            print("ISpan: initial=$maximumISpan, current=$iSpan, logDiff=$iSpanLogDiff")
            val curPLog = ln(pSpan)
            val origPLog = ln(maximumPSpan)
            val pSpanLogDiff = abs(origPLog - curPLog).roundToInt()
            print("PSpan: initial=$maximumPSpan, current=$pSpan, logDiff=$pSpanLogDiff")
        }

        val iSpanFmt =
            spanColor(iSpan, maximumISpan)("I%.6f".format(iSpan))
        val pSpanFmt = spanColor(pSpan, maximumPSpan)("P%.6f".format(pSpan))
        val formattedSpans = "$iSpanFmt / $pSpanFmt"
        val iterCountFmt = TextColors.brightCyan("[[${fmtIterations(iterCount)}]]")
        val scoreText = if (bestScore.constraintScore > 0) {
            TextColors.brightRed(bestScore.toString())
        } else {
            TextColors.brightGreen(bestScore.toString())
        }
        val completed = (((maxIntonationScore-bestScore.intonationScore)/maxIntonationScore)*100.0).toInt()
        displayProgressAsTable(iterCountFmt, scoreText, size, maxScore, updates, satisfactory, formattedSpans, completed)

    }

    private fun displayProgressAsTable(
        iterCountFmt: String,
        scoreText: String,
        size: Int,
        maxScore: Score,
        updates: Int,
        satisfactory: Int,
        formattedSpans: String,
        completed: Int
    ) {
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
                width = ColumnWidth.Fixed(6)
            }
            column(3) {
                width = ColumnWidth.Fixed(12)

            }
            column(4) {
                width = ColumnWidth.Fixed(8)
            }
            column(5) {
                width = ColumnWidth.Fixed(8)
            }
            column(6) {
                width = ColumnWidth.Fixed(25)
            }

            header {
                row {
                    cell("Chalumier Instrument Designer/Optimizer: designing $instrumentName") {
                        style = TextStyles.bold + TextColors.brightMagenta
                        columnSpan = 7
                    }
                }
                row {
                    cellBorders = Borders.NONE
                    cell("Iterations")
                    cell("Best")
                    cell("Size")
                    cell("Max")
                    cell("Updates")
                    cell("Goods")
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
                column(6) {
                    align = TextAlign.RIGHT
                    cellBorders = Borders.ALL
                }
                row(
                    iterCountFmt, scoreText, size, maxScore, updates,
                    satisfactory, formattedSpans
                )
                row {
                    cell("$completed%")
                    cell(
                        ProgressBar(
                            total = 100,
                            completed = completed.toLong(),
                            pendingChar = "*",
                            pendingStyle = TextColors.red,
                            completeChar = "#",
                            completeStyle = TextColors.brightGreen
                        )
                    ) {
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
                            columnSpan = 7
                            style = TextColors.brightGreen
                        }
                    }
                }
            }
        })
    }

}