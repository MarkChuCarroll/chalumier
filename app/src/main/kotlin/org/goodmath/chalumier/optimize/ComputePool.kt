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

import kotlinx.coroutines.*
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.DesignParameters
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.collections.ArrayList

/*
 * Ok, here's where things get hairy.
 *
 * @See docs/demakein-and-nesoni.md for my notes from reverse-engineering/spelunking
 *   nesoni and how demakein uses it for parallelism.
 *
 * Actually, it turns out to not be so hairy - in fact, this was
 * one of the easiest parts of demakein to rewrite. Demakein's whole
 * parallel computation was basically a rather cryptically written
 * futures-based work queue.
 */
class ComputePool(
    private val poolSize: Int,
    val instrumentDesigner: InstrumentDesigner) {

    private var done: Boolean = false
    private val workerThreads = ArrayList<Thread>()
    private val tasks = ArrayDeque<Pair<CompletableFuture<ScoredParameters>,DesignParameters>>()
    private val results = ArrayDeque<Future<ScoredParameters>>()
    private var totalSubmittedTasks = 0

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

    fun addTask(designParameters: DesignParameters) {
        totalSubmittedTasks++
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
            val inst = computePool.instrumentDesigner.makeInstrumentFromParameters(designParameters)
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
