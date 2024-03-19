package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.vvecmath.Transform
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.instruments.Instrument
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.errors.ConfigurationParameterException
import org.goodmath.chalumier.shape.*
import org.goodmath.chalumier.util.Point
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.math.*

typealias JoinFunction = (Double, Double,  Double, Double, Double, Double) -> Pair<CSG, CSG>
enum class JoinType {
    WeldedJoin, StraightJoin, TaperedJoin;

    fun joiner(maker: InstrumentMaker<*>): JoinFunction {
        return when(this) {
            WeldedJoin -> maker::weldJoin
            StraightJoin -> maker::straightSocket
            TaperedJoin -> maker::taperedSocket
        }
    }

    companion object {
        fun fromString(s: String): JoinType {
            return when(s) {
                "Straight", "StraightJoin" -> StraightJoin
                "Tapered", "TaperedJoin" -> TaperedJoin
                "Weld", "Welded", "WeldedJoin" -> WeldedJoin
                else -> throw ConfigurationParameterException("Invalid value ${s }for jointype")
            }
        }
    }
}

/**
 * @param gap Amount of gap around the joins between segments for
 *     straight or tapered joins. The best value for this will depend
 *     on the accuracy of your printer. If a joint is too loose and
 *     leaks, it can be sealed using wax.
 * @param thickSockets Add some extra thickness around sockets?
 * @param dilate Increase bore diameter by this much.
 *     Use if your 3D printer is poorly calibrated on concave curves.
 * @param join type of join between segments.
 * @param draft
 */
