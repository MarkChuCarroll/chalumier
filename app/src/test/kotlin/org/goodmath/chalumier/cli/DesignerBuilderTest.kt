package org.goodmath.chalumier.cli

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.goodmath.chalumier.design.Fingering
import org.goodmath.chalumier.design.Hole
import org.goodmath.chalumier.design.Hole.*
import org.goodmath.chalumier.design.TaperedFluteDesigner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText

class DesignerBuilderTest {

    lateinit var designerBuilder: DesignerBuilder
    val outputPath = Path("/tmp/junk")

    @BeforeEach
    fun setUp() {

        val templates = mapOf("folkFlute" to { n: String, dir: Path ->  org.goodmath.chalumier.design.folkFluteDesigner(n, outputPath) },
            "pFlute" to { n: String, dir: Path -> org.goodmath.chalumier.design.pFluteDesigner(n, outputPath)})
        designerBuilder = DesignerBuilder(templates)
    }

    @Test
    fun testLoadSpec() {
        val file = Path("src/test/resources/flute.json")
        val specTxt = file.readText()
        val spec = Json.decodeFromString<InstrumentSpec>(specTxt)
        val expected = InstrumentSpec("minorflute",
            instrumentType = "folkFlute",
            fingerings = listOf(
                Fingering("D4", arrayListOf(X, X, X, X, X, X, O)),
                Fingering("E4", arrayListOf(O, X, X, X, X, X, O)),
                Fingering("F4", arrayListOf(X, O, X, X, X, X, O)),
                Fingering("F#4", arrayListOf(X, X, O, X, X, X, O)),
                Fingering("G4", arrayListOf(O, O, O, X, X, X, O)),
                Fingering("G#4", arrayListOf(X, X, X, O, X, X, O)),
                Fingering("A4", arrayListOf(O, O, O, O, X, X, O)),
                Fingering("Bb4", arrayListOf(O, O, X, X, O, X, O)),
                Fingering("B4", arrayListOf(O, O, O, O, O, X, O)),
                Fingering("C5", arrayListOf(O, O, O, X, X, X, O)),
                Fingering("C#5", arrayListOf(O, O, O, O, O, O, O)),
                Fingering("D5", arrayListOf(X, X, X, X, X, O, O)),
                Fingering("D5", arrayListOf(X, X, X, X, X, X, O)),
                Fingering("E5", arrayListOf(O, X, X, X, X, X, O)),
                Fingering("F5", arrayListOf(X, O, X, X, X, X, O)),
                Fingering("F#5", arrayListOf(O, X, O, X, X, X, O)),
                Fingering("G5", arrayListOf(O, O, O, X, X, X, O)),
                Fingering("A5", arrayListOf(O, O, O, O, X, X, O)),
                Fingering("Bb5", arrayListOf(X, X, X, O, X, X, O)),
                Fingering("B5", arrayListOf(O, X, X, O, X, X, O)),
                Fingering("C6", arrayListOf(O, X, X, O, O, X, O)),
                Fingering("D6", arrayListOf(X, X, X, X, X, X, O))),
            numberOfHoles = 7)

        assertEquals(expected, spec)
    }

    @Test
    fun testGetDesigner() {
        val d = designerBuilder.getDesigner(Path("src/test/resources/flute.json"), outputPath)
        assertTrue(d is TaperedFluteDesigner)
        assertEquals("minorflute", d.name)
        assertEquals(7, d.numberOfHoles)
    }

}
