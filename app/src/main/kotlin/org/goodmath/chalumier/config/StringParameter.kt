package org.goodmath.chalumier.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.goodmath.chalumier.errors.ConfigurationParameterException

object StringParameterKind: ParameterKind<String> {
    override val name: String = "String"

    override fun checkValue(v: Any?): Boolean {
        return v != null && v is String
    }

    override fun load(t: JsonElement): String? {
        return when (t) {
            JsonNull -> {
                null
            }
            is JsonPrimitive -> {
                t.content
            }

            else -> {
                throw ConfigurationParameterException("Parameter expected a string, but found '$t'")
            }
        }
    }

    override fun dump(t: String?): JsonElement {
        return t?.let { JsonPrimitive(it) } ?: JsonNull
    }
}

fun<T: Configurable<T>> StringParameter(help: String="", gen: (T) -> String): ConfigParameter<T, String> {
    return ConfigParameter(StringParameterKind, help, gen=gen)
}


fun<T: Configurable<T>> OptStringParameter(help: String="", gen: (T) -> String?): ConfigParameter<T,String?> {
    return ConfigParameter(opt(StringParameterKind), help, gen=gen)
}

