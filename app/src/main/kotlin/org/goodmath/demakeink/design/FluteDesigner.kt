package org.goodmath.demakeink.design

import org.goodmath.demakeink.util.repeat
import kotlin.math.pow
import kotlin.math.sqrt

open class Flute : Instrument() {

    companion object {
        val generator =
                object : InstrumentGenerator<Flute> {
                    override fun create(): Flute {
                        return Flute()
                    }
                }
    }
}

open class FluteDesigner<T: Flute>(gen: InstrumentGenerator<T>) : InstrumentDesigner<T>(gen) {

    /* ph:
    # 2.5/8 = 0.3    ~ 20 cents flat
    # 5/8 = 0.6      ~ too high
    # 0.45 (sop_flute_5)   ~ a tiny bit low
    # 0.53 (sop_flute_8)   ~ a tiny bit low?, needed to push cork in 1.5mm
    #                        + perfect, printed plastic sop flute
    # 0.56                 ~ definitely high, printed plastic sop flute
    */
    open var embExtra: Double by ConfigurationParameter(
            "Constant controlling extra effective height of the embouchure hole due to lips, etc.\n" +
                    "Small adjustments of this value will change the angle at which the flute needs to be blown\n" +
                    "in order to be in tune.") { 0.53 }

    init {
        closedTop = true
        initialLength = wavelength("D4") * 0.5
        numberOfHoles = 7

    }

    override fun patchInstrument(inst: T): T {
        val newInst = inst.copy()
        inst.holeLengths[inst.holeLengths.size - 1] += (inst.holeDiameters.fromEnd(1) * embExtra)
        return inst
    }

    override fun calcEmission(emission: ArrayList<Double>, fingers: ArrayList<Double>): Double {
        /* ph:  Emission is relative to embouchure hole, ie we assume the amplitude at the
         * embouchure hole is fixed
         */
        return sqrt(emission.subList(0, emission.size - 1).map { it * it }.sum() / emission.fromEnd(-1))
    }

    override var initialHoleFractions: List<Double?> by ConfigurationParameter("initialHoleFractions") { it ->
        val l = ArrayList((0 until numberOfHoles - 1).map { i -> 0.175 + 0.5 * i / (numberOfHoles - 1) })
        l.add(0.97)
        l
    }

    // ph:
    //    min_hole_diameters = design.sqrt_scaler([ 6.5 ] * 6  + [ 12.2 ])
    //    max_hole_diameters = design.sqrt_scaler([ 11.4 ] * 6 + [ 13.9 ])
    //    max_hole_diameters = design.sqrt_scaler([ 11.4 ] * 6 + [ 10.5 ])
    //
    //    min_hole_diameters = design.power_scaler(1/3., [ 3.0 ] * 6  + [ 11.3 ])
    //    max_hole_diameters = design.power_scaler(1/3., [ 11.4 ] * 6 + [ 11.4 ])

    override var minHoleDiameters: List<Double> by ConfigurationParameter("minHoleDiameters") {
        val x = ArrayList(listOf(3.0).repeat(numberOfHoles - 1))
        x.add(11.3)
        val scale = this.scale.pow(1.0 / 3.0)
        x.map { it * scale }
    }

    override var maxHoleDiameters: List<Double> by ConfigurationParameter("maxHoleDiameters") {
        val x = ArrayList(listOf(11.4).repeat(numberOfHoles - 1))
        x.add(11.4)
        val scale = this.scale.pow(1.0 / 3.0)
        x.map { it * scale }
    }


    // ph: These assume a six hole flute, and must be overridden on flutes with a different number of holes:


