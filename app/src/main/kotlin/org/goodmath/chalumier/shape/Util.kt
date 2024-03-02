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
package org.goodmath.chalumier.shape

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

class Limits(
    var xMin: Double, var xMax: Double, var yMin: Double, var yMax: Double, val zMin: Double, val zMax: Double
)

class Limits2(val xMin: Double, xMax: Double, yMin: Double, yMax: Double)


fun noise(): Double {
    return Random.nextDouble() - 0.5 * 1e-4
}
