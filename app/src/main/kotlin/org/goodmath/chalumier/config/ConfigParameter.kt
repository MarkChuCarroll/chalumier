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

import org.goodmath.chalumier.errors.ConfigurationParameterException
import org.goodmath.chalumier.errors.ConfigurationParameterValueException
import java.io.BufferedWriter
import kotlin.reflect.KProperty

/**
 * A helpful utility class for managing parameters in a system with a large
 * number of configurable parameters.
 *
 * This works as a property delegate in a Kotlin class, allowing you to provide
 * a default value for the parameter based on other values. If the parameter is
 * never assigned, it will use the default value.
 *
 * The parameter is also registered into its class configurable parameters table,
 * which will make is serializable as part of its class.
 */
open class ConfigParameter<T: Configurable<T>, V>(
    val kind: ParameterKind<V>,
    val help: String = "",
    var gen: (T) -> V) {

    lateinit var name: String
    lateinit var ref: T

    private object UNINITIALIZED

    var v: Any? = UNINITIALIZED

    operator fun provideDelegate(
        thisRef: T,
        prop: KProperty<*>
    ): ConfigParameter<T, V> {
        this.name = prop.name
        this.ref = thisRef
        thisRef.addConfigParameter(prop.name, this)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun get(thisRef: T): V {
        return if (v == UNINITIALIZED) {
            synchronized(this) {
                v = gen(thisRef)
                v as V
            }
        } else {
            v as V
        }
    }

    operator fun getValue(thisRef: T, property: KProperty<*>): V {
        return get(thisRef)
    }


    fun setConfigValue(v: Any?): Boolean {
        if (kind.checkConfigValue(v)) {
            set(kind.fromConfigValue(v))
            return true
        } else {
            return false
        }
    }

    fun set(value: V) {
        v = value
    }

    operator fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        set(value)
    }

    /**
     * Generate a string containing a description of the field, its type,
     * and its default value.
     */
    fun render(fieldName: String): String {
        val result = StringBuilder()
        val optStr = if (kind.isOptional) {
            " (This field is optional.)"
        } else {
            ""
        }
        result.append("   # Field:$fieldName\n")
        result.append("   # Type: ${kind.name}$optStr\n")
        if (help != "") {
            result.append("   # Description:\n")
            result.append(Configurable.wrapString(help, "   #    "))
        }
        result.append("   $fieldName = ${kind.toConfigValue(get(ref))}")
        return result.toString()
    }

}
