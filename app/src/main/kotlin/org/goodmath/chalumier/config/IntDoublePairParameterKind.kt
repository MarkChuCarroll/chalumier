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

fun JsonObject.fieldSatisfiesPredicate(name: String, pred: (JsonElement?) -> Boolean): Boolean {
    return this.containsKey(name) && this[name]?.let { pred(it) }?: false
}

object IntDoublePairParameterKind: ParameterKind<Pair<Int, Double>> {
    override val name: String = "Pair<Int, Double>"
    override val isOptional: Boolean = false
    override val sampleValueString: String = "(Pair: 3, 27.1)"


    override fun checkValue(v: Any?): Boolean {
        return when(v) {
            null, is JsonNull -> false
            is Pair<*, *> -> v.first is Int && v.second is Double
            is JsonObject -> v.fieldSatisfiesPredicate("first") { f ->
                f is JsonPrimitive &&
                        f.intOrNull != null
            } &&
                    v.fieldSatisfiesPredicate("second") { f -> f is JsonPrimitive && f.doubleOrNull != null }

            is JsonArray ->
                v.run {
                    if (size == 2) {
                        val first = v[0]
                        val second = v[1]
                        first is JsonPrimitive && first.doubleOrNull != null &&
                                second is JsonPrimitive && second.doubleOrNull != null
                    } else {
                        false
                    }
                }

            else -> false
        }
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return when(v) {
            null ->  false
            is Map<*, *> ->
                v.containsKey("first") && IntParameterKind.checkConfigValue(v["first"]) &&
                        v.containsKey("second") && DoubleParameterKind.checkConfigValue(v["second"])
            is List<*> ->
                v.size == 2 && IntParameterKind.checkConfigValue(v[0]) &&
                        DoubleParameterKind.checkConfigValue(v[1])
            else ->  false
        }
    }

    override fun fromJson(t: JsonElement): Pair<Int, Double>? {
        if (t == JsonNull) { return null }
        return if (t is JsonObject) {
            Pair(IntParameterKind.fromJson(t["first"]!!)!!,
                DoubleParameterKind.fromJson(t["second"]!!)!!)
        } else {
            throw error(t)
        }
    }

    override fun dump(t: Pair<Int, Double>?): JsonElement {
        if (t == null) { return JsonNull }
        return buildJsonObject {
            put("first", t.first.toString())
            put("second", t.second.toString())
        }
    }
}

val ListOfIntDoublePairKind = ListParameterKind(IntDoublePairParameterKind)
val ListOfListOfIntDoublePairKind = ListParameterKind(ListOfIntDoublePairKind)

fun<T: Configurable<T>> ListOfListOfIntDoublePairParam(help: String = "", gen: (T) -> List<List<Pair<Int, Double>>>): ConfigParameter<T, List<List<Pair<Int, Double>>>> {
    val mutGen: (T) -> List<List<Pair<Int, Double>>> = { target ->
        ArrayList(gen(target).map { ArrayList(it) })
    }
    return ConfigParameter(ListOfListOfIntDoublePairKind, help, gen=mutGen)
}
