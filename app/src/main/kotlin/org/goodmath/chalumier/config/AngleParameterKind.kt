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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.goodmath.chalumier.design.Angle
import org.goodmath.chalumier.errors.ConfigurationParameterException

object AngleParameterKind: ParameterKind<Angle> {
    override val name: String = "Angle"
    override val isOptional: Boolean = false

    override fun fromConfigValue(v: Any?): Angle {
        return if (v is Angle) { v }
        else if (v is Tuple) {
            if (v.body.size == 2) {
                Angle(
                    Angle.AngleDirection.valueOf(v.body[0] as String),
                    v.body[1] as Double
                )
            } else {
                Angle(Angle.AngleDirection.valueOf(v.body[0] as String))
            }
        }
        else if (v is Map<*, *>) {
            val dirStr = v["dir"]!! as String
            val optVal = v["v"]
            if (optVal == null) {
                Angle(Angle.AngleDirection.valueOf(dirStr))
            } else {
                Angle(Angle.AngleDirection.valueOf(dirStr), optVal as Double)
            }
        } else {
            throw error(v)
        }
    }
    override fun toConfigValue(t: Angle): String {
        return "(Angle: ${t.dir}${t.value?.let { ", $it" }?:""})"
    }



    override fun checkValue(v: Any?): Boolean {
        return when (v) {
            null -> false
            is Angle -> true
            is JsonNull ->  false
            is JsonObject -> {
                v.containsKey("dir") && v.keys.size <= 2
            }
            else -> {
                false
            }
        }

    }

    override fun checkConfigValue(v: Any?): Boolean {
        return when (v) {
            null -> false
            is Map<*, *> ->
                v.containsKey("dir") && listOf("Up", "Down", "Mean", "Exact").contains(v["dir"]) &&
                        (v["value"]?.let {
                            it is Double || it is String && it.toDoubleOrNull() != null
                        } ?: false)
            is Tuple -> {
                v.name == "Angle" && v.body.size <= 2 &&
                        setOf("Up", "Down", "Exact", "Mean").contains(v.body[0]) &&
                        (v.body.size == 1 ||
                                (v.body.size == 2 && v.body[1] == null || v.body[1] is Double))
            }
            else -> false
        }
    }

    override fun fromJson(t: JsonElement): Angle? {
        return when (t) {
            JsonNull -> { null }
            is JsonPrimitive -> {
                when (val label = t.content) {
                    "Mean" -> Angle(Angle.AngleDirection.Mean)
                    "Up" -> Angle(Angle.AngleDirection.Up)
                    "Down" -> Angle(Angle.AngleDirection.Down)
                    else ->
                        if (name.startsWith("Exact:")) {
                            Angle(
                                Angle.AngleDirection.Exact,
                                label.substring(5).toDouble()
                            )
                        } else {
                            throw error(t)
                        }
                }
            }

            else -> {
                throw error(t)
            }
        }
    }

    override fun dump(t: Angle?): JsonElement {
        return when {
            t == null -> JsonNull
            t.dir == Angle.AngleDirection.Up -> JsonPrimitive("Up")
            t.dir == Angle.AngleDirection.Down -> JsonPrimitive("Down")
            t.dir == Angle.AngleDirection.Mean -> JsonPrimitive("Mean")
            t.dir == Angle.AngleDirection.Exact -> JsonPrimitive("Exact:${t.value}")
            else -> throw ConfigurationParameterException("invalid value for Angle parameter: '${t}'")
        }
    }
}

val PairOfAnglesParameterKind = object: PairParameterKind<Angle, Angle>(AngleParameterKind, AngleParameterKind) {
    override fun checkConfigValue(v: Any?): Boolean {
        return when (v) {
            is Tuple -> {
                when (v.name) {
                    "Angle" -> {
                        AngleParameterKind.checkConfigValue(v)
                    }
                    "Pair" -> {
                        v.body.size == 2 && AngleParameterKind.checkConfigValue(v.body[0]) &&
                                AngleParameterKind.checkConfigValue(v.body[1])
                    }
                    else -> {
                        false
                    }
                }
            }

            else -> {
                false
            }
        }

    }

    override fun fromConfigValue(v: Any?): Pair<Angle, Angle> {
        return if (v is Tuple) {
            when (v.name) {
                "Angle" -> {
                    val single = AngleParameterKind.fromConfigValue(v)
                    Pair(single, single)
                }
                "Pair" -> {
                    val first = AngleParameterKind.fromConfigValue(v.body[0])
                    val second = AngleParameterKind.fromConfigValue(v.body[1])
                    return Pair(first, second)
                }
                else -> { throw error(v) }
            }
        } else { throw error(v) }
    }

}

val optAnglePairKind = object: ParameterKind<Pair<Angle, Angle>?> {
    override val name: String = "Pair<Angle, Angle>?"

    override val isOptional: Boolean = true


    override fun checkValue(v: Any?): Boolean {
        return v == null || v is JsonNull || PairOfAnglesParameterKind.checkValue(v)
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return v == null || v == "null" || PairOfAnglesParameterKind.checkConfigValue(v)
    }

    override fun fromJson(t: JsonElement): Pair<Angle, Angle>? {
        return when(t) {
            is JsonNull -> null
            else -> PairOfAnglesParameterKind.fromJson(t)
        }
    }

    override fun dump(t: Pair<Angle, Angle>?): JsonElement {
        return when(t) {
            null -> JsonNull
            else -> PairOfAnglesParameterKind.dump(t)
        }
    }

    override fun toConfigValue(t: Pair<Angle, Angle>?): String {
        return if (t == null) { "null" } else { PairOfAnglesParameterKind.toConfigValue(t) }
    }

    override fun fromConfigValue(v: Any?): Pair<Angle, Angle>? {
        return if (v == null || v == "null") {
            null
        } else {
            PairOfAnglesParameterKind.fromConfigValue(v)
        }
    }

}

val ListOfOptAnglePairsKind = ListOfOptParameterKind(optAnglePairKind)
fun<T: Configurable<T>> PairOfAnglesParameter(help: String = "", gen: (T) -> Pair<Angle, Angle>): ConfigParameter<T, Pair<Angle, Angle>> {
    return ConfigParameter(PairOfAnglesParameterKind, help, gen=gen)
}

fun<T: Configurable<T>> ListOfOptAnglePairsParameter(help: String = "", gen: (T) -> List<Pair<Angle, Angle>?>): ConfigParameter<T, List<Pair<Angle, Angle>?>> {
    val mutGen = { t: T -> ArrayList(gen(t))}
    return ConfigParameter(ListOfOptAnglePairsKind, help, gen=mutGen)
}


