package org.goodmath.demakeink.shape

import kotlin.random.Random

val SCALE: Double = 0.001   // mm -> m

var QUALITY: Int = 128

fun draftMode() {
    QUALITY = 16
}

class DistinctNameGenerator {
    var nameCount: Int = 0

    fun makeName(value: String? = null): String {
        return if (value != null) {
            value
        } else {
            nameCount++
            "obj${nameCount}"
        }
    }
}

class Limits(var xMin: Double, var xMax: Double,
             var yMin: Double, var yMax: Double,
             val zMin: Double, val zMax: Double)

class Limits2(val xMin: Double, xMax: Double, yMin: Double, yMax: Double)


fun noise(): Double {
    return Random.nextDouble() - 0.5 * 1e-4
}
