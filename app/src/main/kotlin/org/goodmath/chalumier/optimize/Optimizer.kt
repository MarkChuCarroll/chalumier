package org.goodmath.chalumier.optimize

import org.goodmath.chalumier.design.Instrument
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.DesignParameters
import kotlin.math.max


/**
 * A lot of the optimization process works with constraint scores
 * and frequency scores. It's a lot easier to follow the code when
 * it's written as "bestScore.frequencyScore" than "best[0][1]".
 */
data class Score(val constraintScore: Double, val frequencyScore: Double): Comparable<Score> {
    override operator fun compareTo(other: Score): Int {
        return if (constraintScore == 0.0 && other.constraintScore != 0.0) {
            -1
        } else {
            when (val c = constraintScore.compareTo(other.constraintScore)) {
                0 -> frequencyScore.compareTo(other.frequencyScore)
                else -> c
            }
        }
    }

    override fun toString(): String {
        return if (constraintScore != 0.0) {
            "C%.6f".format(constraintScore)
        } else {
            "F%.6f".format(frequencyScore)
        }
    }

}

/**
 * Similarly, we keep a list of states and scores, so
 * a custom object makes it easier to read: candidate.state.minHoleDiameter[3]
 * is much nicer than c[0]][11].
 */
data class ScoredState(
    val score: Score,
    val state: DesignParameters
)

/**
 * I'm not sure why, but I was struggling the number of variable declarations
 * in the optimizer. Clustering them into an object like this made the
 * code easier for me to read.
 */
data class OptimizationState(
    var lastReport: Long,
    var best: DesignParameters,
    var bestScore: Score,
    var nGood: Int,
    var nReal: Int,
    var iterations: Int,
    var poolSize: Int,
    var currents: ArrayList<ScoredState>
) {
    companion object {
        fun initialOptimizationState(
            designer: InstrumentDesigner<*>,
            constrainer: (DesignParameters) -> Double,
            scorer: (DesignParameters) -> Double
        ): OptimizationState {
            val designState = designer.initialDesignParameters
            val cScore = constrainer(designState)
            val score = if (cScore == 0.0) { Score(0.0, scorer(designState))} else { Score(cScore, 0.0) }
            return OptimizationState(
                lastReport = System.currentTimeMillis(),
                best = designState,
                bestScore = score,
                nGood = 0,
                nReal = 0,
                iterations =  0,
                poolSize = (designState.size*5).toInt(),
                currents = arrayListOf(ScoredState(score, designState)))
        }
    }
}

class Optimizer<T: Instrument<T>>(val designer: InstrumentDesigner<T>) {

