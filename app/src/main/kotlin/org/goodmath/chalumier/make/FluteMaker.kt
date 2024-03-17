package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import org.goodmath.chalumier.cli.InstrumentDescription
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.instruments.Instrument
import org.goodmath.chalumier.design.instruments.TaperedFlute
import org.goodmath.chalumier.util.fromEnd
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

open class FluteMaker(
    prefix: String,
    dir: Path,
    spec: TaperedFlute,
    instrument: InstrumentDescription):
    InstrumentMaker<TaperedFlute>(prefix, dir, spec, instrument) {

    val openBothEnds = instrument.getBooleanOption("openBothEnds", false)
    val decorate = instrument.getBooleanOption("decorate", false)
    val embAspect = instrument.getDoubleOption("embAspect", 1.5)
    val embSquareness = instrument.getDoubleOption("embSquareness", 0.0)



    override fun run(): List<CSG> {
        val length = instrument.length * 1.05   // Extend a bit to allow cork.
        val innerProfile = if (openBothEnds) {
            val corkLength = length - instrument.length
            val corkDiameter = spec.inner(instrument.length)
            (workingDir / "$outputPrefix-cork.txt").writeText(
                "Cork length = ${corkLength}mm\n" +
                "Cork diameter = ${corkDiameter}mm\n"
            )
            spec.inner.clipped(-50.0, length+50.0)
        } else {
            spec.inner.clipped(-50.0, length)
        }
        var outerProfile = spec.outer.clipped(0.0, length)
        if (decorate) {
            val emFract = 1.0 - spec.holePositions.fromEnd(1)/length
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
        val (embXpad, embYpad) = if  (embAspect > 1.0) {
            Pair(embSquareness, (embSquareness + 1.0) * embAspect - 1.0)
        } else {
            Pair((embSquareness + 1.0)/embAspect - 1.0, embSquareness)
        }
        val whole = makeInstrument(innerProfile, outerProfile,
            holePositions =  spec.holePositions,
            holeDiameters = spec.holeDiameters,
            holeVertAngles = spec.holeAngles,
            holeHorizAngles = instrument.holeHorizAngles,
            xPad = listOf(0.0).repeat(instrument.numberOfHoles - 1) + listOf(embXpad),
            yPad = listOf(0.0).repeat(instrument.numberOfHoles - 1) + listOf(embYpad),
            withFingerpad = listOf(true).repeat(instrument.numberOfHoles-1) + listOf(false))
        instrumentBody = whole
        val parts = makeParts(up=false)
        return listOf(whole) + parts
    }

}
