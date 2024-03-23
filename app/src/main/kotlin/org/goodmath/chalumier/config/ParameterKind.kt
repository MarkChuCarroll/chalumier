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
@file:Suppress("UNCHECKED_CAST")

package org.goodmath.chalumier.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.goodmath.chalumier.errors.ConfigurationParameterValueException

/**
 * A parameter kind is an object describing the type of a
 * configuration parameter. It includes methods for checking
 * if a value is convertable to the type, and for serializing and
 * deserializing configuration values of the type.
 */
interface ParameterKind<T> {
    val name: String

    val isOptional: Boolean

    val sampleValueString: String


    /**
     * Check if an arbitrary value is either this type, or a
     * JSON representation of this type.
     */
    fun checkValue(v: Any?): Boolean

    /**
     * Check if a configuration value is convertable to this type.
     */
    fun checkConfigValue(v: Any?): Boolean

    /**
     * Convert a configuration value (assumed to have already passed
     * checkConfigValue) to this type.
     */
    fun fromConfigValue(v: Any?): T {
        return v as T
    }

    /**
     * convert a value of this type to json.
     */
    fun dump(t: T?): JsonElement


    /**
     * Internal method used for serializing an entire configurable
     * object to JSON.
     */
    fun<C: Configurable<C>> dumpByName(c: C, paramName: String): JsonElement? {
        val v = c.getConfigParameterValue<T>(paramName)
        return dump(v)
    }

    /**
     * Convert a JSON object to this type.
     */
    fun fromJson(t: JsonElement): T?

    fun error(v: Any?): ConfigurationParameterValueException =
        ConfigurationParameterValueException(name, v)

}

/**
 * Given a kind of type T, generate a kind of type T?
 */
fun<T> opt(pk: ParameterKind<T>): ParameterKind<T?> {
    return object: ParameterKind<T?> {
        override val name: String = "${pk.name}?"
        override val isOptional = true
        override val sampleValueString: String = "pk.sampleValueString or null"

        override fun fromConfigValue(v: Any?): T? {
            return if (v == null) {
                null
            } else {
                 pk.fromConfigValue(v)
            }
        }

        override fun checkValue(v: Any?): Boolean {
            return pk.checkValue(v) || v == null
        }

        override fun checkConfigValue(v: Any?): Boolean {
            return v?.let { pk.checkConfigValue(it) }?: true
        }

        override fun fromJson(t: JsonElement): T? {
            return pk.fromJson(t)
        }

        override fun dump(t: T?): JsonElement {
            return if (t == null) { JsonNull }
            else { pk.dump(t) }
        }
    }
}
