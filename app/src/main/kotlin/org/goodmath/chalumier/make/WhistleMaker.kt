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

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.vvecmath.Transform
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.chalumier.design.AbstractWhistleDesigner
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.instruments.Whistle
import org.goodmath.chalumier.errors.dAssert
import org.goodmath.chalumier.shape.*
import org.goodmath.chalumier.util.Point
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path
import kotlin.math.PI
import kotlin.math.sqrt

class WhistleHeadMaker(
    prefix: String,
    dir: Path,
    instrument: Whistle,
    override val designer: AbstractWhistleDesigner,
    val boreDiam: Double = 15.0,
    val outsideDiam: Double = 21.0): InstrumentMaker<Whistle>(prefix, dir, instrument, designer) {

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
    fun construct(): List<CSG> {
        reporter.print("Running head constructor")
        Thread.sleep(2000)
        val boreLength = boreDiam * 1.5
        val gapLength = boreLength * this.gapLength
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
        var body = extrudeProfile(
                Profile.makeProfile(
                    listOf(listOf(zMin, outsideDiam), listOf(zMax, outsideDiam))
            )
        )
        val boreSpace = extrudeProfile(
                Profile.makeProfile(
                    listOf(
                        listOf(zMin, boreDiam),
                        listOf(zGap1, boreDiam)
                    )
                )
        )

        val windCutterSpace = extrudeProfile(
            zWindCutterLine.clipped(zWindCutter0 - 1.0, zGap1),
            crossSection = { xList ->
                val x = xList.first()
                roundedRectangle(
                    Point(x, windCutterYSize * -0.5),
                    Point(x + boreDiam, windCutterYSize * 0.5,),
                    windCutterRounding
                )
            })
        body = body.difference(windCutterSpace)
        val underCutterSpace = extrudeProfile(
            underCutterLine.clipped(zWindCutter0, zGap1),
            crossSection = { xs ->
                dAssert(xs.size == 1, "Only expected 1 element for the crosssection of undercutter")
                val x = xs.first()
                rectangle(
                    Point(x - airwayXSize, airwayYSize * -0.5),
                    Point(x, airwayYSize * 0.5)
                )
            })
        var space = boreSpace.difference(underCutterSpace).union(underCutterSpace)
        val airwaySpace = extrudeProfile(
            airwayLine0.clipped(zGap0, zAirway1 + airwayXSize * 2),
            airwayLine1.clipped(zGap0, zAirway1 + airwayXSize * 2),
            crossSection = { xs ->
                dAssert(xs.size == 2, "Expected 2 params for airwayspace")
                val x0 = xs[0]
                val x1 = xs[1]
                rectangle(
                    Point(x0, airwayYSize * -0.5),
                    Point(x1, airwayYSize * 0.5)
                )
            })
        body = body.difference(airwaySpace)
        space = space.union(airwaySpace)
        val gapSpace = block(
            Vector3d.xyz(0.0, airwayYSize * -0.5, zGap0),
            Vector3d.xyz(boreDiam, airwayYSize * 0.5, zGap1)
        )
        body = body.difference(gapSpace)
        space = space.union(gapSpace)

        val cutawayDiameter = outsideDiam * 1.5
        var cutawaySpace = extrudeProfile(
                Profile.makeProfile(
                    listOf(
                        listOf(-outsideDiam * 0.51, cutawayDiameter),
                        listOf(outsideDiam * 0.51, cutawayDiameter)
                    )
                )
        )
        cutawaySpace = cutawaySpace.transformed(
            Transform()
                .rotX(90.0)
                .translate(-cutawayDiameter * 0.5 + boreDiam * 0.5 - (outsideDiam * 0.5 - boreDiam * 0.5), 0.0, zMax)
        )
        body = body.difference(cutawaySpace)
        space = space.union(cutawaySpace)

        var d = airwayXLow * 2
        val jawClipper = extrudeProfile(
            Profile.makeProfile(
                listOf(listOf(-outsideDiam * 0.5 - 10, d), listOf(outsideDiam * 0.5 + 10.0, d))
            ),
            crossSection = { xs ->
                dAssert(xs.size == 1, "Expected 1 parameter extruding jawclipper")
                d = xs[0]
                halfRoundedRectangle(
                    Point(-0.001, zAirway0 - d * 0.5),
                    Point(airwayXLow * 1.001, zMax + d * 0.5)
                )
            }).transformed(Transform().rotX(-90.0))
        return listOf(body, space, jawClipper)
    }

    override fun run(): List<CSG> {
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
class WhistleMaker(
    prefix: String,
    workingDir: Path,
    spec: Whistle,
    override val designer: AbstractWhistleDesigner): InstrumentMaker<Whistle>(prefix, workingDir, spec, designer) {

    override fun getCuts(): List<List<Double>> {
        val cuts = super.getCuts()
        return cuts.map { item -> item + listOf(designer.length) }
    }

    override fun run(): List<CSG> {
        val headMaker = WhistleHeadMaker(outputPrefix, workingDir, instrument, designer,
            boreDiam = instrument.inner(instrument.length),
            outsideDiam = instrument.outer(instrument.length))
        val before = System.currentTimeMillis()
        var (whistleHeadOuter, whistleHeadInner, whistleJawClipper) = headMaker.construct()
        whistleHeadInner = whistleHeadInner.transformed(Transform()
            .translate(0.0, 0.0, designer.length)
            .rotZ(90.0))
        whistleHeadOuter = whistleHeadOuter.transformed(Transform()
            .translate(0.0, 0.0, designer.length).rotZ(90.0))
        val afterHead = System.currentTimeMillis()
        reporter.print("Head took ${afterHead - before}ms")
        val inst = makeInstrument(
            innerProfile = instrument.inner.clipped(-50.0, instrument.length),
            outerProfile = instrument.outer.clipped(0.0, instrument.length - headMaker.boreDiam*1.5),
            holePositions = instrument.holePositions,
            holeDiameters = instrument.holeDiameters,
            holeVertAngles = instrument.holeAngles,
            holeHorizAngles = designer.holeHorizAngles,
            withFingerpad = listOf(true).repeat(designer.numberOfHoles),  // TODO
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


