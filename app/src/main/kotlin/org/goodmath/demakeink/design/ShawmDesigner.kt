package org.goodmath.demakeink.design

import org.goodmath.demakeink.util.repeat

open class Shawm: Instrument() {

    companion object {
        fun generator(): InstrumentGenerator<Shawm> {
            return object: InstrumentGenerator<Shawm> {
                override fun create(): Shawm {
                    return Shawm()
                }
            }
        }
    }
}

open class ReedInstrumentDesigner<T: Instrument>(gen: InstrumentGenerator<T>): InstrumentDesigner<T>(gen) {

    val boreBaseline = 4.0

    var bore: Double by ConfigurationParameter("Bore diameter at top. (ie reed diameter)") {
        4.0
    }

    // ph: reed_virtual_length = 25.0
    // ph: reed_virtual_top = 1.0
    // ph: reed_virtual_length = 50.0
    // ph: reed_virtual_top = 0.125

    // ph: From c5 drone
    var reedVirtualLength: Double by ConfigurationParameter("Virtual length of reed, as a multiple of bore diameter.") {
        34.0
    }

    var reedVirtualTop: Double by ConfigurationParameter("Virtual diameter of top of reed, proportion of bore diameter.") {
        1.0
    }

    init {
        transpose = 0
        closedTop = true
    }

    override fun patchInstrument(origInst: T): T {
        val inst = origInst.copy() as T
        // MarkCC: ph originally had a "true_length" and "true_inner" defined
        // here. But "true_inner" was never used, so I dropped it. True_length
        // was just initialized to "length" before length was modified.
        inst.trueLength = length
        val reedLength = bore * reedVirtualLength
        val reedTop = bore * reedVirtualTop
        val reed = Profile.makeProfile(
                listOf(listOf(0.0, bore),
                        listOf(reedLength, reedTop)))
        inst.inner += reed
        inst.length += reedLength
        return inst
    }



    fun boreScaler(value: List<Double>): List<Double> {
        val scale = bore / boreBaseline
        return value.map { it * scale }
    }

    companion object {
        fun inRange(low: Double,high: Double,n: Int): List<Double> {
            return (1 until n+1).map {
                val i = it.toDouble()
                (i+1.0)*(high-low)/(n+2)+low
            }
        }

        fun fullRange(low: Double,high: Double,n: Int):List<Double> {
            return (0 until n).map {
                val i = it.toDouble()
                i * (high - low) / (n - 1.0) + low
            }
        }

    }
}

class ReedDrone: Instrument() {
    companion object {
        val gen = object: InstrumentGenerator<ReedDrone> {
            override fun create(): ReedDrone {
                return ReedDrone()
            }
        }
    }
}
class ReedDroneDesigner: ReedInstrumentDesigner<ReedDrone>(ReedDrone.gen) {
    init {
        initialLength = wavelength("C4") * 0.25
        innerDiameters = boreScaler(listOf(4.0, 4.0)).map { Pair(it, it) }
        outerDiameters = boreScaler(listOf(24.0, 12.0)).map { Pair(it, it) }
        holeHorizAngles = listOf()

        // I"m not sure what this is for; I"m going to leave it out until I"m
        // sure it does something.
        // withFingerPad = listOf()

        fingerings = listOf(Fingering("C4", ArrayList<Double>(), 1.0))
        divisions = emptyList<List<Pair<Int, Double>>>()
    }
}


class Reedpipe: Instrument() {

}
open class ReedpipeDesigner<T: Reedpipe>(gen: InstrumentGenerator<T>): ReedInstrumentDesigner<T>(gen) {
    override var innerDiameters: List<Pair<Double, Double>> by ConfigurationParameter {
        boreScaler(listOf(4.0, 4.0)).map { Pair(it, it) }
    }

    override var outerDiameters: List<Pair<Double, Double>> by ConfigurationParameter {
        boreScaler(listOf(12.0, 12.0)).map { Pair(it, it) }
    }

    override var minHoleDiameters: List<Double> by ConfigurationParameter {
        boreScaler(listOf(2.5).repeat(8))
    }

    override var maxHoleDiameters: List<Double> by ConfigurationParameter {
        boreScaler(listOf(4.0).repeat(8))
    }

    // ph: max_hole_spacing = design.scaler([ 8O, 4O,4O,4O,None,4O,4O, 20 ])

    override var balance: List<Double?> by ConfigurationParameter {
        listOf(0.2, 0.075, 0.3, 0.3, 0.075, null)
    }

