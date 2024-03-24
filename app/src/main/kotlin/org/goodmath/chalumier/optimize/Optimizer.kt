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

import org.goodmath.chalumier.design.DesignParameters


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
    private val initialDesignParameters: DesignParameters,
    private val constraintScorer: (DesignParameters) -> Double,
    private val intonationScorer: (DesignParameters) -> Double,
    private val reportingInterval: Int = 5000,
    private val accuracy: Double = 0.001,
    private val progressReporter: ProgressDisplay,
    private val monitor: (ScoredParameters,
                          List<DesignParameters>) -> Unit = { _, _ ->  }) {


    private var lastReport: Long = System.currentTimeMillis()
    private var lastParameterSpan: Double = Double.NaN
    private var lastIntonationSpan = Double.NaN
    private var best: ScoredParameters = ScoredParameters(initialDesignParameters,
        Score(constraintScorer(initialDesignParameters),
            intonationScorer(initialDesignParameters)))
    private var candidates = ArrayList<ScoredParameters>()
    private val computePool = ComputePool(16, constraintScorer, intonationScorer)
    private var parameterSetUpdateCount = 0
    private var constraintSatisfactionCount = 0
    private var iterations = 0
    private val maxCandidates = initialDesignParameters.size * 5
    private var initialSpans: Pair<Double, Double> = Pair(Double.NaN, Double.NaN)

    /**
     * Bump the iteration count, and if the reporting interval has elapsed,
     * print out a brief status report.
     */
    private fun periodicallyReportStatus() {
        iterations++
        val now = System.currentTimeMillis()
        if (now - lastReport > reportingInterval) {
            progressReporter.updateStats(iterations, best.score, candidates.size,
                candidates.maxOf { it.score }, lastIntonationSpan,
                lastParameterSpan, parameterSetUpdateCount, constraintSatisfactionCount )
            lastReport = now
            if (best.score.constraintScore == 0.0) {
                monitor(best, candidates.map { it.parameters })
            }
        }
    }

    private fun generateNewCandidate(): ScoredParameters? {
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
    private fun updateCandidatePool(newCandidateOpt: ScoredParameters?): Boolean {
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
        // then pick the least accurate element that we want
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


    private var parameterUpdatesAsOfLastCheck = 0
    /**
     * Evaluate the current candidate pool and decide if it's good
     * enough to declare success.
     */
    private fun evaluateResults(parameterSpanTolerance: Double, frequencyTolerance: Double) {
        if (candidates.size >= maxCandidates && best.score.constraintScore == 0.0 && (parameterSetUpdateCount - parameterUpdatesAsOfLastCheck) > 200) {
            parameterUpdatesAsOfLastCheck = parameterSetUpdateCount
            val parameterSpans = (0 until initialDesignParameters.size).map { idx ->
                val allAtIdx = candidates.map { c -> c.parameters[idx] }
                val maxVal = allAtIdx.max()
                val minVal = allAtIdx.min()
                (maxVal - minVal)
            }
            val pMax = parameterSpans.mapIndexed { idx, span -> Pair(span, idx)}.minBy { it.first }
            val scores = candidates.map { c -> c.parameters[pMax.second] }
            val maxP = scores.max()
            val minP = scores.min()
            progressReporter.print("Maximum variation is on parameter ${initialDesignParameters.parameterName(pMax.second)}, ranging $minP..$maxP")
            val parameterSpan = pMax.first

            val maxIntonationScore = candidates.map { it.score }.max().intonationScore
            val intonationSpan = maxIntonationScore - best.score.intonationScore
            lastParameterSpan = parameterSpan
            lastIntonationSpan = intonationSpan
            progressReporter.setMaximumSpans(lastIntonationSpan, lastParameterSpan)
            if (parameterSpan < parameterSpanTolerance || (parameterSetUpdateCount >= 5000 && intonationSpan < frequencyTolerance)) {
                computePool.finish()
            }
        }
    }

    fun optimizeInstrument(frequencyTolerance: Double = 1e-6,
                           parameterSpanTolerance: Double = 1e-6): DesignParameters {
        progressReporter.setMaximumSpans(initialSpans.first, initialSpans.second)
        computePool.start()
        candidates = arrayListOf(best)

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
