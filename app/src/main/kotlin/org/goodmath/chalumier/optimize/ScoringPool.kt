package org.goodmath.chalumier.optimize

import kotlinx.coroutines.*
import org.goodmath.chalumier.design.Instrument
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.DesignParameters
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/*
 * Ok, here's where things get hairy.
 *
 * @See docs/demakein-and-nesoni.md for my notes from reverse-engineering/spelunking
 *   nesoni and how demakein uses it for parallelism.
 *
 * Or... Maybe not so hairy. Demakein made this look really hard,
 * when what it actually implemented was a pretty bog-standard worker
 * pool computation. I did it the simplest way that I could using
 * threading in Kotlin. It could probably be made even a bit simpler/cleaner
 * using coroutines with a threadpool.
 */


data class ScoreResult(val designParameters: DesignParameters,
                       val frequencyScore: Double)

class ScoringPool<T: Instrument<T>>(val poolSize: Int, val instrumentDesigner: InstrumentDesigner<T>) {
    private var done: Boolean = false
    private val workerThreads = ArrayList<Thread>()
    private val tasks = ArrayDeque<DesignParameters>()
    private val results = ArrayDeque<ScoreResult>()
    fun isDone(): Boolean {
        return false
    }

    fun joinAll() {
        for (t in workerThreads) {
            t.join()
        }
    }

    class Worker<T: Instrument<T>>(val pool: ScoringPool<T>): Runnable {
        fun process(designParameters: DesignParameters): Double {
            val inst = pool.instrumentDesigner.makeInstrumentFromParameters(designParameters)
            return pool.instrumentDesigner.score(inst)
        }

        override fun run() {
            while (!pool.isDone()) {
                val task =
                    synchronized(pool.tasks) {
                        if (pool.tasks.peek() != null) {
                            pool.tasks.removeFirst()
                        } else {
                            null
                        }
                    }
                if (task == null) {
                    Thread.sleep(1000)
                } else {
                    val result = process(task)
                    synchronized(pool.results) {
                        pool.results.add(ScoreResult(task, result))
                    }
                }
            }
        }
    }

    fun start() {
        for (i in 0 until poolSize) {
            val newThread = Thread(Worker(this))
            newThread.start()
            workerThreads.add(newThread)
            System.err.println("LAUNCHED THREAD")
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

    fun hasAvailableScores(): Boolean {
        return (results.size > 0)
    }

    fun getNextScore(): ScoreResult? {
        synchronized(results) {
            if (results.size > 0) {
                return results.removeFirst()
            } else {
                return null
            }
        }
    }

    var taskCount = 0
    fun addParameterSetToScore(designParameters: DesignParameters) {
        taskCount++
        synchronized(tasks) {
            tasks.add(designParameters)
        }
    }

    fun finish() {
        done = true
    }
}
