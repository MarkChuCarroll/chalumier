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
package org.goodmath.chalumier.design

import kotlinx.serialization.json.*
import org.goodmath.chalumier.config.ConfigParameter
import org.goodmath.chalumier.config.Configurable
import org.goodmath.chalumier.config.ListParameterKind
import org.goodmath.chalumier.config.ParameterKind
import org.goodmath.chalumier.errors.ConfigurationParameterException

data class Fingering(
    val noteName: String, val fingers: List<Double>, val nth: Double? = null
) {
    fun wavelength(transpose: Int): Double {
        return wavelength(noteName, transpose)
    }
}

object FingeringParameterKind: ParameterKind<Fingering> {
    override val name: String = "Fingering"

    override fun checkValue(v: Any?): Boolean {
        return v is Fingering
    }

    override fun load(f: JsonElement): Fingering? {
        if (f == JsonNull) {
            return null
        }
        if (f !is JsonObject) {
            throw ConfigurationParameterException("Expected a loadable json  ${name} object, but found ${f}")
        }
        val noteName = f["noteName"]?.toString() ?: throw ConfigurationParameterException("Expected a noteName field in $f")
        val fingersStr = f["fingers"]?.toString() ?: throw ConfigurationParameterException("Expected a fingers field in $f")
        val nth = f["nth"]?.toString()?.toDouble()
        val fingers = fingersStr.split(",").map { it.toDouble() }
        return Fingering(noteName, fingers, nth)
    }

    override fun dump(t: Fingering?): JsonElement {
        if (t == null) { return JsonNull }
        return buildJsonObject {
            put("noteName", JsonPrimitive(t.noteName))
            put("fingersStr", JsonPrimitive(t.fingers.joinToString(",") { it.toString() }))
            put("nth", t.nth?.let { JsonPrimitive(it)} ?: JsonNull)
        }
    }
}

val ListOfFingeringsKind = ListParameterKind(FingeringParameterKind)
fun<T: Configurable<T>> ListOfFingeringsParam(help: String = "", gen: (T) -> List<Fingering>): ConfigParameter<T, ArrayList<Fingering>> {
    val mutGen: (T) -> ArrayList<Fingering> = { target ->
        ArrayList(gen(target))
    }
    return ConfigParameter(ListOfFingeringsKind, help, mutGen)
}