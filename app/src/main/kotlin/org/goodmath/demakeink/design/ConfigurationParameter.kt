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
package org.goodmath.demakeink.design

import kotlinx.serialization.Serializable
import org.goodmath.demakeink.errors.DemakeinException
import java.io.ObjectInputFilter.Config
import kotlin.reflect.KProperty


/**
 * A helpful utility class for managing parameters in a system with a large
 * number of configurable parameters.
 *
 * This works as a property delegate in a Kotlin class, allowing you to provide
 * a default value for the parameter based on other values. If the parameter is
 * never assigned, it will use the default value.

 */
class ConfigurationParameter<T, V>(
        val help: String = "",
        val gen: (T) -> V) {


    private var v: V? = null

    operator fun getValue(thisRef: T, property: KProperty<*>): V {
        val result = v ?: gen(thisRef)
        setValue(thisRef, property, result)
        return result
    }

    operator fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        v = value
    }
}

