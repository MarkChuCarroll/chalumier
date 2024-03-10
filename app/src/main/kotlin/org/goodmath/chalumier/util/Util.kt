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
package org.goodmath.chalumier.util


interface Copyable<T> {
    fun copy(): T
}
fun <T> List<T>.fromEnd(i: Int): T = this[this.size - (i)]

fun <T> List<T>.repeat(i: Int): List<T> {
    return (0 until i).flatMap { this }
}

fun <T> Int.repeat(f: (i: Int) -> T): List<T> {
    return (0 until this).map { f(it) }
}


