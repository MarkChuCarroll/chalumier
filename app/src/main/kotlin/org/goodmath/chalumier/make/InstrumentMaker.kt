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
package org.goodmath.chalumier.make

import java.nio.file.Path
import kotlin.math.*
import org.goodmath.chalumier.design.InstrumentDesigner
import org.goodmath.chalumier.design.instruments.Instrument
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.errors.ConfigurationParameterException
import org.goodmath.chalumier.geom.ThreeDBody
import org.goodmath.chalumier.geom.ThreeDGeometry
import org.goodmath.chalumier.geom.TwoDShape
import org.goodmath.chalumier.shape.*
import org.goodmath.chalumier.util.Point



class JoinFunction<Body: ThreeDBody<Body>>(
    val f: (z0: Double, z1: Double, zMax: Double, d0: Double, d1: Double, dMax: Double) -> Pair<Body, Body>) {

    fun apply(z0: Double, z1: Double, zMax: Double, d0: Double, d1: Double, dMax: Double): Pair<Body, Body> {
        return f(z0, z1, zMax, d0, d1, dMax)
    }
}



enum class JoinType {
    WeldedJoin, StraightJoin, TaperedJoin;

    fun<Shape: TwoDShape<Shape>, Body: ThreeDBody<Body>> joiner(maker: InstrumentMaker<*, Shape,
            Body>): JoinFunction<Body> {
        return when(this) {
            WeldedJoin -> JoinFunction(maker::weldJoin)
            StraightJoin -> JoinFunction(maker::straightSocket)
            TaperedJoin -> JoinFunction(maker::taperedSocket)
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

interface InstrumentMakerProgressUpdater {
    fun update(name: String, stage: String, total: Int, current: Int)
    fun print(a: Any)
}


/**
 *
 */
abstract class InstrumentMaker<Inst: Instrument,
        Shape: TwoDShape<Shape>, Body: ThreeDBody<Body>>(
    open val geometry: ThreeDGeometry<Body, Shape>,
    open val outputPrefix: String,
    open val workingDir: Path,
    open val instrument: Inst,
    open val designer: InstrumentDesigner<Inst>,
) {
    open var reporter: InstrumentMakerProgressUpdater = object:InstrumentMakerProgressUpdater {
        override fun update(name: String, stage: String, total: Int, current: Int) {
            System.err.println("Update: ($name, $stage, $total, $current)")
        }

        override fun print(a: Any) {
            System.err.println(a)
        }

    }

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

    open var instrumentBody: Body? = null
    open var outside: Body? = null
    open var bore: Body? = null
    open var progress: Int = 0
    var stage: String = "not started"
    val name: String
        get() = designer.name

    open fun report() {
        reporter.update(name, stage, totalSteps().toInt(), progress)
    }

    fun save(shape: Body, name: String) {
        shape.save(workingDir, "${outputPrefix}-${name}")
    }

    var top: Double = 0.0

    open fun makeParts(up: Boolean = false, flipTop: Boolean = false): List<Body> {
        return makeSegments(up, flipTop)
    }

    open fun getCuts(): List<List<Double>> {
        return designer.divisions.map { divisions ->
            divisions.map { (holeIndex, above) ->
                val hole = if (holeIndex < 0) {
                    designer.numberOfHoles + holeIndex
                } else {
                    holeIndex
                }
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

    fun makeSegments(up: Boolean = false, flipTop: Boolean = false): List<Body> {
        return getCuts().map { cuts -> segment(cuts, up, flipTop) }.flatten()
    }

    val circleCrossSection: (List<Double>) -> Shape = { args -> geometry.lowerGeometry.circle(args[0])}
    fun makeInstrument(
        innerProfile: Profile, outerProfile: Profile,
        holePositions: List<Double>, holeDiameters: List<Double>,
        holeVertAngles: List<Double>,
        holeHorizAngles: List<Double>,
        xPad: List<Double>,
        yPad: List<Double>,
        withFingerpad: List<Boolean>,
        outsideExtras: List<Body> = emptyList(),
        boreExtras: List<Body> = emptyList()
    ): Body {
        stage = "building profile"
        report()
        val before = System.currentTimeMillis()
        var outside = geometry.extrudeShape(
            circleCrossSection,
            listOf(outerProfile))

        progress += bodyCost
        report()
        var instrumentBody = outside
        stage = "building bore"
        report()
        var bore = geometry.extrudeShape(circleCrossSection,
            listOf(innerProfile + designer.dilate))
        progress += boreCost
        val afterBore = System.currentTimeMillis()
        reporter.print("Main body took ${afterBore - before}ms")
        report()
        holePositions.forEachIndexed { i, pos ->
            val beforeHole = System.currentTimeMillis()
            stage = "Drilling hole $i"
            report()
            val angle = holeVertAngles[i]
            val radians = angle * PI / 180.0
            val height = outerProfile(pos) * 0.5
            val insideHeight = innerProfile(pos) * 0.5
            val shift = sin(radians) * height
            val holeDiameterCorrection = cos(radians).pow(-0.5)
            val holeDiameter = holeDiameters[i] * holeDiameterCorrection
            val crossSection = { a: Double ->
                geometry.lowerGeometry.squaredCircle(a + xPad[i]) }
//                squaredCircle(xPad[i], yPad[i]).withEffectiveDiameter(a) }
            val h1 = insideHeight * 0.5
            val shift1 = sin(radians) * h1
            val h2 = height * 1.5
            val shift2 = sin(radians) * h2
            var hole = geometry.extrudeShapes(
                listOf(h1, h2),
                listOf(
                    crossSection(holeDiameter).offset(0.0, shift1),
                    crossSection(holeDiameter).offset(0.0, shift2)
                )
            )
            hole = hole.rotate(-90.0, holeHorizAngles[i], 0.0)
                .translate(0.0, 0.0, pos + shift)
            if (withFingerpad[i] && designer.generatePads) {
                val padHeight = height * 0.5 + 0.5 * sqrt(height * height - (holeDiameters[i] * 0.5).pow(2))
                val padDepth = padHeight - insideHeight
                val padMid = padDepth / 4.0
                val padDiam = holeDiameter * 1.3
                var fingerPad = geometry.extrudeShape(
                    { cs: List<Double> ->
                        if (cs.size != 1) {
                            throw Exception("Invalid parameters in CS")
                        }
                        crossSection(cs[0]) },
                    listOf(Profile(
                        arrayListOf(-padDepth, -padMid, 0.0),
                        arrayListOf(padDiam + padMid * 2.0, padDiam + padMid * 2, padDiam)
                    )))
                var fingerPadNegative = geometry.extrudeShape(
                    { cs: List<Double> -> crossSection(cs[0]) },
                    listOf(Profile(
                        arrayListOf(0.0, padMid, padDepth),
                        arrayListOf(padDiam, padDiam + padMid * 8.0, padDiam + padMid * 8.0)
                    )))

                val wallAngle = -atan2(
                    0.5 * (outerProfile(pos + padDiam * 0.5) -
                            outerProfile(pos - padDiam * 0.5)),
                    padDiam
                ) * 180.0 / PI
                fingerPad = fingerPad
                    .rotate(wallAngle, 0.0, 0.0)
                    .translate(0.0, -padHeight, 0.0)
                    .rotate(-90.0, 0.0, holeHorizAngles[i])
                    .translate(0.0, pos, 0.0)
                fingerPadNegative = fingerPadNegative
                    .rotate(wallAngle, 0.0, 0.0)
                    .translate(0.0, -padHeight, 0.0)
                    .rotate(-90.0, 0.0, holeHorizAngles[i])
                    .translate(0.0, pos, 0.0)
                outside = outside.union(fingerPad)
                    .difference(fingerPadNegative)
                instrumentBody = instrumentBody.union(fingerPad)
                    .difference(fingerPadNegative)
            }
            bore = bore.union(hole)
            if (angle != 0.0 || holeHorizAngles[i] != 0.0) {
                outside = outside.difference(hole)
            }
            reporter.print("Hole $i took ${System.currentTimeMillis() - beforeHole}ms")
            progress += holeCost
            report()
        }
        stage = "assembling body"
        report()
        val beforeAssembly = System.currentTimeMillis()
        outsideExtras.forEach { i ->
            outside = outside.union(i)
            instrumentBody = instrumentBody.union(i)
        }
        boreExtras.forEach { i ->
            bore = bore.union(i)
        }
        instrumentBody = instrumentBody.difference(bore)
        val afterAssembly = System.currentTimeMillis()
        reporter.print("Assembly took ${afterAssembly - beforeAssembly}ms")
        progress += bodyMinusBoreCost
        report()
        instrumentBody.rotate(0.0, 180.0, 0.0)
        progress += bodyRotateCost
        stage = "writing"
        report()
        this.instrumentBody = instrumentBody
        this.outside = outside
        this.bore = bore
        this.top = instrumentBody.bounds().max.z
        save(instrumentBody, "full")
        reporter.print("Body model size = ${instrumentBody.toText().length}")

        return instrumentBody
    }


    fun segment(originalCuts: List<Double>, up: Boolean, flipTop: Boolean): List<Body> {
        val length = top
        var remainder = instrumentBody!!
        var workingBore = bore
        var inner = instrument.inner
        var outer = instrument.outer
        var cuts = originalCuts
        stage = "segmenting"
        reporter.print("Doing segmentation ${originalCuts}")
        val before = System.currentTimeMillis()
        report()
        if (up) {
            cuts = cuts.reversed().map { length - it }
            remainder = remainder.rotate(0.0, 180.0, 0.0).translate(0.0, length, 0.0);
            if (designer.thickSockets) {
                workingBore = workingBore!!.rotate(0.0, 180.0, 0.0).translate(0.0, length, 0.0);
            }
            inner = inner.reversed().moved(length)
            outer = outer.reversed().moved(length)
            progress += 11
            report()
        }
        val socket = JoinType.fromString(designer.join).joiner(this)
        val shapes = ArrayList<Body>()
        cuts.indices.forEach { idx ->
            val beforeCut = System.currentTimeMillis()
            reporter.print("..Performing cut $idx")
            report()
            val cut = cuts[idx]
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

                val thicker = geometry.extrudeShape(circleCrossSection, listOf(profThicker)).difference(workingBore!!)
                remainder = remainder.union(thicker)
            }

            val (maskInside, maskOutside) = socket.apply(p1, p3, length, d1, d4, d5)
            var item = remainder
            item = item.difference(maskOutside)
            remainder = remainder.intersect(maskInside)
            shapes.add(item)
            progress += 12
            val afterCut = System.currentTimeMillis()
            reporter.print("Cut $idx took ${afterCut - beforeCut}ms")
            report()
        }
        shapes.add(remainder)
        shapes.reverse()
        val afterCuts = System.currentTimeMillis()
        reporter.print("All cuts took ${afterCuts - before}ms")
        report()
        return shapes.mapIndexed { i, item ->
            val updatedItem = if (!flipTop || (up && i != shapes.size - 1) ||
                (!up && i != 0)) {
                item.rotate(0.0, 180.0, 0.0)
            } else {
                item
            }
            val positioned = updatedItem.positionNicely()
            save(positioned, "${shapes.size}-piece-${i + 1}")
            progress += 13
            report()
            positioned
        }
    }

    fun weldJoin(_z0: Double, z1: Double, zMax: Double, d0: Double, d1: Double, dMax: Double): Pair<Body, Body> {
        val prof = Profile(
            arrayListOf(z1, zMax+50.0),
            arrayListOf(dMax, dMax)
        )
        var maskUpper = geometry.extrudeShape(circleCrossSection, listOf(prof))
        var maskLower = maskUpper

        val triangle = geometry.lowerGeometry.polygon(listOf(
            Point(0.5, 0.0),
            Point(0.0, sqrt(0.75)),
            Point(-0.5, 0.0)
        ))
        val triangleUpper = triangle.scale(d0*0.5+designer.gap)
        val triangleLower  = triangle.scale(d0*0.5-designer.gap)
        val d1_3 = d0*0.6666+d1*0.3334
        val d2_3 = d0*0.3334+d1*0.6666
        for (i in (1 until 5)) {
            val upperBump = geometry.extrudeShapes(
                arrayListOf(d1_3 * 0.5 - designer.gap * 0.5, d2_3 * 0.5 - designer.gap * 0.5, dMax * 0.5),
                arrayListOf(triangleUpper.scale(0.0), triangleUpper, triangleUpper)
            )
            val ubTransform = { shape: Body -> shape.rotate(-90.0,
                180.0 + 360.0 / 5.0 * i, 0.0).translate(0.0, z1, 0.0) }
            maskUpper = maskUpper.union(ubTransform(upperBump))
            val lowerBump = geometry.extrudeShapes(
                arrayListOf(d1_3 * 0.5 + designer.gap * 0.5, d2_3 * 0.5 + designer.gap * 0.5, 0.5),
                arrayListOf(triangleLower.scale(0.0), triangleLower, triangleLower)
            )
            val lbTransform = { shape: Body ->
                shape.rotate(-90.0, 180 + 360.0 / 5 * i, 0.0)
                .translate(0.0, z1, 0.0) }
            maskLower = maskLower.union(lbTransform(lowerBump))
        }
        return Pair(maskLower, maskUpper)
    }

    fun straightSocket(p1: Double, p3: Double, length: Double, d1: Double, d3: Double, d4: Double): Pair<Body, Body> {
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
        val maskInside = geometry.extrudeShape(circleCrossSection, listOf(profInside))
        val maskOutside = geometry.extrudeShape(circleCrossSection, listOf(profOutside))
        return Pair(maskInside, maskOutside)
    }
    fun taperedSocket(p1: Double, p3: Double, length: Double, d1: Double, d4: Double, d5: Double): Pair<Body, Body> {

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

        val maskInside = geometry.extrudeShape(circleCrossSection, listOf(profInside))
        val maskOutside = geometry.extrudeShape(circleCrossSection, listOf(profOutside))
        return Pair(maskInside, maskOutside)
    }

    fun decorateProfile(prof: Profile, pos: Double, align: Double, amount: Double=0.2): Profile {
        stage = "decorate profile"
        report()
        val decoThickness = prof(pos) * amount
        val updatedPos = pos + decoThickness * align
        val decoratedProfile = Profile(ArrayList(listOf(-1.0, 0.0, 1.0).map { i -> updatedPos + decoThickness * i }),
            ArrayList(listOf(0.0, 1.0, 0.0).map { i -> decoThickness * i })
        )
        return prof + decoratedProfile.clipped(prof.start(), prof.end())
    }

    abstract fun run(): List<Body>

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
