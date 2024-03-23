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

open class PairParameterKind<T, U>(
    private val tKind: ParameterKind<T>,
    private val uKind: ParameterKind<U>): ParameterKind<Pair<T, U>> {
    override val name: String = "Pair<${tKind.name}, ${uKind.name}>"
    override val sampleValueString: String = "(Pair: ${tKind.sampleValueString}, ${uKind.sampleValueString})"
    override val isOptional = false


    override fun fromConfigValue(v: Any?): Pair<T, U> {
        return when(v) {
            is List<*> -> {
                val first = tKind.fromConfigValue(v[0])
                val second = uKind.fromConfigValue(v[1])
                Pair(first, second)
            }
            is Map<*, *> -> {
                val first = tKind.fromConfigValue(v["first"])
                val second = uKind.fromConfigValue(v["second"])
                Pair(first, second)
            }
            is Tuple -> {
                val first = tKind.fromConfigValue(v.body[0])
                val second = uKind.fromConfigValue(v.body[1])
                Pair(first, second)
            }
            else ->
                throw error(v)
        }
    }

    override fun checkValue(v: Any?): Boolean {
        return when(v) {
            null, is JsonNull -> false
            is Pair<*, *> ->
                    tKind.checkValue(v.first) &&
                    uKind.checkValue(v.second)
            else -> false
        }
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return when(v) {
            null -> false
            is Map<*, *> ->
                tKind.checkValue(v["first"]) && uKind.checkValue(v["second"])
            is List<*> ->
                v.size == 2 && tKind.checkValue(v[0]) && uKind.checkValue(v[1])
            is Tuple ->
                v.name == "Pair" &&
                        v.body.size == 2 &&
                        tKind.checkValue(v.body[0]) &&
                        uKind.checkValue(v.body[1])
            else ->  false
        }
    }

    override fun fromJson(t: JsonElement): Pair<T, U>? {
        if (t == JsonNull) { return null }
        return if (t is JsonObject) {
            val first = tKind.fromJson(t["first"]!!)
                ?: throw error(t)
            val second = uKind.fromJson(t["second"]!!)
                ?: throw error(t)
            Pair(first, second)
        } else {
            throw error(t)
        }
    }

    override fun dump(t: Pair<T, U>?): JsonElement {
        if (t == null) { return JsonNull }
        return buildJsonObject {
            put("first", tKind.dump(t.first))
            put("second", uKind.dump(t.second))
        }
    }

}
