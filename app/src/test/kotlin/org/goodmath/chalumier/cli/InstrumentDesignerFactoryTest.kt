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
