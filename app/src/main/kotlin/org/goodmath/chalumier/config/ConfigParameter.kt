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
import kotlin.reflect.KProperty

/**
 * A helpful utility class for managing parameters in a system with a large
 * number of configurable parameters.
 *
 * This works as a property delegate in a Kotlin class, allowing you to provide
 * a default value for the parameter based on other values. If the parameter is
 * never assigned, it will use the default value.

 */

interface ParameterKind<T> {
    val name: String
    fun checkValue(v: Any?): Boolean
    fun dump(t: T?): JsonElement

    fun<C: Configurable<C>> dumpByName(c: C, paramName: String): JsonElement? {
        val v = c.getConfigParameterValue<T>(paramName)
        return dump(v)
    }

    fun load(t: JsonElement): T?
}

fun<T> opt(pk: ParameterKind<T>): ParameterKind<T?> {
    return object: ParameterKind<T?> {
        override val name: String = "${pk.name}?"

        override fun checkValue(v: Any?): Boolean {
            return pk.checkValue(v) || v == null
        }

        override fun load(t: JsonElement): T? {
            return pk.load(t)
        }

        override fun dump(t: T?): JsonElement {
            return if (t == null) { JsonNull }
            else { pk.dump(t) }
        }
    }
}

open class ConfigParameter<T: Configurable<T>, V>(
    val kind: ParameterKind<V>,
    val help: String = "",
    val gen: (T) -> V
) {
    var v: V? = null
    operator fun provideDelegate(
        thisRef: T,
        prop: KProperty<*>
    ): ConfigParameter<T, V> {
        thisRef.addConfigParameter(prop.name, this)
        return this
    }

    fun get(thisRef: T): V {
        val result = v ?: gen(thisRef)
        set(result)
        return result
    }

    fun set(value: V) {
        v = value
    }

    operator fun getValue(thisRef: T, property: KProperty<*>): V {
        return get(thisRef)
    }

    operator fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        set(value)
    }
}


