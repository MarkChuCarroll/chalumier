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

import io.github.xn32.json5k.Json5
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.goodmath.chalumier.config.*
import org.goodmath.chalumier.design.Hole.X
import org.goodmath.chalumier.design.Hole.O
import org.goodmath.chalumier.design.instruments.Instrument
import org.goodmath.chalumier.design.instruments.InstrumentFactory
import org.goodmath.chalumier.design.instruments.Whistle
import org.goodmath.chalumier.make.InstrumentMaker
import org.goodmath.chalumier.make.WhistleHeadMaker
import org.goodmath.chalumier.make.WhistleMaker
import org.goodmath.chalumier.util.fromEnd
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText


abstract class AbstractWhistleDesigner(n: String,
                                       outputDir: Path,
                                       builder: InstrumentFactory<Whistle>
):
    InstrumentDesignerWithBoreScale<Whistle>(n, outputDir, builder) {
    // ph: From 2014-15-whistle-tweaking
    open var tweakGapExtra by DoubleParameter {
        0.6
    }
    open var tweakBoreLess: Double by DoubleParameter {
        0.3

    }

    override var boreScale: Double by DoubleParameter { 1.1 }
    override var closedTop: Boolean by BooleanParameter { false }
    override var divisions by ListOfListOfIntDoublePairParam  {
        emptyList()
    }


    private fun getWhistleHeadProportions(): Pair<Double, Double> {
        val bore = innerDiameters.fromEnd(1)
        val baseOutside = outerDiameters.fromEnd(1)
        val outside = if (outerAdd) {
            Pair(baseOutside.first + bore.first, baseOutside.second + bore.second)
        } else {
            baseOutside
        }

        return  Pair(WhistleHeadMaker.effectiveGapDiameter(bore.first),
                WhistleHeadMaker.effectiveGapHeight(bore.first, outside.first))

    }


    /* ph:
    # For gap_width 0.5, gap_length 0.25, from soprano
    # tweak_gapExtra = 0.37

    # From soprano recorder
    #tweak_gapExtra = 0.75 #0.71
    #tweak_boreless = 0.65 #0.49
    */

    open var xPad: Double by DoubleParameter {
        0.0
    }

    open var yPad: Double by DoubleParameter {
        0.0
    }

    override fun patchInstrument(inst: Instrument): Instrument {
        val patchedInst = inst.dup()
        patchedInst.trueLength = patchedInst.length

        // TODO
        //patchedInst.trueInner = patchedInst.inner

        val boreDiameter = patchedInst.inner(patchedInst.length)
        patchedInst.length -= (boreDiameter * tweakBoreLess)
        patchedInst.inner = patchedInst.inner.clipped(0.0, patchedInst.length)

        // ph: #bulge_pos1 = self.tweak_bulgePos1 * inst.length
        // ph: #bulge_pos2 = self.tweak_bulgePos2 * inst.length
        // ph: if bulge_pos1 > bulge_pos2:
        // ph:     bulge_pos1, bulge_pos2 = bulge_pos2, bulge_pos1
        // ph: bulge_amount = (self.tweak_bulgeDiameter-1.0) * bore_diameter
        // ph: inst.inner = inst.inner + profile.Profile(
        // ph:     [ bulge_pos1, bulge_pos2 ],
        // ph:     [ 0.0, bulge_amount ],
        // ph:     [ bulge_amount, 0.0 ],
        // ph:     )

        val props = getWhistleHeadProportions()
        val diameter = props.first
        val length = (props.second/ 2.0) + diameter * tweakGapExtra
        // ph:         ^ The gap is this high on one side and a blade on the other side
        // ph:           split the difference.

        patchedInst.inner = Profile(
            ArrayList(patchedInst.inner.pos + listOf(patchedInst.length + length)),
            ArrayList(patchedInst.inner.low + listOf(diameter)),
            ArrayList(patchedInst.inner.high.slice(0 until patchedInst.inner.high.size - 1) + listOf(diameter, diameter))
        )
        patchedInst.length += length
        return patchedInst
    }

    override fun getInstrumentMaker(
        spec: Whistle,
    ): InstrumentMaker<Whistle> {
        return WhistleMaker("whistle", outputDir, spec, this)
    }
}