    // ph: balance = [ None, 0.1, 0.1, 0.3, 0.3, 0.1, None ]
    // ph: balance = [ 0.2, 0.1, None, None, 0.1, None ]

    override var holeAngles: List<Double> by ConfigurationParameter {
        listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    override var holeHorizAngles: List<Double> by ConfigurationParameter {
        listOf(-25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 180.0)
    }

    // with_fingerpad = [1,1,1,1,1,1,1,1]

    override var initialLength: Double by ConfigurationParameter {
        wavelength("C4") * 0.25
    }

    override var fingerings: List<Fingering> by ConfigurationParameter {
        listOf(Fingering("C4", listOf(X, X, X, X, X, X, X, X), 1.0),
                Fingering("D4", listOf(O, X, X, X, X, X, X, X), 1.0),
                Fingering("E4", listOf(O, O, X, X, X, X, X, X), 1.0),
                Fingering("F4", listOf(O, O, O, X, X, X, X, X), 1.0),
                Fingering("G4", listOf(O, O, O, O, X, X, X, X), 1.0),
                Fingering("A4", listOf(O, O, O, O, O, X, X, X), 1.0),
                // ph: ("Bb4", [O,O,O,X,X,O,X,X), 1),
                Fingering("B4", listOf(O, O, O, O, O, O, X, X), 1.0),
                Fingering("C5", listOf(O, O, O, O, O, X, O, X), 1.0),
                // ph: ("C#5", [O,O,O,O,O,X,X,O), 1),
                Fingering("D5", listOf(O, O, O, O, O, X, O, O), 1.0))
    }


    override var divisions: List<List<Pair<Int, Double>>> by ConfigurationParameter {
        // ph: [ (3, 0.5) ],
        // ph: [ (3, 0.5), (7, 0.1) ],
        listOf(
                listOf(Pair(0, 0.5), Pair(3, 0.5), Pair(7, 0.1))
        )
    }
}

abstract class AbstractShawmDesigner<Inst: Shawm>(gen: InstrumentGenerator<Inst>): ReedInstrumentDesigner<Inst>(gen) {
    override var innerDiameters: List<Pair<Double, Double>> by ConfigurationParameter {
        boreScaler(fullRange(16.0, 4.0, 10)).map { Pair(it, it) }
        // ph: listOf(  16.0, 14.0, 12.0, 10.0, 8.0, 6.0, 4.0, 1.5 )
    }
    override var initialInnerFractions: List<Double?> by ConfigurationParameter {
        fullRange(0.2, 0.9, 8)
    }
    override var minInnerFractionSep: List<Double> by ConfigurationParameter {
        listOf(0.02).repeat(9)
    }
    override var outerDiameters: List<Pair<Double, Double>> by ConfigurationParameter {
        boreScaler(listOf(70.0, 25.0, 25.0)).map { Pair(it, it) }
    }
    override var minOuterFractionSep: List<Double> by ConfigurationParameter {
        listOf(0.19, 0.8)
    }
    override var initialOuterFractions: List<Double?> by ConfigurationParameter {
        listOf(0.19)
    }
    override var outerAngles: List<Pair<Angle, Angle>?> by ConfigurationParameter {
        listOf(Angle(null, -35.0), Angle(AngleDirection.Up), Angle(AngleDirection.Down)).map { Pair(it, it) }
    }

}

/**
 * Designer for a shawm/haut-bois/oboe/bombard with a fingering system similar to recorder.
 *
 * The flare at the end is purely decorative.
 */
open class ShawmDesigner<T: Shawm>(gen: InstrumentGenerator<T>): AbstractShawmDesigner<T>(gen) {

    override var minHoleDiameters: List<Double>  by ConfigurationParameter {
        boreScaler(listOf(2.0).repeat(9))
    }

    override var maxHoleDiameters: List<Double>  by ConfigurationParameter {
        // ph:  max_hole_diameters = bore_scaler([ 12.0 ] * 8)
        boreScaler(listOf(6.0).repeat(9))
    }

    override var initialHoleDiameterFractions: List<Double?> by ConfigurationParameter {
        listOf(0.5).repeat(9)
    }

    override var initialHoleFractions: List<Double?> by ConfigurationParameter {
        listOf(7, 6, 5, 4, 3, 2, 1, 0, 0).map { i -> 0.5-(0.6*i.toDouble()) }
    }


    override var maxHoleSpacing: List<Double?> by ConfigurationParameter {
        scaler(listOf(80.0, 40.0, 40.0, 40.0, null, 40.0, 40.0, 20.0))
    }