    companion object {
        val pFluteFingers =
                listOf(
                        Fingering("D4", arrayListOf(X, X, X, X, X, X)),
                        Fingering("E4", arrayListOf(O, X, X, X, X, X)),
                        Fingering("F4", arrayListOf(X, O, X, X, X, X)),
                        Fingering("F#4", arrayListOf(X, X, O, X, X, X)),
                        Fingering("G4", arrayListOf(O, O, O, X, X, X)),
                        Fingering("G#4", arrayListOf(X, X, X, O, X, X)),
                        Fingering("A4", arrayListOf(O, O, O, O, X, X)),
                        Fingering("Bb4", arrayListOf(O, O, X, X, O, X)),
                        Fingering("B4", arrayListOf(O, O, O, O, O, X)),
                        Fingering("C5", arrayListOf(O, O, O, X, X, X)),
                        Fingering(
                                "C#5",
                                arrayListOf(O, O, O, O, O, O)
                        ), // ph: Note: O,O,X,O,O,0 lets you put a finger dow
                        Fingering("D5", arrayListOf(X, X, X, X, X, O)),
                        Fingering("D5", arrayListOf(X, X, X, X, X, X)),
                        Fingering("E5", arrayListOf(O, X, X, X, X, X)),
                        Fingering("F5", arrayListOf(X, O, X, X, X, X)),
                        Fingering("F#5", arrayListOf(O, X, O, X, X, X)),
                        Fingering("G5", arrayListOf(O, O, O, X, X, X)),
                        // pf: Fingering("G#5",  arrayListOf(O,O,X,O,X,X)),
                        Fingering("A5", arrayListOf(O, O, O, O, X, X)),
                        Fingering("Bb5", arrayListOf(X, X, X, O, X, X)),
                        Fingering("B5", arrayListOf(O, X, X, O, X, X)),

                        // pf:        Fingering("C6",   arrayListOf(X,X,X,O,X,O)),  # X,O,X,O,X,0 may also be
                        // good
                        Fingering("C6", arrayListOf(O, X, X, O, O, X)),
                        // pf: Fingering("C#6",  arrayListOf(X,X,X,O,O,O)),

                        Fingering("D6", arrayListOf(X, X, X, X, X, X)),
                        // pf: Fingering("E6",   arrayListOf(O,X,O,O,X,X)),
                )

        val folkFingerings =
                listOf(
                        Fingering("D4", arrayListOf(X, X, X, X, X, X)),
                        Fingering("E4", arrayListOf(O, X, X, X, X, X)),
                        Fingering("F#4", arrayListOf(O, O, X, X, X, X)),
                        Fingering("G4", arrayListOf(O, O, O, X, X, X)),
                        Fingering("A4", arrayListOf(O, O, O, O, X, X)),
                        Fingering("B4", arrayListOf(O, O, O, O, O, X)),
                        Fingering("C5", arrayListOf(O, O, O, X, X, O)),
                        Fingering("C#5", arrayListOf(O, O, O, O, O, O)),
                        Fingering("D5", arrayListOf(X, X, X, X, X, O)),
                        Fingering("E5", arrayListOf(O, X, X, X, X, X)),
                        Fingering("F#5", arrayListOf(O, O, X, X, X, X)),
                        Fingering("G5", arrayListOf(O, O, O, X, X, X)),
                        Fingering("A5", arrayListOf(O, O, O, O, X, X)),
                        Fingering("B5", arrayListOf(O, O, O, O, O, X)),
                        // ph: #Fingering("C#6", arrayListOf(X,X,X,O,O,O)),
                        Fingering("C#6", arrayListOf(O, O, O, O, O, O)),
                        Fingering("D6", arrayListOf(X, X, X, X, X, O)),

                        // ph:  Fingering("D4*3", arrayListOf(X,X,X,X,X,X)),
                        // ph:  Fingering("E4*3", arrayListOf(O,X,X,X,X,X)),
                        // ph:  Fingering("F#4*3", arrayListOf(O,O,X,X,X,X)),
                        // ph:  Fingering("G4*3", arrayListOf(O,O,O,X,X,X)),
                        // ph:  Fingering("A4*3", arrayListOf(O,O,O,O,X,X)),
                        // ph:Fingering("B4*3", arrayListOf(O,O,O,O,O,X)),
                        // ph:Fingering("C#5*3", arrayListOf(O,O,O,O,O,O)),

                        // ph:Fingering("E6", arrayListOf(O,X,X,X,X,X)),
                        // ph:Fingering("F6", arrayListOf(X,O,X,X,X,X)),
                        // ph:Fingering("G6", arrayListOf(X,O,O,X,X,X)),
                        // ph:Fingering("A6", arrayListOf(O,X,X,X,X,O)), #?
                )

        val dorianFingerings =
                listOf(
                        Fingering("D4", arrayListOf(X, X, X, X, X, X)),
                        Fingering("E4", arrayListOf(O, X, X, X, X, X)),
                        Fingering("F4", arrayListOf(O, O, X, X, X, X)),
                        Fingering("G4", arrayListOf(O, O, O, X, X, X)),
                        Fingering("A4", arrayListOf(O, O, O, O, X, X)),
                        Fingering("Bb4", arrayListOf(O, O, X, X, O, X)),
                        Fingering("B4", arrayListOf(O, O, O, O, O, X)),
                        Fingering("C5", arrayListOf(O, O, O, O, O, O)),
                        Fingering("D5", arrayListOf(X, X, X, X, X, X)),
                        Fingering("E5", arrayListOf(O, X, X, X, X, X)),
                        Fingering("F5", arrayListOf(O, O, X, X, X, X)),
                        Fingering("G5", arrayListOf(O, O, O, X, X, X)),
                        Fingering("A5", arrayListOf(O, O, O, O, X, X)),
                        Fingering("Bb5", arrayListOf(O, O, O, X, O, X)),
                        Fingering("B5", arrayListOf(O, O, O, O, O, X)),
                        Fingering("C6", arrayListOf(O, O, O, O, O, O)),
                        Fingering("D6", arrayListOf(X, X, X, X, X, X)),
                )

        fun fingeringsWithEmbouchure(fingers: List<Fingering>): List<Fingering> {
            return fingers.map { old ->
                Fingering(old.noteName,
                        ArrayList(old.fingers + O),
                        old.nth)
            }
        }
    }
}

