package org.goodmath.demakeink

import kotlin.reflect.KProperty

class AssignableParameter<T, V>(val gen: (T) -> V) {
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
