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

import org.goodmath.chalumier.design.Fingering
import org.goodmath.chalumier.design.Hole
import org.goodmath.chalumier.design.TaperedFluteDesigner
import org.goodmath.chalumier.design.instruments.TaperedFlute
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.StringReader
import kotlin.io.path.Path
import kotlin.test.assertIs

class ConfigurableTest {

    @Test
    fun testUpdateFromDict() {
        val fluteDesigner = TaperedFluteDesigner(
            "tapered", Path("/tmp/whocares"),
            TaperedFlute.builder
        )
        val cfg = DescriptionParser(StringReader(InstrumentDescriptionParserTest.flute)).parseConfig()
        fluteDesigner.updateFromConfig(cfg)
        assertEquals("EMinorFlute", fluteDesigner.name)
        assertEquals("E4", fluteDesigner.rootNote)
        assertIs<List<*>>(fluteDesigner.fingerings)
        val fing = fluteDesigner.fingerings[0]
        assertEquals("E4", fing.noteName)
        assertEquals("F#4", fluteDesigner.fingerings[1].noteName)
        assertEquals(Fingering("G4", listOf(Hole.O, Hole.O, Hole.X, Hole.X, Hole.X, Hole.X, Hole.X, Hole.O), null),
            fluteDesigner.fingerings[2])
    }
}
