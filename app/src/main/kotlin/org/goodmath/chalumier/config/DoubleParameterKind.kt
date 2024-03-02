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

    override fun checkValue(v: Any?): Boolean {
        return v != null && v is Double
    }

    override fun load(t: JsonElement): Double? {
        return if (t == JsonNull) {
            null
        } else if (t is JsonPrimitive) {
            return t.double
        } else {
            throw ConfigurationParameterException("Parameter expected a double, but found ${t}")
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
    ConfigParameter<T, Double>(DoubleParameterKind, help, gen)

fun<T: Configurable<T>> OptDoubleParameter(help: String = "", gen: (T) -> Double?): ConfigParameter<T, Double?> =
    ConfigParameter<T, Double?>(OptDoubleParameterKind, help, gen)

fun<T: Configurable<T>> ListOfDoubleParameter(help: String = "", gen: (T) -> List<Double>): ConfigParameter<T, ArrayList<Double>> {
    val genMutable = { target: T -> ArrayList(gen(target)) }
    return ConfigParameter<T, ArrayList<Double>>(ListOfDoubleParameterKind, help, genMutable)
}

fun<T: Configurable<T>> ListOfOptDoubleParameter(help: String = "", gen: (T) -> List<Double?>): ConfigParameter<T, ArrayList<Double?>> {
    val genMutable = { target: T -> ArrayList(gen(target)) }
    return ConfigParameter<T, ArrayList<Double?>>(ListOfOptDoubleParameterKind, help, genMutable)
}
