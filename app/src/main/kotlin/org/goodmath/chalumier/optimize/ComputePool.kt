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
import org.goodmath.chalumier.design.DesignParameters
import java.util.*
import java.util.concurrent.*
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
    private val constraintScorer: (DesignParameters) -> Double,
    private val intonationScorer: (DesignParameters) -> Double) {
    var active = 0


    private var done: Boolean = false
    private val workerThreads = ArrayList<Thread>()
    private val tasks = ArrayDeque<Pair<CompletableFuture<ScoredParameters>,DesignParameters>>()
    private val results = ArrayDeque<Future<ScoredParameters>>()
    private val threadQueue: BlockingQueue<Runnable> = ArrayBlockingQueue<Runnable>(poolSize)
    private var totalSubmittedTasks = 0
    private var exec = ThreadPoolExecutor(poolSize, poolSize*2, 2000L, TimeUnit.MILLISECONDS, threadQueue)


    fun isDone(): Boolean {
        return done
    }


    fun start() {
        exec = ThreadPoolExecutor(poolSize, poolSize*2, 2000L, TimeUnit.MILLISECONDS, threadQueue)
        for (i in 0 until poolSize) {
            val worker = ParameterScorer(this, i)
            exec.execute(worker)
        }
    }

    fun workerStatus(): Int {
        return active
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
                val result = results.removeFirst().get()
                active--
                return result
            } else {
                return null
            }
        }
    }

    fun addTask(designParameters: DesignParameters) {
        synchronized(tasks) {
            active++
            val f = CompletableFuture<ScoredParameters>()
            tasks.add(Pair(f, designParameters))
            results.add(f)
        }
    }

    fun finish() {
        done = true
        exec.shutdown()
        exec.awaitTermination(1000, TimeUnit.MILLISECONDS)
        results.clear()
    }

    class ParameterScorer(val computePool: ComputePool, val idx: Int): Runnable {
        fun process(designParameters: DesignParameters): ScoredParameters {
            return ScoredParameters(designParameters,
                Score(computePool.constraintScorer(designParameters),
                    computePool.intonationScorer(designParameters)))
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
