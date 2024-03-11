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

import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.DesignParameters
import kotlin.math.max


data class Score(val constraintScore: Double, val intonationScore: Double): Comparable<Score> {
    override operator fun compareTo(other: Score): Int {
        return when (val c = constraintScore.compareTo(other.constraintScore)) {
            0 -> intonationScore.compareTo(other.intonationScore)
            else -> c
        }
    }

    override fun toString(): String {
        if (constraintScore != 0.0) {
            return "(C)%4f".format(constraintScore)
        } else {
            return "(I)%.4f".format(intonationScore)
        }
    }
}

data class ScoredParameters(val parameters: DesignParameters,
                            val score: Score)


/**
 * Now this is where things did get ugly. Figuring out how to rewrite
 * this was a total bear. The original demakein code is sloppily
 * structured, undocumented, and relies on a lot of rather subtle
 * semantics. I've tried to document as much as possible, and
 * I plan to do further cleanups and refactorings once I'm sure
 * it's working.
 */
class Optimizer(
    private val designer: InstrumentDesigner,
    private val constrainer: (DesignParameters) -> Double,
    private val scorer: (DesignParameters) -> Double,
    private val reportingInterval: Int = 5000,
    private val accuracy: Double = 0.001,
    private val monitor: (ScoredParameters,
                  List<DesignParameters>) -> Unit = { _, _ -> Unit }) {


    var lastReport: Long = System.currentTimeMillis()
    var best: ScoredParameters = ScoredParameters(designer.initialDesignParameters(),
        designer.fullScore(designer.initialDesignParameters()))
    var candidates = ArrayList<ScoredParameters>()
    val poolSize = 8
    val computePool = ComputePool(8, designer)
    var parameterSetUpdateCount = 0
    var constraintSatisfactionCount = 0
    var iterations = 0
    val maxCandidates = (designer.initialDesignParameters().size * 5).toInt()


    fun maybeReportStatus() {
        // bump iteration count and print status
        iterations++
        val now = System.currentTimeMillis()
        if (now - lastReport > reportingInterval) {
            System.err.println(
                "[[${iterations}]]: best: ${best.score}, \tmax: ${
                    candidates.map { it.score }.max()
                }, \tcount=${candidates.size}, \tupdates: ${parameterSetUpdateCount}, \tsats: ${constraintSatisfactionCount}")
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
            val constraintScore = constrainer(newDesignParameters)
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


    fun improve(initialDesignParameters: DesignParameters,
                frequencyTolerance: Double = 1e-4,
                parameterSpanTolerance: Double = 1e-6): DesignParameters {

        computePool.start()
        candidates = arrayListOf(best)


        while (!computePool.isDone() || computePool.hasAvailableResults()) {
            maybeReportStatus()

            // If we're not done, and there's available workers waiting, we can
            // add some new instruments to the pool.
            var newCandidate = generateNewCandidate()

            // If newscore is null, that means that in the previous step,
            // we generated something that satisfies constraints, so we
            // don't have anything to move forward with until the compute
            // pool retfirurns something. If there are compute pool score
            // results available, we grab the first one, and use it as our
            // next candidate. Otherwise, we skip out, because we have nothing
            // to evaluate.
            if (newCandidate == null) {
                if (!computePool.hasAvailableResults() || !computePool.isDone() && !computePool.hasAvailableWorkers()) {
                    continue
                } else {
                    newCandidate = computePool.getNextResult() ?: continue
                }
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
            val l = candidates.map { it.score }.sortedBy { it }
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

            // And finally, we look at what we've got to see if it's good
            // enough to declare success. It's not even worth looking if
            // the currents pool isn't full, and the constraint score is greater than 0.
            if (candidates.size >= maxCandidates && best.score.constraintScore == 0.0) {
                var parameterSpan = 0.0
                for (i in 0 until designer.initialDesignParameters().size) {
                    parameterSpan = max(
                        parameterSpan,
                        candidates.map { it.parameters[i] }.max() -
                                candidates.map { it.parameters[i] }.min()
                    )
                }
                var intonationSpan =
                    ArrayList(candidates.map { it.score }).max().intonationScore - best.score.intonationScore
                if (parameterSpan < parameterSpanTolerance || (parameterSetUpdateCount >= 5000 && intonationSpan < frequencyTolerance)) {
                    computePool.finish()
                }
            }
        }
        computePool.finish()
        computePool.joinAll()
        return best.parameters
    }
}