class SixHoleWhistleDesigner(n: String,
                             outputDir: Path,
                             builder: InstrumentFactory<Whistle>
    ) : AbstractWhistleDesigner(n, outputDir, builder) {
    override var transpose: Int by IntParameter { 12 }

    override var divisions by ListOfListOfIntDoublePairParam {
        listOf(
            listOf(Pair(5, 0.0)),
            listOf(Pair(1, 0.0), Pair(5, 0.0), Pair(5, 0.5)),
            listOf(Pair(-1, 0.75), Pair(1, 0.0), Pair(2, 1.0), Pair(5, 0.0), Pair(5, 0.3), Pair(5, 0.6))
        )
    }

    override fun readInstrument(path: Path): Whistle {
        return Json5.decodeFromString<Whistle>(path.readText())
    }

    override fun writeInstrument(instrument: Whistle, path: Path) {
        path.writeText(Json5.encodeToString<Whistle>(instrument))
    }

    override var minHoleDiameters by ListOfDoubleParameter {
        boreScaler(listOf(3.0).repeat(6))
    }


    override var maxHoleDiameters by ListOfDoubleParameter {
        boreScaler(listOf(12.0).repeat(6), maximum = 12.0)
    }

    override var holeHorizAngles by ListOfDoubleParameter {
        listOf(0.0).repeat(6)
    }

    override var balance by ListOfOptDoubleParameter {
        listOf(0.05, null, null, 0.05)
    }

    override var minHoleSpacing by ListOfOptDoubleParameter {
        scaler(listOf(null, null, 35.0, null, null))
    }

    override var maxHoleSpacing by ListOfOptDoubleParameter {
        sqrtScaler(listOf(35.0, 35.0, null, 35.0, 35.0))
    }

    override var innerDiameters by ListOfDoublePairParameter {
        boreScaler(listOf(14.0, 14.0, 20.0, 22.0, 22.0, 20.0, 20.0)).map { Pair(it, it) }
    }

    override var initialInnerFractions by ListOfDoubleParameter {
        listOf(0.2, 0.6, 0.65, 0.7, 0.75)
    }

    override var minInnerFractionSep by ListOfDoubleParameter {
        listOf(0.01, 0.5, 0.01, 0.01, 0.01, 0.01)
    }

    override var outerDiameters by ListOfDoublePairParameter {
        boreScaler(listOf(40.0, 28.0, 28.0, 32.0, 32.0)).map { Pair(it, it) }
    }
    override var outerAngles by ListOfOptAnglePairsParameter {
        listOf(
            Angle(Angle.AngleDirection.Exact, -15.0), Angle(Angle.AngleDirection.Exact, 0.0), null, null, null
        ).map { angle -> angle?.let { Pair(it, it) } }
    }

    override var initialOuterFractions by ListOfDoubleParameter {
        listOf(0.15, 0.5, 0.85)
    }

    override var minOuterFractionSep by ListOfDoubleParameter {
        listOf(0.15, 0.3, 0.35, 0.15)
    }
}

