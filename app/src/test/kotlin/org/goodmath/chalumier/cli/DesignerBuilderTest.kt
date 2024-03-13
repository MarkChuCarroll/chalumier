package org.goodmath.chalumier.cli

import kotlinx.serialization.encodeToString
import net.peanuuutz.tomlkt.Toml
import org.goodmath.chalumier.design.InstrumentDesigner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.io.path.Path

class DesignerBuilderTest {

    lateinit var designerBuilder: DesignerBuilder

    @BeforeEach
    fun setUp() {
        val outputPath = Path("/tmp/junk")
        var templates = mapOf("folkFlute" to { n: String ->  org.goodmath.chalumier.design.folkFluteDesigner(n, outputPath) },
            "pFlute" to { n: String -> org.goodmath.chalumier.design.pFluteDesigner(n, outputPath)})
        designerBuilder = DesignerBuilder(templates)
    }

    @Test
    fun testGetDesigner() {
        val spec = InstrumentSpec("minorflute",
            instrumentType = "folkFlute",
            fingerings = listOf(
                FingeringSpec("D4", arrayListOf(Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("E4", arrayListOf(Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("F4", arrayListOf(Hole.Closed, Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("F#4", arrayListOf(Hole.Closed, Hole.Closed, Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("G4", arrayListOf(Hole.Open, Hole.Open, Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("G#4", arrayListOf(Hole.Closed, Hole.Closed, Hole.Closed, Hole.Open, Hole.Closed, Hole.Closed)),
                FingeringSpec("A4", arrayListOf(Hole.Open, Hole.Open, Hole.Open, Hole.Open, Hole.Closed, Hole.Closed)),
                FingeringSpec("Bb4", arrayListOf(Hole.Open, Hole.Open, Hole.Closed, Hole.Closed, Hole.Open, Hole.Closed)),
                FingeringSpec("B4", arrayListOf(Hole.Open, Hole.Open, Hole.Open, Hole.Open, Hole.Open, Hole.Closed)),
                FingeringSpec("C5", arrayListOf(Hole.Open, Hole.Open, Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("C#5", arrayListOf(Hole.Open, Hole.Open, Hole.Open, Hole.Open, Hole.Open, Hole.Open)),
                FingeringSpec("D5", arrayListOf(Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Open)),
                FingeringSpec("D5", arrayListOf(Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("E5", arrayListOf(Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("F5", arrayListOf(Hole.Closed, Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("F#5", arrayListOf(Hole.Open, Hole.Closed, Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("G5", arrayListOf(Hole.Open, Hole.Open, Hole.Open, Hole.Closed, Hole.Closed, Hole.Closed)),
                FingeringSpec("A5", arrayListOf(Hole.Open, Hole.Open, Hole.Open, Hole.Open, Hole.Closed, Hole.Closed)),
                FingeringSpec("Bb5", arrayListOf(Hole.Closed, Hole.Closed, Hole.Closed, Hole.Open, Hole.Closed, Hole.Closed)),
                FingeringSpec("B5", arrayListOf(Hole.Open, Hole.Closed, Hole.Closed, Hole.Open, Hole.Closed, Hole.Closed)),
                FingeringSpec("C6", arrayListOf(Hole.Open, Hole.Closed, Hole.Closed, Hole.Open, Hole.Open, Hole.Closed)),
                FingeringSpec("D6", arrayListOf(Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed, Hole.Closed))),
            numberOfHoles = 7)

        val s = Toml.encodeToString(spec)
        assertEquals("", s)



    }
}
