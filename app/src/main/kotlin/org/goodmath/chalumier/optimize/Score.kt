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

import org.goodmath.chalumier.design.DesignParameters


data class Score(val constraintScore: Double, val intonationScore: Double): Comparable<Score> {
    override operator fun compareTo(other: Score): Int {
        return when (val c = constraintScore.compareTo(other.constraintScore)) {
            0 -> intonationScore.compareTo(other.intonationScore)
            else -> c
        }
    }

    override fun toString(): String {
        return if (constraintScore != 0.0) {
            "(C)%4f".format(constraintScore)
        } else {
            "(I)%.4f".format(intonationScore)
        }
    }
}

data class ScoredParameters(val parameters: DesignParameters,
                            val score: Score)
