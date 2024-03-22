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

import org.junit.jupiter.api.Assertions.assertEquals

fun assertFloatListEquals(expected: List<Double>, actual: List<Double>, delta: Double, description: String) {
    assertEquals(expected.size, actual.size)
    (0 until actual.size).forEach { i ->
        assertEquals(
            expected[i], actual[i], delta,
            "${description}[${i}] was incorrect"
        )
    }
}

fun assertOptFloatListEquals(expected: List<Double?>, actual: List<Double?>, delta: Double, description: String) {
    assertEquals(expected.size, actual.size)
    (0 until actual.size).forEach { i ->
        val expI = expected[i]
        val actI = actual[i]
        assert((expI == null && actI == null ) || (expI != null && actI != null))
        if (expI == null || actI == null) { return }
        assertEquals(expI, actI, delta,
            "${description}[${i}] was incorrect"
        )
    }
}
