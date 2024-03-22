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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import org.goodmath.chalumier.errors.ConfigurationParameterException

object DoubleParameterKind: ParameterKind<Double> {
    override val name: String = "Double"

    override fun fromConfigValue(v: Any?): Double {
        return when (v) {
            is String -> {
                v.toDouble()
            }

            is Double -> {
                v
            }

            else -> {
                throw ConfigurationParameterException("Expected a number value, but found '$v'")
            }
        }
    }

    override fun checkValue(v: Any?): Boolean {
        return v != null && v is Double
    }

    override fun load(t: JsonElement): Double? {
        return when (t) {
            JsonNull -> {
                null
            }
            is JsonPrimitive -> {
                return t.double
            }

            else -> {
                throw ConfigurationParameterException("Parameter expected a double, but found $t")
            }
        }
    }

    override fun dump(t: Double?): JsonElement {
        return if (t == null) {
            JsonNull
        } else {
            JsonPrimitive(t)
        }
    }
}

val OptDoubleParameterKind = opt(DoubleParameterKind)
val ListOfDoubleParameterKind = ListParameterKind(DoubleParameterKind)
val ListOfOptDoubleParameterKind = ListOfOptParameterKind(OptDoubleParameterKind)

fun<T: Configurable<T>> DoubleParameter(help: String = "", gen: (T) -> Double): ConfigParameter<T, Double> =
    ConfigParameter(DoubleParameterKind, help, gen=gen)

fun<T: Configurable<T>> OptDoubleParameter(help: String = "", gen: (T) -> Double?): ConfigParameter<T, Double?> =
    ConfigParameter(OptDoubleParameterKind, help, gen=gen)

fun<T: Configurable<T>> ListOfDoubleParameter(help: String = "", gen: (T) -> List<Double>): ConfigParameter<T, List<Double>> {
    val genMutable = { target: T -> ArrayList(gen(target)) }
    return ConfigParameter(ListOfDoubleParameterKind, help, gen=genMutable)
}

fun<T: Configurable<T>> ListOfOptDoubleParameter(help: String = "", gen: (T) -> List<Double?>): ConfigParameter<T, List<Double?>> {
    val genMutable = { target: T -> ArrayList(gen(target)) }
    return ConfigParameter(ListOfOptDoubleParameterKind, help, gen=genMutable)
}

