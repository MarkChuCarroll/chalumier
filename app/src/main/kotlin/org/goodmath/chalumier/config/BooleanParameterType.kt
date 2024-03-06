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
import kotlinx.serialization.json.boolean
import org.goodmath.chalumier.errors.ConfigurationParameterException

object BooleanParameterKind: ParameterKind<Boolean> {
    override val name: String = "Boolean"


    override fun checkValue(v: Any?): Boolean {
        return v != null && v is Boolean
    }

    override fun load(t: JsonElement): Boolean? {
        return if (t == JsonNull) {
            null
        } else if (t is JsonPrimitive) {
            t.boolean
        } else {
            throw ConfigurationParameterException("Expected a boolean, found ${t}")
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

val OptBooleanParameterKind: ParameterKind<Boolean?> = opt(BooleanParameterKind)

fun<T: Configurable<T>> BooleanParameter(help: String = "", gen: (T) -> Boolean): ConfigParameter<T, Boolean> {
    return ConfigParameter(BooleanParameterKind, help, gen=gen)
}

fun<T: Configurable<T>> OptBooleanParameter(help: String = "", gen: (T) -> Boolean?): ConfigParameter<T, Boolean?> {
    return ConfigParameter(OptBooleanParameterKind, help, gen=gen)
}