fun folkWhistleDesigner(name: String, outputDir: Path): SixHoleWhistleDesigner {
    val result = SixHoleWhistleDesigner(name,  outputDir, Whistle.builder)
    result.rootNote = "D4"
    result.fingerings = arrayListOf(
        Fingering("D4", listOf(X, X, X, X, X, X)),
        Fingering("E4", listOf(O, X, X, X, X, X)),
        Fingering("F#4", listOf(O, O, X, X, X, X)),
        Fingering("G4", listOf(O, O, O, X, X, X)),
        Fingering("A4", listOf(O, O, O, O, X, X)),
        Fingering("B4", listOf(O, O, O, O, O, X)),
        Fingering("C5", listOf(O, O, O, X, X, O)),
        Fingering("C#5", listOf(O, O, O, O, O, O)),
        Fingering("D5", listOf(X, X, X, X, X, O)),
        Fingering("E5", listOf(O, X, X, X, X, X)),
        Fingering("F#5", listOf(O, O, X, X, X, X)),
        Fingering("G5", listOf(O, O, O, X, X, X)),
        Fingering("A5", listOf(O, O, O, O, X, X)),
        Fingering("B5", listOf(O, O, O, O, O, X)),
        // ("C#6", listOf(X,X,X,O,O,0]),
        // ("C#6", listOf(O,O,O,O,O,0]),
        Fingering("D6", listOf(X, X, X, X, X, X))
    )
    return result
}

fun dorianWhistleDesigner(outputDir: Path): SixHoleWhistleDesigner {
    val result = SixHoleWhistleDesigner("DorianWhistle", outputDir,
        Whistle.builder)
    result.initialLength = wavelength("D4") * 0.5
    result.fingerings = arrayListOf(
        Fingering("D4", listOf(X, X, X, X, X, X)),
        Fingering("E4", listOf(O, X, X, X, X, X)),
        Fingering("F4", listOf(O, O, X, X, X, X)),
        Fingering("G4", listOf(O, O, O, X, X, X)),
        Fingering("A4", listOf(O, O, O, O, X, X)),
        Fingering("Bb4", listOf(O, O, O, O, O, X)),
        Fingering("B4", listOf(O, O, O, X, X, O)),
        Fingering("C5", listOf(O, O, O, O, O, O)),
        Fingering("D5", listOf(X, X, X, X, X, X)),
        Fingering("E5", listOf(O, X, X, X, X, X)),
        Fingering("F5", listOf(O, O, X, X, X, X)),
        Fingering("G5", listOf(O, O, O, X, X, X)),
        Fingering("A5", listOf(O, O, O, O, X, X)),
        Fingering("Bb5", listOf(O, O, O, O, O, X)),
        Fingering("B5", listOf(O, O, O, X, X, O))
    )

    return result
}