open class TaperedFluteDesigner<T: Flute>(gen: InstrumentGenerator<T>): FluteDesigner<T>(gen) {
    open var innerTaper: Double by ConfigurationParameter("Amount of tapering of bore. Smaller = more tapered.") {
        0.75
    }

    open var outerTaper: Double by ConfigurationParameter("Amount of tapering of exterior. Smaller = more tapered.") {
        0.85
    }

    // ph: inner_diameters = design.sqrt_scaler([ 14.0, 14.0, 18.4, 21.0, 18.4, 18.4 ])
    override var innerDiameters: List<Pair<Double, Double>> by ConfigurationParameter("innerDiameters") {
        val myScale = scale.pow(1.0 / 2.0)
        listOf(18.4 * innerTaper * myScale,
                18.4 * innerTaper * myScale,
                18.4 * (0.5 + innerTaper * 0.5) * myScale,
                18.4 * myScale,
                21.0 * myScale,
                21.0 * myScale,
                18.4 * myScale,
                18.4 * myScale).map { Pair(it, it)}
    }

    // ph: initial_inner_fractions = [ 0.25, 0.75 ]
    // ph: min_inner_fraction_sep = [ 0.0, 0.0, 0.0 ]

    override var initialInnerFractions: List<Double?> by ConfigurationParameter("initialInnerFractions") {
        listOf(0.25, 0.3, 0.7, 0.8, 0.81, 0.9)
    }

    override var minInnerFractionSep: List<Double> by ConfigurationParameter("minInnerFractionSep") {
        listOf(0.01, 0.1, 0.1, 0.01, 0.01, 0.01, 0.01)
    }


    // ph: outer_diameters = design.sqrt_scaler([ 22.1, 32.0, 26.1 ])
    override var outerDiameters: List<Pair<Double, Double>> by ConfigurationParameter("outerDiameters") {
        val scale = this.scale.pow(1.0/2.0)
        listOf(29.0 * outerTaper * scale,
                29.0 * outerTaper * scale,
                29.0 * scale,
                29.0 * scale
                // ph: #30.0 * scale,
                // ph: #30.0 * scale,
                // ph: #32.0 * scale,
                // ph: #29.0 * scale,
        ).map {  d: Double -> Pair(d, d) }
    }

    override var initialOuterFractions: List<Double?> by ConfigurationParameter("initialOuterFractions") {
        listOf(0.01, 0.666)
    }

    override var minOuterFractionSep: List<Double> by ConfigurationParameter("minOuterFractionSep") {
        listOf(0.0, 0.5, 0.0) // ph: Looks and feels nicer
    }
}

/**
 * Design a flute with a recorder-like fingering system.
 */
fun pFluteDesigner(): TaperedFluteDesigner<Flute> {
    val flute = TaperedFluteDesigner(Flute.generator)
    flute.fingerings = FluteDesigner.fingeringsWithEmbouchure(FluteDesigner.pFluteFingers)
    flute.balance = listOf(0.1, null, null, 0.05)
    // ph: hole_angles = [ -30.0, -30.0, 30.0, -30.0, 30.0, -30.0, 0.0 ]
    // ph: hole_angles = [ 30.0, -30.0, 30.0, 0.0, 0.0, 0.0, 0.0 ]
    flute.holeAngles = listOf(30.0, -30.0, 30.0, 0.0, 0.0, 0.0, 0.0)
    flute.maxHoleSpacing = scaler(flute.instrument!!, listOf(45.0, 45.0, null, 45.0, 45.0, null))
    return flute
}


/**
 * Design a flute with a pennywhistle-like fingering system.
 */
fun folkFluteDesigner(): TaperedFluteDesigner<Flute> {
    val flute = TaperedFluteDesigner(Flute.generator)
    flute.fingerings = FluteDesigner.fingeringsWithEmbouchure(FluteDesigner.folkFingerings)
    flute.balance = listOf(0.1, null, null, 0.1)
    // ph: hole_angles = [ -30.0, -30.0, 30.0, -30.0, 30.0, -30.0, 0.0 ]
    // ph: hole_angles = [ 30.0, -30.0, 30.0, 0.0, 0.0, 0.0, 0.0 ]
    flute.holeAngles = listOf(-30.0, 30.0, 30.0,  -30.0, 0.0, 30.0, 0.0)
    flute.maxHoleSpacing = scaler(flute.instrument!!, listOf(45.0, 45.0, null, 45.0, 45.0, null))
    // min_hole_diameters = design.sqrt_scaler([ 7.5 ] * 6  + [ 12.2 ])
    // max_hole_diameters = design.sqrt_scaler([ 11.4 ] * 6 + [ 13.9 ])
    flute.maxHoleSpacing = scaler(flute.instrument!!, listOf(35.0, 35.0, null, 35.0, 35.0, null ))
    return flute
}





