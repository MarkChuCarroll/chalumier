/*
 * Copyright 2024 Mark C. Chu-Carroll and Paul Francis Harrison
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

object StringParameterKind: ParameterKind<String> {
    override val name: String = "String"
    override val isOptional = false


    override fun checkValue(v: Any?): Boolean {
        return when(v) {
            null, is JsonNull -> false
            is String -> true
            is JsonPrimitive -> v.isString
            else -> false
        }
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return v != null && v is String
    }

    override fun fromConfigValue(v: Any?): String {
        return v as String
    }

    override fun toConfigValue(t: String): String {
        return "\"$t\""
    }

    override fun fromJson(t: JsonElement): String? {
        return when (t) {
            JsonNull -> {
                null
            }
            is JsonPrimitive -> {
                t.content
            }

            else -> {
                throw error(t)
            }
        }
    }

    override fun dump(t: String?): JsonElement {
        return t?.let { JsonPrimitive(it) } ?: JsonNull
    }
}

fun<T: Configurable<T>> stringParameter(help: String="", gen: (T) -> String): ConfigParameter<T, String> {
    return ConfigParameter(StringParameterKind, help, gen=gen)
}


fun<T: Configurable<T>> optStringParameter(help: String="", gen: (T) -> String?): ConfigParameter<T,String?> {
    return ConfigParameter(opt(StringParameterKind), help, gen=gen)
}