class RecorderDesigner(override val instrumentName: String,
                       outputDir: Path
) : AbstractWhistleDesigner(instrumentName, outputDir, Whistle.builder) {


    override var initialLength by DoubleParameter {
        wavelength("C4") * 0.5
    }

    override var transpose by IntParameter { 12 }

    override var divisions by ListOfListOfIntDoublePairParam {
        listOf(
            listOf(Pair(7, 0.0)),
            listOf(Pair(0, 0.0), Pair(7, 0.0)),
            listOf(Pair(0, 0.0), Pair(3, 0.0), Pair(7, 0.0)),
            listOf(Pair(0, 0.0), Pair(3, 0.0), Pair(7, 0.0), Pair(7, 0.5)),
            listOf(Pair(0, 0.0), Pair(2, 0.0), Pair(4, 0.0), Pair(7, 0.0), Pair(7, 0.5))
        )
    }

    override fun readInstrument(path: Path): Whistle {
        return Json5.decodeFromString<Whistle>(path.readText())
    }

    override fun writeInstrument(instrument: Whistle, path: Path) {
        path.writeText(Json5.encodeToString(instrument))
    }

    override var minHoleDiameters by ListOfDoubleParameter {
        boreScaler(listOf(3.0).repeat(8))
    }

    override var maxHoleDiameters by ListOfDoubleParameter {
        boreScaler(listOf(12.0) + listOf(14.0).repeat(7))
    }

    override var minHoleSpacing by ListOfOptDoubleParameter {
        sqrtScaler(listOf(0.0).repeat(6) + listOf(-50.0))
    }

    override var holeHorizAngles by ListOfDoubleParameter {
        listOf(-15.0) + listOf(0.0).repeat(6) + listOf(180.0)
    }

    override var holeAngles by ListOfDoubleParameter {
        listOf(-30.0, 30.0, -30.0, 30.0, 0.0, 0.0, 0.0, 0.0)
    }

    override var balance by ListOfOptDoubleParameter {
        listOf(0.1, 0.05, null, null, 0.05, null)
    }

    override var innerDiameters by ListOfDoublePairParameter {
        boreScaler(listOf(20.0, 20.0, 19.0, 23.0, 23.0, 20.0, 20.0)).map { Pair(it, it) }
    }

    override var initialInnerFractions by ListOfDoubleParameter {
        listOf(0.6, 0.65, 0.7, 0.75, 0.8)
    }

    override var minInnerFractionSep by ListOfDoubleParameter {
        listOf(0.3, 0.01, 0.01, 0.01, 0.01, 0.01)
    }

    override var outerDiameters by ListOfDoublePairParameter {
        boreScaler(listOf(40.0, 28.0, 28.0, 32.0, 32.0)).map { Pair(it, it) }
    }

    override var initialOuterFractions by ListOfDoubleParameter {
        listOf(0.15, 0.6, 0.85)
    }

    override var minOuterFractionSep by ListOfDoubleParameter {
        listOf(0.15, 0.3, 0.35, 0.15)
    }

    override var fingerings by ListOfFingeringsParam {
        listOf(
            Fingering("C4", listOf(X, X, X, X, X, X, X, X)),
            // ph:  Inter-register locking
            Fingering("C5", listOf(X, X, X, X, X, X, X, X)),
            Fingering("G5", listOf(X, X, X, X, X, X, X, X)),

            Fingering("D4", listOf(O, X, X, X, X, X, X, X)),
            // ph: Inter-register locking
            Fingering("D5", listOf(O, X, X, X, X, X, X, X)),
            Fingering("A5", listOf(O, X, X, X, X, X, X, X)),

            Fingering("E4", listOf(O, O, X, X, X, X, X, X)),
            // ph: Inter-register locking
            Fingering("B5", listOf(O, O, X, X, X, X, X, X)),

            // ph: Tendency to be sharp, due to inter register locking?
            Fingering("F4", listOf(X, X, O, X, X, X, X, X)),
            Fingering("F4", listOf(O, X, O, X, X, X, X, X)),

            Fingering("F#4", listOf(O, X, X, O, X, X, X, X)),
            Fingering("G4", listOf(O, O, O, O, X, X, X, X)),
            Fingering("G#4", listOf(O, X, X, X, O, X, X, X)),
            Fingering("A4", listOf(O, O, O, O, O, X, X, X)),
            Fingering("Bb4", listOf(O, O, O, X, X, O, X, X)),
            Fingering("B4", listOf(O, O, O, O, O, O, X, X)),
            Fingering("C5", listOf(O, O, O, O, O, X, O, X)),
            Fingering("C#5", listOf(O, O, O, O, O, X, X, O)),
            Fingering("D5", listOf(O, O, O, O, O, X, O, O)),


            Fingering("Eb5", listOf(O, X, X, X, X, X, O, O)),

            Fingering("E5", listOf(O, O, X, X, X, X, X, O)),
            Fingering("E5", listOf(O, O, X, X, X, X, X, X)),

            Fingering("F5", listOf(O, X, O, X, X, X, X, O)),
            Fingering("F5", listOf(O, X, O, X, X, X, X, X)),

            Fingering("F#5", listOf(O, O, X, O, X, X, X, O)),
            Fingering("F#5", listOf(O, O, X, O, X, X, X, X)),

            // ph: #Fingering("G5",    listOf(O,O,O,O,X,X,X,O)),
            Fingering("G5", listOf(O, O, O, O, X, X, X, X)),

            // ph: #Fingering("G#5",   listOf(O,O,O,X,O,X,X,X)),
            Fingering("A5", listOf(O, O, O, O, O, X, X, X))
            // ph:         Fingering("Bb5",   listOf(O,X,X,X,O,X,X,X)),
            // ph:         Fingering("B5",    listOf(O,O,X,X,O,X,X,X)),
        )
    }
}


