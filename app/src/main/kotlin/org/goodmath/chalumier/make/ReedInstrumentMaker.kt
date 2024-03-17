package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.vvecmath.Transform
import org.goodmath.chalumier.cli.InstrumentDescription
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.instruments.ReedInstrument
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path

class ReedInstrumentMaker(prefix: String,
                          outDir: Path,
                          spec: ReedInstrument,
                          desc: InstrumentDescription):
    InstrumentMaker<ReedInstrument>(prefix, outDir, spec, desc) {


    val dock: Boolean by lazy { instrument.getBooleanOption("dock",  false) }
    val dockTop: Double by lazy { instrument.getDoubleOption("dockTop", 8.5) }
    val dockBottom: Double by lazy { instrument.getDoubleOption("dockBottom", 5.5) }
    val dockLength: Double by lazy { instrument.getDoubleOption("dockLength", 15.0) }
    val decorate: Boolean by lazy { instrument.getBooleanOption("decorate", false) }
    val addBauble: Boolean by lazy { instrument.getBooleanOption("decorate", false) }

    override fun run(): List<CSG> {
        val length = spec.length
        var outerProfile = spec.outer
        var innerProfile = spec.inner
        var bauble: CSG? = null
        var endDockLength: Double = 0.0
        if (dock) {
            val dockInner = Profile.makeProfile(
                arrayListOf(
                    arrayListOf(0.0, dockBottom),
                    arrayListOf(dockLength, dockTop)))
            val dockOuter = Profile.makeProfile(
                arrayListOf(
                    arrayListOf(length - 5.0, outerProfile(length)),
                    arrayListOf(length, dockTop + 5.0),
                    arrayListOf(length + dockLength, dockTop + 5.0)))
            innerProfile = innerProfile.appendedWith(dockInner)
            outerProfile = outerProfile.appendedWith(dockOuter)
        }
        val m = outerProfile.maximum()
        if (addBauble) {
            endDockLength = 5.0
            print("Bauble dock: ${m}mm diameter, ${endDockLength}mm length")
            bauble = BaubleMaker(outputPrefix, workingDir,spec, instrument).run().first()
            val fixer = Profile.makeProfile(
                arrayListOf(
                    arrayListOf(0.0, m),
                    arrayListOf(endDockLength, m),
                    arrayListOf(endDockLength* 2, 0.0)))
            outerProfile = outerProfile.maxWith(fixer)
        }
        if (!dock) {
            val dockDiam = spec.bore
            val dockLength = 20.0
            val fixer = Profile.makeProfile(
                arrayListOf(
                    arrayListOf(length - dockLength*1.25, 0.0),
                    arrayListOf(length-dockLength, dockDiam)))
            outerProfile = outerProfile.maxWith(fixer)
        }
        if (decorate) {
            if (!addBauble) {
                outerProfile = decorateProfile(outerProfile, 0.0, 1.0, 0.05)
            }
            outerProfile = decorateProfile(outerProfile, length-dockLength*1.25, -1.0, 0.15)
        }
        val nHoles = instrument.numberOfHoles
        var result = makeInstrument(
            innerProfile=innerProfile.clipped(-50.0, innerProfile.end()+50.0),
            outerProfile=outerProfile,
            holePositions=spec.holePositions,
            holeDiameters=spec.holeDiameters,
            holeVertAngles=spec.holeAngles,
            holeHorizAngles=instrument.holeHorizAngles,
            xPad = listOf(0.0).repeat(nHoles),
            yPad = listOf(0.0).repeat(nHoles),
            withFingerpad = listOf(instrument.getBooleanOption("withFingerpad", false)).repeat(nHoles))
        if (addBauble) {
            bauble = bauble!!.transformed(
                Transform().rotX(180.0)
                    .translate(0.0, 0.0, endDockLength)
            )
            result = result.union(bauble)
        }
        instrumentBody = result
        save(result, "instrument")
        val parts = makeParts(up=true)
        return listOf(result) + parts
    }


        /*


        if self.bauble:
            bauble.rotate(1,0,0, 180)
            bauble.move(0,0,end_dock_length)
            binst = self.working.instrument.copy()
            binst.add(bauble)
            self.save(binst, 'baubled-instrument')

        self.make_parts(up = True)

         */



}
