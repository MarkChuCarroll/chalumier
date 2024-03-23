package org.goodmath.chalumier.util

import java.util.*
import kotlin.collections.ArrayList

/**
 * A little utility that makes it easier to test the scorer and
 * parameter generator.
 */
interface Randomizer  {
    fun nextDouble(): Double
    fun nextInt(i:Int): Int
    fun nextGaussian(a: Double, b: Double): Double
}

/**
 * The normal Java randomizer.
 */
object StandardRandomizer: Randomizer {
    private val random = Random()
    override fun nextDouble() = random.nextDouble()
    override fun nextInt(i: Int) = random.nextInt(i)

    override fun nextGaussian(a: Double, b: Double): Double {
        return random.nextGaussian(a, b)
    }
}

/**
 * For testing, a randomizer that works from a pre-recorded list of values.
 */
class RecordedRandomizer(
    private val doubles: List<Double>,
    private val integers: List<Int>,
    private val gaussians: List<Double>, ): Randomizer {

    var id = 0
    var ii = 0
    var ig = 0
    override fun nextDouble(): Double {
        val result = doubles[id]
        id++
        return result
    }

    override fun nextInt(i: Int): Int {
        val result = integers[ii]
        ii++
        return result
    }

    override fun nextGaussian(a: Double, b: Double): Double {
        val result = gaussians[ig]
        ig++
        return result
    }

}

/**
 * The counterpart to the recorded randomizer, this one runs as
 * the system randomizer, but it records every random number
 * it generates, so that it can be used to populated a recorded
 * randomizer.
 */
class RecordingRandomizer: Randomizer {
    private val random = Random()
    val doubles = ArrayList<Double>()
    val integers = ArrayList<Int>()
    val gaussians = ArrayList<Double>()

    override fun nextDouble(): Double {
        return random.nextDouble().also { doubles.add(it) }
    }

    override fun nextInt(i: Int): Int {
        return random.nextInt().also { integers.add(it) }
    }

    override fun nextGaussian(a: Double, b: Double): Double {
        return random.nextGaussian().also { gaussians.add(it) }
    }

}
