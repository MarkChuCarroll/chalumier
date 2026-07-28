/*
 * Copyright 2024 Mark C. Chu-Carroll and Paul Francis Harrison
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
import org.goodmath.chalumier.config.booleanParameter
import org.goodmath.chalumier.config.doubleParameter
import org.goodmath.chalumier.config.intParameter
import org.goodmath.chalumier.config.listOfBooleanParameter
import org.goodmath.chalumier.config.listOfDoublePairParameter
import org.goodmath.chalumier.config.listOfDoubleParameter
import org.goodmath.chalumier.config.listOfListOfIntDoublePairParam
import org.goodmath.chalumier.config.listOfOptAnglePairsParameter
import org.goodmath.chalumier.config.listOfOptDoubleParameter
import org.goodmath.chalumier.design.Hole.O
import org.goodmath.chalumier.design.Hole.X
import org.goodmath.chalumier.design.instruments.Instrument
import org.goodmath.chalumier.design.instruments.InstrumentFactory
import org.goodmath.chalumier.design.instruments.ReedInstrument
import org.goodmath.chalumier.make.InstrumentMaker
import org.goodmath.chalumier.make.ReedInstrumentMaker
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

abstract class ReedInstrumentDesigner<Inst : ReedInstrument>(
    override val instrumentName: String,
    outputDir: Path,
    builder: InstrumentFactory<ReedInstrument>,
) :
    InstrumentDesigner<ReedInstrument>(instrumentName, outputDir, builder) {
    open val boreBaseline = 4.0

    open var bore by doubleParameter("Bore diameter at top. (ie reed diameter)") { 4.0 }

    // ph: reed_virtual_length = 25.0
    // ph: reed_virtual_top = 1.0
    // ph: reed_virtual_length = 50.0
    // ph: reed_virtual_top = 0.125

    // ph: From c5 drone
    open var reedVirtualLength by
        doubleParameter("Virtual length of reed, as a multiple of bore diameter.") { 34.0 }

    open var reedVirtualTop by
        doubleParameter("Virtual diameter of top of reed, proportion of bore diameter.") { 1.0 }

    override var transpose by intParameter { 0 }
    override var closedTop by booleanParameter { true }

    open var dock by booleanParameter { false }
    open var dockTop by doubleParameter { 8.5 }
    open var dockBottom by doubleParameter { 5.5 }
    open var dockLength by doubleParameter { 15.0 }
    open var dockDiameter by doubleParameter { 40.0 }
    open var addBauble by booleanParameter { false }

    override fun patchInstrument(inst: Instrument): Instrument {
        val patchedInst = inst.dup()
        // MarkCC: ph originally had a "true_length" and "true_inner" defined
        // here. But "true_inner" was never used, so I dropped it. True_length
        // was just initialized to "length" before length was modified.
        patchedInst.trueLength = length
        val reedLength = bore * reedVirtualLength
        val reedTop = bore * reedVirtualTop
        val reed = Profile.makeProfile(listOf(listOf(0.0, bore), arrayListOf(reedLength, reedTop)))
        patchedInst.inner += reed
        patchedInst.length += reedLength
        return patchedInst
    }

    fun boreScaler(value: List<Double>): ArrayList<Double> {
        val scale = bore / boreBaseline
        return ArrayList(value.map { it * scale })
    }

    companion object {
        fun inRange(
            low: Double,
            high: Double,
            n: Int,
        ): MutableList<Double> {
            return ArrayList(
                (1 until n + 1).map {
                    val i = it.toDouble()
                    (i + 1.0) * (high - low) / (n + 2) + low
                },
            )
        }

        fun fullRange(
            low: Double,
            high: Double,
            n: Int,
        ): MutableList<Double> {
            return ArrayList(
                (0 until n).map {
                    val i = it.toDouble()
                    i * (high - low) / (n - 1.0) + low
                },
            )
        }
    }
}

class ReedDroneDesigner(
    override val instrumentName: String,
    outputDir: Path,
) : ReedInstrumentDesigner<ReedInstrument>(instrumentName, outputDir, ReedInstrument.builder) {
    override var initialLength by doubleParameter { wavelength("C4") * 0.25 }
    override var innerDiameters by
        listOfDoublePairParameter {
            boreScaler(listOf(4.0, 4.0)).map { Pair(it, it) }.toMutableList()
        }
    override var outerDiameters by listOfDoublePairParameter {
        boreScaler(listOf(24.0, 12.0)).map { Pair(it, it) }.toMutableList()
    }
    override var holeHorizAngles by listOfDoubleParameter { emptyList() }

    override fun readInstrument(path: Path): ReedInstrument {
        return Json5.decodeFromString(path.readText())
    }

    override fun writeInstrument(
        instrument: ReedInstrument,
        path: Path,
    ) {
        path.writeText(Json5.encodeToString(instrument))
    }

    override fun getInstrumentMaker(spec: ReedInstrument): InstrumentMaker<ReedInstrument> {
        return ReedInstrumentMaker(name, outputDir, spec, this)
    }

    override var fingerings by listOfFingeringsParam {
        listOf(Fingering("C4", ArrayList(), 1)).toMutableList()
    }

    override var divisions by listOfListOfIntDoublePairParam { emptyList() }
}

open class ReedPipeDesigner(override val instrumentName: String, outputDir: Path) :
    ReedInstrumentDesigner<ReedInstrument>(instrumentName, outputDir, ReedInstrument.builder) {
    override fun readInstrument(path: Path): ReedInstrument {
        return Json5.decodeFromString(path.readText())
    }

    override fun writeInstrument(
        instrument: ReedInstrument,
        path: Path,
    ) {
        path.writeText(Json5.encodeToString(instrument))
    }

    override fun getInstrumentMaker(spec: ReedInstrument): InstrumentMaker<ReedInstrument> {
        return ReedInstrumentMaker(name, outputDir, spec, this)
    }

    override var innerDiameters by listOfDoublePairParameter {
        boreScaler(listOf(4.0, 4.0)).map { Pair(it, it) }.toMutableList()
    }

    override var outerDiameters by
        listOfDoublePairParameter { boreScaler(listOf(12.0, 12.0)).map { Pair(it, it) } }

    override var minHoleDiameters by
        listOfDoubleParameter { boreScaler(listOf(2.5).repeat(8)).toMutableList() }

    override var maxHoleDiameters by
        listOfDoubleParameter { boreScaler(listOf(4.0).repeat(8)).toMutableList() }

    // ph: max_hole_spacing = design.scaler([ 8O, 4O,4O,4O,None,4O,4O, 20 ])

    override var balance by
        listOfOptDoubleParameter { listOf(0.2, 0.075, 0.3, 0.3, 0.075, null) }

    // ph: balance = [ None, 0.1, 0.1, 0.3, 0.3, 0.1, None ]
    // ph: balance = [ 0.2, 0.1, None, None, 0.1, None ]

    override var holeAngles by listOfDoubleParameter { mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) }

    override var holeHorizAngles by
        listOfDoubleParameter { mutableListOf(-25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 180.0) }

    override var fingerPads by listOfBooleanParameter {
        listOf(true).repeat(it.numberOfHoles)
    }

    override var initialLength by doubleParameter { wavelength("C4") * 0.25 }

    override var fingerings by listOfFingeringsParam {
        mutableListOf(
            Fingering("C4", arrayListOf(X, X, X, X, X, X, X, X), 1),
            Fingering("D4", arrayListOf(O, X, X, X, X, X, X, X), 1),
            Fingering("E4", arrayListOf(O, O, X, X, X, X, X, X), 1),
            Fingering("F4", arrayListOf(O, O, O, X, X, X, X, X), 1),
            Fingering("G4", arrayListOf(O, O, O, O, X, X, X, X), 1),
            Fingering("A4", arrayListOf(O, O, O, O, O, X, X, X), 1),
            // ph: ("Bb4", [O,O,O,X,X,O,X,X), 1),
            Fingering("B4", arrayListOf(O, O, O, O, O, O, X, X), 1),
            Fingering("C5", arrayListOf(O, O, O, O, O, X, O, X), 1),
            // ph: ("C#5", [O,O,O,O,O,X,X,O), 1),
            Fingering("D5", arrayListOf(O, O, O, O, O, X, O, O), 1),
        )
    }

    override var divisions by listOfListOfIntDoublePairParam {
        // ph: [ (3, 0.5) ],
        // ph: [ (3, 0.5), (7, 0.1) ],
        mutableListOf(mutableListOf(Pair(0, 0.5), Pair(3, 0.5), Pair(7, 0.1)))
    }
}

abstract class AbstractShawmDesigner(
    override val instrumentName: String,
    outputDir: Path,
) :
    ReedInstrumentDesigner<ReedInstrument>(instrumentName, outputDir, ReedInstrument.builder) {
    override fun readInstrument(path: Path): ReedInstrument {
        return Json5.decodeFromString(path.readText())
    }

    override fun writeInstrument(
        instrument: ReedInstrument,
        path: Path,
    ) {
        path.writeText(Json5.encodeToString(instrument))
    }

    override var innerDiameters: List<Pair<Double, Double>> by listOfDoublePairParameter {
        boreScaler(fullRange(16.0, 4.0, 10)).map { Pair(it, it) }
        // ph:arrayListOf(  16.0, 14.0, 12.0, 10.0, 8.0, 6.0, 4.0, 1.5 )
    }
    override var initialInnerFractions by listOfDoubleParameter { fullRange(0.2, 0.9, 8) }
    override var minInnerFractionSep by listOfDoubleParameter { ArrayList(listOf(0.02).repeat(9)) }
    override var outerDiameters by listOfDoublePairParameter {
        boreScaler(listOf(70.0, 25.0, 25.0)).map { Pair(it, it) }
    }
    override var minOuterFractionSep by listOfDoubleParameter { arrayListOf(0.19, 0.8) }
    override var initialOuterFractions by listOfDoubleParameter { arrayListOf(0.19) }
    override var outerAngles by listOfOptAnglePairsParameter {
        ArrayList(
            listOf(Angle(Angle.AngleDirection.Exact, -35.0), Angle(Angle.AngleDirection.Up), Angle(Angle.AngleDirection.Down))
                .map { Pair(it, it) },
        )
    }
}

/**
 * Designer for a shawm/haut-bois/oboe/bombard with a fingering system similar to recorder.
 *
 * The flare at the end is purely decorative.
 */