abstract class InstrumentMaker<Inst: Instrument>(
    open val outputPrefix: String,
    open val workingDir: Path,
    open val instrument: Inst,
    open val designer: InstrumentDesigner<Inst>,
) {
    val bodyCost: Int = 21
    val boreCost: Int = 23
    val cutCost: Int = 1
    val socketCost: Int = 12
    val segmentCost: Int = 13
    val holeCost: Int = 5
    val bodyMinusBoreCost: Int = 6
    val bodyRotateCost: Int = 7
    val costMap = mapOf(
        bodyCost to "body",
        boreCost to "bore",
        cutCost to "cut",
        holeCost to "hole",
        segmentCost to "segment",
        bodyMinusBoreCost to "minus",
        socketCost to "socket",
        bodyRotateCost to "rot"
    )



    open var instrumentBody: CSG? = null
    open var outside: CSG? = null
    open var bore: CSG? = null
    open var progress: Int = 0
    var statusUpdater: ((String) -> Unit) = { s -> System.out.println(s) }

    fun save(shape: CSG, name: String) {
        (workingDir / "${outputPrefix}-${name}.stl").writeText(shape.toStlString())
    }

    var top: Double = 0.0

    open fun makeParts(up: Boolean = false, flipTop: Boolean = false): List<CSG> {
        return makeSegments(up, flipTop)
    }

    open fun getCuts(): List<List<Double>> {
        return designer.divisions.map { divisions ->
            divisions.map { (hole, above) ->
                progress += cutCost
                val lower = if (hole >= 0) {
                    instrument.holePositions[hole] + 2 * instrument.holeDiameters[hole]
                } else {
                    0.0
                }
                val upper = if (hole < designer.divisions.size - 1) {
                    instrument.holePositions[hole + 1] - 2 * instrument.holeDiameters[hole]
                } else {
                    top
                }
                lower + (upper - lower) * above
            }
        }
    }

    fun makeSegments(up: Boolean = false, flipTop: Boolean = false): List<CSG> {
        return getCuts().map { segment(it, up, flipTop) }.flatten()
    }

    fun makeInstrument(
        innerProfile: Profile, outerProfile: Profile,
        holePositions: List<Double>, holeDiameters: List<Double>,
        holeVertAngles: List<Double>,
        holeHorizAngles: List<Double>,
        xPad: List<Double>,
        yPad: List<Double>,
        withFingerpad: List<Boolean>,
        outsideExtras: List<CSG> = emptyList(),
        boreExtras: List<CSG> = emptyList()
    ): CSG {

        var outside = extrudeProfile(listOf(outerProfile))
        progress += bodyCost
        var instrumentBody = outside.clone()
        var bore = extrudeProfile(listOf(innerProfile + designer.dilate))
        progress += boreCost
        holePositions.forEachIndexed { i, pos ->
            val angle = holeVertAngles[i]
            val radians = angle * PI / 180.0
            val height = outerProfile(pos) * 0.5
            val insideHeight = innerProfile(pos) * 0.5
            val shift = sin(radians) * height
            val holeDiameterCorrection = cos(radians).pow(-0.5)
            val holeDiameter = holeDiameters[i] * holeDiameterCorrection
            val crossSection = { a: Double -> squaredCircle(xPad[i], yPad[i]).withEffectiveDiameter(a) }
            val h1 = insideHeight * 0.5
            val shift1 = sin(radians) * h1
            val h2 = height * 1.5
            val shift2 = sin(radians) * h2
            var hole = extrusion(
                listOf(h1, h2),
                listOf(
                    crossSection(holeDiameter).offset(0.0, shift1),
                    crossSection(holeDiameter).offset(0.0, shift2)
                )
            )
            hole = hole.transformed(Transform()
                .rotX(-90.0)
                .rotY(holeHorizAngles[i])
                .translate(0.0, pos + shift, 0.0))
            if (withFingerpad[i] && designer.generatePads) {
                val padHeight = height * 0.5 + 0.5 * sqrt(height * height - (holeDiameters[i] * 0.5).pow(2))
                val padDepth = padHeight - insideHeight
                val padMid = padDepth / 4.0
                val padDiam = holeDiameter * 1.3
                var fingerPad = extrudeProfile(listOf(
                    Profile(
                        arrayListOf(-padDepth, -padMid, 0.0),
                        arrayListOf(padDiam + padMid * 2.0, padDiam + padMid * 2, padDiam)
                    )
                ),
                    { cs -> crossSection(cs[0]) }
                )
                var fingerPadNegative = extrudeProfile(
                    listOf(
                        Profile(
                            arrayListOf(0.0, padMid, padDepth),
                            arrayListOf(padDiam, padDiam + padMid * 8.0, padDiam + padMid * 8.0)
                        )
                    ),
                    { cs -> crossSection(cs[0]) })
                val wallAngle = -atan2(
                    0.5 * (outerProfile(pos + padDiam * 0.5) -
                            outerProfile(pos - padDiam * 0.5)),
                    padDiam
                ) * 180.0 / PI
                val fpTransform = Transform()
                    .rotX(wallAngle)
                    .translate(0.0, padHeight, 0.0)
                    .rotX(-90.0)
                    .rotY(holeHorizAngles[i])
                    .translate(0.0, pos, 0.0)
                val fpNegTransform = Transform()
                    .rotX(wallAngle)
                    .translate(0.0, padHeight, 0.0)
                    .rotX(-90.0)
                    .rotY(holeHorizAngles[i])
                    .translate(0.0, pos, 0.0)

                fingerPad = fingerPad.transformed(fpTransform)
                fingerPadNegative = fingerPadNegative.transformed(fpNegTransform)
                outside = outside.union(fingerPad)
                    .difference(fingerPadNegative)
                instrumentBody = instrumentBody.union(fingerPad)
                    .difference(fingerPadNegative)
            }
            bore = bore.union(hole)
            if (angle != 0.0 || holeHorizAngles[i] != 0.0) {
                outside = outside.difference(hole)
            }
            progress += holeCost
        }
        outsideExtras.forEach { i ->
            outside = outside.union(i)
            instrumentBody = instrumentBody.union(i)
        }
        boreExtras.forEach { i ->
            bore = bore.union(i)
        }
        instrumentBody = instrumentBody.difference(bore)
        progress += bodyMinusBoreCost
        instrumentBody.transformed(Transform().rotY(180.0))
        progress += bodyRotateCost
        this.instrumentBody = instrumentBody
        this.outside = outside
        this.bore = bore
        this.top = instrumentBody.bounds.bounds.z
        save(instrumentBody, "full")
        return instrumentBody
    }


    fun segment(originalCuts: List<Double>, up: Boolean, flipTop: Boolean): List<CSG> {
        val length = top
        var remainder = instrumentBody!!.clone()
        var workingBore = bore
        var inner = instrument.inner
        var outer = instrument.outer
        var cuts = originalCuts

        if (up) {
            cuts = cuts.reversed().map { length - it }
            remainder = remainder.transformed(Transform().rotY(180.0).translate(0.0, length, 0.0))
            if (designer.thickSockets) {
                workingBore = workingBore!!.transformed(
                    Transform().rotY(180.0).translate(0.0, length, 0.0))
            }
            inner = inner.reversed().moved(length)
            outer = outer.reversed().moved(length)
            progress += 11
        }
        val socket = JoinType.fromString(designer.join).joiner(this)
        val shapes = ArrayList<CSG>()
        for (cut in cuts) {
            val d1 = inner(cut)
            var d4 = outer(cut)
            val d5 = outer.maximum() * 2.0
            val sockLength = d4 * 0.8
            var p1 = cut - sockLength
            var p3 = cut
            if (!up && JoinType.fromString(designer.join) != JoinType.WeldedJoin) {
                p1 += sockLength
                p3 += sockLength
            }
            if (designer.thickSockets) {
                val d4Orig = d4
                d4 += min(d4 * 0.2, (d4 - d1) * 0.5)
                val profThicker = Profile(
                    arrayListOf(p1 - (d4 - d4Orig), p1, p3),
                    arrayListOf((d1 + d4) * 0.5, d4, d4)
                )
                val thicker = extrudeProfile(listOf(profThicker)).difference(workingBore!!)
                remainder = remainder.union(thicker)
            }

            val (maskInside, maskOutside) = socket(p1, p3, length, d1, d4, d5)
            var item = remainder.clone()
            item = item.difference(maskOutside)
            remainder = remainder.intersect(maskInside)
            shapes.add(item)
            progress += 12
        }
        shapes.add(remainder)
        shapes.reverse()
        return shapes.mapIndexed { i, item ->
            val updatedItem = if (!flipTop || (up && i != shapes.size - 1) ||
                (!up && i != 0)) {
                item.transformed(Transform().rotY(180.0))
            } else {
                item
            }
            val positioned = updatedItem.positionNicely()
            save(positioned, "${shapes.size}-piece-${i + 1}")
            progress += 13
            positioned
        }
    }


    fun weldJoin(_z0: Double, z1: Double, zMax: Double, d0: Double, d1: Double, dMax: Double): Pair<CSG, CSG> {
        val prof = Profile(
            arrayListOf(z1, zMax+50.0),
            arrayListOf(dMax, dMax)
        )
        var maskUpper = extrudeProfile(listOf(prof))
        var maskLower = maskUpper

        val triangle = Loop(listOf(
            Point(0.5, 0.0),
            Point(0.0, sqrt(0.75)),
            Point(-0.5, 0.0)
        ))
        val triangleUpper = triangle.scale(d0*0.5+designer.gap)
        val triangleLower  = triangle.scale(d0*0.5-designer.gap)
        val d1_3 = d0*0.6666+d1*0.3334
        val d2_3 = d0*0.3334+d1*0.6666
        for (i in (1 until 5)) {
            val upperBump = extrusion(
                arrayListOf(d1_3 * 0.5 - designer.gap * 0.5, d2_3 * 0.5 - designer.gap * 0.5, dMax * 0.5),
                arrayListOf(triangleUpper.scale(0.0), triangleUpper, triangleUpper)
            )
            val ubTransform = Transform()
                .rotX(-90.0)
                .rotY(180.0 + 360.0 / 5.0 * i)
                .translate(0.0, z1, 0.0)
            maskUpper = maskUpper.union(upperBump.transformed(ubTransform))
            val lowerBump = extrusion(
                arrayListOf(d1_3 * 0.5 + designer.gap * 0.5, d2_3 * 0.5 + designer.gap * 0.5, 0.5),
                arrayListOf(triangleLower.scale(0.0), triangleLower, triangleLower)
            )
            val lbTransform = Transform()
                .rotX(-90.0)
                .rotY(180 + 360.0 / 5 * i)
                .translate(0.0, z1, 0.0)
            maskLower = maskLower.union(lowerBump.transformed(lbTransform))
        }
        return Pair(maskLower, maskUpper)
    }

    fun straightSocket(p1: Double, p3: Double, length: Double, d1: Double, d3: Double, d4: Double): Pair<CSG, CSG> {
        val d2 = (d1 + d3) / 2.0
        val p2 = p1 + (d2 - d1) / 2.0
        val d1a = d1 - designer.gap
        val p1b = p1 - designer.gap
        val d2a = d2 - designer.gap
        val d2b = d2 + designer.gap
        val profInside = Profile(
            arrayListOf(p1, p2, p3, length + 50),
            arrayListOf(d1a, d2a, d2a, d4),
            arrayListOf(d1a, d2a, d4, d4)
        )
        val profOutside = Profile(
            arrayListOf(p1b, p2, p3, length + 50),
            arrayListOf(d1, d2b, d2b, d4),
            arrayListOf(d1, d2b, d4, d4)
        )
        val maskInside = extrudeProfile(listOf(profInside))
        val maskOutside = extrudeProfile(listOf(profOutside))
        return Pair(maskInside, maskOutside)
    }
    fun taperedSocket(p1: Double, p3: Double, length: Double, d1: Double, d4: Double, d5: Double): Pair<CSG, CSG> {

        val d3 = (d1+d4) / 2.0
        val d2 = (d1+d3) / 2.0

        val p2 = p1 + (d2-d1)

        val d1a = d1 - designer.gap
        val p1b = p1 - designer.gap

        val d2a = d2 - designer.gap
        val d2b = d2 + designer.gap

        val d3a = d3 - designer.gap
        val d3b = d3 + designer.gap

        val profInside = Profile(
            arrayListOf(p1,  p2,  p3,  length+50.0),
            arrayListOf(d1a, d2a, d3a, d5),
            arrayListOf( d1a, d2a, d5,  d5))
        val profOutside = Profile(
                arrayListOf(p1b, p2,  p3,  length+50.0),
            arrayListOf(d1,  d2b, d3b, d5),
            arrayListOf( d1,  d2b, d5,  d5 ))

        val maskInside = extrudeProfile(listOf(profInside))
        val maskOutside = extrudeProfile(listOf(profOutside))
        return Pair(maskInside, maskOutside)
    }

    fun decorateProfile(prof: Profile, pos: Double, align: Double, amount: Double=0.2): Profile {
        val decoThickness = prof(pos) * amount
        val updatedPos = pos + decoThickness * align
        val decoratedProfile = Profile(ArrayList(listOf(-1.0, 0.0, 1.0).map { i -> updatedPos + decoThickness * i }),
            ArrayList(listOf(0.0, 1.0, 0.0).map { i -> decoThickness * i })
        )
        return prof + decoratedProfile.clipped(prof.start(), prof.end())
    }

    abstract fun run(): List<CSG>

    open fun totalSteps(): Long {
        val numberOfCuts = designer.divisions.sumOf { d -> d.size }
        val numberOfParts = numberOfCuts + designer.divisions.size
                return (bodyCost + boreCost +
                numberOfParts*segmentCost +
                numberOfCuts * (cutCost + socketCost) +
                designer.numberOfHoles*holeCost
                + bodyMinusBoreCost + bodyRotateCost).toLong()
    }

}
