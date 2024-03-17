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
    fun testLoadSpec() {
        val spec = Json5.decodeFromString<InstrumentDescription>(Path("src/test/resources/flute.json5").readText())
        val expected = InstrumentDescription("majorFlute",
            instrumentType = "folkFlute",
            rootNote = "D4",
            fingerings = listOf(
                Fingering("D4",arrayListOf(X,X,X,X,X,X,O)),
                Fingering("E4",arrayListOf(O,X,X,X,X,X,O)),
                Fingering("F#4",arrayListOf(O,O,X,X,X,X,O)),
                Fingering("G4",arrayListOf(O,O,O,X,X,X,O)),
                Fingering("A4",arrayListOf(O,O,O,O,X,X,O)),
                Fingering("B4",arrayListOf(O,O,O,O,O,X,O)),
                Fingering("C#5",arrayListOf(O,O,O,O,O,O,O)),
                Fingering("D5",arrayListOf(X,X,X,X,X,O,O)),
                Fingering("D5",arrayListOf(X,X,X,X,X,X,O)),
                Fingering("E5",arrayListOf(O,X,X,X,X,X,O)),
                Fingering("F#5",arrayListOf(O,O,X,X,X,X,O)),
                Fingering("G5",arrayListOf(O,O,O,X,X,X,O)),
                Fingering("A5",arrayListOf(O,O,O,O,X,X,O)),
                Fingering("B5",arrayListOf(O,O,O,O,O,X,O))),
        numberOfHoles = 7)

        assertEquals(expected.toString(), spec.toString())
    }

    @Test
    fun testGetDesigner() {
        val (_, d) = instrumentDesignerFactory.getDesigner(Path("src/test/resources/flute.json5"), outputPath)
        assertTrue(d is TaperedFluteDesigner)
        assertEquals("majorFlute", d.name)
        assertEquals(7, d.numberOfHoles)
    }

}
