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
