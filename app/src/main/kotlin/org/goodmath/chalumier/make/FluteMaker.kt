package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.TaperedFluteDesigner
import org.goodmath.chalumier.design.instruments.TaperedFlute
import org.goodmath.chalumier.util.fromEnd
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

open class FluteMaker(
    prefix: String,
    dir: Path,
    instrument: TaperedFlute,
    override val designer: TaperedFluteDesigner):
    InstrumentMaker<TaperedFlute>(prefix, dir, instrument, designer) {

    override fun run(): List<CSG> {
        val length = designer.length * 1.05   // Extend a bit to allow cork.
        val innerProfile = if (designer.openBothEnds) {
            val corkLength = length - designer.length
            val corkDiameter = instrument.inner(designer.length)
            (workingDir / "$outputPrefix-cork.txt").writeText(
                "Cork length = ${corkLength}mm\n" +
                "Cork diameter = ${corkDiameter}mm\n"
            )
            instrument.inner.clipped(-50.0, length+50.0)
        } else {
            instrument.inner.clipped(-50.0, length)
        }
        var outerProfile = instrument.outer.clipped(0.0, length)
        if (designer.decorate) {
            val emFract = 1.0 - instrument.holePositions.fromEnd(1)/length
            listOf( Pair(1.0 - emFract*2.0, 1.0), Pair(1.0, -1.0)).forEach { (frac, align) ->
                var dPos = length * frac
                val dAmount = outerProfile(dPos) * 0.1
                dPos += dAmount * align
                val decoProfile = Profile(
                    ArrayList(listOf(-1.0, -0.333, 0.333, 1.0).map { i ->
                        dPos + dAmount*i }),
                    ArrayList(listOf(0.0, 1.0, 1.0, 0.0).map { i -> dAmount * i}))
                outerProfile += decoProfile
            }
        }
        val (embXpad, embYpad) = if  (designer.embAspect > 1.0) {
            Pair(designer.embSquareness, (designer.embSquareness + 1.0) * designer.embAspect - 1.0)
        } else {
            Pair((designer.embSquareness + 1.0)/designer.embAspect - 1.0, designer.embSquareness)
        }
        val whole = makeInstrument(innerProfile, outerProfile,
            holePositions =  instrument.holePositions,
            holeDiameters = instrument.holeDiameters,
            holeVertAngles = instrument.holeAngles,
            holeHorizAngles = designer.holeHorizAngles,
            xPad = listOf(0.0).repeat(designer.numberOfHoles - 1) + listOf(embXpad),
            yPad = listOf(0.0).repeat(designer.numberOfHoles - 1) + listOf(embYpad),
            withFingerpad = listOf(true).repeat(designer.numberOfHoles-1) + listOf(false))
        instrumentBody = whole
        val parts = makeParts(up=false)
        return listOf(whole) + parts
    }

}
