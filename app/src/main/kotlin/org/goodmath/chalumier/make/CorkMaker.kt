package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.TaperedFluteDesigner
import org.goodmath.chalumier.design.instruments.TaperedFlute
import org.goodmath.chalumier.shape.extrudeProfile
import java.nio.file.Path

class CorkMaker(
    outputPrefix: String,
    workingDir: Path,
    instrument: TaperedFlute,
    override val designer: TaperedFluteDesigner,
    val length: Double = 10.0,
    val diameter: Double = 10.0,
    val taperIn: Double = 0.25,
    val taperOut: Double = 0.125,
): InstrumentMaker<TaperedFlute>(outputPrefix,workingDir, instrument, designer) {

    override fun run(): List<CSG> {
        val d1 = diameter - taperOut
        val d2 = diameter - taperIn
        val cork = extrudeProfile(
            listOf(Profile.makeProfile(
                listOf(listOf(0.0, d1),
                    listOf(length, d2)))))
        save(cork, "cork")
        return listOf(cork)
    }

}
