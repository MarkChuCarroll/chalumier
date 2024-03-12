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
package org.goodmath.chalumier.design

import org.goodmath.chalumier.design.InstrumentDesigner.Companion.O
import org.goodmath.chalumier.design.InstrumentDesigner.Companion.X
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import kotlin.io.path.Path

class InstrumentTest {

    lateinit var fluteDesigner: InstrumentDesigner
    lateinit var flute: Instrument
    @BeforeEach
    fun setU() {
        fluteDesigner = makeDesigner()
        val initial = fluteDesigner.initialDesignParameters()
        flute = fluteDesigner.makeInstrumentFromParameters(initial)
        flute.prepare()
        flute.preparePhase()
    }

    fun makeDesigner(): InstrumentDesigner {
        val flute = TaperedFluteDesigner("FolkFlute",
            Path("/tmp/test"))
        flute.numberOfHoles = 7
        flute.fingerings = FluteDesigner.fingeringsWithEmbouchure(FluteDesigner.folkFingerings)
        flute.balance = arrayListOf(0.1, null, null, 0.1)
        // ph: hole_angles = [ -30.0, -30.0, 30.0, -30.0, 30.0, -30.0, 0.0 ]
        // ph: hole_angles = [ 30.0, -30.0, 30.0, 0.0, 0.0, 0.0, 0.0 ]
        flute.holeAngles = arrayListOf(-30.0, 30.0, 30.0, -30.0, 0.0, 30.0, 0.0)
        flute.maxHoleSpacing = flute.scaler(listOf(45.0, 45.0, null, 45.0, 45.0, null))
        // min_hole_diameters = design.sqrt_scaler([ 7.5 ] * 6  + [ 12.2 ])
        // max_hole_diameters = design.sqrt_scaler([ 11.4 ] * 6 + [ 13.9 ])
        return flute
    }


    @Test
    fun testResonanceScore() {
        assertEquals(-0.919196398601, flute.resonanceScore(1178.0, arrayListOf(X, X, X, X, X, X, O)).first, 1E-10)
        assertEquals(-0.806748283178, flute.resonanceScore(1178.0, arrayListOf(O, X, X, O, X, X, O)).first, 1E-10)
        assertEquals(-0.807559491778, flute.resonanceScore(1178.0, arrayListOf(X, O, X, O, X, X, O)).first, 1E-10)
    }

    @Test
    fun testResonancePhase() {
        assertEquals(1.5404018007, flute.resonancePhase(1178.0, arrayListOf(X, X, X, X, X, X, O)), 1E-10)
        assertEquals(0.596625858411, flute.resonancePhase(1178.0, arrayListOf(O, X, X, O, X, X, O)), 1E-10)
        assertEquals(0.596220254111, flute.resonancePhase(1178.0, arrayListOf(X, O, X, O, X, X, O)), 1E-10)


    }

    @Test
    fun testTrueWavelengthNear() {
        assertEquals(1290.32705069, flute.trueWavelengthNear(1178.0, arrayListOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0)), 1E-8)
        assertEquals(1015.42666135, flute.trueWavelengthNear(1178.0, arrayListOf(1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0)), 1E-8)
        assertEquals(717.899892079, flute.trueWavelengthNear(1178.0, arrayListOf(1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0)), 1E-8)
        assertEquals(828.740302396, flute.trueWavelengthNear(1178.0, arrayListOf(1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0)), 1E-8)
    }

    @Test
    fun testTrueNthWavelengthNear() {
        assertEquals(1288.80144381, flute.trueNthWavelengthNear(1178.0, arrayListOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0), 1), 1E-8)
        assertEquals(655.244984487, flute.trueNthWavelengthNear(1178.0, arrayListOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0), 2), 1E-8)
        assertEquals(1015.90777764, flute.trueNthWavelengthNear(1178.0, arrayListOf(1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0), 1), 1E-8)
        assertEquals(370.692806868, flute.trueNthWavelengthNear(1178.0, arrayListOf(1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0), 3), 1E-8)
        assertEquals(495.296861326, flute.trueNthWavelengthNear(1178.0, arrayListOf(1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0), 2), 1E-8)
        assertEquals(302.707071509, flute.trueNthWavelengthNear(1178.0,  arrayListOf(1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0), 4), 1E-8)
        assertEquals(815.086664168, flute.trueNthWavelengthNear(1178.0, arrayListOf(1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0), 1), 1E-8)
        assertEquals(313.972294704, flute.trueNthWavelengthNear(1178.0, arrayListOf(1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0), 4), 1E-8)

    }
}
