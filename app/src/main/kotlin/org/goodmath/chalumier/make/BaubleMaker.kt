package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import org.goodmath.chalumier.design.Angle
import org.goodmath.chalumier.design.AngleDirection
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.shape.Loop
import org.goodmath.chalumier.shape.circleCrossSection
import org.goodmath.chalumier.shape.extrudeProfile
import org.goodmath.chalumier.shape.extrusion
import org.goodmath.chalumier.util.Point
import java.nio.file.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BaubleMaker(
    override val outputPrefix: String,
    override val specFile: Path,
    override val parameterFile: Path,
    override val workingDir: Path,
    val dockDiameter: Double = 40.0,
    val dockLength: Double = 5.0): InstrumentMaker(outputPrefix, specFile, parameterFile, workingDir) {

        fun wobble(diameter: Double = 1.0, wobble: Double = 0.5, spin: Double = 0.0,
                   period: Double = 16.0, n: Int = 256): Loop {
           val radius = diameter * 0.5
          return Loop((0 until n).map { i ->
                val a = (i + 0.5) * PI * 2.0 / n
                val s = spin * PI / 100.0
                val b = cos((a * s) * period)
                val c = cos(((a - s * 0.5) * period)) * 0.75
                val d = 1.0 - (1.0 - b) * (1.0 - c)
                val r2 = radius * (1.0 + wobble * (d - 0.5))
                Point(cos(a) * r2, sin(a) * r2)
            })

        }

        override fun run(): List<CSG> {
            val length = dockDiameter * 1.5
            val inLength = length * 0.9
            val posOuter = arrayListOf(0.0, length)
            val diamOuter = arrayListOf(dockDiameter + 2.0, 0.0)
            val angle: ArrayList<Angle?> = arrayListOf(
                Angle(AngleDirection.Here, 20.0), Angle(AngleDirection.Here, -10.0))
            val pOuter = Profile.curvedProfile(
                posOuter, diamOuter, diamOuter, angle, angle
            )
            val posInner = arrayListOf(0.0, inLength)
            val diamInner = arrayListOf(dockDiameter, 0.0)
            val pInner = Profile.curvedProfile(
                posInner, diamInner, diamInner, angle, angle
            )
            val spin = Profile.makeProfile(listOf(listOf(0.0, 0.0), listOf(length, 120.0)))
            val wob = Profile.makeProfile(listOf(listOf(dockLength*0.5 ,0.0), listOf(dockLength,1.0), listOf(length,0.0)))
            var bauble = extrudeProfile(listOf(pOuter, spin, wob), crossSection={ l ->
                val d = l[0]
                val s = l[1]
                val w = l[2]
                wobble(d, w*0.1, s, 12.0)})
            val inside = extrudeProfile(listOf(pInner.clipped(dockLength+1.0, inLength),
                spin.clipped(dockLength + 1.0, inLength),
                wob.clipped(dockLength + 1.0, inLength)),
                crossSection={ l ->
                    val d = l[0]
                    val s = l[1]
                    val w = l[2]
                    wobble(d, w*0.1, s, 12.0)})
            bauble = bauble.difference(inside)

            val dockProfile = Profile.makeProfile(listOf(
                listOf(0.0, dockDiameter), listOf(dockLength, dockDiameter), listOf(dockLength+dockDiameter*0.5, 0.0)))
            val dock = extrudeProfile(listOf(dockProfile))
            bauble = bauble.difference(dock)
            save(bauble, "bauble")
            return listOf(bauble)
        }
}

