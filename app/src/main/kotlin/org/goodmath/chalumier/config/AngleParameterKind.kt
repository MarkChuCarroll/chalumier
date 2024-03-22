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
import org.goodmath.chalumier.design.Angle
import org.goodmath.chalumier.errors.ConfigurationParameterException

object AngleParameterKind: ParameterKind<Angle> {
    override val name: String = "Angle"

    override fun fromConfigValue(v: Any?): Angle {
        return if (v is Angle) { v }
        else if (v is Map<*, *>) {
            val dirStr = v["dir"]!! as String
            val optVal = v["v"]
            if (optVal == null) {
                Angle(Angle.AngleDirection.valueOf(dirStr))
            } else {
                Angle(Angle.AngleDirection.valueOf(dirStr), optVal as Double)
            }
        } else {
            throw ConfigurationParameterException("Expected an angle, but found $v")
        }
    }

    override fun checkValue(v: Any?): Boolean {
        return v != null && (v is Angle ||
                v is Map<*, *> && v.containsKey("dir"))
    }

    override fun load(t: JsonElement): Angle? {
        return when (t) {
            JsonNull -> { null }
            is JsonPrimitive -> {
                when (val label = t.content) {
                    "Mean" -> Angle(Angle.AngleDirection.Mean)
                    "Up" -> Angle(Angle.AngleDirection.Up)
                    "Down" -> Angle(Angle.AngleDirection.Down)
                    else ->
                        if (name.startsWith("Here:")) {
                            Angle(
                                Angle.AngleDirection.Here,
                                label.substring(5).toDouble()
                            )
                        } else {
                            throw ConfigurationParameterException("Angle parameter expected an Angle, but found $t")
                        }
                }
            }

            else -> {
                throw ConfigurationParameterException("Angle parameter expected an Angle, but found $t")
            }
        }
    }

    override fun dump(t: Angle?): JsonElement {
        return when {
            t == null -> JsonNull
            t.dir == Angle.AngleDirection.Up -> JsonPrimitive("Up")
            t.dir == Angle.AngleDirection.Down -> JsonPrimitive("Down")
            t.dir == Angle.AngleDirection.Mean -> JsonPrimitive("Mean")
            t.dir == Angle.AngleDirection.Here -> JsonPrimitive("Here:${t.v}")
            else -> throw ConfigurationParameterException("invalid value for Angle parameter: '${t}'")
        }
    }
}

val PairOfAnglesParameterKind = PairParameterKind(AngleParameterKind, AngleParameterKind)
val ListOfOptAnglePairsKind = ListOfOptParameterKind(opt(PairOfAnglesParameterKind))
fun<T: Configurable<T>> PairOfAnglesParameter(help: String = "", gen: (T) -> Pair<Angle, Angle>): ConfigParameter<T, Pair<Angle, Angle>> {
    return ConfigParameter(PairOfAnglesParameterKind, help, gen=gen)
}

fun<T: Configurable<T>> ListOfOptAnglePairsParameter(help: String = "", gen: (T) -> List<Pair<Angle, Angle>?>): ConfigParameter<T, List<Pair<Angle, Angle>?>> {
    val mutGen = { t: T -> ArrayList(gen(t))}
    return ConfigParameter(ListOfOptAnglePairsKind, help, gen=mutGen)
}