open class ShawmDesigner(
    override val instrumentName: String,
    outputDir: Path,
) :
    AbstractShawmDesigner(instrumentName, outputDir) {
    override var minHoleDiameters by listOfDoubleParameter {
        boreScaler(listOf(2.0).repeat(9))
    }

    override var maxHoleDiameters by listOfDoubleParameter {
        // ph:  max_hole_diameters = bore_scaler([ 12.0 ] * 8)
        boreScaler(listOf(6.0).repeat(9))
    }

    override var initialHoleDiameterFractions by listOfDoubleParameter {
        ArrayList(listOf(0.5).repeat(9))
    }

    override var initialHoleFractions by listOfDoubleParameter {
        ArrayList(arrayListOf(7, 6, 5, 4, 3, 2, 1, 0, 0).map { i -> 0.5 - (0.6 * i.toDouble()) })
    }

    override var maxHoleSpacing by listOfOptDoubleParameter {
        scaler(listOf(80.0, 40.0, 40.0, 40.0, null, 40.0, 40.0, 20.0))
    }

    override var balance by listOfOptDoubleParameter {
        // ph: balance = [0.2, 0.1, 0.3, 0.3, 0.1, None]
        arrayListOf(null, 0.1, 0.1, 0.3, 0.3, 0.1, null)
        // ph: balance = [0.2, 0.1, None, None, 0.1, None]
    }

    override var holeAngles by listOfDoubleParameter {
        arrayListOf(0.0, -30.0, -30.0, -30.0, 30.0, 0.0, 0.0, 0.0, 0.0)
    }

    override var holeHorizAngles by listOfDoubleParameter {
        arrayListOf(30.0, -25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 180.0)
    }

    override var initialLength by doubleParameter { wavelength("B3") * 0.35 }

    override var fingerings by listOfFingeringsParam {
        arrayListOf(
            Fingering("B3", arrayListOf(X, X, X, X, X, X, X, X, X), 1),
            Fingering("B4", arrayListOf(X, X, X, X, X, X, X, X, X), 2),
            Fingering("C4", arrayListOf(O, X, X, X, X, X, X, X, X), 1),
            Fingering("D4", arrayListOf(O, O, X, X, X, X, X, X, X), 1),
            Fingering("E4", arrayListOf(O, O, O, X, X, X, X, X, X), 1),
            Fingering("F4", arrayListOf(O, X, X, O, X, X, X, X, X), 1),
            Fingering("F#4", arrayListOf(O, O, X, X, O, X, X, X, X), 1),
            Fingering("G4", arrayListOf(O, O, O, O, O, X, X, X, X), 1),
            // ph: Fingering("G#4",arrayListOf(O, O,X,X,X,O,X,X,X), 1),
            Fingering("A4", arrayListOf(O, O, O, O, O, O, X, X, X), 1),
            Fingering("Bb4", arrayListOf(O, O, O, O, X, X, O, X, X), 1),
            Fingering("B4", arrayListOf(O, O, O, O, O, O, O, X, X), 1),
            Fingering("C5", arrayListOf(O, O, O, O, O, O, X, O, X), 1),
            Fingering("C#5", arrayListOf(O, O, O, O, O, O, X, X, O), 1),
            Fingering("D5", arrayListOf(O, O, O, O, O, O, X, O, O), 1), // ph: #?
            Fingering("C5", arrayListOf(O, X, X, X, X, X, X, X, X), 2),
            Fingering("D5", arrayListOf(O, O, X, X, X, X, X, X, X), 2),
            Fingering("E5", arrayListOf(O, O, O, X, X, X, X, X, X), 2),
            Fingering(
                "E5",
                arrayListOf(O, O, O, X, X, X, X, X, O),
                2,
            ), // Register hole exactly at node for E
            Fingering("F5", arrayListOf(O, O, X, O, X, X, X, X, X), 2),
            Fingering("F#5", arrayListOf(O, O, O, X, O, X, X, X, X), 2),
            Fingering("G5", arrayListOf(O, O, O, O, O, X, X, X, X), 2),
            // ph: Fingering("G#5",arrayListOf(O,O,O,X,O,X,X,X), 2),
            Fingering("A5", arrayListOf(O, O, O, O, O, O, X, X, X), 2),
            // ph: Fingering("B5", arrayListOf(O,O,X,X,O,X,X,X), 2),
            // ph: Fingering("C5", arrayListOf(O,O,X,X,O,O,X,X), 2),
            // ph: Fingering("B5", arrayListOf(O,O,O,O,O,O,X,X)),
            // ph: Fingering("C6", arrayListOf(O,O,O,O,O,X,O,X))
        )
    }

    override var divisions by listOfListOfIntDoublePairParam {
        arrayListOf(
            arrayListOf(Pair(4, 0.5)),
            arrayListOf(Pair(1, 0.25), Pair(4, 0.5)),
            arrayListOf(Pair(0, 0.25), Pair(2, 0.5), Pair(5, 0.0)),
        )
    }

    override fun getInstrumentMaker(spec: ReedInstrument): InstrumentMaker<ReedInstrument> {
        return ReedInstrumentMaker(name, outputDir, spec, this)
    }
}

