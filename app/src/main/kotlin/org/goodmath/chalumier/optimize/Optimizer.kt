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
import com.github.ajalt.mordant.rendering.BorderType.Companion.SQUARE_DOUBLE_SECTION_SEPARATOR
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import org.goodmath.chalumier.design.DesignParameters
import org.kotlinmath.NaN
import java.awt.FlowLayout.CENTER
import java.awt.FlowLayout.RIGHT
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt


/**
 * This is where things _did_ get ugly. Figuring out how to rewrite
 * this was a total bear. The original demakein code is sloppily
 * structured, undocumented, and relies on a lot of rather subtle
 * semantics. I've tried to document as much as possible, and I think
 * this is a lot clearer than the original demakein code, while doing
 * the same underlying task.
 */
class Optimizer(
    private val instrumentName: String,
    private val echo: (Any?) -> Unit,
    private val initialDesignParameters: DesignParameters,
    private val constraintScorer: (DesignParameters) -> Double,
    private val intonationScorer: (DesignParameters) -> Double,
    private val reportingInterval: Int = 5000,
    private val accuracy: Double = 0.001,
    private val monitor: (ScoredParameters,
                  List<DesignParameters>) -> Unit = { _, _ -> Unit }) {


    var lastReport: Long = System.currentTimeMillis()
    var lastParameterSpan: Double = Double.NaN
    var lastIntonationSpan: Double = Double.NaN
    var best: ScoredParameters = ScoredParameters(initialDesignParameters,
        Score(constraintScorer(initialDesignParameters),
            intonationScorer(initialDesignParameters)))
    var candidates = ArrayList<ScoredParameters>()
    val computePool = ComputePool(16, constraintScorer, intonationScorer)
    var parameterSetUpdateCount = 0
    var constraintSatisfactionCount = 0
    var iterations = 0
    val maxCandidates = (initialDesignParameters.size * 2).toInt()
    var initialSpans: Pair<Double, Double> = Pair(Double.NaN, Double.NaN)

    val colorSequence =listOf(TextColors.brightRed, TextColors.brightYellow, TextColors.brightMagenta, TextColors.brightBlue, TextColors.brightGreen)
    val term = Terminal()
    val height = term.info.height


    fun color(current: Double, orig: Double): TextStyle {
        if (orig.isNaN()) {
            return TextColors.brightYellow
        }
        val curlog = log10(current).roundToInt()
        val origlog = log10(orig).roundToInt()
        return  colorSequence[abs(origlog - curlog) % colorSequence.size]
    }


    /**
     * Bump the iteration count, and if the reporting interval has elapsed,
     * print out a brief status report.
     */
    fun periodicallyReportStatus() {
        fun fmtIterations(its: Int): String {
            return "%05d".format(its)
        }
        iterations++
        val now = System.currentTimeMillis()
        if (now - lastReport > reportingInterval) {
            val iSpan = color(lastIntonationSpan, initialSpans.first)("I%.6f".format(lastIntonationSpan))
            val pSpan = color(lastParameterSpan, initialSpans.second)("P%.6f".format(lastParameterSpan))
            val formattedSpans = "$iSpan / $pSpan"
            val iterCount = TextColors.brightCyan("[[${fmtIterations(iterations)}]]")
            val scoreText = if (best.score.constraintScore > 0) {
                TextColors.brightRed(best.score.toString())
            } else {
                TextColors.brightGreen(best.score.toString())
            }

            term.cursor.move {
                setPosition(0, height - 6)
                startOfLine()
                clearScreenAfterCursor()
            }
            echo(table {
                borderType = BorderType.SQUARE
                borderStyle = rgb("#4b25b9")
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
                    style = TextColors.brightBlue + TextStyles.bold
                    row {
                        cellBorders = Borders.NONE
                        cell("Iterations")
                        cell("Best")
                        cell("Max")
                        cell("Updates")
                        cell("Satisfactory")
                        cell("Spans")
                    }
                }
                body {
                    style = TextColors.green
                    column(0) {
                        align = TextAlign.RIGHT
                        cellBorders = Borders.ALL
                        style = TextColors.brightBlue + TextStyles.bold
                    }
                    column(1) {
                        cellBorders = Borders.ALL
                        align = TextAlign.RIGHT
                        style = TextColors.brightBlue
                    }
                    column(2) {
                        align = TextAlign.RIGHT
                        cellBorders = Borders.ALL
                        style = TextColors.brightBlue
                    }
                    column(3) {
                        align = TextAlign.RIGHT
                        cellBorders = Borders.ALL
                        style = TextColors.brightBlue
                    }
                    column(4) {
                        align = TextAlign.RIGHT
                        cellBorders = Borders.ALL
                        style = TextColors.brightBlue
                    }
                    column(5) {
                        align = TextAlign.CENTER
                        cellBorders = Borders.ALL
                        style = TextColors.brightBlue
                    }
                    row(
                        iterCount, scoreText, candidates.map { it.score }.max(), parameterSetUpdateCount,
                        constraintSatisfactionCount, formattedSpans
                    )
                }
            })

/*            echo(
                "$iterCount: best: $scoreText , max: ${
                    candidates.map { it.score }.max()
                }, updates: ${parameterSetUpdateCount},   sats: ${
                    constraintSatisfactionCount
                }, spans=$formattedSpans")*/
            lastReport = now
            if (best.score.constraintScore == 0.0) {
                monitor(best, candidates.map { it.parameters })
            }
        }
    }

    fun generateNewCandidate(): ScoredParameters? {
        if (!computePool.isDone() && computePool.hasAvailableWorkers()) {
            // Generate a new mutant to consider
            val newDesignParameters = DesignParameters.generateNewDesignParameters(
                candidates.map { it.parameters },
                accuracy, candidates.size < maxCandidates
            )
            val constraintScore = constraintScorer(newDesignParameters)
            // If it satisfies constraints, then we're going to dispatch
            // it to the compute pool for evaluation. If not, then
            // we'll just pass it straight through to see if it's
            // closer to satisfying the constraints than anything
            // in the current set.
            return if (constraintScore != 0.0) {
                val newScore = Score(constraintScore, 0.0)
                return ScoredParameters(newDesignParameters, newScore)
            } else {
                computePool.addTask(newDesignParameters)
                null
            }
        } else {
            return null
        }
    }

    /**
     * Update the candidate pool of parameters with a new parameter set.
     *
     * If there is no new parameter set available, then returns false.
     *
     */
    fun updateCandidatePool(newCandidateOpt: ScoredParameters?): Boolean {
        // If the new candidate is null, that means that in the previous step,
        // we generated something that satisfies constraints, so we
        // don't have anything to move forward with until the compute
        // pool returns something. If there are compute pool score
        // results available, we grab the first one, and use it as our
        // next candidate. Otherwise, we skip out, because we have nothing
        // to evaluate.
        val newCandidate = newCandidateOpt ?:
            if (!computePool.hasAvailableResults() || !computePool.isDone() && computePool.hasAvailableWorkers()) {
                return false
            } else {
                computePool.getNextResult() ?: return false

            }

        if (newCandidate.score.constraintScore == 0.0) {
            constraintSatisfactionCount += 1
        }
        // Now we want to decide whether our new candidate should
        // be included in the current pool of parameter sets that
        // will be used to create the next generation of candidates.
        // We sort the current set by its intonation score, and
        // and then pick the least accurate element that we want
        // to keep as a cutoff threshold.
        val l = candidates.map { it.score }.sortedBy { it.intonationScore }
        val c = if (maxCandidates < l.size) {
            l[maxCandidates].intonationScore
        } else {
            1e30
        }
        val cutoff = Score(best.score.constraintScore, c)
        // If our new score is smaller than where we would have put
        // a cutoff, then we consider it viable, and add it to the
        // current candidates pool, also discarding anything beyond
        // the cutoff threshold.
        if (newCandidate.score <= cutoff) {
            candidates = ArrayList(candidates.filter { it.score <= cutoff })
            candidates.add(newCandidate)
            parameterSetUpdateCount += 1
            if (newCandidate.score <= best.score) {
                best = newCandidate
            }
        }
        return true
    }


    var parameterUpdatesAsOfLastCheck = 0
    /**
     * Evaluate the current candidate pool and decide if it's good
     * enough to declare success.
     */
    private fun evaluateResults(parameterSpanTolerance: Double, frequencyTolerance: Double) {
        if (candidates.size >= maxCandidates && best.score.constraintScore == 0.0 && (parameterSetUpdateCount - parameterUpdatesAsOfLastCheck) > 200) {
            parameterUpdatesAsOfLastCheck = parameterSetUpdateCount
            val parameterSpan = (0 until initialDesignParameters.size).map { idx ->
                val allAtIdx = candidates.map { c -> c.parameters[idx] }
                val maxVal = allAtIdx.max()
                val minVal = allAtIdx.min()
                (maxVal - minVal)
            }.max()

            var intonationSpan =
                ArrayList(candidates.map { it.score }).max().intonationScore - best.score.intonationScore
            lastParameterSpan = parameterSpan
            lastIntonationSpan = intonationSpan
            if (initialSpans.first.isNaN()) {
                initialSpans = Pair(lastIntonationSpan, lastParameterSpan)
            }
            if (parameterSpan < parameterSpanTolerance || (parameterSetUpdateCount >= 5000 && intonationSpan < frequencyTolerance)) {
                computePool.finish()
            }
        }
    }


    fun optimizeInstrument(frequencyTolerance: Double = 1e-5,
                           parameterSpanTolerance: Double = 1e-4): DesignParameters {

        computePool.start()
        candidates = arrayListOf(best)

        term.cursor.move {
            setPosition(0, height-7)
            clearScreenAfterCursor()
        }
        echo(TextColors.brightMagenta("Chalumier Optimizer: analyzing instrument design for ${instrumentName}"))
        while (!computePool.isDone() || computePool.hasAvailableResults()) {
            periodicallyReportStatus()

            // If we're not done, and there's available workers waiting, we can
            // add some new instruments to the pool.
            val newCandidate = generateNewCandidate()

            // Next we update the pool with the new candidate. If we
            // couldn't get a new candidate, then update will return false,
            // and we'll skip back to try again to generate another one.
            if (!updateCandidatePool(newCandidate)) {
                continue
            }

            // And finally, we look at what we've got to see if it's good
            // enough to declare success. It's not even worth looking if
            // the currents pool isn't full, and the constraint score is greater than 0.
            evaluateResults(parameterSpanTolerance, frequencyTolerance)
        }
        return best.parameters
    }


}