    fun improve(comment: String,
                designer: InstrumentDesigner<T>,
                constrainer: (DesignParameters) -> Double,
                scorer: (DesignParameters) -> Double,
                frequencyTolerance: Double = 1e-4,
                xTolerance: Double = 1e-6,
                initialAccuracy: Double = 0.001,
                monitor: (Score,
                          DesignParameters,
                          List<DesignParameters>) -> Unit = { _, _, _ -> Unit }): Instrument<T> {
        val opt = OptimizationState.initialOptimizationState(designer, constrainer,scorer)
        val driver = ScoringPool(opt.poolSize, designer)
        driver.start()
        while (!driver.isDone() || driver.hasAvailableWorkers()) {
            // bump iteration count and print status
            opt.iterations++
            val now = System.currentTimeMillis()
            if (now - opt.lastReport > 10000) {
                printStatus(opt)
                if (opt.bestScore.constraintScore == 0.0) {
                    monitor(opt.bestScore, opt.best, opt.currents.map { it.state })
                }
            }

            var newDesignParameters: DesignParameters? = null
            var newScore: Score? = null

            // If we're not done, and there's available workers waiting, we can
            // add some new instruments to the pool.
            if (!driver.isDone() && driver.hasAvailableWorkers()) {
                // Generate a new mutant to consider
                newDesignParameters = DesignParameters.generateNewDesign(
                    opt.currents.map { it.state },
                    initialAccuracy, opt.currents.size < opt.poolSize
                )
                val constraintScore = constrainer(newDesignParameters)
                // If our new state satisfies all constraints, then we
                // submit it to the work queue for evaluation.
                if (constraintScore == 0.0) {
                    driver.addParameterSetToScore(newDesignParameters)
                } else {
                    // Otherwise, add it into the current mix, and maybe
                    // add it back to the mutation pool.
                    newScore = Score(constraintScore, 0.0)
                }

                // If we just added a task to the work queue, then we'll
                //  see if there are any results available from the workers.
                if (newScore == null) {
                    if (!driver.hasAvailableScores() || (!driver.isDone() && driver.hasAvailableWorkers())) {
                        continue
                    }
                    val (completedTaskState, completedTaskScore) = driver.getNextScore() ?: continue
                    newScore = Score(0.0, completedTaskScore)
                    newDesignParameters = completedTaskState
                }

                // We've got a new state and score - either one
                // that needs work on its constraints, or one that came
                // back from the work queue with its evaluation score.
                // So now, we're going to check if it should be added
                // to the current candidate pool.
                if (newScore.constraintScore == 0.0) {
                    opt.nReal += 1
                }
                // We don't want to let the current candidate pool get too large, and
                // it might have grown due to additions, so we need to periodically
                // prune it to keep it at or below the number of available workers.
                // The easy way to do that is to just sort the scores of the
                // current pool, and then pick the cutoff value by looking at the
                // score of the index after the desired number of entries.
                //
                // If the number of entries is smaller than the pool size, then
                // we just throw away anything whos score is so large that it'll
                // never produce a solution.
                val orderedCurrents = opt.currents.map { it.score.frequencyScore }.sorted()
                val c = if (opt.poolSize < orderedCurrents.size) {
                    orderedCurrents[opt.poolSize]
                } else {
                    1e30
                }
                val cutoff = Score(opt.bestScore.constraintScore, c)
                // Now we know a cutoff value. If our new model and score
                // are better that the cutoff, then we drop the top value off
                // the currentset, and add the new one in.
                if (newScore <= cutoff) {
                    opt.currents = ArrayList(opt.currents.filter { it.score <= cutoff })
                    opt.currents.add(ScoredState(newScore, newDesignParameters))
                    opt.nGood += 1
                    if (newScore < opt.bestScore) {
                        opt.bestScore = newScore
                        opt.best = newDesignParameters
                    }
                }

                // Termination check: I don't entirely understand this yet, but...
                // If we've got lots of results that satisfy the required constraints,
                // then we check to see if it's good enough to accept as a final result.
                if (opt.currents.size >= opt.poolSize && opt.bestScore.constraintScore == 0.0) {
                    // xSpan is looking for the maximum distance between values
                    // in the current pool for any parameter.
                    var xSpan = 0.0
                    for (i in 0 until designer.initialDesignParameters.size) {
                        xSpan = max(
                            xSpan,
                            opt.currents.map { it.state[i] }.max() -
                                    opt.currents.map { it.state[i] }.min()
                        )
                    }
                    // fSpan is measuring the maximum distance between the frequency/intonation
                    // scores in the pool and the current best candidate.
                    var fSpan =
                        ArrayList(opt.currents.map { it.score }).max().frequencyScore - opt.bestScore.frequencyScore
                    // If xSpan is smaller that the xTolerance from the original call,
                    // and fSpan is smaller than the frequency tolerance from the call,
                    // then we declare success.
                    if (xSpan < xTolerance || (opt.nGood >= 5000 && fSpan < frequencyTolerance)) {
                        driver.finish()
                    }
                }
            }
        }
        // print status
        driver.joinAll()
        return designer.makeInstrumentFromParameters(opt.best)
    }

    private fun<T: Instrument<T>> printStatus(
        opt: OptimizationState) {
        System.err.println(
            "Best=${opt.bestScore}, max(currents)=${
                opt.currents.map { it.score }.max()
            }"
        )
        System.err.println("\tnumCurrents=${opt.currents.size}, good=${opt.nGood}, real=${opt.nReal}, iteration=${opt.iterations}")
        System.err.println("Current best state: ${opt.best}")
        opt.lastReport = System.currentTimeMillis()
    }

}