/**
 * Designer for a Bb clarinet (or similar closed-top reed instrument).
 * Defaults are for a standard Boehm-system Bb clarinet.
 * All dimensions in mm.
 */
class ClarinetDesigner(
    override val instrumentName: String,
    outputDir: Path,
) : ReedInstrumentDesigner<ReedInstrument>(instrumentName, outputDir, ReedInstrument.builder) {
    override fun readInstrument(path: Path): ReedInstrument {
        return Json5.decodeFromString(path.readText())
    }

    override fun writeInstrument(instrument: ReedInstrument, path: Path) {
        path.writeText(Json5.encodeToString(instrument))
    }

    override fun getInstrumentMaker(spec: ReedInstrument): InstrumentMaker<ReedInstrument> {
        return ReedInstrumentMaker(name, outputDir, spec, this)
    }

    override fun patchInstrument(inst: Instrument): Instrument {
        val patchedInst = inst.dup()
        patchedInst.trueLength = length
        val reedLength = bore * reedVirtualLength
        val reedTop = bore * reedVirtualTop
        val reed = Profile.makeProfile(listOf(listOf(0.0, bore), arrayListOf(reedLength, reedTop)))
        // Use appendedWith instead of += (plus) to avoid doubling bore diameter
        patchedInst.inner = patchedInst.inner.appendedWith(reed)
        patchedInst.length += reedLength
        return patchedInst
    }

    override var bore by doubleParameter { 14.5 }

    override var initialLength by doubleParameter { wavelength("Bb3") * 0.5 }

    // Cylindrical bore ~14.5mm, with slight bell flare at end
    // Using raw mm values (not boreScaler) since bore=14.5 would over-scale
    override var innerDiameters by listOfDoublePairParameter {
        listOf(14.5, 14.5, 14.5, 14.5, 14.5, 16.0, 20.0, 30.0).map { Pair(it, it) }.toMutableList()
    }

    override var outerDiameters by listOfDoublePairParameter {
        listOf(22.0, 22.0, 22.0, 22.0, 22.0, 28.0, 36.0, 60.0).map { Pair(it, it) }.toMutableList()
    }

    override var initialInnerFractions by listOfDoubleParameter {
        mutableListOf(0.15, 0.4, 0.6, 0.75, 0.85, 0.92)
    }

    override var minInnerFractionSep by listOfDoubleParameter {
        mutableListOf(0.05, 0.05, 0.05, 0.05, 0.05, 0.01, 0.01)
    }

    override var minHoleDiameters by listOfDoubleParameter {
        listOf(4.5, 4.5, 4.5, 4.5, 4.5, 4.5, 4.5, 5.5, 5.5, 5.5, 4.5, 4.5, 4.5, 4.5, 4.5, 4.5, 4.5).toMutableList()
    }

    override var maxHoleDiameters by listOfDoubleParameter {
        listOf(8.0, 8.0, 8.0, 8.0, 8.0, 8.0, 8.0, 10.0, 10.0, 10.0, 8.0, 8.0, 8.0, 8.0, 8.0, 8.0, 8.0).toMutableList()
    }

    override var balance by listOfOptDoubleParameter {
        arrayListOf(0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1)
    }

    override var holeAngles by listOfDoubleParameter {
        arrayListOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    override var holeHorizAngles by listOfDoubleParameter {
        arrayListOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    override var fingerings by listOfFingeringsParam {
        arrayListOf(
            // Lower register (chalumeau)
            Fingering("D3",  arrayListOf(X, X, X, X, X, X, X, X, X, X, X, X, X, X, X, X, X)),
            Fingering("Eb3", arrayListOf(O, X, X, X, X, X, X, X, X, X, X, X, X, X, X, X, X)),
            Fingering("E3",  arrayListOf(X, O, X, X, X, X, X, X, X, X, X, X, X, X, X, X, X)),
            Fingering("F3",  arrayListOf(X, X, O, X, X, X, X, X, X, X, X, X, X, X, X, X, X)),
            Fingering("F#3", arrayListOf(X, X, X, O, X, X, X, X, X, X, X, X, X, X, X, X, X)),
            Fingering("G3",  arrayListOf(X, X, X, X, O, X, X, X, X, X, X, X, X, X, X, X, X)),
            Fingering("Ab3", arrayListOf(X, X, X, X, X, O, X, X, X, X, X, X, X, X, X, X, X)),
            Fingering("A3",  arrayListOf(X, X, X, X, X, X, O, X, X, X, X, X, X, X, X, X, X)),
            Fingering("Bb3", arrayListOf(X, X, X, X, X, X, X, O, X, X, X, X, X, X, X, X, X)),
            Fingering("B3",  arrayListOf(X, X, X, X, X, X, X, X, O, X, X, X, X, X, X, X, X)),
            Fingering("C4",  arrayListOf(X, X, X, X, X, X, X, X, X, O, X, X, X, X, X, X, X)),
            // Upper register (clarion)
            Fingering("D4",  arrayListOf(X, X, X, X, X, X, X, O, X, X, X, O, O, O, X, X, X)),
            Fingering("Eb4", arrayListOf(O, X, X, X, X, X, X, O, X, X, X, O, O, O, X, X, X)),
            Fingering("E4",  arrayListOf(X, O, X, X, X, X, X, O, X, X, X, O, O, O, X, X, X)),
            Fingering("F4",  arrayListOf(X, X, O, X, X, X, X, O, X, X, X, O, O, O, X, X, X)),
            Fingering("F#4", arrayListOf(X, X, X, O, X, X, X, O, X, X, X, O, O, O, X, X, X)),
            Fingering("G4",  arrayListOf(X, X, X, X, O, X, X, O, X, X, X, O, O, O, X, X, X)),
            Fingering("Ab4", arrayListOf(X, X, X, X, X, O, X, O, X, X, X, O, O, O, X, X, X)),
            Fingering("A4",  arrayListOf(X, X, X, X, X, X, O, O, X, X, X, O, O, O, X, X, X)),
            Fingering("Bb4", arrayListOf(X, X, X, X, X, X, X, X, X, X, O, O, O, O, X, X, X)),
            Fingering("B4",  arrayListOf(X, X, X, X, X, X, X, X, X, O, X, O, O, O, O, X, X)),
            Fingering("C5",  arrayListOf(X, X, X, X, X, X, X, X, X, X, O, O, O, O, O, X, X)),
            Fingering("C#5", arrayListOf(X, X, X, X, X, X, X, X, X, X, O, O, O, O, O, O, X)),
            Fingering("D5",  arrayListOf(X, X, X, X, X, X, X, O, X, X, X, O, O, O, X, X, O)),
        )
    }

    override var initialHoleFractions by listOfDoubleParameter {
        // Spread holes along the length; register key near top
        mutableListOf(0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.95)
    }

    override var initialHoleDiameterFractions by listOfDoubleParameter {
        (0 until 17).map { 0.5 }.toMutableList()
    }

    override var minHoleSpacing by listOfOptDoubleParameter {
        (0 until 16).map { 8.0 }.toMutableList()
    }

    override var maxHoleSpacing by listOfOptDoubleParameter {
        mutableListOf(50.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0,
            50.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0, 50.0)
    }

    override var divisions by listOfListOfIntDoublePairParam {
        listOf(
            listOf(Pair(5, 0.0)),
            listOf(Pair(2, 0.0), Pair(5, 0.333)),
            listOf(Pair(-1, 0.9), Pair(2, 0.0), Pair(5, 0.333)),
            listOf(Pair(-1, 0.9), Pair(2, 0.0), Pair(5, 0.0), Pair(5, 0.7)),
        )
    }
}

fun clarinetDesigner(name: String, outputDir: Path): ClarinetDesigner {
    return ClarinetDesigner(name, outputDir)
}

/**
 * Designer for a shawm/haut-bois/oboe/bombard with a simple fingering system and compact hole
 * placement. The flare at the end is purely decorative.
 */
class FolkShawmDesigner(
    override val instrumentName: String,
    outputDir: Path,
) : AbstractShawmDesigner(instrumentName, outputDir) {
    override var minHoleDiameters by listOfDoubleParameter {
        boreScaler(listOf(2.0).repeat(7))
    }

    override var maxHoleSpacing by listOfOptDoubleParameter {
        scaler(listOf(80.0, 40.0, 40.0, 40.0, 40.0, 20.0))
    }

    override var maxHoleDiameters by listOfDoubleParameter {
        boreScaler(listOf(12.0).repeat(7))
    }

    override var initialHoleDiameterFractions by listOfDoubleParameter {
        inRange(1.0, 0.5, numberOfHoles)
    }

    override var initialHoleFractions by listOfDoubleParameter {
        (numberOfHoles - 1 downTo 0).map { i ->
            0.75 - 0.1 * i.toDouble()
        }
    }

    override var balance by listOfOptDoubleParameter { arrayListOf(null, 0.05, null, null, 0.05) }

    override var holeHorizAngles by listOfDoubleParameter {
        listOf(45.0) + listOf(0.0).repeat(numberOfHoles - 1)
    }

    override var fingerPads by listOfBooleanParameter {
        listOf(false) + listOf(true).repeat(it.numberOfHoles - 1)
    }

    override var initialLength by doubleParameter { wavelength("C4") * 0.5 }

    override var fingerings: List<Fingering> by listOfFingeringsParam {
        arrayListOf(
            Fingering("C4", arrayListOf(X, X, X, X, X, X, X), 1),
            Fingering("C5", arrayListOf(X, X, X, X, X, X, X), 2),
            Fingering("C4*3", arrayListOf(X, X, X, X, X, X, X), 3),
            Fingering("C4*4", arrayListOf(X, X, X, X, X, X, X), 4),
            Fingering("D4", arrayListOf(O, X, X, X, X, X, X), 1),
            Fingering("E4", arrayListOf(O, O, X, X, X, X, X), 1),
            Fingering("F#4", arrayListOf(O, O, O, X, X, X, X), 1),
            Fingering("G4", arrayListOf(O, O, O, O, X, X, X), 1),
            Fingering("A4", arrayListOf(O, O, O, O, O, X, X), 1),
            Fingering("B4", arrayListOf(O, O, O, O, O, O, X), 1),
            // ph: Fingering("C5",     arrayListOf(O, O,O,O,X,X,O), 1),
            Fingering("C#5", arrayListOf(O, O, O, O, O, O, O), 1),
            Fingering("D5", arrayListOf(O, X, X, X, X, X, O), 2),
            Fingering("D5", arrayListOf(O, X, X, X, X, X, X), 2),
            Fingering("E5", arrayListOf(O, O, X, X, X, X, X), 2),
            // ph: Fingering("E5",     arrayListOf(O,X,X,X,X,O)),
            Fingering("F#5", arrayListOf(O, O, O, X, X, X, X), 2),
            Fingering("G5", arrayListOf(O, O, O, O, X, X, X), 2),
            Fingering("A5", arrayListOf(O, O, O, O, O, X, X), 2),
            Fingering("B5", arrayListOf(O, O, O, O, O, O, X), 2),
            Fingering("C#6", arrayListOf(O, O, O, O, O, O, O), 2),
            // ph: Fingering("D6",    arrayListOf(O, X,X,X,X,X,X), 4),
            // ph: Fingering("D4*3",  arrayListOf(O, X,X,X,X,X,X), 3),
            // ph: Fingering("E4*3",  arrayListOf(O, O,X,X,X,X,X), 3),
            // ph: Fingering("F#4*3", arrayListOf(O, O,O,X,X,X,X), 3),
            // ph: Fingering("G4*3",  arrayListOf(O, O,O,O,X,X,X), 3),
            // ph: Fingering("A4*3",  arrayListOf(O, O,O,O,O,X,X), 3),
            // ph: Fingering("B4*3",  arrayListOf(O, O,O,O,O,O,X), 3),
            // ph: Fingering("C#5*3", arrayListOf(O, O,O,O,O,O,O), 3),
            // ph: Fingering("D4*4",  arrayListOf(O, X,X,X,X,X,X), 4),
            // ph: Fingering("E4*4",  arrayListOf(O, O,X,X,X,X,X), 4),
            // ph: Fingering("F#4*4", arrayListOf(O, O,O,X,X,X,X), 4),
            // ph: Fingering("G4*4",  arrayListOf(O, O,O,O,X,X,X), 4),
            // ph: Fingering("A4*4",  arrayListOf(O, O,O,O,O,X,X), 4),
            // ph: Fingering("B4*4",  arrayListOf(O, O,O,O,O,O,X), 4),
            // ph: Fingering("C#5*4", arrayListOf(O, O,O,O,O,O,O), 4),
        )
    }

    override var divisions by listOfListOfIntDoublePairParam {
        listOf(
            arrayListOf(Pair(3, 0.5)),
            arrayListOf(Pair(0, 0.5), Pair(3, 0.0)),
            arrayListOf(Pair(0, 0.5), Pair(3, 0.25), Pair(5, 0.5)),
            arrayListOf(Pair(-1, 0.5), Pair(0, 0.5), Pair(3, 0.25), Pair(5, 0.5)),
            // ph:arrayListOf( (-X,0.45), (-X,0.9), (2,0.0), (2,0.9), (5,0.0), (5,0.5) ),
        )
    }

    override fun getInstrumentMaker(spec: ReedInstrument): InstrumentMaker<ReedInstrument> {
        return ReedInstrumentMaker(name, outputDir, spec, this)
    }
}