    override var balance: List<Double?> by ConfigurationParameter {
        // ph: balance = [0.2, 0.1, 0.3, 0.3, 0.1, None]
        listOf(null, 0.1, 0.1, 0.3, 0.3, 0.1, null)
        // ph: balance = [0.2, 0.1, None, None, 0.1, None]
    }

    override var holeAngles: List<Double> by ConfigurationParameter {
        listOf(0.0, -30.0, -30.0, -30.0, 30.0, 0.0, 0.0, 0.0, 0.0)
    }

    override var holeHorizAngles: List<Double> by ConfigurationParameter {
        listOf(30.0, -25.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 180.0)
    }


    // with_fingerpad = [0,1,1,1,1,1,1,1,1]

    override var initialLength: Double by ConfigurationParameter{
        wavelength("B3") * 0.35
    }


    override var fingerings: List<Fingering> by ConfigurationParameter {
        listOf(
                Fingering("B3", listOf(X, X, X, X, X, X, X, X, X), 1.0),
                Fingering("B4", listOf(X, X, X, X, X, X, X, X, X), 2.0),
                Fingering("C4", listOf(O, X, X, X, X, X, X, X, X), 1.0),
                Fingering("D4", listOf(O, O, X, X, X, X, X, X, X), 1.0),
                Fingering("E4", listOf(O, O, O, X, X, X, X, X, X), 1.0),
                Fingering("F4", listOf(O, X, X, O, X, X, X, X, X), 1.0),
                Fingering("F#4", listOf(O, O, X, X, O, X, X, X, X), 1.0),
                Fingering("G4", listOf(O, O, O, O, O, X, X, X, X), 1.0),
                // ph: Fingering("G#4", listOf(O, O,X,X,X,O,X,X,X), 1),
                Fingering("A4", listOf(O, O, O, O, O, O, X, X, X), 1.0),
                Fingering("Bb4", listOf(O, O, O, O, X, X, O, X, X), 1.0),
                Fingering("B4", listOf(O, O, O, O, O, O, O, X, X), 1.0),
                Fingering("C5", listOf(O, O, O, O, O, O, X, O, X), 1.0),
                Fingering("C#5", listOf(O, O, O, O, O, O, X, X, O), 1.0),
                Fingering("D5", listOf(O, O, O, O, O, O, X, O, O), 1.0), // ph: #?
                Fingering("C5", listOf(O, X, X, X, X, X, X, X, X), 2.0),
                Fingering("D5", listOf(O, O, X, X, X, X, X, X, X), 2.0),
                Fingering("E5", listOf(O, O, O, X, X, X, X, X, X), 2.0),
                Fingering("E5", listOf(O, O, O, X, X, X, X, X, O), 2.0), // Register hole exactly at node for E
                Fingering("F5", listOf(O, O, X, O, X, X, X, X, X), 2.0),
                Fingering("F#5", listOf(O, O, O, X, O, X, X, X, X), 2.0),
                Fingering("G5", listOf(O, O, O, O, O, X, X, X, X), 2.0),
                // ph: Fingering("G#5", listOf(O,O,O,X,O,X,X,X), 2),
                Fingering("A5", listOf(O, O, O, O, O, O, X, X, X), 2.0),
                // ph: Fingering("B5",  listOf(O,O,X,X,O,X,X,X), 2),
                // ph: Fingering("C5",  listOf(O,O,X,X,O,O,X,X), 2),
                // ph: Fingering("B5",  listOf(O,O,O,O,O,O,X,X)),
                // ph: Fingering("C6",  listOf(O,O,O,O,O,X,O,X))
        )
    }

    override var divisions: List<List<Pair<Int, Double>>> by ConfigurationParameter {
        listOf(
                listOf(Pair(4, 0.5)),
                listOf(Pair(1, 0.25), Pair(4, 0.5)),
                listOf(Pair(0, 0.25), Pair(2, 0.5), Pair(5, 0.0))
        )
    }
}

/**
 * Designer for a shawm/haut-bois/oboe/bombard with a simple fingering
 * system and compact hole placement.
 * The flare at the end is purely decorative.
 */
class FolkShawmDesigner<Inst: Shawm>(gen: InstrumentGenerator<Inst>): AbstractShawmDesigner<Inst>(gen) {


    override var minHoleDiameters: List<Double> by ConfigurationParameter {
        boreScaler(listOf(12.0).repeat(7))
    }

    override var maxHoleDiameters: List<Double> by ConfigurationParameter {
        boreScaler(listOf(12.0).repeat(7))
    }

    override var initialHoleDiameterFractions: List<Double?> by ConfigurationParameter {
        inRange(1.0, 0.5, 7)
    }

