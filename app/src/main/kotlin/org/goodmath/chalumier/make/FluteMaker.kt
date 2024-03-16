package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.util.fromEnd
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

open class FluteMaker(
    override val outputPrefix: String,
    override val specFile: Path,
    override val parameterFile: Path,
    override val workingDir: Path,
    override val generatePads: Boolean = false,
    override val gap: Double = 0.2,
    override val thickSockets: Boolean = false,
    override val dilate: Double = 0.0,
    override val join: JoinType = JoinType.StraightJoin,
    override val draft: Boolean = false,
    open val openBothEnds: Boolean = false,
    open val embSquareness: Double = 0.0,
    open val embAspect: Double = 1.5,
    open val decorate: Boolean = false):
    InstrumentMaker(outputPrefix, specFile, parameterFile,
        workingDir, generatePads, gap, thickSockets, dilate,
        join, draft) {


    override fun run(): List<CSG> {
        val length = instrument.length * 1.05   // Extend a bit to allow cork.
        val innerProfile = if (openBothEnds) {
            val corkLength = length - instrument.length
            val corkDiameter = instrument.inner(instrument.length)
            (workingDir / "$outputPrefix-cork.txt").writeText(
                "Cork length = ${corkLength}mm\n" +
                "Cork diameter = ${corkDiameter}mm\n"
            )
            instrument.inner.clipped(-50.0, length+50.0)
        } else {
            instrument.inner.clipped(-50.0, length)
        }
        var outerProfile = instrument.outer.clipped(0.0, length)
        if (decorate) {
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
        val (embXpad, embYpad) = if  (embAspect > 1.0) {
            Pair(embSquareness, (embSquareness + 1.0) * embAspect - 1.0)
        } else {
            Pair((embSquareness + 1.0)/embAspect - 1.0, embSquareness)
        }
        val whole = makeInstrument(innerProfile, outerProfile,
            holePositions =  instrument.holePositions,
            holeDiameters = instrument.holeDiameters,
            holeVertAngles = spec.holeAngles!!,
            holeHorizAngles = spec.holeHorizAngles!!,
            xPad = listOf(0.0).repeat(instrument.numberOfHoles - 1) + listOf(embXpad),
            yPad = listOf(0.0).repeat(instrument.numberOfHoles - 1) + listOf(embYpad),
            withFingerpad = listOf(true).repeat(6) + listOf(false))

        val parts = makeParts(up=false)
        return listOf(whole) + parts

    }

}
