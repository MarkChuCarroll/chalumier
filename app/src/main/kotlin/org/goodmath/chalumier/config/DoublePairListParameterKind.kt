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
package org.goodmath.chalumier.config

import org.goodmath.chalumier.errors.ConfigurationParameterException

object DoublePairParameterKind: PairParameterKind<Double, Double>(DoubleParameterKind, DoubleParameterKind) {
    override fun fromConfigValue(v: Any?): Pair<Double, Double> {
        when (v) {
            is Pair<*,*> -> {
                val first = v.first
                val second = v.second
                return if (first is Double && second is Double) {
                    Pair(first, second)
                } else if (first is String && second is String) {
                    Pair(first.toDouble(), second.toDouble())
                } else {
                    throw ConfigurationParameterException("Expected either a pair of strings or a pair of doubles, but found $v")
                }
            }

            is Double -> {
                return Pair(v, v)
            }

            is String -> {
                val d = v.toDouble()
                return Pair(d, d)
            }

            else -> {
                throw ConfigurationParameterException("Expected either a pair of strings or a pair of doubles, but found $v")
            }
        }
    }

    override fun checkValue(v: Any?): Boolean {
        return if (v is Pair<*, *>) {
            (v.first is Double && v.second is Double) ||
                    (v.first is String && v.second is String)
        } else {
            v is Double || v is String
        }
    }
}

val DoublePairListParameterKind: ParameterKind<List<Pair<Double, Double>>> = ListParameterKind(DoublePairParameterKind)

fun<T: Configurable<T>> ListOfDoublePairParameter(
    help: String = "", gen: (T) -> List<Pair<Double, Double>>): ConfigParameter<T, List<Pair<Double, Double>>> {
        return ConfigParameter(DoublePairListParameterKind, help, gen=gen)
}