    override var initialHoleFractions: List<Double?> by ConfigurationParameter {
        listOf(6, 5, 4, 3, 2, 1, 0).map { i -> 0.75 - 0.1 * i.toDouble() }
    }

    override var balance: List<Double?> by ConfigurationParameter {
        listOf(null, 0.05, null, null, 0.05)
    }

    override var holeHorizAngles: List<Double> by ConfigurationParameter {
        listOf(45.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    // with_fingerpad = [0,1,1,1,1,1,1]

    override var initialLength: Double by ConfigurationParameter {
        wavelength("C4") * 0.5
    }

    override var fingerings: List<Fingering> by ConfigurationParameter {
        listOf(
                Fingering("C4", listOf(X, X, X, X, X, X, X), 1.0),
                Fingering("C5", listOf(X, X, X, X, X, X, X), 2.0),
                Fingering("C4*3", listOf(X, X, X, X, X, X, X), 3.0),
                Fingering("C4*4", listOf(X, X, X, X, X, X, X), 4.0),
                Fingering("D4", listOf(O, X, X, X, X, X, X), 1.0),
                Fingering("E4", listOf(O, O, X, X, X, X, X), 1.0),
                Fingering("F#4", listOf(O, O, O, X, X, X, X), 1.0),
                Fingering("G4", listOf(O, O, O, O, X, X, X), 1.0),
                Fingering("A4", listOf(O, O, O, O, O, X, X), 1.0),
                Fingering("B4", listOf(O, O, O, O, O, O, X), 1.0),
                // ph: Fingering("C5",      listOf(O, O,O,O,X,X,O), 1),
                Fingering("C#5", listOf(O, O, O, O, O, O, O), 1.0),
                Fingering("D5", listOf(O, X, X, X, X, X, O), 2.0),
                Fingering("D5", listOf(O, X, X, X, X, X, X), 2.0),
                Fingering("E5", listOf(O, O, X, X, X, X, X), 2.0),
                // ph: Fingering("E5",      listOf(O,X,X,X,X,O)),
                Fingering("F#5", listOf(O, O, O, X, X, X, X), 2.0),
                Fingering("G5", listOf(O, O, O, O, X, X, X), 2.0),
                Fingering("A5", listOf(O, O, O, O, O, X, X), 2.0),
                Fingering("B5", listOf(O, O, O, O, O, O, X), 2.0),
                Fingering("C#6", listOf(O, O, O, O, O, O, O), 2.0),
                // ph: Fingering("D6",     listOf(O, X,X,X,X,X,X), 4),
                // ph: Fingering("D4*3",   listOf(O, X,X,X,X,X,X), 3),
                // ph: Fingering("E4*3",   listOf(O, O,X,X,X,X,X), 3),
                // ph: Fingering("F#4*3",  listOf(O, O,O,X,X,X,X), 3),
                // ph: Fingering("G4*3",   listOf(O, O,O,O,X,X,X), 3),
                // ph: Fingering("A4*3",   listOf(O, O,O,O,O,X,X), 3),
                // ph: Fingering("B4*3",   listOf(O, O,O,O,O,O,X), 3),
                // ph: Fingering("C#5*3",  listOf(O, O,O,O,O,O,O), 3),
                // ph: Fingering("D4*4",   listOf(O, X,X,X,X,X,X), 4),
                // ph: Fingering("E4*4",   listOf(O, O,X,X,X,X,X), 4),
                // ph: Fingering("F#4*4",  listOf(O, O,O,X,X,X,X), 4),
                // ph: Fingering("G4*4",   listOf(O, O,O,O,X,X,X), 4),
                // ph: Fingering("A4*4",   listOf(O, O,O,O,O,X,X), 4),
                // ph: Fingering("B4*4",   listOf(O, O,O,O,O,O,X), 4),
                // ph: Fingering("C#5*4",  listOf(O, O,O,O,O,O,O), 4),
        )
    }

    override var divisions: List<List<Pair<Int, Double>>> by ConfigurationParameter {
        listOf(listOf(Pair(3, 0.5)),
                listOf(Pair(0, 0.5), Pair(3, 0.0)),
                listOf(Pair(0, 0.5), Pair(3, 0.25), Pair(5, 0.5)),
                listOf(Pair(-1, 0.5), Pair(0, 0.5), Pair(3, 0.25), Pair(5, 0.5))
                // ph: listOf( (-X,0.45), (-X,0.9), (2,0.0), (2,0.9), (5,0.0), (5,0.5) ),
        )
    }
}
