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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.goodmath.chalumier.errors.ConfigurationParameterException
import org.goodmath.chalumier.errors.ConfigurationParameterValueException
import java.io.FileWriter
import java.nio.file.Path

/**
 * The base for an object with configuration parameters.
 *
 * The idea behind this is that we have objects with lots
 * of configuration parameters, and we need to be able to save
 * and load copies of those objects. But those objects may also have
 * a large amount of transient state data that we don't want to save.
 *
 * In addition, these configuration parameters have standard
 * defaults that are computed from the values of other configuration
 * parameters.
 *
 * The way that I chose to handle that is to set up this system
 * of field delegates. Each field provides a function to generate
 * its default value, and an object called a Kind, which provides
 * the capability to test values, and to serialize and deserialize
 * values that it owns.
 *
 * With all of this, what we get is: a bunch of fields in
 * our objects that to normal Kotlin code behave exactly like normal
 * kotlin vars, but which have this infrastructure backing them.
 *
 */
abstract class Configurable<T: Configurable<T>>(open val instrumentName: String) {

    private val configParameters = HashMap<String, ConfigParameter<T, *>>()

    fun generateDescriptionTemplate(): String {
        val result = StringBuilder()
        result.append("# Chalumier Instrument designer skeleton for a $instrumentName\n")
        result.append("#\n")
        result.append(wrapString("This is an automatically generated template file. To create an instrument, fill in the fields you want to change, and delete the rest",
                                "# "))
        result.append("\n\n")
        result.append("$instrumentName {\n")

        result.append( configParameters.map { (paramName, param) ->
            param.render(paramName)
        }.joinToString(",\n\n" ))
        result.append("\n}\n")
        return result.toString()
    }

    fun listConfigParameters(): List<Pair<String, String>> {
        return configParameters.map { (k, v) ->
            Pair(k, v.kind.name)
        }
    }

    private fun getConfigParameterByName(name: String): ConfigParameter<T, *>? {
        return configParameters[name]
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> addConfigParameter(name: String, option: ConfigParameter<T, V>) {
        val old = configParameters[name]
        if (old == null) {
            configParameters[name] = option
        } else {
            if (old.kind == option.kind) {
                old as ConfigParameter<T, V>
                old.gen = option.gen
            } else {
                configParameters[name] = option
            }
        }
    }

    private fun getConfigParameterKind(name: String): ParameterKind<*>? {
        return getConfigParameterByName(name)?.kind
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> getConfigParameterValue(name: String): V? {
        return configParameters[name]?.get(this as T) as V?
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V> setConfigParameterValue(name: String, value: V) {
        val opt = configParameters[name] as? ConfigParameter<T, V>
        if (opt != null) {
            opt.set(value)
        } else {
            throw ConfigurationParameterException("Unknown config parameter '${name}'")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    val prettyJson = Json { // this returns the JsonBuilder
        prettyPrint = true
        // optional: specify indent
        prettyPrintIndent = " "
    }

    private fun toJsonString(): String {
        val js = toJson()
        val out = js.toString()
        val pr = prettyJson.parseToJsonElement(out)
        return prettyJson.encodeToString(pr)
    }

    fun writeParametersToFile(path: Path) {
        val f = FileWriter(path.toFile())
        f.write(toJsonString())
        f.close()
    }

    @Suppress("UNCHECKED_CAST")
    open fun toJson(): JsonObject {
        val me: T = this as T
        return buildJsonObject {
            put("typeName", me.instrumentName)
            putJsonArray("parameters") {
                for ((name, param) in configParameters) {
                    addJsonObject {
                        put("name", name)
                        put("kind", param.kind.name)
                        param.kind.dumpByName(me, name)?.let {
                            put("value", it)
                        }
                    }
                }
            }
        }
    }

    open fun fromJson(jsonParams: JsonObject) {
        val paramsName = jsonParams["typeName"]?.let {
            if (it is JsonPrimitive) {
                it.content
            } else throw ConfigurationParameterException("Parameter set must have a typeName")
        }
        if (this.instrumentName != paramsName) {
            throw ConfigurationParameterException("Trying to load a parameter set for type |${this.instrumentName}|, but found one for type |${jsonParams["typeName"]}|")
        }
        val params = jsonParams["parameters"]
        if (params is JsonArray) {
            params.forEach {el ->
                if (el is JsonObject) {
                    val elName = el["name"]?.let {
                        if (it is JsonPrimitive) {
                            it.content
                        } else {
                            null
                        }
                    } ?: throw ConfigurationParameterException("Expected parameter in list to have a name")
                    val kind = getConfigParameterKind(elName) ?: throw ConfigurationParameterException(
                        "unknown configuration parameter '${elName}'"
                    )
                    val elValue = el["value"]?.let {
                        kind.fromJson(it)
                    } ?: throw ConfigurationParameterException("Expected parameter in list to have a name")
                    setConfigParameterValue(elName, elValue)
                }
            }
        } else {
            throw ConfigurationParameterException("Expected an array of parameter list entries, but found '$params'")
        }
    }

    //    open fun readConfigDict(path: Path) {
    //        val js = Json5.decodeFromString(path.readText())
    //    }

    open fun updateFromConfig(instrumentConfig: InstrumentDescription) {
        for ((k, v) in instrumentConfig.values) {
            val param = getConfigParameterByName(k)
            if (param == null) {
                System.err.println("Configuration contained unknown key ${k}; skipping")
            } else {
                try {
                    if (!param.setConfigValue(v)) {
                        System.err.println("Configuration parameter $k expected a value of type ${param.kind.name}, but found '$v'")
                    }
                } catch (c: ConfigurationParameterValueException) {
                    System.err.println("Configuration parameter $k expected a value of type ${c.expected}  but found '$v'")
                }
            }
        }
    }

    companion object {
        fun wrapString(str: String, linePrefix: String, width: Int = 80): String {
            val maxChars = width - linePrefix.length
            if (str.length < maxChars) {
                return "$linePrefix$str\n"
            }
            val words = str.split(" ")
            val result = ArrayList<String>()
            var currentLine: String = linePrefix
            for (word in words) {
                if (currentLine.length + word.length < width) {
                    currentLine += "$word "
                } else {
                    result.add(currentLine)
                    currentLine = "$linePrefix$word "
                }
            }
            if (currentLine != linePrefix) {
                result.add(currentLine)
            }
            return result.joinToString("\n") + "\n"
        }
    }
}


