package org.goodmath.demakeink.design

import org.goodmath.demakeink.util.fromEnd
import org.goodmath.demakeink.util.repeat
import java.io.ObjectInputFilter.Config


class WhistleHeadMaker<T>(bore: Pair<Double, Double>, outside: Pair<Double, Double>)
        where T:Whistle<T>, T: Copyable<T> {
    fun effectiveGapDiameter(): Double {
        throw NotImplementedError()
    }
    fun effectiveGapHeight(): Double {
        throw NotImplementedError()
    }
}

abstract class Whistle<T>: Instrument<T>()
        where T: Whistle<T>, T: Copyable<T> {
    open var trueInner: Profile by ConfigurationParameter {
        inner
    }


}

abstract class AbstractWhistleDesigner<Whist>(gen: InstrumentGenerator<Whist>): InstrumentDesignerWithBoreScale<Whist> (gen)
        where Whist: Whistle<Whist>, Whist: Copyable<Whist> {
    // ph: From 2014-15-whistle-tweaking
    open var tweakGapExtra: Double by ConfigurationParameter {
        0.6
    }
    open var tweakBoreless: Double by ConfigurationParameter {
        0.3

    }

    override var boreScale: Double by ConfigurationParameter { 1.1 }
    override var closedTop: Boolean by ConfigurationParameter { false }
    override var divisions: List<List<Pair<Int, Double>>> by ConfigurationParameter {
        emptyList()
    }



    fun getWhistleHeadMaker(): WhistleHeadMaker<Whist> {
        val bore = innerDiameters.fromEnd(1)
        val baseOutside = outerDiameters.fromEnd(1)
        // As usual, ph seems to have forgetten that in some places,
        // the outer diameters are pairs.
        val outside = if (outerAdd) {
            Pair(baseOutside.first + bore.first, baseOutside.second + bore.second)
        } else {
            baseOutside
        }

        return WhistleHeadMaker<Whist>(bore, outside)
    }


    /* ph:
    # For gap_width 0.5, gap_length 0.25, from soprano
    # tweak_gapextra = 0.37

    # From soprano recorder
    #tweak_gapextra = 0.75 #0.71
    #tweak_boreless = 0.65 #0.49
    */

    open var xPad: Double by ConfigurationParameter {
        0.0
    }

    open var yPad: Double by ConfigurationParameter {
        0.0
    }

    override fun patchInstrument(origInst: Whist): Whist {
        val inst = origInst.copy() as Whist
        inst.trueLength = inst.length
        inst.trueInner = inst.inner

        val boreDiameter = inst.inner(inst.length)
        inst.length -= (boreDiameter * tweakBoreless)
        inst.inner = inst.inner.clipped(0.0, inst.length)

        // ph: #bulge_pos1 = self.tweak_bulgepos1 * inst.length
        // ph: #bulge_pos2 = self.tweak_bulgepos2 * inst.length
        // ph: if bulge_pos1 > bulge_pos2:
        // ph:     bulge_pos1, bulge_pos2 = bulge_pos2, bulge_pos1
        // ph: bulge_amount = (self.tweak_bulgediameter-1.0) * bore_diameter
        // ph: inst.inner = inst.inner + profile.Profile(
        // ph:     [ bulge_pos1, bulge_pos2 ],
        // ph:     [ 0.0, bulge_amount ],
        // ph:     [ bulge_amount, 0.0 ],
        // ph:     )

        val maker = getWhistleHeadMaker()
        val diameter = maker.effectiveGapDiameter()
        val length = (maker.effectiveGapHeight() / 2.0) + diameter * tweakGapExtra
        // ph:         ^ The gap is this high on one side and a blade on the other side
        // ph:           split the difference.

        inst.inner = Profile(
                inst.inner.pos + listOf(inst.length + length),
                inst.inner.low + listOf(diameter),
                inst.inner.high.slice(0 until inst.inner.high.size - 1) + listOf(diameter, diameter)
        )
        inst.length += length
        return inst
    }
}

class SixHoleWhistle: Whistle<SixHoleWhistle>(), Copyable<SixHoleWhistle> {
    override fun copy(): SixHoleWhistle {
        TODO("Not yet implemented")
    }

    companion object {
        val generator: InstrumentGenerator<SixHoleWhistle> =
                object : InstrumentGenerator<SixHoleWhistle> {
                    override fun create(): SixHoleWhistle {
                        return SixHoleWhistle()
                    }

                }
    }
}

class SixHoleWhistleDesigner: AbstractWhistleDesigner<SixHoleWhistle>(SixHoleWhistle.generator) {
    override var transpose: Int by ConfigurationParameter { 12 }

    override var divisions: List<List<Pair<Int, Double>>> by ConfigurationParameter {
        listOf(
                listOf(Pair(5,0.0)),
                listOf(Pair(1,0.0),Pair(5,0.0),Pair(5,0.5)),
                listOf(Pair(-1,0.75),Pair(1,0.0),Pair(2,1.0),Pair(5,0.0),Pair(5,0.3),Pair(5,0.6))
        )
    }

    override var minHoleDiameters: List<Double> by ConfigurationParameter {
        boreScaler(listOf(3.0).repeat(6))
    }


    override var maxHoleDiameters: List<Double> by ConfigurationParameter {
        boreScaler(listOf(12.0).repeat(6), maximum=12.0)
    }

    override var holeHorizAngles: List<Double> by ConfigurationParameter {
        listOf(0.0).repeat(6)
    }

    override var balance: List<Double?> by ConfigurationParameter {
        listOf(0.05, null, null, 0.05)
    }

    override var minHoleSpacing: List<Double?> by ConfigurationParameter {
        scaler(listOf(null, null, 35.0, null, null))
    }

    override var maxHoleSpacing: List<Double?> by ConfigurationParameter {
        sqrtScaler(listOf(35.0, 35.0, null, 35.0, 35.0))
    }

    override var innerDiameters: List<Pair<Double, Double>> by ConfigurationParameter {
        boreScaler(listOf(14.0, 14.0, 20.0, 22.0, 22.0, 20.0, 20.0)).map { Pair(it, it)}
    }

    override var initialInnerFractions: List<Double?> by ConfigurationParameter {
        listOf(0.2, 0.6, 0.65, 0.7, 0.75)
    }

    override var minInnerFractionSep: List<Double> by ConfigurationParameter {
        listOf(0.01, 0.5, 0.01, 0.01, 0.01, 0.01)
    }

    override var outerDiameters: List<Pair<Double, Double>> by ConfigurationParameter {
        boreScaler(listOf(40.0, 28.0, 28.0, 32.0, 32.0)).map { Pair(it, it) }
    }
    override var outerAngles: List<Angle?> by ConfigurationParameter {
        listOf(Angle(AngleDirection.Here, -15.0), Angle(AngleDirection.Here, 0.0),
                null, null, null)
    }

    override var initialOuterFractions: List<Double?> by ConfigurationParameter {
        listOf(0.15, 0.5, 0.85)
    }

    override var minOuterFractionSep: List<Double> by ConfigurationParameter {
        listOf(0.15, 0.3, 0.35, 0.15)
    }


}

