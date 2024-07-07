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

import org.goodmath.chalumier.design.AbstractWhistleDesigner
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.instruments.Whistle
import org.goodmath.chalumier.errors.dAssert
import org.goodmath.chalumier.geom.ThreeDBody
import org.goodmath.chalumier.geom.ThreeDGeometry
import org.goodmath.chalumier.geom.ThreeDPoint
import org.goodmath.chalumier.geom.TwoDShape
import org.goodmath.chalumier.util.Point
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path
import kotlin.math.PI
import kotlin.math.sqrt

class WhistleHeadMaker<Shape: TwoDShape<Shape>, Body: ThreeDBody<Body>>(
    geometry: ThreeDGeometry<Body, Shape>,
    prefix: String,
    dir: Path,
    instrument: Whistle,
    override val designer: AbstractWhistleDesigner,
    val boreDiam: Double = 15.0,
    val outsideDiam: Double = 21.0): InstrumentMaker<Whistle, Shape, Body>(geometry, prefix, dir, instrument, designer) {

    val gapWidth: Double = effectiveGapDiameter(boreDiam)
    val gapLength: Double = effectiveGapHeight(boreDiam, outsideDiam)

    companion object {

        fun effectiveGapDiameter(boreDiam: Double,
                                 gapWidth: Double = 0.6,
                                 gapLength: Double = 0.25): Double {
            val area = (boreDiam * gapLength) * (boreDiam * gapWidth)
            return sqrt(area / PI) * 2.0
        }

        fun effectiveGapHeight(boreDiam: Double, outsideDiam: Double): Double {
            return (outsideDiam - boreDiam) * 0.5
        }

    }
    fun construct(): List<Body> {
        reporter.print("Running head constructor")
        val boreLength = boreDiam * 1.5
        val gapLength = boreDiam * this.gapLength
        val windCutterLength = boreDiam * 1.0  // Why???
        val airwayLength = boreDiam * 1.5
        val zMin = -boreLength
        val zGap0 = zMin + boreLength - gapLength
        val zGap1 = zMin + boreLength
        val zWindCutter0 = zGap0 - windCutterLength
        val zWindCutter1 = zGap0
        val zAirway0 = zGap1
        var zAirway1 = zAirway0 + airwayLength
        val zMax = zAirway1
        val airwayXSize = 2.0
        val airwayYSize = boreDiam * gapWidth
        val windCutterRounding = boreDiam * 0.1
        val windCutterYSize = airwayYSize + windCutterRounding
        val windCutterLip = boreDiam * 0.02
        val airwayXLow = boreDiam * 0.5 - airwayXSize * 0.5 + windCutterLip * 0.5
        val airwayXHigh = boreDiam * 0.5 + airwayXSize * 0.5 * windCutterLip * 0.5
        val airwayLine0 = Profile.makeProfile(
            listOf(
                listOf(zAirway0, airwayXLow),
                listOf(zAirway1, airwayXLow)
            )
        )
        val airwayLine1 = Profile.makeProfile(
            listOf(
                listOf(zAirway0, airwayXHigh),
                listOf(zAirway1, airwayXHigh)
            )
        )
        val zWindCutterLine = Profile.makeProfile(
            listOf(
                listOf(zWindCutter0, outsideDiam * 0.5),
                listOf(zWindCutter1, boreDiam * 0.5 + windCutterLip)
            )
        )
        val underCutterLine = Profile.makeProfile(
            listOf(
                listOf(zWindCutter0, boreDiam * 0.35),
                listOf(zWindCutter1, boreDiam * 0.5)
            )
        )
        var body = geometry.extrudeShape(circleCrossSection, listOf(
                Profile.makeProfile(
                    listOf(listOf(zMin, outsideDiam), listOf(zMax, outsideDiam))
            )
        ))
        body.label("head-body")
        val boreSpace = geometry.extrudeShape(circleCrossSection,
                listOf(Profile.makeProfile(
                    listOf(
                        listOf(zMin, boreDiam),
                        listOf(zGap1, boreDiam)
                    )
                ))
        )
        boreSpace.label("borespace")

        val windCutterSpace = geometry.extrudeShape(
            profiles = listOf(zWindCutterLine.clipped(zWindCutter0 - 1.0, zGap1)),
            shape = { xList ->
                val x = xList.first()
                geometry.lowerGeometry.roundedRectangle(boreDiam, windCutterYSize,
                    windCutterRounding,
                    Point(x+(boreDiam/2.0), -windCutterYSize/2.0)
                )
            })
        windCutterSpace.label("windCutter space")
        save(body, "before_windcutter_space")
        body = body.difference(windCutterSpace)
        body.label("headbody - windcutter")
        save(body, "after_windcutter space")
        val underCutterSpace = geometry.extrudeShape(
            profiles=listOf(underCutterLine.clipped(zWindCutter0, zGap1)),
            shape = { xs ->
                dAssert(xs.size == 1, "Only expected 1 element for the crosssection of undercutter")
                val x = xs.first()
                geometry.lowerGeometry.rectangle(airwayXSize, airwayYSize, Point(x + airwayXSize/2.0, -airwayYSize*0.5))
            })
        underCutterSpace.label("underCutter space")

        var space = boreSpace.union(underCutterSpace)
        space.label("bore u undercutter")
        val airwaySpace = geometry.extrudeShape(
            profiles = listOf(airwayLine0.clipped(zGap0, zAirway1 + airwayXSize * 2),
            airwayLine1.clipped(zGap0, zAirway1 + airwayXSize * 2)),
            shape = { xs ->
                dAssert(xs.size == 2, "Expected 2 params for airwayspace")
                val x0 = xs[0]
                val x1 = xs[1]
                geometry.lowerGeometry.rectangle(
                    (x1-x0), airwayYSize, Point(x0 + (x1-x0)/2.0, -airwayYSize/2.0))
            })
        airwaySpace.label("airway space")
        body = body.difference(airwaySpace)
        body.label("headbody - airway")
        save(body, "after_airway_space")
        space = space.union(airwaySpace)
        space.label("space u airwaySpace")
        // TODO: check that block preserves the origin correctly.
        val gapSpace = geometry.block(
            ThreeDPoint(0.0, airwayYSize * -0.5, zGap0),
            ThreeDPoint(boreDiam, airwayYSize * 0.5, zGap1)
        )
        gapSpace.label("gapspace")
        body = body.difference(gapSpace)
        body.label("headbody - gapSpace")
        save(body, "after_gap_space")

        space = space.union(gapSpace)
        space.label("space u gapSpace")

        val cutawayDiameter = outsideDiam * 1.5
        var cutawaySpace = geometry.extrudeShape(
            circleCrossSection,
            listOf(Profile.makeProfile(
                    listOf(
                        listOf(-outsideDiam, cutawayDiameter),
                        listOf(outsideDiam , cutawayDiameter)
                    )
                )))
        cutawaySpace.rotate(-90.0, 0.0, 0.0)
        cutawaySpace.translate(-cutawayDiameter, zMax*1.6, -outsideDiam)
        cutawaySpace.label("cutaway space")

        body = body.difference(cutawaySpace)
        body.label("headbody - cutawaySpace")
        save(body, "after_cutaway_space")
        space = space.union(cutawaySpace)
        space.label("space u cutawaySpace")

        var d = airwayXLow * 2
        val jawClipper = geometry.extrudeShape(
            profiles = listOf(Profile.makeProfile(
                listOf(listOf(-outsideDiam * 0.5 - 10, d), listOf(outsideDiam * 0.5 + 10.0, d))
            )),
            shape = { xs ->
                dAssert(xs.size == 1, "Expected 1 parameter extruding jawclipper")
                d = xs[0]
                val width = airwayXLow*1.001 + 0.001
                val height = (zMax - zAirway0) + d
                geometry.lowerGeometry.halfRoundedRectangle(
                    width, height, Point(0.0, (zMax - zAirway0)/2.0))
            })
        jawClipper.rotate(-90.0, 0.0, 0.0)
        jawClipper.label("clipper")
        save(body, "head-body")
        save(space, "head-space")
        save(jawClipper, "head-clipper")
        return listOf(body, space, jawClipper)
    }

    override fun run(): List<Body> {
        var (body, space, jawClipper) = construct()

        body = body.difference(space)
        save(body, "whistle-body")

        val jaw = body.intersect(jawClipper)
        val head = body.difference(jawClipper)
        save(head, "head")
        save(jaw, "jaw")
        return listOf(body, jaw, head)
    }
}
class WhistleMaker<Shape: TwoDShape<Shape>, Body: ThreeDBody<Body>>(
    geometry: ThreeDGeometry<Body, Shape>,
    prefix: String,
    workingDir: Path,
    spec: Whistle,
    override val designer: AbstractWhistleDesigner): InstrumentMaker<Whistle, Shape, Body>(geometry, prefix, workingDir, spec, designer) {

    override fun getCuts(): List<List<Double>> {
        val cuts = super.getCuts()
        return cuts.map { item -> item + listOf(designer.length) }
    }

    override fun run(): List<Body> {
        val headMaker = WhistleHeadMaker(geometry, outputPrefix, workingDir, instrument, designer,
            boreDiam = instrument.inner(instrument.length),
            outsideDiam = instrument.outer(instrument.length))
        val before = System.currentTimeMillis()
        var (whistleHeadOuter, whistleHeadInner, whistleJawClipper) = headMaker.construct()
        whistleHeadInner.translate(0.0, 0.0, designer.length)
        whistleHeadInner.rotate(0.0, 0.0, 90.0)
        whistleHeadOuter.translate(0.0, 0.0, designer.length)
        whistleHeadOuter.rotate(0.0, 0.0, 90.0)
        val afterHead = System.currentTimeMillis()
        reporter.print("Head took ${afterHead - before}ms")
        val inst = makeInstrument(
            innerProfile = instrument.inner.clipped(-50.0, instrument.length),
            outerProfile = instrument.outer.clipped(0.0, instrument.length - headMaker.boreDiam*1.5),
            holePositions = instrument.holePositions,
            holeDiameters = instrument.holeDiameters,
            holeVertAngles = instrument.holeAngles,
            holeHorizAngles = designer.holeHorizAngles,
            withFingerPad = listOf(true).repeat(designer.numberOfHoles),  // TODO
            outsideExtras = listOf(whistleHeadOuter),
            boreExtras = listOf(whistleHeadInner),
            xPad = designer.xPad,
            yPad = designer.yPad
        )
        val afterBody = System.currentTimeMillis()
        val bodyTime = afterBody - afterHead
        reporter.print("Body took $bodyTime ms")
        val parts = makeParts(true)
        return listOf(inst) + parts
    }
}


