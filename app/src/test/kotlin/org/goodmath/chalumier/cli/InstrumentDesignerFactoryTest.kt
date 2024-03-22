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
package org.goodmath.chalumier.cli

import io.github.xn32.json5k.Json5
import kotlinx.serialization.decodeFromString
import org.goodmath.chalumier.design.Fingering
import org.goodmath.chalumier.design.Hole.*
import org.goodmath.chalumier.design.TaperedFluteDesigner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText

class InstrumentDesignerFactoryTest {

    lateinit var instrumentDesignerFactory: InstrumentDesignerFactory
    val outputPath = Path("/tmp/junk")

    @BeforeEach
    fun setUp() {
        val templates = mapOf("folkFlute" to { n: String, _: Path ->  org.goodmath.chalumier.design.folkFluteDesigner(n, outputPath) },
            "pFlute" to { n: String, _: Path -> org.goodmath.chalumier.design.pFluteDesigner(n, outputPath)})
        instrumentDesignerFactory = InstrumentDesignerFactory(templates)
    }

    @Test
    fun testGetDesigner() {
        val d = instrumentDesignerFactory.getDesigner(Path("src/test/resources/flute.chal"), outputPath)
        assertTrue(d is TaperedFluteDesigner)
        assertEquals("majorFlute", d.name)
        assertEquals(7, d.numberOfHoles)
    }

}
