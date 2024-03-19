package org.goodmath.chalumier.make

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.vvecmath.Transform
import org.goodmath.chalumier.design.Profile
import org.goodmath.chalumier.design.ReedInstrumentDesigner
import org.goodmath.chalumier.design.instruments.ReedInstrument
import org.goodmath.chalumier.util.repeat
import java.nio.file.Path

class ReedInstrumentMaker(prefix: String,
                          outDir: Path,
                          instrument: ReedInstrument,
                          override val designer: ReedInstrumentDesigner<ReedInstrument>):
    InstrumentMaker<ReedInstrument>(prefix, outDir, instrument, designer) {


    override fun run(): List<CSG> {
        val length = instrument.length
        var outerProfile = instrument.outer
        var innerProfile = instrument.inner
        var bauble: CSG? = null
        var endDockLength: Double = 0.0
        if (designer.dock) {
            val dockInner = Profile.makeProfile(
                arrayListOf(
                    arrayListOf(0.0, designer.dockBottom),
                    arrayListOf(designer.dockLength, designer.dockTop)))
            val dockOuter = Profile.makeProfile(
                arrayListOf(
                    arrayListOf(length - 5.0, outerProfile(length)),
                    arrayListOf(length, designer.dockTop + 5.0),
                    arrayListOf(length + designer.dockLength, designer.dockTop + 5.0)))
            innerProfile = innerProfile.appendedWith(dockInner)
            outerProfile = outerProfile.appendedWith(dockOuter)
        }
        val m = outerProfile.maximum()
        if (designer.addBauble) {
            endDockLength = 5.0
            print("Bauble dock: ${m}mm diameter, ${endDockLength}mm length")
            bauble = BaubleMaker(outputPrefix, workingDir,instrument, designer).run().first()
            val fixer = Profile.makeProfile(
                arrayListOf(
                    arrayListOf(0.0, m),
                    arrayListOf(endDockLength, m),
                    arrayListOf(endDockLength* 2, 0.0)))
            outerProfile = outerProfile.maxWith(fixer)
        }
        if (!designer.dock) {
            val dockDiam = instrument.bore
            val dockLength = 20.0
            val fixer = Profile.makeProfile(
                arrayListOf(
                    arrayListOf(length - dockLength*1.25, 0.0),
                    arrayListOf(length-dockLength, dockDiam)))
            outerProfile = outerProfile.maxWith(fixer)
        }
        if (designer.decorate) {
            if (!designer.addBauble) {
                outerProfile = decorateProfile(outerProfile, 0.0, 1.0, 0.05)
            }
            outerProfile = decorateProfile(outerProfile, length-designer.dockLength*1.25, -1.0, 0.15)
        }
        val nHoles = designer.numberOfHoles
        var result = makeInstrument(
            innerProfile=innerProfile.clipped(-50.0, innerProfile.end()+50.0),
            outerProfile=outerProfile,
            holePositions=instrument.holePositions,
            holeDiameters=instrument.holeDiameters,
            holeVertAngles=instrument.holeAngles,
            holeHorizAngles=designer.holeHorizAngles,
            xPad = listOf(0.0).repeat(nHoles),
            yPad = listOf(0.0).repeat(nHoles),
            withFingerpad = listOf(designer.generatePads).repeat(nHoles))
        if (designer.addBauble) {
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
