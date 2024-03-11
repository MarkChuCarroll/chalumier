package org.goodmath.chalumier.optimize

import kotlinx.coroutines.*
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.DesignParameters
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/*
 * Ok, here's where things get hairy.
 *
 * @See docs/demakein-and-nesoni.md for my notes from reverse-engineering/spelunking
 *   nesoni and how demakein uses it for parallelism.
 */


class ComputePool(
    private val poolSize: Int,
    val instrumentDesigner: InstrumentDesigner) {

    private var done: Boolean = false
    private val workerThreads = ArrayList<Thread>()
    private val tasks = ArrayDeque<Pair<CompletableFuture<ScoredParameters>,DesignParameters>>()
    private val results = ArrayDeque<Future<ScoredParameters>>()
    fun isDone(): Boolean {
        return done
    }

    fun joinAll() {
        for (t in workerThreads) {
            t.join()
        }
    }

    fun start() {
        for (i in 0 until poolSize) {
            val newThread = Thread(ParameterScorer(this))
            newThread.start()
            workerThreads.add(newThread)
        }
        val monitor = thread {
            while (!this.isDone()) {
                Thread.sleep(5000)
                System.err.println("Tasks = ${tasks.size}, results = ${results.size}, total = ${taskCount}")
            }
        }
    }
    fun hasAvailableWorkers(): Boolean {
        return  tasks.size < poolSize
    }

    fun hasAvailableResults(): Boolean {
        return (results.size > 0)
    }

    fun getNextResult(): ScoredParameters? {
        synchronized(results) {
            if (results.size > 0) {
                val r = results.removeFirst().get()
                val p = results.peek()
                if (p != null) {
                    System.err.println("Results ${results.peek().get()}")
                }
                return r
            } else {
                return null
            }
        }
    }

    var taskCount = 0
    fun addTask(designParameters: DesignParameters) {
        taskCount++
        synchronized(tasks) {
            val f = CompletableFuture<ScoredParameters>()
            tasks.add(Pair(f, designParameters))
            results.add(f)
        }
    }

    fun finish() {
        done = true
    }

    class ParameterScorer(val computePool: ComputePool): Runnable {
        fun process(designParameters: DesignParameters): ScoredParameters {
            val inst = computePool.instrumentDesigner.makeInstrumentFromState(designParameters)
            return ScoredParameters(designParameters,
                Score(computePool.instrumentDesigner.constraintScore(inst),
                    computePool.instrumentDesigner.score(inst)))
        }

        override fun run() {
            while (!computePool.isDone()) {
                val task =
                    synchronized(computePool.tasks) {
                        if (computePool.tasks.peek() != null) {
                            computePool.tasks.removeFirst()
                        } else {
                            null
                        }
                    }
                if (task == null) {
                    Thread.sleep(1000)
                } else {
                    val result = process(task.second)
                    task.first.complete(result)
                }
            }
        }

    }
}
