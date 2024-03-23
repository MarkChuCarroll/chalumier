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
package org.goodmath.chalumier.errors

fun dAssert(v: Boolean, msg: String) {
    if (!v) {
        throw AssertionException(msg)
    }
}

open class ChalumierException(msg: String, cause: Throwable? = null) : Exception(msg, cause)
class RequiredParameterException(name: String, msg: String = "is a required parameter") :
    ChalumierException("$name $msg")

class ConfigurationParameterValueException(val expected: String, val value: Any?)
    : ChalumierException("Configuration parameter expected a $expected value, but found '$value'")

class ConfigurationParameterException(error: String): ChalumierException("Config error: $error")
class AssertionException(msg: String) : ChalumierException(msg)
