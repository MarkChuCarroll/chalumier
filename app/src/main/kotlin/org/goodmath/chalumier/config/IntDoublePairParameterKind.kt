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

object IntDoublePairParameterKind: ParameterKind<Pair<Int, Double>> {
    override val name: String = "Pair<Int, Double>"

    override fun checkValue(v: Any?): Boolean {
        return v != null &&  (v is Pair<*, *> && v.first is Int && v.second is Double)
    }

    override fun load(t: JsonElement): Pair<Int, Double>? {
        if (t == JsonNull) { return null }
        return if (t is JsonObject) {
            Pair(IntParameterKind.load(t["first"]!!)!!,
                DoubleParameterKind.load(t["second"]!!)!!)
        } else {
            throw ConfigurationParameterException("Parameter of type ${name} expected a JSON object, but found ${t}")
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

fun<T: Configurable<T>> ListOfListOfIntDoublePairParam(help: String = "", gen: (T) -> List<List<Pair<Int, Double>>>): ConfigParameter<T, ArrayList<ArrayList<Pair<Int, Double>>>> {
    val mutGen: (T) -> ArrayList<ArrayList<Pair<Int, Double>>> = { target ->
        ArrayList(gen(target).map { ArrayList(it) })
    }
    return ConfigParameter(ListOfListOfIntDoublePairKind, help, mutGen)
}
