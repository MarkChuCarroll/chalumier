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
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.TaperedFluteDesigner
import org.goodmath.chalumier.design.instruments.TaperedFlute
import org.goodmath.chalumier.geom.ThreeDBody
import org.goodmath.chalumier.geom.ThreeDGeometry
import org.goodmath.chalumier.geom.TwoDShape
import org.goodmath.chalumier.shape.circleCrossSection
import org.goodmath.chalumier.shape.extrudeProfile
import java.nio.file.Path

class CorkMaker<Shape: TwoDShape<Shape>, Body: ThreeDBody<Body>>(
    geometry: ThreeDGeometry<Body, Shape>,
    outputPrefix: String,
    workingDir: Path,
    instrument: TaperedFlute,
    override val designer: TaperedFluteDesigner,
    val length: Double = 10.0,
    val diameter: Double = 10.0,
    val taperIn: Double = 0.25,
    val taperOut: Double = 0.125,
) : InstrumentMaker<TaperedFlute, Shape, Body>(geometry, outputPrefix, workingDir, instrument, designer) {
    override fun run(): List<Body> {
        val d1 = diameter - taperOut
        val d2 = diameter - taperIn
        val cork = geometry.extrudeShape(
            circleCrossSection,
                listOf(Profile.makeProfile(
                    listOf(
                        listOf(0.0, d1),
                        listOf(length, d2),
                    ),
                ),
            ))
        save(cork, "cork")
        return listOf(cork)
    }
}
