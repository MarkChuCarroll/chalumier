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

import kotlinx.serialization.json.*
import org.goodmath.chalumier.errors.ConfigurationParameterException

object DoublePairListParameterKind: ParameterKind<List<Pair<Double, Double>>> {
    override val name: String = "List<Pair<Double, Double>>"

    override fun checkValue(v: Any?): Boolean {
        return when {
            v == null -> false
            v is List<*> && !v.isEmpty() -> {
                val v0 = v[0]
                v0 != null && v0 is Pair<*, *>  && v0.first is Double
            }
            v is List<*> && v.isEmpty() -> true
            else ->  false
        }
    }

    override fun load(t: JsonElement): List<Pair<Double, Double>>? {
        if (t ==  JsonNull) {
            return null
        }
        if (t !is JsonArray) {
            throw ConfigurationParameterException("Parameter of type ${name} cannot be read from invalid json '${t}'")
        } else {
            val tArray = t as JsonArray
            return tArray.map { jsonElement ->
                if (jsonElement !is JsonObject) {
                    throw ConfigurationParameterException("Parameter elements of a ${name} cannot be read from invalid json${jsonElement}")
                } else {
                    Pair(
                        DoubleParameterKind.load(jsonElement["first"]!!)!!,
                        DoubleParameterKind.load(jsonElement["second"]!!)!!
                    )
                }
            }
        }
    }

    override fun dump(t: List<Pair<Double, Double>>?): JsonElement {
        return if (t == null) {
            JsonNull
        } else {
            buildJsonArray {
                t.map { pair ->
                    addJsonObject {
                        put("first", pair.first.toString())
                        put("second", pair.second.toString())
                    }
                }
            }
        }
    }
}

fun<T: Configurable<T>> ListOfDoublePairParameter(
    help: String = "", gen: (T) -> List<Pair<Double, Double>>): ConfigParameter<T, List<Pair<Double, Double>>> {
        return ConfigParameter<T, List<Pair<Double, Double>>>(DoublePairListParameterKind, help, gen=gen)
}
