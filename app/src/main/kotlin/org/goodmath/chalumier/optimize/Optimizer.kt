package org.goodmath.chalumier.optimize

import kotlinx.coroutines.*
import org.goodmath.chalumier.design.Instrument
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.DesignState
import java.util.Random
import kotlin.concurrent.thread
import kotlin.math.max


data class Score(val constraintScore: Double, val score: Double): Comparable<Score> {
    override operator fun compareTo(other: Score): Int {
        return if (constraintScore == 0.0 && other.constraintScore != 0.0) {
            -1
        } else {
            when (val c = constraintScore.compareTo(other.constraintScore)) {
                0 -> score.compareTo(other.score)
                else -> c
            }
        }
    }
}

class Optimizer(val designer: InstrumentDesigner) {
    val random = Random()


    fun  repr(x: Score): String {
        return if (x.constraintScore != 0.0) {
            "C%.6f(%.6f)".format(x.constraintScore, x.score)
        } else {
            "%.6f(C%.6f)".format(x.score, x.constraintScore)
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
            var lastT = 0L
            var best = initialDesignState
            var constraintScore = constrainer(best)
            var bestScore = if (constraintScore != 0.0) {
                Score(constraintScore, 0.0)
            } else {
                Score(0.0, scorer(best))
            }
            var nGood = 0
            var nReal = 0
            var i = 0
            val poolSize = 8
            val driver = Driver(poolSize, designer)
            driver.start()
            var done = false
            var currents = listOf(Pair(best, bestScore))
            val currentsMaxLen = (best.size * 5).toInt()

            while (!done || driver.hasAvailableResults()) {
                // bump iteration count and print status
                val now = System.currentTimeMillis()
                if (now - lastT > 2000) {
                    printStatus(bestScore, currents, nGood, nReal, i)
                    lastT = now
                    if (bestScore.constraintScore == 0.0) {
                        monitor(bestScore, best, currents.map { it.first })
                    }
                }
                i++
                var newDesignState: DesignState? = null
                var newScore: Score? = null

                // If we're not done, and there's available workers waiting, we can
                // add some new instruments to the pool.
                if (!done && driver.hasAvailableWorkers()) {
                    // Generate a new mutant to consider
                    newDesignState = DesignState.generateNewDesignState(
                            currents.map { it.first },
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
                    }
                }

                if (newScore.constraintScore == 0.0) {
                    nReal += 1
                }
                val l = currents.map { it.first[0] }.sortedBy { it }
                val c = if (currentsMaxLen < l.size) {
                    l[currentsMaxLen]
                } else {
                    1e30
                }
                val cutoff = Score(bestScore.constraintScore, c)
                if (newScore <= cutoff) {
                    currents = ArrayList(currents.filter { it.second <= cutoff })
                    currents.add(Pair(newDesignState!!, newScore))
                    nGood += 1
                    if (newScore < bestScore) {
                        bestScore = newScore
                        best = newDesignState
                    }
                }
                if (currents.size >= currentsMaxLen && bestScore.constraintScore == 0.0) {
                    var xSpan = 0.0
                    for (i in 0 until designer.initialDesignState.size) {
                        xSpan = max(
                            xSpan,
                            currents.map { it.first[i] }.max() -
                                    currents.map { it.first[i] }.min()
                        )
                    }
                    var fSpan =
                        ArrayList(currents.map { it.second }).max().score - bestScore.score
                    if (xSpan < xToL || (nGood >= 5000 && fSpan < fToL)) {
                        done = true
                    }
                }
                i += 1
            }
            driver.finish()

            result = best
        }
        th.join()

        return result
    }

    private fun printStatus(
        bestScore: Score,
        currents: List<Pair<DesignState, Score>>,
        nGood: Int,
        nReal: Int,
        i: Int
    ) {
        System.err.println(
            "Best=${repr(bestScore)}, max(currents)=${
                repr(currents.map { it.second }.max())
            }"
        )
        System.err.println("\tnumCurrents=${currents.size}, good=${nGood}, real=${nReal}, iteration=${i}")
    }

}
