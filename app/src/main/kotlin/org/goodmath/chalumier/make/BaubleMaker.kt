package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import org.goodmath.chalumier.cli.InstrumentDescription
import org.goodmath.chalumier.design.Angle
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.ReedInstrumentDesigner
import org.goodmath.chalumier.design.instruments.ReedInstrument
import org.goodmath.chalumier.shape.Loop
import org.goodmath.chalumier.shape.extrudeProfile
import org.goodmath.chalumier.util.Point
import java.nio.file.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BaubleMaker(
    outputPrefix: String,
    workingDir: Path,
    spec: ReedInstrument,
    override val designer: ReedInstrumentDesigner<ReedInstrument>):  InstrumentMaker<ReedInstrument>(outputPrefix, workingDir, spec, designer) {


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
            val length = designer.dockDiameter * 1.5
            val inLength = length * 0.9
            val posOuter = arrayListOf(0.0, length)
            val diamOuter = arrayListOf(designer.dockDiameter + 2.0, 0.0)
            val angle: ArrayList<Angle?> = arrayListOf(
                Angle(Angle.AngleDirection.Here, 20.0), Angle(Angle.AngleDirection.Here, -10.0))
            val pOuter = Profile.curvedProfile(
                posOuter, diamOuter, diamOuter, angle, angle
            )
            val posInner = arrayListOf(0.0, inLength)
            val diamInner = arrayListOf(designer.dockDiameter, 0.0)
            val pInner = Profile.curvedProfile(
                posInner, diamInner, diamInner, angle, angle
            )
            val spin = Profile.makeProfile(listOf(listOf(0.0, 0.0), listOf(length, 120.0)))
            val wob = Profile.makeProfile(listOf(listOf(designer.dockLength*0.5 ,0.0), listOf(designer.dockLength,1.0), listOf(length,0.0)))
            var bauble = extrudeProfile(listOf(pOuter, spin, wob), crossSection={ l ->
                val d = l[0]
                val s = l[1]
                val w = l[2]
                wobble(d, w*0.1, s, 12.0)})
            val inside = extrudeProfile(listOf(pInner.clipped(designer.dockLength+1.0, inLength),
                spin.clipped(designer.dockLength + 1.0, inLength),
                wob.clipped(designer.dockLength + 1.0, inLength)),
                crossSection={ l ->
                    val d = l[0]
                    val s = l[1]
                    val w = l[2]
                    wobble(d, w*0.1, s, 12.0)})
            bauble = bauble.difference(inside)

            val dockProfile = Profile.makeProfile(listOf(
                listOf(0.0, designer.dockDiameter), listOf(designer.dockLength, designer.dockDiameter), listOf(designer.dockLength+designer.dockDiameter*0.5, 0.0)))
            val dock = extrudeProfile(listOf(dockProfile))
            bauble = bauble.difference(dock)
            save(bauble, "bauble")
            return listOf(bauble)
        }
}

