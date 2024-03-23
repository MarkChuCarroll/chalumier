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

import kotlinx.serialization.json.JsonObject
import org.goodmath.chalumier.errors.ConfigurationParameterException

object DoublePairParameterKind: PairParameterKind<Double, Double>(DoubleParameterKind, DoubleParameterKind) {
    override fun fromConfigValue(v: Any?): Pair<Double, Double> {
        return when (v) {
            is Pair<*,*> -> {
                val first = v.first
                val second = v.second
                if (first is Double && second is Double) {
                    Pair(first, second)
                } else if (first is String && second is String) {
                    Pair(first.toDouble(), second.toDouble())
                } else {
                    throw error(v)
                }
            }
            is Tuple -> {
                val first = v.body[0]
                val second = v.body[1]
                Pair(DoubleParameterKind.fromConfigValue(first),
                    DoubleParameterKind.fromConfigValue(second))
            }

            is Double -> {
                Pair(v, v)
            }

            is String -> {
                val d = v.toDouble()
                Pair(d, d)
            }

            else -> {
                throw error(v)
            }
        }
    }

    override fun checkValue(v: Any?): Boolean {
        return when(v) {
            is Pair<*, *> ->
                (v.first is Double && v.second is Double) ||
                        (v.first is String && v.second is String)
            is JsonObject ->
                listOf("first", "second").all {
                    v.containsKey(it) && DoubleParameterKind.checkValue(v[it])
                }
            is Double, is String -> true
            else -> false
        }
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return when(v) {
            is Map<*, *> -> v.containsKey("first") && v.containsKey("second") &&
                    DoubleParameterKind.checkConfigValue(v["first"]) &&
                    DoubleParameterKind.checkConfigValue(v["second"])
            is Tuple -> v.name == "Pair" && v.body.size == 2 &&
                    DoubleParameterKind.checkConfigValue(v.body[0]) &&
                    DoubleParameterKind.checkConfigValue(v.body[1])
            is Double -> true
            is String -> true
            else -> false
        }
    }

    override val isOptional: Boolean = false


    override val sampleValueString: String = "(Pair: "

}

val DoublePairListParameterKind: ParameterKind<List<Pair<Double, Double>>> = ListParameterKind(DoublePairParameterKind)

fun<T: Configurable<T>> ListOfDoublePairParameter(
    help: String = "", gen: (T) -> List<Pair<Double, Double>>): ConfigParameter<T, List<Pair<Double, Double>>> {
        return ConfigParameter(DoublePairListParameterKind, help, gen=gen)
}
