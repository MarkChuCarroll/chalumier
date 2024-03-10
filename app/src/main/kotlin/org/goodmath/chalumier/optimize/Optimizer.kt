package org.goodmath.chalumier.optimize

import kotlinx.coroutines.*
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.DesignState
import java.util.Random
import kotlin.concurrent.thread
import kotlin.math.max


data class Score(val constraintScore: Double, val score: Double): Comparable<Score> {
    override operator fun compareTo(other: Score): Int {
        return when (val c = constraintScore.compareTo(other.constraintScore)) {
            0 -> score.compareTo(other.score)
            else -> c
        }
    }

    override fun toString(): String {
        return "C%.4f/F%.4f".format(constraintScore, score)
    }
}

data class ScoredParameters(val parameters: DesignState,
    val score: Score)

class Optimizer(val designer: InstrumentDesigner) {
    val random = Random()


    fun  repr(x: Score): String {
        return if (x.constraintScore != 0.0) {
            "C%.6f(%.6f)".format(x.constraintScore, x.score)
        } else {
            "%.6f(C%.6f)".format(x.score, x.constraintScore)
        }
    }

    var lastReport: Long = System.currentTimeMillis()
    lateinit var best: DesignState
    lateinit var bestScore: Score
    var currents = ArrayList<ScoredParameters>()
    val poolSize = 8
    val driver = Driver(8, designer)
    var nGood = 0
    var nReal = 0
    var iterations = 0
    var ct = 0


    fun maybeReportStatus() {
        // bump iteration count and print status
        iterations++
        val now = System.currentTimeMillis()
        if (now - lastReport > 5000) {
            printStatus(bestScore, currents, nGood, nReal, iterations)
            lastReport = now
            //if (bestScore.constraintScore == 0.0) {
            //monitor(bestScore, best, currents.map { it.first })
            //}
        }
    }



    fun improve(comment: String,
                designer: InstrumentDesigner,
                constrainer: (DesignState) -> Double,
                scorer: (DesignState) -> Double,
                initialDesignState: DesignState,
                fToL: Double = 1e-4,
                xToL: Double = 1e-6,
                initialAccuracy: Double = 0.001,
                monitor: (Score,
                          DesignState,
                          List<DesignState>) -> Unit = { _, _, _ -> Unit }): DesignState {

        var result: DesignState = initialDesignState

        val th = thread {
            System.err.println("Runner started")
            best = initialDesignState
            var constraintScore = constrainer(best)
            bestScore = if (constraintScore != 0.0) {
                Score(constraintScore, 0.0)
            } else {
                Score(0.0, scorer(best))
            }
            driver.start()
            var done = false
            currents = arrayListOf(ScoredParameters(best, bestScore))
            val currentsMaxLen = (best.size * 5).toInt()
            System.err.println("Max len = ${currentsMaxLen}")

            while (!done || driver.hasAvailableResults()) {
                maybeReportStatus()
                var newDesignState: DesignState? = null
                var newScore: Score? = null

                // If we're not done, and there's available workers waiting, we can
                // add some new instruments to the pool.
                if (!done && driver.hasAvailableWorkers()) {
                    // Generate a new mutant to consider
                    newDesignState = DesignState.generateNewDesignState(
                            currents.map { it.parameters },
                            initialAccuracy, currents.size < currentsMaxLen)
                    constraintScore = constrainer(newDesignState)
                    if (constraintScore != 0.0) {
                        newScore = Score(constraintScore, 0.0)
                    } else {
                        driver.addTask(newDesignState)
                    }
                }

                if (newScore == null) {
                    if (!driver.hasAvailableResults() || !done && !driver.hasAvailableWorkers()) {
                        continue
                    } else {
                        val (completedTaskState, completedTaskScore) = driver.getNextResult() ?: continue
                        newScore = Score(0.0, completedTaskScore)
                        newDesignState = completedTaskState
                        System.err.println("Retrieved task, score=${newScore}")
                    }
                }

                if (newScore.constraintScore == 0.0) {
                    nReal += 1
                }
                val l = currents.map { it.score }.sortedBy { it }
                val c = if (currentsMaxLen < l.size) {
                    l[currentsMaxLen].score
                } else {
                    1e30
                }
                ct++
                if (ct % 100000 == 0) {
                    System.err.println("SC($ct): ${newScore.constraintScore}, C=${currents.map{it.score}}")
                    System.err.println("NewState = ${newDesignState}")
                }
                val cutoff = Score(bestScore.constraintScore, c)
                System.err.println("Cutoff = ${cutoff}, new = ${newScore}")
                if (newScore <= cutoff) {
                    System.err.println("Filtering ${currents.size} for <$cutoff")
                    currents = ArrayList(currents.filter { it.score <= cutoff })
                    val uniques = HashSet(currents.map { it.score.constraintScore + it.score.score })
                    System.err.println("Currents.size = ${currents.size}, unique = ${uniques.size}")
                    currents.add(ScoredParameters(newDesignState!!, newScore))
                    System.err.println("Added ${newScore} to currents, len(currents)=${currents.size}")

                    nGood += 1
                    if (newScore <= bestScore) {
                        bestScore = newScore
                        best = newDesignState
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
                        ArrayList(currents.map { it.score }).max().score - bestScore.score
                    if (xSpan < xToL || (nGood >= 5000 && fSpan < fToL)) {
                        done = true
                    }
                }
                iterations += 1
            }
            driver.finish()

            result = best
        }
        th.join()

        return result
    }

    private fun printStatus(
        bestScore: Score,
        currents: List<ScoredParameters>,
        nGood: Int,
        nReal: Int,
        i: Int
    ) {
        System.err.println(
            "Best=${repr(bestScore)}, max(currents)=${
                repr(currents.map { it.score }.max())
            }"
        )
        System.err.println("\tnumCurrents=${currents.size}, good=${nGood}, real=${nReal}, iteration=${i}")
    }

}
