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

class PairParameterKind<T, U>(
    val tKind: ParameterKind<T>,
    val uKind: ParameterKind<U>): ParameterKind<Pair<T, U>> {
    override val name: String = "Pair<${tKind.name}, ${uKind.name}>"

    override fun checkValue(v: Any?): Boolean {
        return when {
            v == null -> false
            v is Pair<*, *> &&
                    tKind.checkValue(v.first) &&
                    uKind.checkValue(v.second) -> true
            else -> false
        }
    }

    override fun load(t: JsonElement): Pair<T, U>? {
        if (t == JsonNull) { return null }
        return if (t is JsonObject) {
            val first = tKind.load(t["first"]!!)
                ?: throw ConfigurationParameterException("Expected a ${tKind.name} but found null")
            val second = uKind.load(t["second"]!!)
                ?: throw ConfigurationParameterException("Expected a ${uKind.name} but found null")
            Pair(first, second)
        } else {
            throw ConfigurationParameterException("Expected a pair, found ${t}")
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
