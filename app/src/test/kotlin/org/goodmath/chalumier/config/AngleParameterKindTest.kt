package org.goodmath.chalumier.config

import org.goodmath.chalumier.design.Angle
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.StringReader

class AngleParameterKindTest {

    @Test
    fun testFromConfigValue() {
        val c = DescriptionParser(StringReader("inst {\nangles=[(Angle: \"Down\"), (Angle: \"Exact\", 73.1)]}\n")).parseConfig()
        val lst: Any? = c.values["angles"]
        val listOfAngles = ListParameterKind<Angle>(AngleParameterKind)
        assertTrue(listOfAngles.checkConfigValue(lst))
        val angles = listOfAngles.fromConfigValue(lst)
        assertEquals(angles.size, 2)
        assertEquals(Angle(Angle.AngleDirection.Down), angles[0])
        assertEquals(Angle(Angle.AngleDirection.Exact, 73.1), angles[1])

    }
}