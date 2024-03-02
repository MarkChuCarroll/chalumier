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

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.math.PI

class DelegateTest: Configurable<DelegateTest>("test") {
    var a: Double by DoubleParameter { 3.0 }
}

class MultiFieldTest: Configurable<MultiFieldTest>("multi-test") {
    var one by DoubleParameter { 27.3 }
    var two by ListOfDoubleParameter { listOf(21.0, 1.0, 2.0, 3.5)}
    var three by IntParameter { 17  }
}

class MultiFieldTestWithNulls: Configurable<MultiFieldTestWithNulls>("multi-test-nullable") {
    var one by DoubleParameter { 27.3 }
    var two by ListOfDoubleParameter { listOf(21.0, 1.0, 2.0, 3.5)}
    var three by OptIntParameter { 17  }
    var four by OptIntParameter { null }
    var five by ListOfOptDoubleParameter { listOf(-1.0, null, -2.0, null, 3.0) }
}

class ConfigParameterTest {

    @Test
    fun testGetValue() {
        val del = DelegateTest()
        assertEquals(del.a, 3.0)
    }

    @Test
    fun testSetUnAccessedValue() {
        val target = DelegateTest()
        target.a = 7.2
        assertEquals(target.a, 7.2)
    }

    @Test
    fun testSetAccessedValue() {
        val target = DelegateTest()
        assertEquals(target.a, 3.0)
        target.a = 7.2
        assertEquals(target.a, 7.2)
    }

    @Test
    fun testIndependence() {
        val targetOne = DelegateTest()
        val targetTwo = DelegateTest()

        assertEquals(targetOne.a, targetTwo.a)
        targetOne.a = 17.2
        assertNotEquals(targetOne.a, targetTwo.a)
        assertEquals(targetOne.a, 17.2)
        assertEquals(targetTwo.a, 3.0)
        targetTwo.a = PI
        assertNotEquals(targetOne.a, targetTwo.a)
        assertEquals(targetOne.a, 17.2)
        assertEquals(targetTwo.a, PI)
    }

    @Test
    fun testRendering() {
        val v = MultiFieldTest()
        v.one = PI
        v.two = ArrayList(v.two + listOf(1.2, 2.3))
        v.three = 314

        val rendered = v.toJson()
        assertEquals(
            "{\"typeName\":\"multi-test\",\"parameters\":[" +
                    "{\"name\":\"one\",\"kind\":\"Double\",\"value\":3.141592653589793}," +
                    "{\"name\":\"two\",\"kind\":\"List<Double>\",\"value\":[21.0,1.0,2.0,3.5,1.2,2.3]}," +
                    "{\"name\":\"three\",\"kind\":\"Int\",\"value\":314}]}",
            rendered.toString()
        )
    }

    @Test
    fun testRenderingWithNulls() {
        val v = MultiFieldTestWithNulls()
        v.one = PI
        v.two = ArrayList(v.two + listOf(1.2, 2.3))
        v.three = 314

        val rendered = v.toJson()
        assertEquals(
            "{\"typeName\":\"multi-test-nullable\",\"parameters\":[" +
                    "{\"name\":\"four\",\"kind\":\"Int?\",\"value\":null}," +
                    "{\"name\":\"one\",\"kind\":\"Double\",\"value\":3.141592653589793}," +
                    "{\"name\":\"two\",\"kind\":\"List<Double>\",\"value\":[21.0,1.0,2.0,3.5,1.2,2.3]}," +
                    "{\"name\":\"three\",\"kind\":\"Int?\",\"value\":314}," +
                    "{\"name\":\"five\",\"kind\":\"List<Double?>\",\"value\":[-1.0,null,-2.0,null,3.0]}]}",
            rendered.toString()
        )
    }

    @Test
    fun testLoading() {
        val v = MultiFieldTest()
        v.one = PI
        v.two = ArrayList(v.two + listOf(1.2, 2.3))
        v.three = 314

        val rendered = v.toJson()


        val w = MultiFieldTest()
        w.fromJson(rendered)
        assertEquals(PI, w.one)
        assertEquals(arrayListOf(21.0, 1.0, 2.0, 3.5, 1.2, 2.3), w.two)
        assertEquals(314, w.three)
    }


    @Test
    fun testLoadingWithNulls() {
        val v = MultiFieldTestWithNulls()
        v.one = PI
        v.two = ArrayList(v.two + listOf(1.2, 2.3))
        v.three = 314
        v.four = 3
        v.five = arrayListOf(2.0, null, 3.0, null, 48.0)

        val rendered = v.toJson()


        val w = MultiFieldTestWithNulls()
        w.fromJson(rendered)
        assertEquals(PI, w.one)
        assertEquals(arrayListOf(21.0, 1.0, 2.0, 3.5, 1.2, 2.3), w.two)
        assertEquals(314, w.three)
        assertEquals(3, w.four)
        assertEquals(arrayListOf(2.0, null, 3.0, null, 48.0), w.five)
    }

}
