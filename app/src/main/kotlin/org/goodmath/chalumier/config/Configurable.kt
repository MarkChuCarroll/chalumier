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
abstract class Configurable<T: Configurable<T>>(open val name: String) {
    val configParameters = HashMap<String, ConfigParameter<T, *>>()

    val myNum: Int = getNextNum()

    fun listConfigParameters(): List<Pair<String, String>> {
        return configParameters.map { (k, v) ->
            Pair(k, v.kind.name)
        }
    }


    fun getConfigParameterByName(name: String): ConfigParameter<T, *>? {
        return configParameters[name]
    }

    fun <V> addConfigParameter(name: String, option: ConfigParameter<T, V>) {
        configParameters[name] = option
    }

    fun getConfigParameterKind(name: String): ParameterKind<*>? {
        return getConfigParameterByName(name)?.kind
    }

    fun <V> getConfigParameterValue(name: String): V? {
        return configParameters[name]?.get(this as T) as V?
    }

    fun <V> setConfigParameterValue(name: String, value: V) {
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

    fun toJsonString(): String {
        val js = toJson()
        val out = js.toString()
        val pr = prettyJson.parseToJsonElement(out)
        return prettyJson.encodeToString(pr)
    }

    fun writeParametersToFile(path: Path) {
        val f = FileWriter(path.toFile())
        f.write(toString())
        f.close()
    }

    open fun toJson(): JsonObject {
        val me: T = this as T
        return buildJsonObject {
            put("typeName", me.name)
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
        if (this.name != paramsName) {
            throw ConfigurationParameterException("Trying to load a parameter set for type |${this.name}|, but found one for type |${jsonParams["typeName"]}|")
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
                        kind.load(it)
                    } ?: throw ConfigurationParameterException("Expected parameter in list to have a name")
                    setConfigParameterValue(elName, elValue)
                }
            }
        } else {
            throw ConfigurationParameterException("Expected an array of parameter list entries, but found '$params'")
        }
    }

    var num: Int = 0
    fun getNextNum(): Int {
        num++
        return num
    }


}
