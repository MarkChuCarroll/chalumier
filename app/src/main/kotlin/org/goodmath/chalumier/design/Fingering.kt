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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.goodmath.chalumier.config.*
import org.goodmath.chalumier.errors.ConfigurationParameterException

@Serializable
enum class Hole {
    O, X
}

/**
 * A single fingered note on an instrument.
 * @param noteName the name of the musical note that should be
 *   produced by this fingering.
 * @param fingers a list of Hole values specifying whether each
 *    hole is open or closed for this note. X means closed, O means open.
 * @param nth an optional value that specifies the dominant overtone
 *    number (primarily used on reeded instruments, where a register break
 *    forces to switch to a different dominant overtine.
 *
 * For description files, this is written as an object:
 * ```
 *  { noteName = "D4", fingers = [ "X", "O", "O", "X", "O", "O"] }
 */
@Serializable
data class Fingering(
    val noteName: String, val fingers: List<Hole>, val nth: Int? = null
) {
    fun wavelength(transpose: Int): Double {
        return wavelength(noteName, transpose)
    }
}

object FingeringParameterKind: ParameterKind<Fingering> {
    override val name: String = "Fingering"
    override val isOptional = false

    override fun checkConfigValue(v: Any?): Boolean {
        return when (v) {
            is Map<*, *> -> {
                val noteName = v["noteName"]
                val fingers = v["fingers"]
                val nth = v["nth"]
                noteName != null &&
                        fingers != null && fingers is List<*> &&
                        fingers.all { it == "X" || it == "O" }
                        (nth == null || nth is Double)
            }
            is Tuple -> {
                if (v.name != "Fingering" ||  (v.body.size != 2 && v.body.size != 3)) {
                    false
                } else {
                    val noteName = v.body[0]
                    val fingers = v.body[1]
                    val nth = if (v.body.size == 3) { v.body[2] } else {null}
                    noteName is String && fingers is List<*> && fingers.all { it == "X" || it == "O" } &&
                            (nth == null || nth is Double)
                }
            }
            else ->  false
        }
    }

    override fun checkValue(v: Any?): Boolean {
        return when(v) {
            null, is JsonNull -> false
            is Fingering -> true
            is JsonObject ->  {
                val noteName = v["noteName"]
                val fingers = v["fingers"]
                val nth = v["nth"]
                noteName != null && noteName is JsonPrimitive && noteName.isString &&
                        fingers != null && fingers is JsonArray &&
                        fingers.all { it is JsonPrimitive && it.isString &&
                                (it.content == "X" || it.content == "O") } &&
                        (nth == null || nth is JsonNull || (nth is JsonPrimitive && nth.doubleOrNull != null))
            }
            else -> false
        }

    }

    override fun fromConfigValue(v: Any?): Fingering {
        return when(v) {
            is Map<*, *> -> {
                val name = v["noteName"] as String
                val fingers = (v["fingers"] as List<*>).map {
                    if (it == "X") {
                        Hole.X
                    } else {
                        Hole.O
                    }
                }
                val nth = (v["nth"] as Double?)?.toInt()
                Fingering(name, fingers, nth)
            }
            is Tuple -> {
                val name = v.body[0] as String
                val fingers =  (v.body[1] as List<*>).map {
                    if (it == "X") {
                        Hole.X
                    } else {
                        Hole.O
                    }
                }
                val nth = if (v.body.size == 3) { v.body[2]?.let { it as Double} } else { null }
                Fingering(name,  fingers, nth?.toInt())
            }
            else ->
                throw ConfigurationParameterException("Invalid fingering entry $v")
        }
    }

    override fun toConfigValue(t: Fingering): String {
        val fs = t.fingers.joinToString(", ") { "\"$it\"" }
        val nth = t.nth?.let { ", nth=$it"} ?: ""
        return "{ noteName=\"${t.noteName}\", fingers=[$fs]$nth }"
    }

    override fun fromJson(t: JsonElement): Fingering? {
        if (t == JsonNull) {
            return null
        }
        if (t !is JsonObject) {
            throw ConfigurationParameterException("Expected a loadable json  $name object, but found $t")
        }
        val noteName = t["noteName"]?.toString() ?: throw ConfigurationParameterException("Expected a noteName field in $t")
        val fingersStr = t["fingers"]?.toString() ?: throw ConfigurationParameterException("Expected a fingers field in $t")
        val nth = t["nth"]?.toString()?.toInt()
        val fingers = fingersStr.split(",").map { if (it == "O") { Hole.O } else {Hole.X } }
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

val ListOfFingeringsKind = object: ListParameterKind<Fingering>(FingeringParameterKind) {
    override fun toConfigValue(t: List<Fingering>): String {
        val fingerings = t.joinToString(",\n      ") { FingeringParameterKind.toConfigValue(it) }
        return "[\n      $fingerings\n   ]"
    }
}
fun<T: Configurable<T>> ListOfFingeringsParam(help: String = "", gen: (T) -> List<Fingering>): ConfigParameter<T, List<Fingering>> {
    val mutGen: (T) -> List<Fingering> = { target ->
        ArrayList(gen(target))
    }
    return ConfigParameter(ListOfFingeringsKind, help, gen=mutGen)
}
