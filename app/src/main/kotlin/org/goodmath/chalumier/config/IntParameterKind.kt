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
import kotlin.random.Random

object IntParameterKind: ParameterKind<Int> {
    override val name = "Int"
    override val isOptional = false

    override fun checkValue(v: Any?): Boolean {
        return v != null && (v is Int || (v is Double && v.toInt().toDouble() == v))
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return v is Double || v is Int || (v is String &&
                v.toDoubleOrNull() != null)
    }

    override fun fromConfigValue(v: Any?): Int {
        return when (v) {
            is Int -> {
                v
            }

            is Double -> {
                return v.toInt()
            }

            else -> {
                throw error(v)
            }
        }
    }

    override fun toConfigValue(t: Int): String {
        return t.toString()
    }

    override fun fromJson(t: JsonElement): Int? {
        return when (t) {
            JsonNull -> {
                null
            }
            is JsonPrimitive -> {
                t.double.toInt()
            }

            else -> {
                throw error(t)
            }
        }
    }

    override fun dump(t: Int?): JsonElement {
        return t?.let { JsonPrimitive(it) } ?: JsonNull
    }
}


fun<T: Configurable<T>> IntParameter(help: String="", gen: (T) -> Int): ConfigParameter<T,Int> {
    return ConfigParameter(IntParameterKind, help, gen=gen)
}


fun<T: Configurable<T>> OptIntParameter(help: String="", gen: (T) -> Int?): ConfigParameter<T,Int?> {
    return ConfigParameter(opt(IntParameterKind), help, gen=gen)
}
