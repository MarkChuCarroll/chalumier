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
            return "C%4f".format(constraintScore)
        } else {
            return "I%.4f".format(intonationScore)
        }
    }
}

data class ScoredParameters(val parameters: DesignParameters,
                            val score: Score)

class Optimizer(
    val designer: InstrumentDesigner,
    val constrainer: (DesignParameters) -> Double,
    val scorer: (DesignParameters) -> Double,
    val reportingInterval: Int = 5000,
    val monitor: (Score,
                  DesignParameters,
                  List<DesignParameters>) -> Unit = { _, _, _ -> Unit }) {


    var lastReport: Long = System.currentTimeMillis()
    lateinit var best: DesignParameters
    lateinit var bestScore: Score
    var currents = ArrayList<ScoredParameters>()
    val poolSize = 8
    val computePool = ComputePool(8, designer)
    var nGood = 0
    var nReal = 0
    var iterations = 0


    fun maybeReportStatus() {
        // bump iteration count and print status
        iterations++
        val now = System.currentTimeMillis()
        if (now - lastReport > reportingInterval) {
            System.err.println(
                "Best=${bestScore}, max=${
                    currents.map { it.score }.max()
                }, count=${currents.size}, good=${nGood}, real=${nReal}, iteration=${iterations}")
            lastReport = now
            if (bestScore.constraintScore == 0.0) {
                monitor(bestScore, best, currents.map { it.parameters })
            }
        }
    }


    fun improve(initialDesignParameters: DesignParameters,
                fToL: Double = 1e-4,
                xToL: Double = 1e-6,
                initialAccuracy: Double = 0.001): DesignParameters {

        var result: DesignParameters = initialDesignParameters

        best = initialDesignParameters
        var constraintScore = constrainer(best)
        bestScore = if (constraintScore != 0.0) {
            Score(constraintScore, 0.0)
        } else {
            Score(0.0, scorer(best))
        }
        computePool.start()
        currents = arrayListOf(ScoredParameters(best, bestScore))
        val currentsMaxLen = (best.size * 5).toInt()

        while (!computePool.isDone() || computePool.hasAvailableResults()) {
            maybeReportStatus()
            var newDesignParameters: DesignParameters? = null
            var newScore: Score? = null

            // If we're not done, and there's available workers waiting, we can
            // add some new instruments to the pool.
            if (!computePool.isDone() && computePool.hasAvailableWorkers()) {
                // Generate a new mutant to consider
                newDesignParameters = DesignParameters.generateNewDesignParameters(
                    currents.map { it.parameters },
                    initialAccuracy, currents.size < currentsMaxLen
                )
                constraintScore = constrainer(newDesignParameters)
                if (constraintScore != 0.0) {
                    newScore = Score(constraintScore, 0.0)
                } else {
                    computePool.addTask(newDesignParameters)
                }
            }

            if (newScore == null) {
                if (!computePool.hasAvailableResults() || !computePool.isDone() && !computePool.hasAvailableWorkers()) {
                    continue
                } else {
                    val (completedTaskState, completedTaskScore) = computePool.getNextResult() ?: continue
                    newScore = completedTaskScore
                    newDesignParameters = completedTaskState
                }
            }

            if (newScore.constraintScore == 0.0) {
                nReal += 1
            }
            val l = currents.map { it.score }.sortedBy { it }
            val c = if (currentsMaxLen < l.size) {
                l[currentsMaxLen].intonationScore
            } else {
                1e30
            }
            val cutoff = Score(bestScore.constraintScore, c)
            if (newScore <= cutoff) {
                currents = ArrayList(currents.filter { it.score <= cutoff })
                currents.add(ScoredParameters(newDesignParameters!!, newScore))
                nGood += 1
                if (newScore <= bestScore) {
                    bestScore = newScore
                    best = newDesignParameters
                }
            }
            if (currents.size >= currentsMaxLen && bestScore.constraintScore == 0.0) {
                var xSpan = 0.0
                for (i in 0 until designer.initialDesignState().size) {
                    xSpan = max(
                        xSpan,
                        currents.map { it.parameters[i] }.max() -
                                currents.map { it.parameters[i] }.min()
                    )
                }
                var fSpan =
                    ArrayList(currents.map { it.score }).max().intonationScore - bestScore.intonationScore
                if (xSpan < xToL || (nGood >= 5000 && fSpan < fToL)) {
                    computePool.finish()
                }
            }
        }
        computePool.finish()
        computePool.joinAll()
        return best
    }
}
