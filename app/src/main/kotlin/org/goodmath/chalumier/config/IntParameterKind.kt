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
import kotlinx.serialization.json.int
import org.goodmath.chalumier.errors.ConfigurationParameterException

object IntParameterKind: ParameterKind<Int> {
    override val name: String = "Int"

    override fun checkValue(v: Any?): Boolean {
        return v != null && v is Int
    }

    override fun load(t: JsonElement): Int? {
        return if (t == JsonNull) {
            null
        } else if (t is JsonPrimitive) {
            t.int
        } else {
            throw ConfigurationParameterException("Parameter expected an int, but found ${t}")
        }
    }

    override fun dump(t: Int?): JsonElement {
        return t?.let { JsonPrimitive(it) } ?: JsonNull
    }
}


val OptIntParameterKind = opt(IntParameterKind)

fun<T: Configurable<T>> IntParameter(help: String="", gen: (T) -> Int): ConfigParameter<T,Int> {
    return ConfigParameter<T, Int>(IntParameterKind, help, gen=gen)
}


fun<T: Configurable<T>> OptIntParameter(help: String="", gen: (T) -> Int?): ConfigParameter<T,Int?> {
    return ConfigParameter<T, Int?>(opt(IntParameterKind), help, gen=gen)
}
