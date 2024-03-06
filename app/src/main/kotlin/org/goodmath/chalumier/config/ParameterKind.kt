package org.goodmath.chalumier.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull


interface ParameterKind<T> {
    val name: String

    /**
     *
     */
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
