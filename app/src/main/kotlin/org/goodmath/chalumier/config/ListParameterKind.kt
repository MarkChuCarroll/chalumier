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

class ListParameterKind<T>(val pk: ParameterKind<T>): ParameterKind<ArrayList<T>> {
    override val name: String = "List<${pk.name}>"

    override fun checkValue(v: Any?): Boolean {
        return when {
            v == null -> false
            v is MutableList<*> && v.isNotEmpty() -> {
                val v0 = v[0]
                v0 != null && pk.checkValue(v0)
            }
            v is MutableList<*> -> true
            else ->  false
        }
    }

    override fun fromConfigValue(v: Any?): ArrayList<T> {
        if (v is List<*>) {
            val contents = v.map { it -> pk.fromConfigValue(it) }
            return ArrayList(contents)
        } else {
            throw ConfigurationParameterException("Invalid list parameter ${v}")
        }
    }

    override fun load(t: JsonElement): ArrayList<T>? {
        if (t ==  JsonNull) {
            return null
        }
        if (t !is JsonArray) {
            throw ConfigurationParameterException("Parameter of type ${name} cannot be read from invalid json '${t}'")
        } else {
            return ArrayList(t.map { jsonElement ->
                pk.load(jsonElement) ?: throw ConfigurationParameterException("Unexpected null reading config parameter")
            })
        }
    }
    override fun dump(t: ArrayList<T>?): JsonElement {
        return if (t == null) {
            JsonNull
        } else {
            buildJsonArray {
                t.map { i ->
                    add(pk.dump(i))
                }
            }
        }
    }
}

class ListOfOptParameterKind<T>(val pk: ParameterKind<T?>): ParameterKind<ArrayList<T?>> {
    override val name: String = "List<${pk.name}>"

    override fun checkValue(v: Any?): Boolean {
        return when {
            v == null -> false
            v is ArrayList<*> && !v.isEmpty() -> {
                val v0 = v[0]
                v0 == null || pk.checkValue(v0)
            }
            v is ArrayList<*> && v.isEmpty() -> true
            else ->  false
        }
    }

    override fun load(t: JsonElement): ArrayList<T?>? {
        if (t ==  JsonNull) {
            return null
        }
        if (t !is JsonArray) {
            throw ConfigurationParameterException("Parameter of type ${name} cannot be read from invalid json '${t}'")
        } else {
            return ArrayList(t.map { jsonElement ->
                if (jsonElement == JsonNull) {
                    null
                } else {
                    pk.load(jsonElement)
                }
            }.toMutableList())
        }
    }

    override fun dump(t: ArrayList<T?>?): JsonElement {
        return if (t == null) {
            JsonNull
        } else {
            buildJsonArray {
                t.forEach { i ->
                    add(pk.dump(i))
                }
            }
        }
    }
}
