/*
 * Copyright 2024 Mark C. Chu-Carroll and Paul Francis Harrison
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

package org.goodmath.chalumier.cli.ui

/**
 * For text UIs, it's useful to have a transcript area. This provides
 * a buffer for a bounded number of lines that will be displayed.
 */
class BoundedTranscript(val size: Int) {
    val elements: Array<Any> = Array<Any>(size) { "" }
    var front = 0

    fun print(a: Any) {
        synchronized(this) {
            elements[front] = a
            front = (front + 1) % size
        }
    }

    operator fun get(idx: Int): Any {
        return elements[(front + idx) % size]
    }

    fun transcript(): List<Any> {
        synchronized(this) {
            return (0 until size).map {
                this[it]
            }
        }
    }
}
