package org.goodmath.chalumier.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.goodmath.chalumier.errors.ConfigurationParameterException

object StringParameterKind: ParameterKind<String> {
    override val name: String = "String"
    override val sampleValueString: String = "\"abcdef\""
    override val isOptional = false


    override fun checkValue(v: Any?): Boolean {
        return when(v) {
            null, is JsonNull -> false
            is String -> true
            is JsonPrimitive -> v.isString
            else -> false
        }
    }

    override fun checkConfigValue(v: Any?): Boolean {
        return v != null && v is String
    }

    override fun fromJson(t: JsonElement): String? {
        return when (t) {
            JsonNull -> {
                null
            }
            is JsonPrimitive -> {
                t.content
            }

            else -> {
                throw error(t)
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

