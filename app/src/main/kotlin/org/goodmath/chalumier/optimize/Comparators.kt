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
import kotlin.math.min


/*
 * In trying to match semantics with demakein's scoring system, we need
 * to be able to do order comparisons on things that are not normally
 * ordered in Kotlin. They're all cases of sequences of some type,
 * where (a_0, ... a_n) is less than (b_0, ..., b_n) if a_0 < b_0,
 * or a_0 == b_0 and a_1 < b_1, ...
 */

object DoublePairComparator: Comparator<Pair<Double, Double>> {
    override fun compare(o1: Pair<Double, Double>?, o2: Pair<Double, Double>?): Int {
        if (o1 == null) {
            if (o2 == null) {
                return 0
            } else {
                return -1
            }
        }
        if (o2 == null) {
            return 1
        }

        return when {
            o1.first < o2.first -> -1
            o1.first > o2.first -> 1
            o1.second < o2.second -> -1
            o1.second > o2.second -> 1
            else -> 0
        }
    }
}

object DoubleListComparator: Comparator<ArrayList<Double>> {
    override fun compare(one: ArrayList<Double>?, other: java.util.ArrayList<Double>?): Int {
        if (one == null) {
            if (other == null) {
                return 0
            } else {
                return -1
            }
        }
        if (other == null) {
            return 1
        }
        val minLen = min(one.size, other.size)
        for (i in 0 until minLen) {
            if (one[i] < other[i]) {
                return -1
            } else if (one[i] > other[i]) {
                return 1
            }
        }
        // All the elements up to the minlength are equal.
        if (one.size < other.size) {
            return -1
        } else if (one.size > other.size) {
            return 1
        } else {
            return 0
        }
    }

}
operator fun  ArrayList<Double>.compareTo(other: ArrayList<Double>): Int {
    val minLen = min(size, other.size)
    for (i in 0 until minLen) {
        if (this[i] < other[i]) {
            return -1
        } else if (this[i] > other[i]) {
            return 1
        }
    }
    // All the elements up to the minlength are equal.
    if (size < other.size) {
        return -1
    } else if (size > other.size) {
        return 1
    } else {
        return 0
    }
}


operator fun  Pair<Double, Double>.compareTo(other: Pair<Double, Double>): Int {
    return DoublePairComparator.compare(this, other)
}


