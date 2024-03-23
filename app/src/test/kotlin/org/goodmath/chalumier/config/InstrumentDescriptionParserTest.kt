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
import java.io.StringReader

class InstrumentDescriptionParserTest {

    @Test
    fun testParseConfig() {
        val input = "whatever {\n name = \"twaddle\", \nvalue=32.123,\n ar=[1.0, 2, 3] \n}"
        val c = DescriptionParser(StringReader(input))
        val cfg = c.parseConfig()
        assertEquals("whatever", cfg.name)
        assertEquals(32.123, cfg.values["value"])
        assertEquals(listOf(1.0, 2.0, 3.0), cfg.values["ar"])
        assertEquals("twaddle", cfg.values["name"])
        assertEquals(setOf("name", "value", "ar"), cfg.values.keys.toSet())
    }

    @Test
    fun testParseTuples() {
        val input = "this { x = (me: 1, 2, \"abc\", false),\ny=38,z=[1, 2]\n}\n"
        val c = DescriptionParser(StringReader(input)).parseConfig()
        assertEquals("this", c.name)
        val t = c.values["x"] as Tuple
        assertEquals("me", t.name)
        assertEquals(4, t.body.size)
        assertEquals(1.0, t.body[0])
        assertEquals(2.0, t.body[1])
        assertEquals("abc", t.body[2])
        assertEquals(false, t.body[3])
    }

    @Test
    fun testRealInput() {
        val c = DescriptionParser(StringReader(flute))
        val cfg = c.parseConfig()
        assertEquals("folkflute", cfg.name)
        val fingerings = cfg.values["fingerings"] as List<Any>
        assertEquals(16, fingerings.size)
        assertEquals("F#4", (fingerings[1] as Map<String, Any>)["noteName"])
        assertEquals(
            setOf(
                "fingerings", "minHoleDiameters", "numberOfHoles", "maxHoleDiameters",
                "name", "minHoleSpacing", "rootNote", "holeAngles"
            ), cfg.values.keys.toSet()
        )
    }

    companion object {

        val flute = """
    folkflute {
        name = "EMinorFlute",
        rootNote = "E4",
        numberOfHoles = 8,
        fingerings = [
            { noteName = "E4", fingers = [ "X", "X", "X", "X", "X", "X", "X", "O" ] },
            {
                noteName = "F#4",
                fingers = [
                    "O",
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "G4",
                fingers = [
                    "O",
                    "O",
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "G#4",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "X",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "A4",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "O",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "B4",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "C5",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "C#5",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "X",
                    "X",
                    "X",
                    "O",
                    "O"
                ]
            },
            {
                noteName = "D5",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "O"
                ]
            },
            {
                noteName = "E5",
                fingers = [
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "F#5",
                fingers = [
                    "O",
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName ="G5",
                fingers = [
                    "O",
                    "O",
                    "X",
                    "X",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "G#5",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "X",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "A5",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "O",
                    "X",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "B5",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "X",
                    "X",
                    "O"
                ]
            },
            {
                noteName = "C6",
                fingers = [
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "O",
                    "X",
                    "O"
                ]
            }
        ],

        maxHoleDiameters = [
            15.0,
            15.0,
            15.0,
            15.0,
            15.0,
            15.0,
            15.0,
            11.3
        ],
        minHoleDiameters = [
            4.0,
            4.0,
            4.0,
            4.0,
            4.0,
            4.0,
            4.0,
            11.3
        ],
        holeAngles = [
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0
        ],
        minHoleSpacing = [
            15.0,
            15.0,
            10.0,
            null,
            10.0,
            20.0,
            null
        ]
    }

""".trimIndent()

    }
}
