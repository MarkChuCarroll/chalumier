package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.shape.extrudeProfile
import java.nio.file.Path

class CorkMaker(
    override val outputPrefix: String,
    override val specFile: Path,
    override val parameterFile: Path,
    override val workingDir: Path,
    val length: Double = 10.0, // 10.0, 0.25, 0.125
    val diameter: Double = 10.0,
    val taperIn: Double = 0.25,
    val taperOut: Double = 0.125,
    ): InstrumentMaker(outputPrefix, specFile, parameterFile, workingDir) {

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
