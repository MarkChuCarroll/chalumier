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

open class ListParameterKind<T>(val pk: ParameterKind<T>): ParameterKind<List<T>> {
    override val name: String = "List<${pk.name}>"
    override val isOptional = false

    override fun checkValue(v: Any?): Boolean {
        return when {
            v == null -> false
            v is List<*> && v.isNotEmpty() -> {
                val v0 = v[0]
                v0 != null && pk.checkValue(v0)
            }
            v is List<*> -> true
            else ->  false
        }
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return when(v) {
            null -> false
            is List<*> -> v.isEmpty() ||
                    v.all { pk.checkConfigValue(it) }
            else -> false
        }
    }

    override fun fromConfigValue(v: Any?): List<T> {
        if (v is List<*>) {
            if (v.isEmpty()) {
                return emptyList()
            }
            val contents = v.map { pk.fromConfigValue(it) }
            return ArrayList(contents)
        } else {
            throw ConfigurationParameterException("Invalid list parameter '$v'")
        }
    }

    override fun toConfigValue(t: List<T>): String {
        val elements = t.map { pk.toConfigValue(it) }
        var result = "[\n"
        var line = "      "
        var sep = ""
        elements.forEach { el ->
            if (line.length + sep.length + el.length  < 80) {
                line += "$sep$el"
            } else {
                line += "$sep\n"
                result += line
                line = "      $el"
            }
            sep = ", "
        }
        result += line + "\n   ]"
        return result
    }

    override fun fromJson(t: JsonElement): List<T>? {
        if (t ==  JsonNull) {
            return null
        }
        if (t !is JsonArray) {
            throw ConfigurationParameterException("Parameter of type $name cannot be read from invalid json '$t'")
        } else {
            return ArrayList(t.map { jsonElement ->
                pk.fromJson(jsonElement) ?: throw ConfigurationParameterException("Unexpected null reading config parameter")
            })
        }
    }
    override fun dump(t: List<T>?): JsonElement {
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

class ListOfOptParameterKind<T>(private val pk: ParameterKind<T?>): ParameterKind<List<T?>> {
    override val name: String = "List<${pk.name}>"
    override val isOptional = false

    override fun fromConfigValue(v: Any?): List<T?> {
        return if (v is List<*>) {
            v.map { pk.fromConfigValue(it) }
        } else {
            throw error(v)
        }
    }
    override fun checkValue(v: Any?): Boolean {
        return when(v) {
            null, is JsonNull -> true
            is List<*> -> v.isEmpty() || v.all { pk.checkValue(it) }
            else ->  false
        }
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return (v == null)  || v == "null" || (v is List<*> && v.all { pk.checkConfigValue(it) })
    }

    override fun toConfigValue(t: List<T?>): String {
        val elements = t.map { pk.toConfigValue(it) }.joinToString(", ")
        return "[$elements]"
    }

    override fun fromJson(t: JsonElement): List<T?>? {
        if (t ==  JsonNull) {
            return null
        }
        if (t !is JsonArray) {
            throw error(t)
        } else {
            return ArrayList(t.map { jsonElement ->
                if (jsonElement == JsonNull) {
                    null
                } else {
                    pk.fromJson(jsonElement)
                }
            }.toMutableList())
        }
    }

    override fun dump(t: List<T?>?): JsonElement {
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
