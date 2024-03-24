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

object BooleanParameterKind: ParameterKind<Boolean> {
    override val name: String = "Boolean"
    override val isOptional: Boolean = false

    override fun fromConfigValue(v: Any?): Boolean {
        return when {
            v is Boolean -> v
            v is String && v == "true" -> true
            v is String && v == "false" -> false
            else -> throw error(v)
        }
    }

    override fun toConfigValue(t: Boolean): String {
        return t.toString()
    }

    override fun checkValue(v: Any?) : Boolean {
        return when (v) {
            null -> false
            is JsonNull -> false
            is Boolean -> true
            is String -> v == "true" || v == "false"
            is JsonPrimitive -> v.booleanOrNull != null
            else -> false
        }
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return (v is Boolean) || (v is String &&
                (v == "true" || v == "false"))
    }

    override fun fromJson(t: JsonElement): Boolean? {
        return when (t) {
            JsonNull -> {
                null
            }
            is JsonPrimitive -> {
                t.boolean
            }

            else -> {
                throw error(t)
            }
        }
    }

    override fun dump(t: Boolean?): JsonElement {
        return if (t == null) {
            JsonNull
        } else {
            JsonPrimitive(t)
        }
    }
}

fun<T: Configurable<T>> BooleanParameter(help: String = "", gen: (T) -> Boolean): ConfigParameter<T, Boolean> {
    return ConfigParameter(BooleanParameterKind, help, gen=gen)
}

val ListOfBooleanParameterKind = ListParameterKind(BooleanParameterKind)

fun <T: Configurable<T>> ListOfBooleanParameter(help: String = "", gen: (T) -> List<Boolean>): ConfigParameter<T, List<Boolean>> {
    return ConfigParameter(ListOfBooleanParameterKind, help, gen=gen)
}
