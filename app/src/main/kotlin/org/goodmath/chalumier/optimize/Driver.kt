package org.goodmath.chalumier.optimize

import kotlinx.coroutines.*
import org.goodmath.chalumier.design.Instrument
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.DesignState
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

/*
 * Ok, here's where things get hairy.
 *
 * @See docs/demakein-and-nesoni.md for my notes from reverse-engineering/spelunking
 *   nesoni and how demakein uses it for parallelism.
 */


data class WorkerResult(val designState: DesignState,
                        val score: Double)

class Driver<T: Instrument<T>>(val poolSize: Int, val instrumentDesigner: InstrumentDesigner<T>) {

    private var done: Boolean = false
    private val workerThreads = ArrayList<Thread>()
    private val tasks = ArrayDeque<DesignState>()
    private val results = ArrayDeque<WorkerResult>()
    fun isDone(): Boolean {
        return false
    }

    fun joinAll() {
        for (t in workerThreads) {
            t.join()
        }
    }

    class Worker<T: Instrument<T>>(val driver: Driver<T>): Runnable {
        fun process(designState: DesignState): Double {
            val inst = driver.instrumentDesigner.makeInstrumentFromState(designState)
            return driver.instrumentDesigner.score(inst)
        }

        override fun run() {
            while (!driver.isDone()) {
                val task =
                    synchronized(driver.tasks) {
                        if (driver.tasks.peek() != null) {
                            driver.tasks.removeFirst()
                        } else {
                            null
                        }
                    }
                if (task == null) {
                    Thread.sleep(1000)
                } else {
                    val result = process(task)
                    synchronized(driver.results) {
                        driver.results.add(WorkerResult(task, result))
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

    fun getNextResult(): WorkerResult? {
        synchronized(results) {
            if (results.size > 0) {
                return results.removeFirst()
            } else {
                return null
            }
        }
    }

    var taskCount = 0
    fun addTask(designState: DesignState) {
        taskCount++
        synchronized(tasks) {
            tasks.add(designState)
        }
    }

    fun finish() {
        done = true
    }
}
